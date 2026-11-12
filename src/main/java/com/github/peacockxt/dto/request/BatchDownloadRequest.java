package com.github.peacockxt.dto.request;
import java.util.List;
public class BatchDownloadRequest {
    private List<String> objectNames;

    public BatchDownloadRequest() {}

    public BatchDownloadRequest(List<String> objectNames) {
        this.objectNames = objectNames;
    }

    public List<String> getObjectNames() {
        return objectNames;
    }

    public void setObjectNames(List<String> objectNames) {
        this.objectNames = objectNames;
    }
}