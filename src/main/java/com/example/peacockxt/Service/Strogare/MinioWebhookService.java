package com.example.peacockxt.Service.Strogare;

import com.fasterxml.jackson.databind.JsonNode;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Service
public class MinioWebhookService {

    private final MinioClient minioClient;

    @Value("${minio.public.bucket-name}")
    private String publicBucket;

    @Value("${minio.private.bucket-name}")
    private String privateBucket;

    public MinioWebhookService(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    public void processEventPayload(JsonNode payload) {
        JsonNode records = payload.path("Records");
        if (!records.isArray() || records.isEmpty()) {
            return;
        }

        for (JsonNode record : records) {
            processRecord(record);
        }
    }

    private void processRecord(JsonNode record) {
        try {
            String publicBucketName = record.path("s3").path("bucket").path("name").asText(publicBucket);
            String privateBucketName = record.path("s3").path("bucket").path("name").asText(privateBucket);
            String rawKey = record.path("s3").path("object").path("key").asText();
            String objectKey = URLDecoder.decode(rawKey, StandardCharsets.UTF_8);

            JsonNode userMetadata = record.path("s3").path("object").path("userMetadata");
            String metaType = "";
            if (userMetadata.has("x-amz-meta-type")) {
                metaType = userMetadata.get("x-amz-meta-type").asText();
            } else if (userMetadata.has("type")) {
                metaType = userMetadata.get("type").asText();
            }

            switch (metaType.toLowerCase()) {
                case "user_avatar":
                case "user-avatar":
                case "user avatar":
                    handleUserAvatar(objectKey);
                    break;

                case "server_emoji":
                case "server-emoji":
                case "server emoji":
                    handleServerEmoji(objectKey);
                    break;

                case "comment_file":
                case "comment_files":
                case "comment-file":
                case "comment-files":
                case "comment file":
                case "comment files":
                    handleCommentFile(objectKey);
                    break;

                case "message_file":
                case "message_files":
                case "message-file":
                case "message-files":
                case "message file":
                case "message files":
                    handleMessageFile(objectKey);
                    break;

                case "direct_message_file":
                case "direct_message_files":
                case "direct-message-file":
                case "direct-message-files":
                case "direct message file":
                case "direct message files":
                case "dm_file":
                case "dm_files":
                case "dm-file":
                case "dm-files":
                    handleDirectMessageFile(objectKey);
                    break;

                default:
                    deleteObject(publicBucketName,objectKey);
                    deleteObject(privateBucketName, objectKey);
                    break;
            }
        } catch (Exception e) {
            System.err.println("[ERROR] Failed to process record: " + e.getMessage());
        }
    }

    private void handleUserAvatar(String key) {
        // TODO: Xử lý logic cập nhật avatar của user
    }

    private void handleServerEmoji(String key) {
        // TODO: Xử lý logic cập nhật custom emoji của server
    }

    private void handleCommentFile(String key) {
        // TODO: Xử lý logic liên kết file đính kèm với comment
    }

    private void handleMessageFile(String key) {
        // TODO: Xử lý logic liên kết file đính kèm với channel message
    }

    private void handleDirectMessageFile(String key) {
        // TODO: Xử lý logic liên kết file đính kèm với tin nhắn trực tiếp (DM)
    }

    private void deleteObject(String bucket, String key) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(key).build());
        } catch (Exception e) {
            System.err.println("[ERROR] Failed to delete object: " + e.getMessage());
        }
    }
}