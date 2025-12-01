package com.corporationxyz.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @GetMapping("/send")
    public ResponseEntity<String> sendNotification() {
        // Actual logic to send SMS/Email goes here, but it is not part of the assignment
        return ResponseEntity.ok("Notification sent successfully.");
    }
}
