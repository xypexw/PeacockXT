package com.github.peacockxt.controller;

import com.github.peacockxt.dto.request.BatchDownloadRequest;
import com.github.peacockxt.infrastructure.storage.PrivateStorageUploader;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/private-storage")
public class PrivateStorageController {

    private final PrivateStorageUploader storageUploader;
    private static final int MAX_BATCH_LIMIT = 5000; // Safe threshold per batch request

    public PrivateStorageController(PrivateStorageUploader minioUploader) {
        this.storageUploader = minioUploader;
    }

    /**
     * Batching API: Retrieves batch download URLs via Request Body.
     * POST /api/v1/private-storage/batch-download-urls
     */
    @PostMapping("/batch-download-urls")
    public ResponseEntity<?> getBatchDownloadUrls(@RequestBody BatchDownloadRequest request) {
        if (request == null || request.getObjectNames() == null || request.getObjectNames().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "The objectNames list must not be empty"));
        }

        if (request.getObjectNames().size() > MAX_BATCH_LIMIT) {
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                    .body(Map.of("message", "The number of objects exceeds the allowed limit (" + MAX_BATCH_LIMIT + ")"));
        }

        Map<String, String> response = storageUploader.getBatchDownloadUrls(request.getObjectNames());
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves formData and uploadUrl for the client to directly upload to MinIO (into the Private Bucket).
     * Example: GET /api/v1/private-storage/upload-policy?objectName=documents/doc-123.pdf
     */
    @GetMapping("/upload-policy")
    public ResponseEntity<Map<String, String>> getUploadPolicy(@RequestParam String objectName) {
        Map<String, String> policyData = storageUploader.getUploadPolicy(objectName);
        return ResponseEntity.ok(policyData);
    }

    /**
     * Retrieves a temporary presigned URL (valid for 10 minutes) to download or view a file.
     * Example: GET /api/v1/private-storage/download-url?objectName=documents/doc-123.pdf
     */
    @GetMapping("/download-url")
    public ResponseEntity<Map<String, String>> getDownloadUrl(@RequestParam String objectName) {
        String downloadUrl = storageUploader.getDownloadUrl(objectName);
        return ResponseEntity.ok(Map.of("url", downloadUrl));
    }

    /**
     * Retrieves a presigned URL with the DELETE method for the client/service to delete a file (valid for 10 minutes).
     * Example: GET /api/v1/private-storage/delete-url?objectName=documents/doc-123.pdf
     */
    @GetMapping("/delete-url")
    public ResponseEntity<Map<String, String>> getDeleteUrl(@RequestParam String objectName) {
        String deleteUrl = storageUploader.getDeleteUrl(objectName);
        return ResponseEntity.ok(Map.of("url", deleteUrl));
    }

}