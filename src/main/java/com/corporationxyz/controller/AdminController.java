package com.corporationxyz.controller;


import com.corporationxyz.dto.ClientLimitConfigDto;
import com.corporationxyz.dto.ClientLimitDto;
import com.corporationxyz.mapper.ClientLimitConfigMapper;
import com.corporationxyz.persistence.entity.ClientLimitConfig;
import com.corporationxyz.persistence.repository.ClientLimitRepository;
import com.corporationxyz.service.RateLimitConfigService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@RestController
@RequestMapping("/admin/limits")
public class AdminController {

    private final ClientLimitRepository clientLimitRepository;
    private final RateLimitConfigService rateLimitConfigService;
    private final ClientLimitConfigMapper mapper;

    @Autowired
    public AdminController(ClientLimitRepository clientLimitRepository,
                           RateLimitConfigService rateLimitConfigService, ClientLimitConfigMapper mapper) {
        this.clientLimitRepository = clientLimitRepository;
        this.rateLimitConfigService = rateLimitConfigService;
        this.mapper = mapper;
    }

    @GetMapping("/{clientId}")
    public Optional<ClientLimitConfig> getClientLimit(@PathVariable String clientId) {
        return clientLimitRepository.findByClientId(clientId);
    }

    @GetMapping
    public List<ClientLimitConfig> getAllLimits() {
        return clientLimitRepository.findAll();
    }

    /**
     * Allows company to define/update rate limits for a specific client.
     * This meets Requirement #1 and #2 (defining limits).
     */
    @PostMapping
    public ResponseEntity<ClientLimitConfigDto> defineClientLimit(@Valid @RequestBody ClientLimitDto limitDto) {
        Optional<ClientLimitConfig> existingOpt =
                clientLimitRepository.findByClientId(limitDto.getClientId());

        boolean monthlyChanged = false;
        boolean windowChanged = false;

        if (existingOpt.isPresent()) {
            ClientLimitConfig oldConfig = existingOpt.get();

            monthlyChanged = !Objects.equals(oldConfig.getMonthlyLimit(), limitDto.getMonthlyLimit());
            windowChanged =
                    !Objects.equals(oldConfig.getWindowCapacity(), limitDto.getWindowCapacity()) ||
                            !Objects.equals(oldConfig.getWindowDurationSeconds(), limitDto.getWindowDurationSeconds());
        }

        ClientLimitConfig saved = clientLimitRepository.save(mapper.toEntity(limitDto));

        if (monthlyChanged) {
            //"rate_limit:client-x:month:*"
            rateLimitConfigService.resetMonthlyCounter(saved.getClientId());
        }

        if (windowChanged) {
            // Must reload bucket, because per-window settings changed
            rateLimitConfigService.resetWindowCounter(saved.getClientId());
        }

        return new ResponseEntity<>(mapper.toDto(saved), HttpStatus.CREATED);
    }

    // Optional: Add a DELETE endpoint
    @DeleteMapping("/{clientId}")
    public ResponseEntity<Void> deleteLimit(@PathVariable String clientId) {
        clientLimitRepository.deleteById(clientId);
        rateLimitConfigService.reloadBucketAndClientConfig(clientId);
        return ResponseEntity.noContent().build();
    }
}
