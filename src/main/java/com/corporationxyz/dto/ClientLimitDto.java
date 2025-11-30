package com.corporationxyz.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ClientLimitDto {
    @NotBlank
    private String clientId;
    @NotNull
    @Min(0)
    private Long monthlyLimit;
    @NotNull
    @Min(1)
    private Integer windowCapacity;
    @NotNull
    @Min(1)
    private Integer windowDurationSeconds;
}
