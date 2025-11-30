package com.corporationxyz.controller;


import com.corporationxyz.dto.ClientLimitDto;
import com.corporationxyz.persistence.entity.ClientLimitConfig;
import com.corporationxyz.persistence.repository.ClientLimitRepository;
import com.corporationxyz.service.RateLimitConfigService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/admin/limits")
public class AdminController {

    private final ClientLimitRepository clientLimitRepository;
    private final RateLimitConfigService rateLimitConfigService;

    @Autowired
    public AdminController(ClientLimitRepository clientLimitRepository,
                           RateLimitConfigService rateLimitConfigService) {
        this.clientLimitRepository = clientLimitRepository;
        this.rateLimitConfigService = rateLimitConfigService;
    }

    @GetMapping
    public List<ClientLimitConfig> getAllLimits() {
        return clientLimitRepository.findAll();
    }

    @GetMapping("/{clientId}")
    public Optional<ClientLimitConfig> getClientLimit(@PathVariable String clientId) {
        return clientLimitRepository.findByClientId(clientId);
    }

    /**
     * Allows company to define/update rate limits for a specific client.
     * This meets Requirement #1 and #2 (defining limits).
     */
    @PostMapping
    public ResponseEntity<ClientLimitConfig> defineClientLimit(@Valid @RequestBody ClientLimitDto limitDto) {
        // Convert DTO to Entity
        ClientLimitConfig config = new ClientLimitConfig(
                limitDto.getClientId(),
                limitDto.getMonthlyLimit(),
                limitDto.getWindowCapacity(),
                limitDto.getWindowDurationSeconds()
        );

        ClientLimitConfig savedConfig = clientLimitRepository.save(config);
        rateLimitConfigService.reloadBucketAndClientConfig(savedConfig.getClientId());

        // Changes applied here are picked up by RateLimitConfigService on the next request/cache clear.
        return new ResponseEntity<>(savedConfig, HttpStatus.CREATED);
    }

    // Optional: Add a DELETE endpoint
    @DeleteMapping("/{clientId}")
    public ResponseEntity<Void> deleteLimit(@PathVariable String clientId) {
        clientLimitRepository.deleteById(clientId);
        return ResponseEntity.noContent().build();
    }
}
