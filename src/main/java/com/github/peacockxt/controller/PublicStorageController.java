package com.github.peacockxt.controller;

import com.github.peacockxt.infrastructure.storage.PublicStorageUploader;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/public-storage")
public class PublicStorageController {

    private final PublicStorageUploader storageUploader;

    public PublicStorageController(PublicStorageUploader storageUploader) {
        this.storageUploader = storageUploader;
    }

    /**
     * Retrieves formData and uploadUrl for clients (Web/Mobile) to upload directly to MinIO.
     * Example: GET /api/v1/public-storage/public-upload-policy?objectName=avatars/user-123.webp
     */
    @GetMapping("/upload-policy")
    public ResponseEntity<Map<String, String>> getUploadPolicy(@RequestParam String objectName) {
        Map<String, String> policyData = storageUploader.getUploadPolicy(objectName);
        return ResponseEntity.ok(policyData);
    }

    /**
     * Retrieves a Presigned URL to download or directly view the file.
     * Example: GET /api/v1/public-storage/download-url?objectName=avatars/user-123.webp
     */
    @GetMapping("/download-url")
    public ResponseEntity<Map<String, String>> getDownloadUrl(@RequestParam String objectName) {
        String downloadUrl = storageUploader.getDownloadUrl(objectName);
        return ResponseEntity.ok(Map.of("url", downloadUrl));
    }

    /**
     * Retrieves a Presigned URL with the DELETE method for the client/service to delete the file.
     * Example: GET /api/v1/public-storage/delete-url?objectName=avatars/user-123.webp
     */
    @GetMapping("/delete-url")
    public ResponseEntity<Map<String, String>> getDeleteUrl(@RequestParam String objectName) {
        String deleteUrl = storageUploader.getDeleteUrl(objectName);
        return ResponseEntity.ok(Map.of("url", deleteUrl));
    }

}