package com.seba.tasks.utils;

import com.seba.tasks.dto.TaskDto;
import com.seba.tasks.model.Task;
import lombok.experimental.UtilityClass;

@UtilityClass
public class TaskUtility {

    public TaskDto toTaskDto(Task task) {
        return new TaskDto(
                task.getTaskId(),
                task.getTitle(),
                task.getStatus(),
                task.getCreatedAt(),
                task.getCreatedBy(),
                task.getUpdatedAt(),
                task.getUpdatedBy()
        );
    }
}
