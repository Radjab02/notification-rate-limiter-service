package com.corporationxyz.api;


import com.corporationxyz.persistence.entity.ClientLimitConfig;
import com.corporationxyz.persistence.repository.ClientLimitRepository;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.TokensInheritanceStrategy;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.api.StatefulRedisConnection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class UseCase5GlobalLimitTest extends AbstractIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ClientLimitRepository repo;

    @Autowired
    StatefulRedisConnection<String, byte[]> redis;


    @BeforeEach
    void setup() {
        redis.sync().flushall();
        repo.save(new ClientLimitConfig("client-u5", 1001L, 1001, 300));

    }

    @Test
    void systemEnforcesGlobalLimit() throws Exception {

        // Consume global bucket until it blocks
        for (int i = 0; i < 1000; i++) {
           // System.out.println("i:" + i);
            mockMvc.perform(get("/api/notifications/send")
                            .header("X-Client-ID", "client-u5"))
                    .andExpect(status().isOk());
        }

        // One more = global overflow
        mockMvc.perform(get("/api/notifications/send")
                        .header("X-Client-ID", "client-u5"))
                .andExpect(status().isTooManyRequests());
    }
}
