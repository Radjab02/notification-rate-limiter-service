package com.corporationxyz.controller;

import com.corporationxyz.controller.AdminController;
import com.corporationxyz.dto.ClientLimitDto;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

class AdminControllerTest {

    @Mock
    private ClientLimitRepository clientLimitRepository;
    @Mock
    private RateLimitConfigService rateLimitConfigService;

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

        ClientLimitConfig savedConfig = new ClientLimitConfig(
                dto.getClientId(),
                dto.getMonthlyLimit(),
                dto.getWindowCapacity(),
                dto.getWindowDurationSeconds()
        );

        when(clientLimitRepository.save(any(ClientLimitConfig.class))).thenReturn(savedConfig);
        doNothing().when(rateLimitConfigService).reloadBucketAndClientConfig(any());

        ResponseEntity<ClientLimitConfig> response = adminController.defineClientLimit(dto);

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

        when(clientLimitRepository.save(any(ClientLimitConfig.class)))
                .thenReturn(new ClientLimitConfig());

        doNothing().when(rateLimitConfigService).reloadBucketAndClientConfig(any());
        ResponseEntity<ClientLimitConfig> response = adminController.defineClientLimit(dto);

        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());

        verify(clientLimitRepository, times(1)).save(any(ClientLimitConfig.class));
    }

}
