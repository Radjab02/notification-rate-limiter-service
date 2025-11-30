package com.corporationxyz.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.function.Supplier;

@Configuration
public class RedisConfig {


    @Value("${notification-rate-limiter-service.global-bucket.key:rate_limit:global_system}")
    private String globalBucketKey;

    @Value("${notification-rate-limiter-service.global-bucket.capacity:1000}")
    private int globalBucketCapacity;

    @Value("${notification-rate-limiter-service.global-bucket.duration-in-minutes:1}")
    private int refillDurationInMinutes;

    @Bean
    public RedisClient redisClient() {
        return RedisClient.create("redis://localhost:6379/0");
    }

    @Bean
    public StatefulRedisConnection<String, byte[]> statefulRedisConnection(RedisClient redisClient) {
        return redisClient.connect(RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE));
    }

    @Bean
    public LettuceBasedProxyManager<String> lettuceBasedProxyManager(StatefulRedisConnection<String, byte[]> connection) {
        return LettuceBasedProxyManager.builderFor(connection).build();
    }


    @Bean
    public Bucket globalBucket(LettuceBasedProxyManager<String> proxyManager) {
        Supplier<BucketConfiguration> configSupplier = () -> BucketConfiguration.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(globalBucketCapacity)
                        .refillIntervally(globalBucketCapacity,
                                Duration.ofMinutes(refillDurationInMinutes))
                        .build())
                .build();

        return proxyManager.builder().build(globalBucketKey, configSupplier);
    }

}
