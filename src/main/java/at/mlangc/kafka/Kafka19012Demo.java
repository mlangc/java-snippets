package at.mlangc.kafka;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.BaseUnits;
import io.micrometer.core.instrument.binder.kafka.KafkaClientMetrics;
import io.micrometer.dynatrace.DynatraceApiVersion;
import io.micrometer.dynatrace.DynatraceConfig;
import io.micrometer.dynatrace.DynatraceMeterRegistry;
import org.apache.commons.lang3.exception.UncheckedInterruptedException;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.config.TopicConfig;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.record.CompressionType;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Kafka19012Demo implements AutoCloseable {
    private static final Logger LOG = LogManager.getLogger(Kafka19012Demo.class);

    static final short REPLICATION_FACTOR = 1;
    static final int NUM_PARTITIONS = 3;
    static final String BOOTSTRAP_SERVERS = "localhost:9092";
    static final List<String> TOPIC_NAMES = List.of("test0", "test1", "test2");
    static final long MAX_OFFSET_LAG = 1_000_000;
    static final int MAX_SENDS_IN_FLIGHT = 500;

    static final Set<TopicPartition> TOPIC_PARTITIONS = TOPIC_NAMES.stream()
            .flatMap(topic -> IntStream.range(0, NUM_PARTITIONS).mapToObj(partition -> new TopicPartition(topic, partition)))
            .collect(Collectors.toUnmodifiableSet());

    public static final Duration CONSUMER_POLL_DURATION = Duration.ofMillis(250);

    private final MeterRegistry meterRegistry = new DynatraceMeterRegistry(
            new DynatraceConfig() {
                @Override
                public DynatraceApiVersion apiVersion() {
                    return DynatraceApiVersion.V2;
                }

                @Override
                public @Nullable String get(String s) {
                    return switch (s) {
                        case "dynatrace.connectTimeout" -> "5s";
                        default -> null;
                    };
                }

                @Override
                public String deviceId() {
                    try {
                        return InetAddress.getLocalHost().getHostName();
                    } catch (UnknownHostException e) {
                        LOG.warn("Error loading hostname", e);
                        return "unknown";
                    }
                }

                @Override
                public String uri() {
                    return "https://" + System.getenv("DYNATRACE_TENANT_ID") + ".dev.dynatracelabs.com/api/v2/metrics/ingest";
                }

                @Override
                public String apiToken() {
                    return System.getenv("DYNATRACE_API_TOKEN");
                }
            }, Clock.SYSTEM);

    private final AdminClient adminClient;
    private final KafkaProducer<String, Integer> producer;
    private final KafkaClientMetrics producerMetrics;
    private final Semaphore sendsInFlightSemaphore = new Semaphore(MAX_SENDS_IN_FLIGHT);

    private final AtomicBoolean keepProducing = new AtomicBoolean();
    private final AtomicBoolean keepConsuming = new AtomicBoolean();

    private final ExecutorService executor = Executors.newThreadPerTaskExecutor(
            Thread.ofVirtual().name(Kafka19012Demo.class.getSimpleName(), 0).factory());


    private record FlowStats(long generation, long nanoTime, long consumerLag, long totalConsumed) {}

    private volatile FlowStats flowStats = new FlowStats(0, System.nanoTime(), 0, 0);
    private final AtomicBoolean updatingFlowStats = new AtomicBoolean();

    private final LongAdder consumedRecords = new LongAdder();

    private final Properties kafkaProps = new Properties();

    private List<CompletableFuture<Long>> asyncProducers;
    private List<CompletableFuture<Long>> asyncConsumers;

    static void main() throws ExecutionException, InterruptedException, TimeoutException {
        try (var demo = new Kafka19012Demo()) {
            demo.prepareTopics();
            demo.startProducing();
            demo.startConsuming();
            demo.waitForExternalStopSignal();
            demo.stopProducing();
            demo.stopConsuming();
        }
    }

    private void waitForExternalStopSignal() throws InterruptedException {
        var latch = new CountDownLatch(1);

        var stopSignalFile = Path.of(System.getProperty("user.home"), getClass() + ".stop.signal").toFile();

        Thread.startVirtualThread(() -> {
            IO.readln("Press enter to stop or touch " + stopSignalFile.getAbsolutePath());
            latch.countDown();
        });

        Thread.startVirtualThread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                if (stopSignalFile.exists()) {
                    LOG.info("Stop signal file detected at {}", stopSignalFile.getAbsolutePath());
                    break;
                }

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            latch.countDown();
        });

        latch.await();
    }

    Kafka19012Demo() {
        kafkaProps.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        kafkaProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        kafkaProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, IntegerSerializer.class.getName());
        kafkaProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        kafkaProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, IntegerDeserializer.class.getName());
        kafkaProps.put(ConsumerConfig.GROUP_ID_CONFIG, getClass().getSimpleName());
        kafkaProps.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, CompressionType.NONE.name);
        /*
        kafkaProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 786432);
        kafkaProps.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 268435456);
        kafkaProps.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 10200);
        kafkaProps.put(ProducerConfig.LINGER_MS_CONFIG, 10);
        kafkaProps.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 15000);
        kafkaProps.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 10);
        kafkaProps.put(ProducerConfig.MAX_REQUEST_SIZE_CONFIG, 52428800);
        kafkaProps.put(ProducerConfig.PARTITIONER_ADPATIVE_PARTITIONING_ENABLE_CONFIG, true);
        kafkaProps.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 5000);
        kafkaProps.put(ProducerConfig.RETRIES_CONFIG, 1);*/

        this.adminClient = AdminClient.create(kafkaProps);
        this.producer = new KafkaProducer<>(kafkaProps);
        this.producerMetrics = new KafkaClientMetrics(producer);
        this.producerMetrics.bindTo(meterRegistry);
    }

    void prepareTopics() throws ExecutionException, InterruptedException, TimeoutException {
        adminClient.listTopics().names().toCompletionStage()
                .thenCompose(topics -> adminClient.deleteTopics(topics).all().toCompletionStage())
                .toCompletableFuture().get(1, TimeUnit.SECONDS);

        var config = Map.of(
                TopicConfig.RETENTION_MS_CONFIG, "" + 60_000,
                TopicConfig.RETENTION_BYTES_CONFIG, "" + 1_000_000_000);

        Set<NewTopic> topicsToCreate = TOPIC_NAMES.stream()
                .map(name -> new NewTopic(name, NUM_PARTITIONS, REPLICATION_FACTOR).configs(config))
                .collect(Collectors.toUnmodifiableSet());

        adminClient.createTopics(topicsToCreate).all().get(1, TimeUnit.SECONDS);

        LOG.info("Topics prepared");
    }

    void startProducing() {
        if (keepProducing.getAndSet(true)) {
            return;
        }

        asyncProducers = IntStream.range(0, 4)
                .mapToObj(ignore -> CompletableFuture.supplyAsync(this::keepProducingRandomly, executor))
                .toList();

        LOG.info("Producers started");
    }

    void stopProducing() {
        if (!keepProducing.getAndSet(false)) {
            return;
        }

        var produced = asyncProducers.stream()
                .mapToLong(f -> f.orTimeout(5, TimeUnit.SECONDS).join())
                .sum();

        asyncProducers = null;
        LOG.info("Producers stopped after producing {} messages", produced);
    }

    void startConsuming() {
        if (keepConsuming.getAndSet(true)) {
            return;
        }

        asyncConsumers = IntStream.range(0, 3)
                .mapToObj(_ -> CompletableFuture.supplyAsync(this::consumerLoop, executor))
                .toList();

        LOG.info("Consumers started");
    }

    void stopConsuming() {
        if (!keepConsuming.getAndSet(false)) {
            return;
        }

        var consumed = asyncConsumers.stream()
                .mapToLong(f -> f.orTimeout(1, TimeUnit.SECONDS).join())
                .sum();

        asyncConsumers = null;
        LOG.info("Consumers stopped after consuming {} messages", consumed);
    }

    private void updateFlowStatsIfNeeded(KafkaConsumer<?, ?> consumer) {
        FlowStats currentStats = flowStats;

        var nanosSinceLastUpdate = System.nanoTime() - currentStats.nanoTime;
        if (TimeUnit.NANOSECONDS.toMillis(nanosSinceLastUpdate) < 100) {
            return;
        }

        if (!updatingFlowStats.compareAndSet(false, true)) {
            return;
        }

        try {
            var committedOffsets = consumer.committed(TOPIC_PARTITIONS);
            var endOffsets = consumer.endOffsets(TOPIC_PARTITIONS);

            var consumerLag = TOPIC_PARTITIONS.stream()
                    .mapToLong(topicPartition -> {
                        var committed = committedOffsets.get(topicPartition);
                        var end = endOffsets.get(topicPartition);

                        if (committed == null || end == null) {
                            return 0;
                        }

                        return end - committed.offset();
                    }).sum();

            var nanoTime = System.nanoTime();
            var nanoDiff = nanoTime - currentStats.nanoTime;
            var totalConsumed = consumedRecords.longValue();
            var consumedDiff = totalConsumed - currentStats.totalConsumed;
            var generation = currentStats.generation + 1;
            flowStats = new FlowStats(generation, nanoTime, consumerLag, totalConsumed);

            if (generation % 50 == 0) {
                LOG.info("Updated consumer lag to {}; consuming at {} rps & {} messages consumed so far", consumerLag, consumedDiff * 1e9 / nanoDiff, totalConsumed);
            }
        } catch (WakeupException _) {
            // OK - means that we are shutting down.
        } catch (Exception e) {
            LOG.error("Error updating consumer lag", e);
        } finally {
            updatingFlowStats.set(false);
        }
    }

    private long consumerLoop() {
        try (var consumer = new KafkaConsumer<String, Integer>(kafkaProps); var consumerMetrics = new KafkaClientMetrics(consumer)) {
            consumerMetrics.bindTo(meterRegistry);

            var counter = 0L;
            consumer.subscribe(TOPIC_NAMES, new ConsumerRebalanceListener() {
                @Override
                public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
                    LOG.info("{} - Partitions revoked: {}", consumer, partitions);
                }

                @Override
                public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
                    LOG.info("{} - Partitions assigned: {}", consumer, partitions);
                }
            });

            var mmCounters = new HashMap<TopicPartition, Counter>();
            try {
                while (keepConsuming.get()) {
                    var consumerRecords = consumer.poll(CONSUMER_POLL_DURATION);

                    var polledRecordsCount = 0L;
                    for (var consumerRecord : consumerRecords) {
                        polledRecordsCount++;

                        var mmCounter = mmCounters.computeIfAbsent(
                                new TopicPartition(consumerRecord.topic(), consumerRecord.partition()),
                                tp -> Counter.builder(getClass().getSimpleName() + ".consumed")
                                        .baseUnit(BaseUnits.MESSAGES)
                                        .tag("topic", tp.topic())
                                        .tag("partition", "" + tp.partition())
                                        .tag("compression", "" + kafkaProps.get(ProducerConfig.COMPRESSION_TYPE_CONFIG))
                                        .tag("max_sends_in_flight", "" + MAX_SENDS_IN_FLIGHT)
                                        .register(meterRegistry));

                        mmCounter.increment();

                        if (!consumerRecord.topic().equals(consumerRecord.key())) {
                            LOG.error("Got record for topic {} on topic {}", consumerRecord.key(), consumerRecord.topic());
                        }

                        if (consumerRecord.partition() != consumerRecord.value()) {
                            LOG.error("Got record for partition {} on partition {}", consumerRecord.value(), consumerRecord.partition());
                        }
                    }

                    consumedRecords.add(polledRecordsCount);
                    counter += polledRecordsCount;
                    updateFlowStatsIfNeeded(consumer);
                }
            } catch (WakeupException _) {
                // Just exit the loop
            }

            return counter;
        }
    }

    private long keepProducingRandomly() {
        var rng = ThreadLocalRandom.current();
        var counter = new LongAdder();
        var mmCounters = new HashMap<TopicPartition, Counter>();

        try {
            producerLoop:
            while (true) {
                var topic = TOPIC_NAMES.get(rng.nextInt(TOPIC_NAMES.size()));
                var partition = rng.nextInt(NUM_PARTITIONS);

                var mmCounter = mmCounters.computeIfAbsent(new TopicPartition(topic, partition),
                        tp -> Counter.builder(getClass().getSimpleName() + ".produced")
                                .tag("topic", tp.topic())
                                .tag("partition", "" + tp.partition())
                                .tag("compression", "" + kafkaProps.get(ProducerConfig.COMPRESSION_TYPE_CONFIG))
                                .tag("max_sends_in_flight", "" + MAX_SENDS_IN_FLIGHT)
                                .baseUnit(BaseUnits.MESSAGES)
                                .register(meterRegistry));

                var producerRecord = new ProducerRecord<>(topic, partition, topic, partition);

                var sleepMillis = 1;
                while (flowStats.consumerLag > MAX_OFFSET_LAG) {
                    Thread.sleep(1);

                    if (!keepProducing.get()) {
                        break producerLoop;
                    }

                    sleepMillis = Math.min(250, 2 * sleepMillis);
                }

                while (!sendsInFlightSemaphore.tryAcquire(250, TimeUnit.MILLISECONDS)) {
                    if (!keepProducing.get()) {
                        break producerLoop;
                    }
                }

                producer.send(producerRecord, (_, e) -> {
                    sendsInFlightSemaphore.release();

                    if (e != null) {
                        LOG.error("Error sending {}", producerRecord, e);
                    } else {
                        mmCounter.increment();
                        counter.increment();
                    }
                });

                if (!keepProducing.get()) {
                    break;
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new UncheckedInterruptedException(e);
        } catch (Exception e) {
            LOG.error("Unexpected exception from the producer", e);
        }

        return counter.longValue();
    }

    @Override
    public void close() throws InterruptedException {
        keepProducing.set(false);
        keepConsuming.set(false);

        producerMetrics.close();
        adminClient.close();
        producer.close();
        executor.shutdown();
        meterRegistry.close();

        if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
            LOG.error("Executor termination timed out");
        }
    }
}
