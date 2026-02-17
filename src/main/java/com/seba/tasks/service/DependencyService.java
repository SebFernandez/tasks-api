package com.seba.tasks.service;

import com.seba.tasks.dto.TaskDto;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface DependencyService {

    Mono<TaskDto> addDependency(UUID taskId, UUID blockerTaskId);

    Mono<TaskDto> removeDependency(UUID taskId, UUID blockerTaskId);
}
