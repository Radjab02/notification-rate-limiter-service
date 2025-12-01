package com.corporationxyz.component;

import com.corporationxyz.persistence.entity.ClientLimitConfig;
import com.corporationxyz.persistence.repository.ClientLimitRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class RateLimitStartupSync {

    private final ClientLimitRepository clientLimitRepository;
    private final StringRedisTemplate redisTemplate;
    private final RedisKeyGenerator keyGenerator;

    @Autowired
    public RateLimitStartupSync(ClientLimitRepository clientLimitRepository,
                                StringRedisTemplate redisTemplate) {
        this.clientLimitRepository = clientLimitRepository;
        this.redisTemplate = redisTemplate;
        this.keyGenerator = new RedisKeyGenerator();
    }

    @PostConstruct
    public void initializeRateLimits() {
        log.info("Cleaning and synchronizing Redis rate-limit configuration...");

        purgeRateLimitKeys();

        List<ClientLimitConfig> configs = clientLimitRepository.findAll();
        //configs.forEach(this::populateRedisFromDbConfig);

        log.info("Redis synchronized successfully.");
    }

    public void purgeRateLimitKeys() {

        deleteByPattern("rate_limit:*"); //monthly and global keys
        deleteByPattern("client:*:config"); // clients
    }

    private void deleteByPattern(String pattern) {
        ScanOptions opts = ScanOptions.scanOptions().match(pattern).count(500).build();
        try (Cursor<byte[]> c =
                     redisTemplate.getConnectionFactory().getConnection().scan(opts)) {

            while (c.hasNext()) {
                redisTemplate.delete(new String(c.next()));
            }
        }
    }

}
