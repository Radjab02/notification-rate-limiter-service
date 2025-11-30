package com.corporationxyz.service;


import com.corporationxyz.persistence.entity.ClientLimitConfig;
import com.corporationxyz.persistence.repository.ClientLimitRepository;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;


@Service
public class RateLimitConfigService {
    private final ClientLimitRepository clientLimitRepository;
    private final LettuceBasedProxyManager<String> proxyManager;

    // Cache for bucket proxies
    private final ConcurrentHashMap<String, Bucket> bucketCache = new ConcurrentHashMap<>();

    // Cache for client limit configs
    private final ConcurrentHashMap<String, ClientLimitConfig> configCache = new ConcurrentHashMap<>();

    @Autowired
    public RateLimitConfigService(ClientLimitRepository clientLimitRepository, LettuceBasedProxyManager<String> proxyManager) {
        this.clientLimitRepository = clientLimitRepository;
        this.proxyManager = proxyManager;
    }

    public ClientLimitConfig getClientConfiguration(String clientId) {
        return configCache.computeIfAbsent(clientId, id ->
                clientLimitRepository.findByClientId(id)
                        .orElseThrow(() -> new RuntimeException("Client config not found: " + id))
        );
    }

    public long getClientMonthlyLimit(String clientId) {
        ClientLimitConfig config = getClientConfiguration(clientId);
        return config.getMonthlyLimit();
    }

    public Bucket resolveBucket(String clientId) {

        return bucketCache.computeIfAbsent(clientId, id -> {

            ClientLimitConfig config = getClientConfiguration(id);

            Supplier<BucketConfiguration> bucketConf = () -> BucketConfiguration.builder()
                    .addLimit(Bandwidth.builder()
                            .capacity(config.getWindowCapacity())
                            .refillIntervally(
                                    config.getWindowCapacity(),
                                    Duration.ofSeconds(config.getWindowDurationSeconds()))
                            .build())
                    .build();

            return proxyManager.builder().build(id, bucketConf);
        });
    }

    public void reloadBucketAndClientConfig(String clientId) {
        configCache.remove(clientId);
        bucketCache.remove(clientId);
    }

    private record ClientConfig(int capacity, Duration refillDuration, long monthlyLimit) {
    }
}


