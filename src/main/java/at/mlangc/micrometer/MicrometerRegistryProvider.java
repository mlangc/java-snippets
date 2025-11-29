package at.mlangc.micrometer;

import io.micrometer.core.instrument.Clock;
import io.micrometer.dynatrace.DynatraceApiVersion;
import io.micrometer.dynatrace.DynatraceConfig;
import io.micrometer.dynatrace.DynatraceMeterRegistry;
import org.jspecify.annotations.Nullable;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Optional;

public class MicrometerRegistryProvider {
    private static final DynatraceConfig DYNATRACE_CONFIG = new DynatraceConfig() {
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
                return "unknown";
            }
        }

        @Override
        public String uri() {
            return "https://" + getEnvOrDie("DYNATRACE_TENANT_ID") + ".dev.dynatracelabs.com/api/v2/metrics/ingest";
        }

        @Override
        public String apiToken() {
            return getEnvOrDie("DYNATRACE_API_TOKEN");
        }
    };

    private static String getEnvOrDie(String name) {
        return Optional.ofNullable(System.getenv(name)).orElseThrow(() -> new IllegalArgumentException("Missing environment variable `" + name + "`"));
    }

    public static DynatraceMeterRegistry newDynatraceMeterRegistry() {
        return new DynatraceMeterRegistry(DYNATRACE_CONFIG, Clock.SYSTEM);
    }
}
