package com.corporationxyz.controller;

import com.corporationxyz.controller.NotificationController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class NotificationControllerTest {

    private NotificationController notificationController;

    @BeforeEach
    void setUp() {
        notificationController = new NotificationController();
    }

    @Test
    void sendNotification_ShouldReturnSuccessMessage() {
        ResponseEntity<String> response = notificationController.sendNotification();

        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());
        assertEquals("Notification sent successfully.", response.getBody());
    }
}
