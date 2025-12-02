package com.corporationxyz.service;

import com.corporationxyz.component.RedisKeyGenerator;
import com.corporationxyz.persistence.entity.ClientLimitConfig;
import com.corporationxyz.persistence.repository.ClientLimitRepository;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Service
public class RateLimitConfigService {
    private final ClientLimitRepository clientLimitRepository;
    private final LettuceBasedProxyManager<String> proxyManager;
    private final StringRedisTemplate redisTemplate;
    private final RedisKeyGenerator keyGenerator;

    private final ConcurrentHashMap<String, Bucket> bucketCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ClientLimitConfig> configCache = new ConcurrentHashMap<>();

    @Autowired
    public RateLimitConfigService(ClientLimitRepository clientLimitRepository,
                                  LettuceBasedProxyManager<String> proxyManager,
                                  StringRedisTemplate redisTemplate,
                                  RedisKeyGenerator keyGenerator) {
        this.clientLimitRepository = clientLimitRepository;
        this.proxyManager = proxyManager;
        this.redisTemplate = redisTemplate;
        this.keyGenerator = keyGenerator;
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

        String redisKey = generateWindowLimitBucketKey(clientId);

        return bucketCache.computeIfAbsent(redisKey, key -> {

            ClientLimitConfig config = getClientConfiguration(clientId);

            Supplier<BucketConfiguration> bucketConf = () -> BucketConfiguration.builder()
                    .addLimit(Bandwidth.builder()
                            .capacity(config.getWindowCapacity())
                            .refillIntervally(
                                    config.getWindowCapacity(),
                                    Duration.ofSeconds(config.getWindowDurationSeconds()))
                            .build())
                    .build();

            return proxyManager.builder().build(redisKey, bucketConf);
        });
    }

    public void reloadBucketAndClientConfig(String clientId) {
        configCache.remove(clientId);
        bucketCache.remove(generateWindowLimitBucketKey((clientId)));

        resetMonthlyCounter(clientId);
        resetWindowCounter(clientId);
    }

    public void resetMonthlyCounter(String clientId) {
        String pattern = "rate_limit:" + clientId + ":month:*";
        deleteByPattern(pattern);
        configCache.remove(clientId);
        bucketCache.remove(generateWindowLimitBucketKey((clientId)));
    }

    public void resetWindowCounter(String clientId) {
        resetBucket(clientId);
        //String clientPattern = "*:" + clientId + ":*";
        String clientPattern = "rate_limit:" + clientId + ":window";
        deleteByPattern(clientPattern);

        String bucketKey = generateWindowLimitBucketKey(clientId);
        deleteByPattern(bucketKey + "*");
        configCache.remove(clientId);
        bucketCache.remove(generateWindowLimitBucketKey((clientId)));
    }

    record ClientConfig(int capacity, Duration refillDuration, long monthlyLimit) {
    }

    private String generateWindowLimitBucketKey(String clientId) {
        return keyGenerator.windowBucketKey(clientId);
    }

    private void resetBucket(String clientId) {
        try {
            proxyManager.removeProxy(clientId);
            proxyManager.removeProxy(generateWindowLimitBucketKey(clientId));
        } catch (Exception ignored) {
        }

    }

    public void deleteByPattern(String pattern) {

        ScanOptions options = ScanOptions.scanOptions()
                .match(pattern)
                .count(500)
                .build();

        RedisConnection connection = redisTemplate.getConnectionFactory().getConnection();

        try (Cursor<byte[]> cursor = connection.scan(options)) {
            while (cursor.hasNext()) {
                String key = new String(cursor.next(), StandardCharsets.UTF_8);
                redisTemplate.delete(key);
            }
        }
    }
}

