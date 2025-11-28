package at.mlangc.kafka;

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
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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

    static final Set<TopicPartition> TOPIC_PARTITIONS = TOPIC_NAMES.stream()
            .flatMap(topic -> IntStream.range(0, NUM_PARTITIONS).mapToObj(partition -> new TopicPartition(topic, partition)))
            .collect(Collectors.toUnmodifiableSet());

    public static final Duration CONSUMER_POLL_DURATION = Duration.ofMillis(100);

    private final AdminClient adminClient;
    private final KafkaProducer<String, Integer> producer;
    private final List<KafkaConsumer<String, Integer>> consumers;

    private final AtomicBoolean keepProducing = new AtomicBoolean();
    private final AtomicBoolean keepConsuming = new AtomicBoolean();

    private final ExecutorService executor = Executors.newThreadPerTaskExecutor(
            Thread.ofVirtual().name(Kafka19012Demo.class.getSimpleName(), 0).factory());

    private record FlowStats(long generation, long nanoTime, long consumerLag, long totalConsumed) { }
    private volatile FlowStats flowStats = new FlowStats(0, System.nanoTime(), 0, 0);
    private final AtomicBoolean updatingFlowStats = new AtomicBoolean();

    private final LongAdder consumedRecords = new LongAdder();

    private List<CompletableFuture<Long>> asyncProducers;
    private List<CompletableFuture<Long>> asyncConsumers;

    static void main() throws ExecutionException, InterruptedException, TimeoutException {
        try (var demo = new Kafka19012Demo()) {
            demo.prepareTopics();
            demo.startProducing();
            demo.startConsuming();
            IO.readln("Press enter to stop");
            demo.stopProducing();
            demo.stopConsuming();
        }
    }

    Kafka19012Demo() {
        Properties kafkaProps = new Properties();
        kafkaProps.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        kafkaProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        kafkaProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, IntegerSerializer.class.getName());
        kafkaProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        kafkaProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, IntegerDeserializer.class.getName());
        kafkaProps.put(ConsumerConfig.GROUP_ID_CONFIG, getClass().getSimpleName());

        this.adminClient = AdminClient.create(kafkaProps);
        this.producer = new KafkaProducer<>(kafkaProps);
        this.consumers = IntStream.range(0, NUM_PARTITIONS * TOPIC_NAMES.size()).mapToObj(_ -> new KafkaConsumer<String, Integer>(kafkaProps)).toList();
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
    }

    void startProducing() {
        if (keepProducing.getAndSet(true)) {
            return;
        }

        asyncProducers = IntStream.range(0, 4)
                .mapToObj(ignore -> CompletableFuture.supplyAsync(this::keepProducingRandomly, executor))
                .toList();
    }

    void stopProducing() {
        if (!keepProducing.getAndSet(false)) {
            return;
        }

        var produced = asyncProducers.stream()
                .mapToLong(f -> f.orTimeout(1, TimeUnit.SECONDS).join())
                .sum();

        asyncProducers = null;
        LOG.info("Produced {} messages", produced);
    }

    void startConsuming() {
        if (keepConsuming.getAndSet(true)) {
            return;
        }

        asyncConsumers = consumers.stream()
                .map(c -> CompletableFuture.supplyAsync(() -> consumerLoop(c), executor))
                .toList();
    }

    void stopConsuming() {
        if (!keepConsuming.getAndSet(false)) {
            return;
        }

        consumers.forEach(KafkaConsumer::wakeup);

        var consumed = asyncConsumers.stream()
                .mapToLong(f -> f.orTimeout(1, TimeUnit.SECONDS).join())
                .sum();

        asyncConsumers = null;
        LOG.info("Consumed {} messages", consumed);
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

    private long consumerLoop(KafkaConsumer<String, Integer> consumer) {
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

        try {
            while (keepConsuming.get()) {
                var consumerRecords = consumer.poll(CONSUMER_POLL_DURATION);

                var polledRecordsCount = 0L;
                for (var consumerRecord : consumerRecords) {
                    polledRecordsCount++;

                    if (!consumerRecord.topic().equals(consumerRecord.key())) {
                        LOG.error("Get record for topic {} on topic {}", consumerRecord.key(), consumerRecord.topic());
                    }

                    if (consumerRecord.partition() != consumerRecord.value()) {
                        LOG.error("Get record for partition {} on partition {}", consumerRecord.value(), consumerRecord.partition());
                    }
                }

                consumedRecords.add(polledRecordsCount);
                counter += polledRecordsCount;
                updateFlowStatsIfNeeded(consumer);
            }
        } catch (WakeupException _) {
            // Just exit the loop
        } finally {
            consumer.unsubscribe();
        }

        return counter;
    }

    private long keepProducingRandomly() {
        var rng = ThreadLocalRandom.current();
        var semaphore = new Semaphore(32);
        var counter = new LongAdder();

        try {
            while (keepProducing.get()) {
                var topic = TOPIC_NAMES.get(rng.nextInt(TOPIC_NAMES.size()));
                var partition = rng.nextInt(NUM_PARTITIONS);
                var producerRecord = new ProducerRecord<>(topic, partition, topic, partition);

                var sleepMillis = 1;
                while (flowStats.consumerLag > MAX_OFFSET_LAG) {
                    Thread.sleep(1);
                    sleepMillis = Math.min(1000, 2 * sleepMillis);
                }

                semaphore.acquire();
                producer.send(producerRecord, (_, e) -> {
                    semaphore.release();

                    if (e != null) {
                        LOG.error("Error sending {}", producerRecord, e);
                    } else {
                        counter.increment();
                    }
                });
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
        adminClient.close();
        producer.close();
        consumers.forEach(KafkaConsumer::close);
        executor.shutdown();

        if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
            LOG.error("Executor termination timed out");
        }
    }
}
