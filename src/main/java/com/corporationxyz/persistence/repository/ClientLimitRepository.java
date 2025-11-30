
package com.corporationxyz.persistence.repository;

import com.corporationxyz.persistence.entity.ClientLimitConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClientLimitRepository extends JpaRepository<ClientLimitConfig, String> {
    Optional<ClientLimitConfig> findByClientId(String clientId);
}
