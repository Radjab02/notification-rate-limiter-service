package com.corporationxyz.api;


import com.corporationxyz.persistence.entity.ClientLimitConfig;
import com.corporationxyz.persistence.repository.ClientLimitRepository;
import com.corporationxyz.service.RateLimitConfigService;
import io.github.bucket4j.Bucket;
import io.lettuce.core.api.StatefulRedisConnection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class RateLimitConfigServiceRedisBucketTest extends AbstractIntegrationTest {

    @Autowired
    RateLimitConfigService service;

    @Autowired
    ClientLimitRepository repo;


    @Autowired
    StatefulRedisConnection<String, byte[]> redis;


    @BeforeEach
    void setup() {
        redis.sync().flushall();
    }

    @Test
    void testRedisBackedBucketConsumesTokens() {
        repo.save(new ClientLimitConfig("client-bucket", 1000L, 5, 60));

        Bucket bucket = service.resolveBucket("client-bucket");

        assertThat(bucket.tryConsume(1)).isTrue();
        assertThat(bucket.tryConsume(1)).isTrue();

        // Should have 3 tokens left
        assertThat(bucket.getAvailableTokens()).isEqualTo(3);
    }

    @Test
    void testRedisBucketPersistsAcrossInstances() {
        repo.save(new ClientLimitConfig("client-persist", 1000L, 5, 60));

        Bucket one = service.resolveBucket("client-persist");
        one.tryConsume(2);

        Bucket two = service.resolveBucket("client-persist");

        // Verify remaining tokens shared in Redis
        assertThat(two.getAvailableTokens()).isEqualTo(3);
    }

}
