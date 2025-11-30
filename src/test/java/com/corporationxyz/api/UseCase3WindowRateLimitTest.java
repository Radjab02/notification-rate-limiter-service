package com.corporationxyz.api;


import com.corporationxyz.persistence.entity.ClientLimitConfig;
import com.corporationxyz.persistence.repository.ClientLimitRepository;
import io.lettuce.core.api.StatefulRedisConnection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class UseCase3WindowRateLimitTest extends AbstractIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ClientLimitRepository repo;

    @Autowired
    StatefulRedisConnection<String, byte[]> redis;

    @BeforeEach
    void setup() {
        redis.sync().flushall();
        repo.save(new ClientLimitConfig("client-u3", 1000L, 3, 60)); // 3 requests per window
    }

    @Test
    void systemEnforcesWindowLimit() throws Exception {

        // First 3 allowed
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(get("/api/notifications/send")
                            .header("X-Client-ID", "client-u3")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }

        // 4th must be blocked
        mockMvc.perform(get("/api/notifications/send")
                        .header("X-Client-ID", "client-u3"))
                .andExpect(status().isTooManyRequests());
    }
}
