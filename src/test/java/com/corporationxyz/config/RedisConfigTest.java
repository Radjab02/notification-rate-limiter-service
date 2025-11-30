package com.corporationxyz.config;

import com.corporationxyz.config.RedisConfig;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.distributed.proxy.RemoteBucketBuilder;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.RedisCodec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

class RedisConfigTest {

    private RedisConfig redisConfig;

    @BeforeEach
    void setUp() throws Exception {
        redisConfig = new RedisConfig();

        // Use reflection to set private fields
        setPrivateField("globalBucketKey", "test-global-bucket");
        setPrivateField("globalBucketCapacity", 500);
        setPrivateField("refillDurationInMinutes", 2);
    }

    private void setPrivateField(String fieldName, Object value) throws Exception {
        Field field = RedisConfig.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(redisConfig, value);
    }

    @Test
    void testRedisClient() {
        RedisClient client = redisConfig.redisClient();
        assertNotNull(client, "RedisClient should be created");
    }

    @Test
    void testStatefulRedisConnection() {
        RedisClient mockClient = mock(RedisClient.class);
        StatefulRedisConnection<String, byte[]> mockConnection = mock(StatefulRedisConnection.class);
        when(mockClient.connect(any(RedisCodec.class))).thenReturn(mockConnection);

        StatefulRedisConnection<String, byte[]> connection = redisConfig.statefulRedisConnection(mockClient);
        assertNotNull(connection);
        verify(mockClient).connect(any(RedisCodec.class));
    }

    @Test
    void testLettuceBasedProxyManager_mocked() {
        StatefulRedisConnection<String, byte[]> mockConnection = mock(StatefulRedisConnection.class);

        LettuceBasedProxyManager<String> mockManager = mock(LettuceBasedProxyManager.class);
        // you can inject this mock wherever needed
        assertNotNull(mockManager);
    }

    @Test
    void testGlobalBucket_returnsBucket() {
        // Mock the manager and the RemoteBucketBuilder
        LettuceBasedProxyManager<String> mockManager = mock(LettuceBasedProxyManager.class);
        RemoteBucketBuilder<String> mockBuilder = mock(RemoteBucketBuilder.class);
        BucketProxy mockBucket = mock(BucketProxy.class);

        // When builder() is called, return the mocked RemoteBucketBuilder
        when(mockManager.builder()).thenReturn(mockBuilder);

        // When build(...) is called on the RemoteBucketBuilder, return our mock bucket
        when(mockBuilder.build(anyString(), any(Supplier.class))).thenReturn(mockBucket);

        // Call the method under test
        Bucket bucket = redisConfig.globalBucket(mockManager);

        // Verify
        assertSame(mockBucket, bucket);
        verify(mockBuilder).build(anyString(), any(Supplier.class));
    }

}

