package com.ai.texttosql.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
public class MetricsConfig {

    public static final String QUERY_EXECUTION_TIMER = "query.execution.time";
    public static final String QUERY_CONVERSION_TIMER = "query.conversion.time";
    public static final String QUERY_SUCCESS_COUNTER = "query.success.counter";
    public static final String QUERY_FAILURE_COUNTER = "query.failure.counter";

    @Bean
    public Timer queryExecutionTimer(MeterRegistry registry) {
        return Timer.builder(QUERY_EXECUTION_TIMER)
                .description("Time taken to execute SQL queries")
                .publishPercentiles(0.5, 0.95, 0.99) // median, 95th and 99th percentiles
                .publishPercentileHistogram()
                .minimumExpectedValue(Duration.ofMillis(1))
                .maximumExpectedValue(Duration.ofSeconds(30))
                .register(registry);
    }

    @Bean
    public Timer queryConversionTimer(MeterRegistry registry) {
        return Timer.builder(QUERY_CONVERSION_TIMER)
                .description("Time taken to convert natural language to SQL")
                .publishPercentiles(0.5, 0.95, 0.99)
                .publishPercentileHistogram()
                .minimumExpectedValue(Duration.ofMillis(10))
                .maximumExpectedValue(Duration.ofMinutes(2))
                .register(registry);
    }

    public static void recordQueryMetrics(Timer timer, long startTime, boolean success,
                                         MeterRegistry registry, String... tags) {
        long duration = System.currentTimeMillis() - startTime;
        timer.record(duration, TimeUnit.MILLISECONDS);

        String counterName = success ? QUERY_SUCCESS_COUNTER : QUERY_FAILURE_COUNTER;
        registry.counter(counterName, tags).increment();
    }
}
