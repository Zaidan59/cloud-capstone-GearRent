package id.ac.upnvj.gearrent.observability;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import java.time.Duration;

/** Enables Prometheus histogram buckets on http.server.requests for p95 SLO latency queries. */
@ApplicationScoped
public class MetricsConfig {

    @Produces
    @Singleton
    public MeterFilter httpHistogramMeterFilter() {
        return new MeterFilter() {
            @Override
            public DistributionStatisticConfig configure(Meter.Id id, DistributionStatisticConfig config) {
                if ("http.server.requests".equals(id.getName())) {
                    return DistributionStatisticConfig.builder()
                        .percentilesHistogram(true)
                        .serviceLevelObjectives(
                            (double) Duration.ofMillis(50).toNanos(),
                            (double) Duration.ofMillis(100).toNanos(),
                            (double) Duration.ofMillis(250).toNanos(),
                            (double) Duration.ofMillis(500).toNanos(),
                            (double) Duration.ofSeconds(1).toNanos(),
                            (double) Duration.ofSeconds(2).toNanos(),
                            (double) Duration.ofSeconds(5).toNanos()
                        )
                        .build()
                        .merge(config);
                }
                return config;
            }
        };
    }
}
