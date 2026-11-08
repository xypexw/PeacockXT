package com.example.peacockxt.Infrastructure.Strogare;

import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PostPolicy;
import io.minio.http.Method;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Component
public class PrivateStorageUploader {

    private static final String OBJECT_KEY_FIELD = "key";
    private static final String UPLOAD_URL_FIELD = "uploadUrl";
    private static final String META_DATA_FIELD = "x-amz-meta-type";

    // 10 minutes
    private static final int POLICY_EXPIRATION_MINUTES = 10;
    private static final int DOWNLOAD_URL_EXPIRATION_MINUTES = 10;
    private static final int DELETE_URL_EXPIRATION_MINUTES = 10;

    // 50 MB
    private static final long MIN_FILE_SIZE = 1L;
    private static final long MAX_FILE_SIZE = 50L * 1024 * 1024; // 50MB

    private final MinioClient minioClient;

    @Value("${minio.private.bucket-name}")
    private String bucketName;

    @Value("${minio.endpoint}")
    private String minioEndpoint;

    public PrivateStorageUploader(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    /**
     * Generates a batch of Presigned Download URLs using parallelStream()
     * to optimize CPU performance when handling large volumes.
     *
     * @param objectNames List of object names to generate URLs for.
     * @return Map<ObjectName, PresignedUrl>
     */
    public Map<String, String> getBatchDownloadUrls(List<String> objectNames) {
        if (objectNames == null || objectNames.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, String> resultMap = new ConcurrentHashMap<>(objectNames.size());

        // Leverage multicore CPU parallelism for faster cryptographic signature generation
        objectNames.parallelStream().forEach(objectName -> {
            try {
                String url = getDownloadUrl(objectName);
                resultMap.put(objectName, url);
            } catch (Exception e) {
                // If a single key fails, log and assign an empty string or skip without failing the entire batch
                resultMap.put(objectName, "");
            }
        });

        return resultMap;
    }

    /**
     * Generates an upload policy.
     * Allows uploading any file type within size constraints.
     */
    public Map<String, String> getUploadPolicy(String objectName) {
        try {
            PostPolicy policy = buildUploadPolicy(objectName);
            Map<String, String> formData = minioClient.getPresignedPostFormData(policy);
            formData.put(UPLOAD_URL_FIELD, minioEndpoint + "/" + bucketName);
            return formData;

        } catch (Exception exception) {
            throw new RuntimeException("Failed to generate upload policy: " + exception.getMessage(), exception);
        }
    }

    private PostPolicy buildUploadPolicy(String objectName) {
        ZonedDateTime expiration = ZonedDateTime.now().plusMinutes(POLICY_EXPIRATION_MINUTES);
        PostPolicy policy = new PostPolicy(bucketName, expiration);
        // Only allow uploads targeting this specific object
        policy.addStartsWithCondition(META_DATA_FIELD, "");
        policy.addEqualsCondition(OBJECT_KEY_FIELD, objectName);
        // Apply file size limits
        policy.addContentLengthRangeCondition(MIN_FILE_SIZE, MAX_FILE_SIZE);
        return policy;
    }

    /**
     * Generates a temporary download URL.
     * Expires after 10 minutes.
     */
    public String getDownloadUrl(String objectName) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(objectName)
                            .expiry(DOWNLOAD_URL_EXPIRATION_MINUTES, TimeUnit.MINUTES)
                            .build()
            );
        } catch (Exception exception) {
            throw new RuntimeException("Failed to generate download URL: " + exception.getMessage(), exception);
        }
    }

    /**
     * Generates a temporary delete URL using the DELETE HTTP method.
     * Expires after 10 minutes.
     */
    public String getDeleteUrl(String objectName) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.DELETE)
                            .bucket(bucketName)
                            .object(objectName)
                            .expiry(DELETE_URL_EXPIRATION_MINUTES, TimeUnit.MINUTES)
                            .build()
            );
        } catch (Exception exception) {
            throw new RuntimeException("Failed to generate delete URL: " + exception.getMessage(), exception);
        }
    }

}