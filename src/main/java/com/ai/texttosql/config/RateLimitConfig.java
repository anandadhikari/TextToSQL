package com.ai.texttosql.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
@EnableCaching
public class RateLimitConfig {

    private static final int DEFAULT_TOKENS = 100;
    private static final int REFILL_TOKENS = 100;
    private static final int REFILL_DURATION_MINUTES = 1;

    public enum PricingPlan {
        FREE(100),
        BASIC(500),
        PREMIUM(2000);

        private final int bucketCapacity;

        PricingPlan(int bucketCapacity) {
            this.bucketCapacity = bucketCapacity;
        }

        public int getBucketCapacity() {
            return bucketCapacity;
        }

        public static PricingPlan resolvePlanFromApiKey(String apiKey) {
            if (apiKey == null || apiKey.isEmpty()) return FREE;
            if (apiKey.startsWith("BASIC_")) return BASIC;
            if (apiKey.startsWith("PREMIUM_")) return PREMIUM;
            return FREE;
        }
    }

    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    // ✅ No longer a @Bean — runtime method
    public Bucket resolveBucket(String apiKey) {
        PricingPlan pricingPlan = PricingPlan.resolvePlanFromApiKey(apiKey);
        return cache.computeIfAbsent(apiKey, key -> {
            Refill refill = Refill.intervally(pricingPlan.getBucketCapacity(), Duration.ofMinutes(REFILL_DURATION_MINUTES));
            Bandwidth limit = Bandwidth.classic(pricingPlan.getBucketCapacity(), refill);
            return Bucket4j.builder().addLimit(limit).build();
        });
    }

    @Bean
    public Bucket defaultBucket() {
        Refill refill = Refill.intervally(REFILL_TOKENS, Duration.ofMinutes(REFILL_DURATION_MINUTES));
        Bandwidth limit = Bandwidth.classic(DEFAULT_TOKENS, refill);
        return Bucket4j.builder().addLimit(limit).build();
    }
}
