package com.corporationxyz.service;

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

    private RateLimitConfigService rateLimitConfigService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        rateLimitConfigService = new RateLimitConfigService(clientLimitRepository, proxyManager);
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

        ClientLimitConfig fallbackConfig = new ClientLimitConfig(
                "client-z",
                1000L,
                5,
                60
        );

        // Mock proxy manager chain
        RemoteBucketBuilder<String> remoteBucketBuilder = mock(RemoteBucketBuilder.class);
        BucketProxy mockBucket = mock(BucketProxy.class);

        when(proxyManager.builder()).thenReturn(remoteBucketBuilder);
        when(remoteBucketBuilder.build(eq("client-x"), any(Supplier.class)))
                .thenReturn(mockBucket);
        when(clientLimitRepository.findByClientId("client-x")).thenReturn(java.util.Optional.of(config));
        when(clientLimitRepository.findByClientId("client-z")).thenReturn(Optional.of(fallbackConfig));

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
}
