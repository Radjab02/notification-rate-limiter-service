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
public class UseCase1DefineWindowLimitTest extends AbstractIntegrationTest {

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
    void companyCanDefineWindowLimitForClient() throws Exception {

        String body = """
                {
                  "clientId": "client-u1",
                  "monthlyLimit": 500,
                  "windowCapacity": 8,
                  "windowDurationSeconds": 60
                }
                """;

        mockMvc.perform(post("/admin/limits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        var config = repo.findByClientId("client-u1").orElseThrow();

        assertThat(config.getWindowCapacity()).isEqualTo(8);
        assertThat(config.getWindowDurationSeconds()).isEqualTo(60);
    }
}
