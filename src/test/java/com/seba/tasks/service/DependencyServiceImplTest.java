package com.seba.tasks.service;

import com.seba.tasks.error.exceptions.InvalidArgumentException;
import com.seba.tasks.error.exceptions.TaskNotFoundException;
import com.seba.tasks.model.Task;
import com.seba.tasks.model.TaskStatus;
import com.seba.tasks.repository.TaskRepository;
import com.seba.tasks.service.implementation.DependencyServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DependencyServiceImplTest {

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private DependencyServiceImpl dependencyService;

    @Test
    void addDependency_validTasks_addsBlockerAndSetsBlocked() {
        UUID taskId = UUID.randomUUID();
        UUID blockerId = UUID.randomUUID();
        Task task = buildTestTask(taskId, "Task A", TaskStatus.TODO);
        Task blocker = buildTestTask(blockerId, "Task B", TaskStatus.TODO);

        when(taskRepository.findByTaskId(taskId)).thenReturn(Mono.just(task));
        when(taskRepository.findByTaskId(blockerId)).thenReturn(Mono.just(blocker));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(dependencyService.addDependency(taskId, blockerId))
                .expectNextMatches(dto ->
                        dto.dependsOn().contains(blockerId)
                                && dto.status() == TaskStatus.BLOCKED)
                .verifyComplete();
    }

    @Test
    void addDependency_selfDependency_throwsInvalidArgumentException() {
        UUID taskId = UUID.randomUUID();

        StepVerifier.create(dependencyService.addDependency(taskId, taskId))
                .expectError(InvalidArgumentException.class)
                .verify();
    }

    @Test
    void addDependency_alreadyExists_returnsCurrentStateWithoutDuplicate() {
        UUID taskId = UUID.randomUUID();
        UUID blockerId = UUID.randomUUID();
        Task task = buildTestTask(taskId, "Task A", TaskStatus.BLOCKED);
        task.setDependsOn(new ArrayList<>(List.of(blockerId)));
        Task blocker = buildTestTask(blockerId, "Task B", TaskStatus.TODO);

        when(taskRepository.findByTaskId(taskId)).thenReturn(Mono.just(task));
        when(taskRepository.findByTaskId(blockerId)).thenReturn(Mono.just(blocker));

        StepVerifier.create(dependencyService.addDependency(taskId, blockerId))
                .expectNextMatches(dto ->
                        dto.dependsOn().size() == 1
                                && dto.dependsOn().contains(blockerId))
                .verifyComplete();
    }

    @Test
    void addDependency_blockerAlreadyDone_statusStaysTodo() {
        UUID taskId = UUID.randomUUID();
        UUID blockerId = UUID.randomUUID();
        Task task = buildTestTask(taskId, "Task A", TaskStatus.TODO);
        Task blocker = buildTestTask(blockerId, "Task B", TaskStatus.DONE);

        when(taskRepository.findByTaskId(taskId)).thenReturn(Mono.just(task));
        when(taskRepository.findByTaskId(blockerId)).thenReturn(Mono.just(blocker));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(dependencyService.addDependency(taskId, blockerId))
                .expectNextMatches(dto ->
                        dto.dependsOn().contains(blockerId)
                                && dto.status() == TaskStatus.TODO)
                .verifyComplete();
    }

    @Test
    void addDependency_taskNotFound_throwsTaskNotFoundException() {
        UUID taskId = UUID.randomUUID();
        UUID blockerId = UUID.randomUUID();

        when(taskRepository.findByTaskId(taskId)).thenReturn(Mono.empty());

        StepVerifier.create(dependencyService.addDependency(taskId, blockerId))
                .expectError(TaskNotFoundException.class)
                .verify();
    }

    // --- helper ---

    private static Task buildTestTask(UUID taskId, String title, TaskStatus status) {
        Task task = new Task();
        task.setId("mongo-id");
        task.setTaskId(taskId);
        task.setTitle(title);
        task.setStatus(status);
        task.setDependsOn(new ArrayList<>());
        task.setCreatedBy("seba");
        task.setUpdatedBy("seba");
        Instant now = Instant.now();
        task.setCreatedAt(now);
        task.setUpdatedAt(now);
        return task;
    }
}