package com.example.peacockxt.Infrastructure.Strogare;

public interface MinioFileHandler {
    boolean supports(String metaType, String key);
    void handleCreated(String bucket, String key);
    void handleRemoved(String key);
}
