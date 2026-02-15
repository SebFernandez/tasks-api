package com.seba.tasks.service;

import com.seba.tasks.dto.TaskDto;
import com.seba.tasks.model.TaskStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface TaskService {

    Mono<TaskDto> create(String title, String createdBy);

    Mono<TaskDto> getById(UUID taskId);

    Flux<TaskDto> getAll();

    Mono<TaskDto> update(UUID taskId, String title, TaskStatus status, String updatedBy);

    Mono<Void> delete(UUID taskId);
}
