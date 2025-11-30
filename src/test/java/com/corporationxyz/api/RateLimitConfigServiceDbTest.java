package com.corporationxyz.api;


import com.corporationxyz.persistence.entity.ClientLimitConfig;
import com.corporationxyz.persistence.repository.ClientLimitRepository;
import com.corporationxyz.service.RateLimitConfigService;
import io.lettuce.core.api.StatefulRedisConnection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class RateLimitConfigServiceDbTest extends AbstractIntegrationTest {

    @Autowired
    ClientLimitRepository repo;

    @Autowired
    RateLimitConfigService service;

    @Autowired
    StatefulRedisConnection<String, byte[]> redis;

    @BeforeEach
    void setup() {
        redis.sync().flushall();
    }


    @Test
    void testLoadsConfigFromDatabase() {
        repo.save(new ClientLimitConfig("client-test", 100L, 5, 60));

        ClientLimitConfig config = service.getClientConfiguration("client-test");

        assertThat(config.getMonthlyLimit()).isEqualTo(100L);
        assertThat(config.getWindowCapacity()).isEqualTo(5);
        assertThat(config.getWindowDurationSeconds()).isEqualTo(60);
    }

    @Test
    void testConfigIsCachedAfterFirstLookup() {
        repo.save(new ClientLimitConfig("client-cache", 200L, 10, 120));

        ClientLimitConfig first = service.getClientConfiguration("client-cache");
        ClientLimitConfig second = service.getClientConfiguration("client-cache");

        // Same instance == cached
        assertThat(first).isSameAs(second);
    }

    @Test
    void testReloadCacheClearsBothCaches() {
        repo.save(new ClientLimitConfig("client-reload", 300L, 10, 60));

        ClientLimitConfig before = service.getClientConfiguration("client-reload");

        service.reloadBucketAndClientConfig("client-reload");

        ClientLimitConfig after = service.getClientConfiguration("client-reload");

        assertThat(before).isNotSameAs(after);
    }
}
