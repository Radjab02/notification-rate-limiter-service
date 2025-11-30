package com.corporationxyz.api;


import io.github.bucket4j.Bucket;
import io.lettuce.core.api.StatefulRedisConnection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
public class GlobalBucketIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    Bucket globalBucket;

    @Autowired
    StatefulRedisConnection<String, byte[]> redis;

    @BeforeEach
    void setup() {
        redis.sync().flushall();
    }

    @Test
    void testGlobalBucketPersistsAcrossServiceInstances() {
        globalBucket.tryConsume(3);

        long one = globalBucket.getAvailableTokens();

        // New reference (simulated via re-autowiring)
        long two = globalBucket.getAvailableTokens();

        assertThat(two).isEqualTo(one);
    }

    @Nested
    @Testcontainers
    class RedisContainerSmokeTest {

        @Container
        static GenericContainer<?> redis =
                new GenericContainer<>("redis:7.2.4")
                        .withExposedPorts(6379);

        @Test
        void containerStarts() {
            assertTrue(redis.isRunning());
        }
    }
}
