package com.seba.tasks.service;

import com.seba.tasks.error.exceptions.CircularDependencyException;
import com.seba.tasks.error.exceptions.DependencyNotFoundException;
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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

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
        when(taskRepository.findByTaskId(blockerId)).thenReturn(Mono.empty());

        StepVerifier.create(dependencyService.addDependency(taskId, blockerId))
                .expectError(TaskNotFoundException.class)
                .verify();
    }

    @Test
    void addDependency_directCycle_throwsCircularDependencyException() {
        UUID taskIdA = UUID.randomUUID();
        UUID taskIdB = UUID.randomUUID();
        Task taskA = buildTestTask(taskIdA, "Task A", TaskStatus.BLOCKED);
        taskA.setDependsOn(new ArrayList<>(List.of(taskIdB)));
        Task taskB = buildTestTask(taskIdB, "Task B", TaskStatus.TODO);

        when(taskRepository.findByTaskId(taskIdB)).thenReturn(Mono.just(taskB));
        when(taskRepository.findByTaskId(taskIdA)).thenReturn(Mono.just(taskA));

        StepVerifier.create(dependencyService.addDependency(taskIdB, taskIdA))
                .expectError(CircularDependencyException.class)
                .verify();
    }

    @Test
    void addDependency_transitiveCycle_throwsCircularDependencyException() {
        UUID taskIdA = UUID.randomUUID();
        UUID taskIdB = UUID.randomUUID();
        UUID taskIdC = UUID.randomUUID();

        Task taskA = buildTestTask(taskIdA, "Task A", TaskStatus.BLOCKED);
        taskA.setDependsOn(new ArrayList<>(List.of(taskIdB)));

        Task taskB = buildTestTask(taskIdB, "Task B", TaskStatus.BLOCKED);
        taskB.setDependsOn(new ArrayList<>(List.of(taskIdC)));

        Task taskC = buildTestTask(taskIdC, "Task C", TaskStatus.TODO);

        when(taskRepository.findByTaskId(taskIdC)).thenReturn(Mono.just(taskC));
        when(taskRepository.findByTaskId(taskIdA)).thenReturn(Mono.just(taskA));
        when(taskRepository.findByTaskId(taskIdB)).thenReturn(Mono.just(taskB));

        StepVerifier.create(dependencyService.addDependency(taskIdC, taskIdA))
                .expectError(CircularDependencyException.class)
                .verify();
    }

    @Test
    void removeDependency_existingDependency_removesAndRecalculatesStatus() {
        UUID taskId = UUID.randomUUID();
        UUID blockerId = UUID.randomUUID();
        Task task = buildTestTask(taskId, "Task A", TaskStatus.BLOCKED);
        task.setDependsOn(new ArrayList<>(List.of(blockerId)));

        when(taskRepository.findByTaskId(taskId)).thenReturn(Mono.just(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(dependencyService.removeDependency(taskId, blockerId))
                .expectNextMatches(dto ->
                        dto.dependsOn().isEmpty()
                                && dto.status() == TaskStatus.TODO)
                .verifyComplete();
    }

    @Test
    void removeDependency_otherBlockersRemain_staysBlocked() {
        UUID taskId = UUID.randomUUID();
        UUID blockerId1 = UUID.randomUUID();
        UUID blockerId2 = UUID.randomUUID();
        Task task = buildTestTask(taskId, "Task A", TaskStatus.BLOCKED);
        task.setDependsOn(new ArrayList<>(List.of(blockerId1, blockerId2)));

        Task blocker2 = buildTestTask(blockerId2, "Blocker 2", TaskStatus.TODO);

        when(taskRepository.findByTaskId(taskId)).thenReturn(Mono.just(task));
        when(taskRepository.findByTaskId(blockerId2)).thenReturn(Mono.just(blocker2));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(dependencyService.removeDependency(taskId, blockerId1))
                .expectNextMatches(dto ->
                        dto.dependsOn().size() == 1
                                && !dto.dependsOn().contains(blockerId1)
                                && dto.status() == TaskStatus.BLOCKED)
                .verifyComplete();
    }

    @Test
    void removeDependency_notDependent_throwsDependencyNotFoundException() {
        UUID taskId = UUID.randomUUID();
        UUID blockerId = UUID.randomUUID();
        Task task = buildTestTask(taskId, "Task A", TaskStatus.TODO);

        when(taskRepository.findByTaskId(taskId)).thenReturn(Mono.just(task));

        StepVerifier.create(dependencyService.removeDependency(taskId, blockerId))
                .expectError(DependencyNotFoundException.class)
                .verify();
    }
    // --- unblockDependents ---

    @Test
    void unblockDependents_allBlockersDone_movesToTodo() {
        UUID completedId = UUID.randomUUID();
        UUID dependentId = UUID.randomUUID();

        Task dependent = buildTestTask(dependentId, "Dependent", TaskStatus.BLOCKED);
        dependent.setDependsOn(new ArrayList<>(List.of(completedId)));

        Task completed = buildTestTask(completedId, "Completed", TaskStatus.DONE);

        when(taskRepository.findByDependsOnContaining(completedId))
                .thenReturn(Flux.just(dependent));
        when(taskRepository.findByTaskId(completedId)).thenReturn(Mono.just(completed));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(dependencyService.unblockDependents(completedId))
                .verifyComplete();

        verify(taskRepository).save(argThat(task ->
                task.getTaskId().equals(dependentId)
                && task.getStatus() == TaskStatus.TODO));
    }

    @Test
    void unblockDependents_someBlockersNotDone_staysBlocked() {
        UUID completedId = UUID.randomUUID();
        UUID otherBlockerId = UUID.randomUUID();
        UUID dependentId = UUID.randomUUID();

        Task dependent = buildTestTask(dependentId, "Dependent", TaskStatus.BLOCKED);
        dependent.setDependsOn(new ArrayList<>(List.of(completedId, otherBlockerId)));

        Task otherBlocker = buildTestTask(otherBlockerId, "Other", TaskStatus.IN_PROGRESS);

        when(taskRepository.findByDependsOnContaining(completedId))
                .thenReturn(Flux.just(dependent));
        when(taskRepository.findByTaskId(completedId))
                .thenReturn(Mono.just(buildTestTask(completedId, "Completed", TaskStatus.DONE)));
        when(taskRepository.findByTaskId(otherBlockerId))
                .thenReturn(Mono.just(otherBlocker));

        StepVerifier.create(dependencyService.unblockDependents(completedId))
                .verifyComplete();

        verify(taskRepository, never()).save(any());
    }

    @Test
    void unblockDependents_noDependents_completesWithoutSaving() {
        UUID completedId = UUID.randomUUID();

        when(taskRepository.findByDependsOnContaining(completedId))
                .thenReturn(Flux.empty());

        StepVerifier.create(dependencyService.unblockDependents(completedId))
                .verifyComplete();

        verify(taskRepository, never()).save(any());
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