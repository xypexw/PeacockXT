package com.github.peacockxt.infrastructure.storage;

import io.minio.MinioClient;
import io.minio.PostPolicy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.http.Method;
import java.util.concurrent.TimeUnit;

import java.time.ZonedDateTime;
import java.util.Map;

@Component
public class PublicStorageUploader {

    private static final String CONTENT_TYPE_FIELD = "Content-Type";
    private static final String OBJECT_KEY_FIELD = "key";
    private static final String UPLOAD_URL_FIELD = "uploadUrl";
    private static final String META_DATA_FIELD = "x-amz-meta-type";
    private static final String ALLOWED_CONTENT_TYPE = "image/webp";
    private static final int POLICY_EXPIRATION_MINUTES = 30;
    private static final long MIN_FILE_SIZE = 1L;
    private static final long MAX_FILE_SIZE = 1024 * 1024; // 1 MB

    private final MinioClient minioClient;

    @Value("${minio.public.bucket-name}")
    private String bucketName;

    @Value("${minio.endpoint}")
    private String minioEndpoint;

    public PublicStorageUploader(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    public Map<String, String> getUploadPolicy(String objectName) {
        try {
            PostPolicy policy = buildUploadPolicy(objectName);
            Map<String, String> formData = minioClient.getPresignedPostFormData(policy);
            formData.put(UPLOAD_URL_FIELD, minioEndpoint + "/" + bucketName);
            return formData;
        } catch (Exception exception) {
            throw new RuntimeException("Failed to generate upload policy: " + exception.getMessage());
        }
    }

    private PostPolicy buildUploadPolicy(String objectName) {
        ZonedDateTime expiration = ZonedDateTime.now().plusMinutes(POLICY_EXPIRATION_MINUTES);
        PostPolicy policy = new PostPolicy(bucketName, expiration);
        policy.addStartsWithCondition(META_DATA_FIELD, "");
        policy.addEqualsCondition(OBJECT_KEY_FIELD, objectName);
        policy.addEqualsCondition(CONTENT_TYPE_FIELD, ALLOWED_CONTENT_TYPE);
        policy.addContentLengthRangeCondition(MIN_FILE_SIZE, MAX_FILE_SIZE);
        return policy;
    }

    public String getDownloadUrl(String objectName) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(objectName)
                            .expiry(1, TimeUnit.HOURS)
                            .build()
            );
        } catch (Exception exception) {
            throw new RuntimeException("Failed to generate download URL: " + exception.getMessage());
        }
    }

    public String getDeleteUrl(String objectName) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.DELETE)
                            .bucket(bucketName)
                            .object(objectName)
                            .expiry(POLICY_EXPIRATION_MINUTES, TimeUnit.MINUTES)
                            .build()
            );
        } catch (Exception exception) {
            throw new RuntimeException("Failed to generate delete URL: " + exception.getMessage());
        }
    }

}