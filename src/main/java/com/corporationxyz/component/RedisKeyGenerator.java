package com.corporationxyz.component;

import org.springframework.stereotype.Component;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

@Component
public class RedisKeyGenerator {

    public String clientConfigKey(String clientId) {

        return "client:" + clientId + ":config";
    }

    public String windowBucketKey(String clientId) {
        return String.format(
                "rate_limit:%s:window",
                clientId
        );
    }

    public String monthBucketKey(String clientId, YearMonth ym) {
        return String.format(
                "rate_limit:%s:month:%s",
                clientId,
                ym.format(DateTimeFormatter.ofPattern("yyyyMM"))
        );
    }

    public String globalKey() {
        return "rate_limit:global:system";
    }
}
