package com.corporationxyz.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Builder
@Entity
@Table(name = "client_limit_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClientLimitConfig {
    @Id
    @Column(name = "client_id")
    private String clientId;
    @Column(name = "monthly_limit")
    private Long monthlyLimit;
    @Column(name = "window_capacity")
    private Integer windowCapacity;
    @Column(name = "window_duration_seconds")
    private Integer windowDurationSeconds;
}
