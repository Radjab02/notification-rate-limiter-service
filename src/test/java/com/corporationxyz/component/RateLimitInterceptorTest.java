package com.corporationxyz.component;

import com.corporationxyz.component.RateLimitInterceptor;
import com.corporationxyz.service.RateLimitConfigService;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

public class RateLimitInterceptorTest {

    @Mock
    private RateLimitConfigService configService;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    @Mock
    private Bucket globalBucket;

    @Mock
    private Bucket clientBucket;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    private RateLimitInterceptor interceptor;

    private final RedisKeyGenerator keyGen = new RedisKeyGenerator();

    @BeforeEach
    void setup() throws IOException {
        MockitoAnnotations.openMocks(this);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        // Mock getWriter() for response to avoid NPE
        when(response.getWriter()).thenReturn(new java.io.PrintWriter(new java.io.StringWriter()));

        // Inject mocks directly using the new constructor that takes globalBucket
        interceptor = new RateLimitInterceptor(configService, redisTemplate, keyGen, globalBucket);
    }

    @Test
    void shouldAllowRequest_WhenAllLimitsWithinBounds() throws Exception {
        when(request.getHeader("X-Client-ID")).thenReturn("client-x");

        // global bucket allows
        when(globalBucket.tryConsumeAndReturnRemaining(1))
                .thenReturn(ConsumptionProbe.consumed(999,5));

        // monthly limit
        when(configService.getClientMonthlyLimit("client-x")).thenReturn(100L);
        when(valueOps.increment(anyString())).thenReturn(1L);

        // per-window bucket allows
        when(configService.resolveBucket("client-x")).thenReturn(clientBucket);
        when(clientBucket.tryConsumeAndReturnRemaining(1))
                .thenReturn(ConsumptionProbe.consumed(4,1));

        boolean result = interceptor.preHandle(request, response, new Object());
        assertTrue(result);
    }

    @Test
    void shouldRejectRequest_WhenPerWindowLimitExceeded() throws Exception {
        when(request.getHeader("X-Client-ID")).thenReturn("client-x");

        when(globalBucket.tryConsumeAndReturnRemaining(1))
                .thenReturn(ConsumptionProbe.consumed(999,1));

        when(configService.getClientMonthlyLimit("client-x")).thenReturn(100L);
        when(valueOps.increment(anyString())).thenReturn(1L);

        when(configService.resolveBucket("client-x")).thenReturn(clientBucket);
        when(clientBucket.tryConsumeAndReturnRemaining(1))
                .thenReturn(ConsumptionProbe.rejected(0, 2_000_000_000,1));

        boolean result = interceptor.preHandle(request, response, new Object());
        assertFalse(result);
        verify(response).setStatus(429);
    }

    @Test
    void shouldRejectRequest_WhenMonthlyLimitExceeded() throws Exception {
        when(request.getHeader("X-Client-ID")).thenReturn("client-x");

        when(globalBucket.tryConsumeAndReturnRemaining(1))
                .thenReturn(ConsumptionProbe.consumed(999,1));

        when(configService.getClientMonthlyLimit("client-x")).thenReturn(5L);
        when(valueOps.increment(anyString())).thenReturn(6L);

        boolean result = interceptor.preHandle(request, response, new Object());
        assertFalse(result);
        verify(response).setStatus(402); // PAYMENT_REQUIRED
    }

    @Test
    void shouldRejectRequest_WhenGlobalLimitExceeded() throws Exception {
        when(request.getHeader("X-Client-ID")).thenReturn("client-x");

        when(globalBucket.tryConsumeAndReturnRemaining(1))
                .thenReturn(ConsumptionProbe.rejected(0, 1_000_000_000,1));

        boolean result = interceptor.preHandle(request, response, new Object());
        assertFalse(result);
        verify(response).setStatus(429); // TOO_MANY_REQUESTS
    }

    @Test
    void shouldRejectRequest_WhenClientIdMissing() throws Exception {
        when(request.getHeader("X-Client-ID")).thenReturn(null);

        boolean result = interceptor.preHandle(request, response, new Object());
        assertFalse(result);
        verify(response).sendError(400, "Missing X-Client-ID header");
    }


}
