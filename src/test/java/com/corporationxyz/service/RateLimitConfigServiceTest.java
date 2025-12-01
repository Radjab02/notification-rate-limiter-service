package com.corporationxyz.service;

import com.corporationxyz.component.RedisKeyGenerator;
import com.corporationxyz.persistence.entity.ClientLimitConfig;
import com.corporationxyz.persistence.repository.ClientLimitRepository;
import com.corporationxyz.service.RateLimitConfigService;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.distributed.proxy.RemoteBucketBuilder;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

class RateLimitConfigServiceTest {

    @Mock
    private ClientLimitRepository clientLimitRepository;

    @Mock
    private LettuceBasedProxyManager<String> proxyManager;

    @Mock
    private StringRedisTemplate redisTemplate;

    private RateLimitConfigService rateLimitConfigService;
    @Mock
    private RedisKeyGenerator keyGenerator;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        rateLimitConfigService = new RateLimitConfigService(clientLimitRepository, proxyManager, redisTemplate, keyGenerator);
    }

    @Test
    void testResolveBucket_shouldReturnBucketWithCorrectConfiguration() {

        // Arrange: client configuration
        ClientLimitConfig config = new ClientLimitConfig(
                "client-x",
                5000L,
                100,
                60
        );

        // Mock key generator so the keys match
        when(keyGenerator.windowBucketKey("client-x"))
                .thenReturn("client-x");

        // Mock repository
        when(clientLimitRepository.findByClientId("client-x"))
                .thenReturn(Optional.of(config));

        // Mock proxy manager chain
        RemoteBucketBuilder<String> remoteBucketBuilder = mock(RemoteBucketBuilder.class);
        BucketProxy mockBucket = mock(BucketProxy.class);

        when(proxyManager.builder()).thenReturn(remoteBucketBuilder);
        when(remoteBucketBuilder.build(eq("client-x"), any(Supplier.class)))
                .thenReturn(mockBucket);

        // Act
        Bucket result = rateLimitConfigService.resolveBucket("client-x");

        // Assert
        assertNotNull(result);
        assertEquals(mockBucket, result);
    }


    @Test
    void testGetClientMonthlyLimit_shouldReturnCorrectLimit() {
        // Simulate client configuration
        ClientLimitConfig config = new ClientLimitConfig("client-x", 5000L, 100, 60);
        ClientLimitConfig defaultConfig = new ClientLimitConfig("client-z", 1000L, 5, 60);
        when(clientLimitRepository.findByClientId("client-x")).thenReturn(java.util.Optional.of(config));
        when(clientLimitRepository.findByClientId("client-z")).thenReturn(Optional.of(defaultConfig));
        long monthlyLimit = rateLimitConfigService.getClientMonthlyLimit("client-x");

        assertEquals(5000L, monthlyLimit);
    }

    @Test
    void clientConfigRecord_shouldStoreValuesCorrectly() {

        RateLimitConfigService.ClientConfig config =
                new RateLimitConfigService.ClientConfig(
                        10,
                        Duration.ofSeconds(30),
                        500L
                );

        assertEquals(10, config.capacity());
        assertEquals(Duration.ofSeconds(30), config.refillDuration());
        assertEquals(500L, config.monthlyLimit());
    }
}
