package com.github.peacockxt.controller;

import com.github.peacockxt.service.storage.MinioWebhookService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.JsonNode;

@RestController
@RequestMapping("/api/minio")
@AllArgsConstructor
public class MinioWebhookController {

    private final MinioWebhookService minioWebhookService;

    @PostMapping("/webhook")
    public ResponseEntity<String> handleMinioEvent(@RequestBody JsonNode payload) {
        minioWebhookService.processEventPayload(payload);
        return ResponseEntity.ok("Received");
    }

}