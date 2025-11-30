package com.corporationxyz.api;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;

@SpringBootTest
public abstract class AbstractIntegrationTest {

    static final GenericContainer<?> redis =
            new GenericContainer<>("redis:7.0")
                    .withExposedPorts(6379);

    static {
        redis.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));

        // Use in-memory DB, no overrides required unless using PostgreSQL Testcontainer
    }

}
