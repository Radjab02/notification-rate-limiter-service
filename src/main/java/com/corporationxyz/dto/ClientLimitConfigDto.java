package com.corporationxyz.dto;

import lombok.Data;

@Data
public class ClientLimitConfigDto {

    private String clientId;          // what users/admins expect
    private long monthlyLimit;
    private int windowCapacity;
    private int windowDurationSeconds;
}