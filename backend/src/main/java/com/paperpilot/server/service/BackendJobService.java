package com.paperpilot.server.service;

import com.paperpilot.server.entity.BackendJobEntity;
import com.paperpilot.server.repository.BackendJobRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class BackendJobService {
    private final BackendJobRepository backendJobRepository;

    public BackendJobService(BackendJobRepository backendJobRepository) {
        this.backendJobRepository = backendJobRepository;
    }

    public String key(String type, Long userId, String resourceId) {
        return type + ":" + (userId == null ? "system" : userId) + ":" + (resourceId == null ? "" : resourceId);
    }

    @Transactional
    public BackendJobEntity upsert(String type, Long userId, String resourceId, String status, int progress, String message, String detail) {
        String key = key(type, userId, resourceId);
        BackendJobEntity job = backendJobRepository.findByJobKey(key).orElseGet(BackendJobEntity::new);
        job.setJobKey(key);
        job.setJobType(type);
        job.setUserId(userId);
        job.setResourceId(resourceId);
        job.setStatus(status);
        job.setProgress(progress);
        job.setMessage(message);
        job.setDetail(detail == null ? "" : detail);
        return backendJobRepository.save(job);
    }

    @Transactional
    public BackendJobEntity externalTask(String type, Long userId, String resourceId, String externalTaskId, String status, int progress, String message) {
        BackendJobEntity job = upsert(type, userId, resourceId, status, progress, message, "");
        job.setExternalTaskId(externalTaskId);
        return backendJobRepository.save(job);
    }

    public Optional<BackendJobEntity> find(String type, Long userId, String resourceId) {
        return backendJobRepository.findByJobKey(key(type, userId, resourceId));
    }

    public Map<String, Object> toMap(BackendJobEntity job) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("jobKey", job.getJobKey());
        row.put("jobType", job.getJobType());
        row.put("userId", job.getUserId());
        row.put("resourceId", job.getResourceId());
        row.put("taskId", job.getExternalTaskId());
        row.put("status", job.getStatus());
        row.put("state", job.getStatus());
        row.put("progress", job.getProgress() == null ? 0 : job.getProgress());
        row.put("message", job.getMessage() == null ? "" : job.getMessage());
        row.put("detail", job.getDetail() == null ? "" : job.getDetail());
        row.put("done", isDone(job.getStatus()));
        row.put("success", isSuccess(job.getStatus()));
        row.put("updatedAt", job.getUpdatedAt());
        return row;
    }

    private boolean isDone(String status) {
        String value = status == null ? "" : status.toUpperCase();
        return value.equals("SUCCESS") || value.equals("COMPLETED") || value.equals("GENERATED") || value.equals("FAILURE") || value.equals("FAILED");
    }

    private boolean isSuccess(String status) {
        String value = status == null ? "" : status.toUpperCase();
        return value.equals("SUCCESS") || value.equals("COMPLETED") || value.equals("GENERATED");
    }
}
