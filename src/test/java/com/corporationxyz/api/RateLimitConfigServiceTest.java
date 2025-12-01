package com.corporationxyz.api;

import com.corporationxyz.service.RateLimitConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class RateLimitConfigServiceTest extends AbstractIntegrationTest {

    @Autowired
    private RateLimitConfigService rateLimitConfigService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @BeforeEach
    void setup() {
        redisTemplate.getConnectionFactory().getConnection().flushDb();
    }

    @Test
    void deleteByPattern_shouldDeleteAllMatchingKeys() {
        // Arrange
        redisTemplate.opsForValue().set("rate_limit:test:1", "A");
        redisTemplate.opsForValue().set("rate_limit:test:2", "B");
        redisTemplate.opsForValue().set("rate_limit:other", "C");

        // Sanity check
        assertThat(redisTemplate.keys("rate_limit:test:*")).hasSize(2);

        // Act
        rateLimitConfigService.deleteByPattern("rate_limit:test:*");

        // Assert
        assertThat(redisTemplate.keys("rate_limit:test:*")).isEmpty();
        assertThat(redisTemplate.keys("rate_limit:*")).containsExactly("rate_limit:other");
    }
}