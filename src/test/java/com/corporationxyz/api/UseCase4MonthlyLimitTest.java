package com.corporationxyz.api;


import com.corporationxyz.persistence.entity.ClientLimitConfig;
import com.corporationxyz.persistence.repository.ClientLimitRepository;
import io.lettuce.core.api.StatefulRedisConnection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class UseCase4MonthlyLimitTest extends AbstractIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ClientLimitRepository repo;

    @Autowired
    StatefulRedisConnection<String, byte[]> redis;

    @BeforeEach
    void setup() {
        redis.sync().flushall();
        repo.save(new ClientLimitConfig(
                "client-u4", 2L, 10, 60   // monthly limit = 2
        ));
    }

    @Test
    void systemEnforcesMonthlyLimit() throws Exception {

        // 2 allowed
        mockMvc.perform(get("/api/notifications/send")
                        .header("X-Client-ID", "client-u4"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/notifications/send")
                        .header("X-Client-ID", "client-u4"))
                .andExpect(status().isOk());

        // 3rd rejected (monthly limit exceeded)
        mockMvc.perform(get("/api/notifications/send")
                        .header("X-Client-ID", "client-u4"))
                .andExpect(status().isPaymentRequired());
    }
}
