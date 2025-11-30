package com.corporationxyz.api;


import com.corporationxyz.persistence.repository.ClientLimitRepository;
import io.lettuce.core.api.StatefulRedisConnection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class UseCase2DefineMonthlyLimitTest extends AbstractIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ClientLimitRepository repo;

    @Autowired
    StatefulRedisConnection<String, byte[]> redis;

    @BeforeEach
    void setup() {
        redis.sync().flushall();
    }

    @Test
    void companyCanDefineMonthlyLimitForClient() throws Exception {

        String body = """
                    {
                      "clientId": "client-u2",
                      "monthlyLimit": 300,
                      "windowCapacity": 10,
                      "windowDurationSeconds": 30
                    }
                """;

        mockMvc.perform(post("/admin/limits")
                        .content(body)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated());

        var config = repo.findByClientId("client-u2").orElseThrow();
        assertThat(config.getMonthlyLimit()).isEqualTo(300L);
    }
}
