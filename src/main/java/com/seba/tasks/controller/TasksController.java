package com.seba.tasks.controller;

import com.seba.tasks.dto.CreateTaskRequest;
import com.seba.tasks.dto.TaskDto;
import com.seba.tasks.dto.UpdateTaskRequest;
import com.seba.tasks.service.TaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;

@Slf4j
@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
public final class TasksController {

    private final TaskService taskService;

    @GetMapping
    public Flux<TaskDto> getAll() {
        return taskService.getAll();
    }

    @GetMapping("/{taskId}")
    public Mono<TaskDto> getById(@PathVariable UUID taskId) {
        return taskService.getById(taskId);
    }

    @PostMapping
    @ResponseStatus(CREATED)
    public Mono<TaskDto> create(@RequestBody CreateTaskRequest request) {
        return taskService.create(request.title(), request.createdBy());
    }

    @PatchMapping("/{taskId}")
    public Mono<TaskDto> update(@PathVariable UUID taskId, @RequestBody UpdateTaskRequest request) {
        return taskService.update(taskId, request.title(), request.status(), request.updatedBy());
    }

    @DeleteMapping("/{taskId}")
    @ResponseStatus(NO_CONTENT)
    public Mono<Void> delete(@PathVariable UUID taskId) {
        return taskService.delete(taskId);
    }
}
