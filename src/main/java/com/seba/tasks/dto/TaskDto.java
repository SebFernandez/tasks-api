package com.seba.tasks.dto;

import com.seba.tasks.model.TaskStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TaskDto(
        UUID taskId,
        String title,
        TaskStatus status,
        List<UUID> dependsOn,
        Instant createdAt,
        String createdBy,
        Instant updatedAt,
        String updatedBy
) {
}
