package com.corporationxyz.controller;

import com.corporationxyz.dto.ClientLimitConfigDto;
import com.corporationxyz.dto.ClientLimitDto;
import com.corporationxyz.mapper.ClientLimitConfigMapper;
import com.corporationxyz.persistence.entity.ClientLimitConfig;
import com.corporationxyz.persistence.repository.ClientLimitRepository;
import com.corporationxyz.service.RateLimitConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminControllerTest {

    @Mock
    private ClientLimitRepository clientLimitRepository;
    @Mock
    private RateLimitConfigService rateLimitConfigService;

    @Mock
    private ClientLimitConfigMapper mapper;

    @InjectMocks
    private AdminController adminController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getAllLimits_ShouldReturnListOfLimits() {
        ClientLimitConfig c1 = new ClientLimitConfig("client-x", 5000L, 10, 60);
        ClientLimitConfig c2 = new ClientLimitConfig("client-y", 10000L, 20, 120);

        when(clientLimitRepository.findAll()).thenReturn(Arrays.asList(c1, c2));

        List<ClientLimitConfig> result = adminController.getAllLimits();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("client-x", result.get(0).getClientId());
        assertEquals("client-y", result.get(1).getClientId());

        verify(clientLimitRepository, times(1)).findAll();
    }

    @Test
    void defineClientLimit_ShouldSaveAndReturnCreatedEntity() {
        ClientLimitDto dto = new ClientLimitDto();
        dto.setClientId("client-z");
        dto.setMonthlyLimit(1000L);
        dto.setWindowCapacity(5);
        dto.setWindowDurationSeconds(60);

        ClientLimitConfig mappedEntity = new ClientLimitConfig();
        mappedEntity.setClientId("client-z");
        ClientLimitConfig savedConfig = new ClientLimitConfig(
                dto.getClientId(),
                dto.getMonthlyLimit(),
                dto.getWindowCapacity(),
                dto.getWindowDurationSeconds()
        );

        ClientLimitConfigDto savedDto = new ClientLimitConfigDto();
        savedDto.setClientId(dto.getClientId());
        savedDto.setMonthlyLimit(dto.getMonthlyLimit());
        savedDto.setWindowCapacity(dto.getWindowCapacity());
        savedDto.setWindowDurationSeconds(dto.getWindowDurationSeconds());

        when(mapper.toEntity(dto)).thenReturn(mappedEntity);
        when(clientLimitRepository.save(any(ClientLimitConfig.class))).thenReturn(savedConfig);

        when(mapper.toDto(savedConfig)).thenReturn(savedDto);
        doNothing().when(rateLimitConfigService).reloadBucketAndClientConfig(any());

        ResponseEntity<ClientLimitConfigDto> response = adminController.defineClientLimit(dto);

        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("client-z", response.getBody().getClientId());
        assertEquals(1000L, response.getBody().getMonthlyLimit());
        assertEquals(5, response.getBody().getWindowCapacity());

        // Verify save was called with correct entity
        ArgumentCaptor<ClientLimitConfig> captor = ArgumentCaptor.forClass(ClientLimitConfig.class);
        verify(clientLimitRepository, times(1)).save(captor.capture());
        assertEquals("client-z", captor.getValue().getClientId());
    }

    @Test
    void deleteLimit_ShouldCallRepositoryAndReturnNoContent() {
        String clientId = "client-x";

        doNothing().when(clientLimitRepository).deleteById(clientId);

        ResponseEntity<Void> response = adminController.deleteLimit(clientId);

        assertNotNull(response);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

        verify(clientLimitRepository, times(1)).deleteById(clientId);
    }

    @Test
    void defineClientLimit_successful() {
        ClientLimitDto dto = new ClientLimitDto(); // missing required fields
        ClientLimitConfig mappedEntity = new ClientLimitConfig();
        mappedEntity.setClientId("client-x");

        when(mapper.toEntity(dto)).thenReturn(mappedEntity);

        when(clientLimitRepository.save(any(ClientLimitConfig.class)))
                .thenReturn(new ClientLimitConfig());

        doNothing().when(rateLimitConfigService).reloadBucketAndClientConfig(any());
        ResponseEntity<ClientLimitConfigDto> response = adminController.defineClientLimit(dto);

        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());

        verify(clientLimitRepository, times(1)).save(any(ClientLimitConfig.class));
    }

    @Test
    void defineClientLimit_existingConfig_monthlyChanged_only() {

        ClientLimitDto dto = new ClientLimitDto();
        dto.setClientId("client-x");
        dto.setMonthlyLimit(2000L);        // changed
        dto.setWindowCapacity(10);
        dto.setWindowDurationSeconds(60);

        ClientLimitConfig oldCfg =
                new ClientLimitConfig("client-x", 1000L, 10, 60);

        when(clientLimitRepository.findByClientId("client-x"))
                .thenReturn(Optional.of(oldCfg));

        ClientLimitConfig mapped = new ClientLimitConfig("client-x", 2000L, 10, 60);
        when(mapper.toEntity(dto)).thenReturn(mapped);
        when(clientLimitRepository.save(any())).thenReturn(mapped);

        when(mapper.toDto(mapped)).thenReturn(new ClientLimitConfigDto());

        ResponseEntity<ClientLimitConfigDto> resp = adminController.defineClientLimit(dto);

        assertEquals(HttpStatus.CREATED, resp.getStatusCode());

        verify(rateLimitConfigService).resetMonthlyCounter("client-x");
        verify(rateLimitConfigService, never()).resetWindowCounter(any());
    }

    @Test
    void defineClientLimit_existingConfig_windowChanged_only() {

        ClientLimitDto dto = new ClientLimitDto();
        dto.setClientId("client-x");
        dto.setMonthlyLimit(1000L);
        dto.setWindowCapacity(20);
        dto.setWindowDurationSeconds(60);

        ClientLimitConfig oldCfg =
                new ClientLimitConfig("client-x", 1000L, 10, 60);

        when(clientLimitRepository.findByClientId("client-x"))
                .thenReturn(Optional.of(oldCfg));

        ClientLimitConfig mapped =
                new ClientLimitConfig("client-x", 1000L, 20, 60);
        when(mapper.toEntity(dto)).thenReturn(mapped);
        when(clientLimitRepository.save(any())).thenReturn(mapped);

        when(mapper.toDto(mapped)).thenReturn(new ClientLimitConfigDto());

        ResponseEntity<ClientLimitConfigDto> resp = adminController.defineClientLimit(dto);

        assertEquals(HttpStatus.CREATED, resp.getStatusCode());

        verify(rateLimitConfigService).resetWindowCounter("client-x");
        verify(rateLimitConfigService, never()).resetMonthlyCounter(any());
    }

    @Test
    void defineClientLimit_existingConfig_bothChanged() {

        ClientLimitDto dto = new ClientLimitDto();
        dto.setClientId("client-x");
        dto.setMonthlyLimit(5000L);          // changed
        dto.setWindowCapacity(50);           // changed
        dto.setWindowDurationSeconds(120);   // changed

        ClientLimitConfig oldCfg =
                new ClientLimitConfig("client-x", 1000L, 10, 60);

        when(clientLimitRepository.findByClientId("client-x"))
                .thenReturn(Optional.of(oldCfg));

        ClientLimitConfig mapped =
                new ClientLimitConfig("client-x", 5000L, 50, 120);
        when(mapper.toEntity(dto)).thenReturn(mapped);
        when(clientLimitRepository.save(any())).thenReturn(mapped);

        when(mapper.toDto(mapped)).thenReturn(new ClientLimitConfigDto());

        ResponseEntity<ClientLimitConfigDto> resp = adminController.defineClientLimit(dto);

        assertEquals(HttpStatus.CREATED, resp.getStatusCode());

        verify(rateLimitConfigService).resetMonthlyCounter("client-x");
        verify(rateLimitConfigService).resetWindowCounter("client-x");
    }

    @Test
    void defineClientLimit_existingConfig_noChanges() {

        ClientLimitDto dto = new ClientLimitDto();
        dto.setClientId("client-x");
        dto.setMonthlyLimit(1000L);
        dto.setWindowCapacity(10);
        dto.setWindowDurationSeconds(60);

        ClientLimitConfig oldCfg =
                new ClientLimitConfig("client-x", 1000L, 10, 60);

        when(clientLimitRepository.findByClientId("client-x"))
                .thenReturn(Optional.of(oldCfg));

        ClientLimitConfig mapped =
                new ClientLimitConfig("client-x", 1000L, 10, 60);
        when(mapper.toEntity(dto)).thenReturn(mapped);
        when(clientLimitRepository.save(any())).thenReturn(mapped);

        when(mapper.toDto(mapped)).thenReturn(new ClientLimitConfigDto());

        ResponseEntity<ClientLimitConfigDto> resp = adminController.defineClientLimit(dto);

        assertEquals(HttpStatus.CREATED, resp.getStatusCode());

        verify(rateLimitConfigService, never()).resetMonthlyCounter(any());
        verify(rateLimitConfigService, never()).resetWindowCounter(any());
    }

    @Test
    void getClientLimit_ShouldReturnRecord() {
        ClientLimitConfig cfg = new ClientLimitConfig("client-a", 1000L, 10, 60);
        when(clientLimitRepository.findByClientId("client-a")).thenReturn(Optional.of(cfg));

        Optional<ClientLimitConfig> result = adminController.getClientLimit("client-a");

        assertNotNull(result);
        assertTrue(result.isPresent());
        assertEquals("client-a", result.get().getClientId());
        verify(clientLimitRepository, times(1)).findByClientId("client-a");
    }


}
