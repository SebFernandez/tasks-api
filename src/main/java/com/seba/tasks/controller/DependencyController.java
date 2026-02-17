package com.seba.tasks.controller;

import com.seba.tasks.dto.TaskDto;
import com.seba.tasks.service.DependencyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/tasks/{taskId}/dependencies")
@RequiredArgsConstructor
public final class DependencyController {

    private final DependencyService dependencyService;

    @PutMapping("/{blockerTaskId}")
    public Mono<TaskDto> addDependency(@PathVariable UUID taskId,
                                       @PathVariable UUID blockerTaskId) {
        return dependencyService.addDependency(taskId, blockerTaskId);
    }

    @DeleteMapping("/{blockerTaskId}")
    public Mono<TaskDto> removeDependency(@PathVariable UUID taskId,
                                          @PathVariable UUID blockerTaskId) {
        return dependencyService.removeDependency(taskId, blockerTaskId);
    }
}
