package com.corporationxyz.component;


import com.corporationxyz.service.RateLimitConfigService;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimitConfigService rateLimitConfigService;
    private final StringRedisTemplate redisTemplate;
    private final Bucket GLOBAL_BUCKET;
    private final RedisKeyGenerator keyGen;
    private static final String ALLOWED_ORIGIN = "http://localhost:4200";
    private static final String EXPOSE_HEADERS = "*";
    private static final String HTTP_METHOD_OPTIONS = "OPTIONS";
    private static final String HEADER_CLIENT_ID = "X-Client-ID";
    private static final String HEADER_RATE_LIMIT_REMAINING = "X-Rate-Limit-Remaining";
    private static final String HEADER_RETRY_AFTER_SECONDS = "X-Rate-Limit-Retry-After-Seconds";

    private static final String HEADER_ALLOW_ORIGIN_KEY = "Access-Control-Allow-Origin";
    private static final String HEADER_ALLOW_CREDENTIALS_KEY = "Access-Control-Allow-Credentials";
    private static final String HEADER_ALLOW_CREDENTIALS_VALUE = "true";
    private static final String HEADER_EXPOSE_HEADERS_KEY = "Access-Control-Expose-Headers";

    private static final String ERROR_MISSING_CLIENT_ID = "Missing X-Client-ID header";
    private static final String ERROR_GLOBAL_LIMIT_EXCEEDED = "System capacity exceeded. Try again shortly.";
    private static final String ERROR_PER_WINDOW_EXCEEDED = "You have exhausted your request quota within the time window.";

    private static final String ERROR_MONTHLY_QUOTA_START = "Monthly request quota exceeded. Upgrade your plan (Limit: ";
    private static final String ERROR_MONTHLY_QUOTA_END = ").";


    @Autowired
    public RateLimitInterceptor(
            RateLimitConfigService rateLimitConfigService,
            StringRedisTemplate redisTemplate,
            RedisKeyGenerator keyGen,
            @Qualifier("globalBucket") Bucket globalBucket) {

        this.rateLimitConfigService = rateLimitConfigService;
        this.redisTemplate = redisTemplate;
        this.keyGen = keyGen;
        this.GLOBAL_BUCKET = globalBucket;
    }

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                             @NonNull Object handler) throws Exception {

        // Quick fix for Angular to read remaining calls
        applyCorsHeaders(response);

        if (HTTP_METHOD_OPTIONS.equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);
            return true; // skip rate limiting for preflight
        }

        String clientId = request.getHeader(HEADER_CLIENT_ID);
        if (clientId == null || clientId.isEmpty()) {
            response.sendError(HttpStatus.BAD_REQUEST.value(), ERROR_MISSING_CLIENT_ID);
            return false;
        }

        // 1. Global system-level limit (now Redis-backed)
        if (!checkGlobalLimit(response)) return false;

        // 2. Client monthly limit
        if (!checkMonthlyLimit(clientId, response)) return false;

        // 3. Client per-window token bucket limit
        return checkPerWindowLimit(clientId, response);
    }

    private boolean checkGlobalLimit(HttpServletResponse response) throws Exception {
        ConsumptionProbe probe = GLOBAL_BUCKET.tryConsumeAndReturnRemaining(1);
        log.info("Global Bucket Consumed: {}", probe.getRemainingTokens());
        if (!probe.isConsumed()) {
            sendError(response, HttpStatus.TOO_MANY_REQUESTS,
                    ERROR_GLOBAL_LIMIT_EXCEEDED);
            return false;
        }
        return true;
    }

    private boolean checkMonthlyLimit(String clientId, HttpServletResponse response) throws Exception {
        long limit = rateLimitConfigService.getClientMonthlyLimit(clientId);
        YearMonth ym = YearMonth.now();
        String key = keyGen.monthBucketKey(clientId, ym);
        Long currentCount = redisTemplate.opsForValue().increment(key); //atomic

        if (currentCount == null) currentCount = 1L;

        if (currentCount == 1) {
            ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
            boolean isLeapYear = Year.isLeap(now.getYear());
            ZonedDateTime endOfMonth = now.withDayOfMonth(now.getMonth().length(isLeapYear))
                    .withHour(23).withMinute(59).withSecond(59).withNano(0);

            Duration durationUntilEndOfMonth = Duration.between(now, endOfMonth);
            redisTemplate.expire(key, durationUntilEndOfMonth.getSeconds(), TimeUnit.SECONDS);
        }
        log.info("Monthly Bucket consumed Count: {}", currentCount);
        if (currentCount > limit) {
            sendError(response, HttpStatus.PAYMENT_REQUIRED,
                    ERROR_MONTHLY_QUOTA_START + limit + ERROR_MONTHLY_QUOTA_END);
            return false;
        }
        return true;
    }

    private boolean checkPerWindowLimit(String clientId, HttpServletResponse response) throws Exception {
        Bucket bucket = rateLimitConfigService.resolveBucket(clientId);
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        long remaining = probe.getRemainingTokens();
        log.info("Window Bucket Consumed: {}", remaining);

        // Soft throttle:  slow down, but allow
        if (probe.isConsumed() && remaining < 5) {   // Soft-throttle threshold
            Thread.sleep(120);                       // Small delay (120ms)
            response.addHeader(HEADER_RATE_LIMIT_REMAINING, String.valueOf(remaining));
            return true;
        }

        // Normal allowed request
        if (probe.isConsumed()) {
            response.addHeader(HEADER_RATE_LIMIT_REMAINING, String.valueOf(remaining));
            return true;
        }

        // Hard throttle — too many requests
        long waitForRefillSeconds = probe.getNanosToWaitForRefill() / 1_000_000_000;
        response.addHeader(HEADER_RETRY_AFTER_SECONDS, String.valueOf(waitForRefillSeconds));
        sendError(response, HttpStatus.TOO_MANY_REQUESTS, ERROR_PER_WINDOW_EXCEEDED);
        return false;
    }

    private void sendError(HttpServletResponse response, HttpStatus status, String message) throws Exception {
        applyCorsHeaders(response);
        response.setStatus(status.value());
        response.getWriter().write(message);
        response.getWriter().flush();
    }

    private void applyCorsHeaders(HttpServletResponse response) {
        response.setHeader(HEADER_ALLOW_ORIGIN_KEY, ALLOWED_ORIGIN);
        response.setHeader(HEADER_ALLOW_CREDENTIALS_KEY, HEADER_ALLOW_CREDENTIALS_VALUE);
        response.setHeader(HEADER_EXPOSE_HEADERS_KEY, EXPOSE_HEADERS);
    }

}