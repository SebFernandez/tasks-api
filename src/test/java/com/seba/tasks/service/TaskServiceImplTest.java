package com.seba.tasks.service;

import com.seba.tasks.dto.TaskDto;
import com.seba.tasks.error.exceptions.TaskNotFoundException;
import com.seba.tasks.model.Task;
import com.seba.tasks.model.TaskStatus;
import com.seba.tasks.repository.TaskRepository;
import com.seba.tasks.service.implementation.TaskServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceImplTest {

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private TaskServiceImpl taskService;

    // --- create ---

    @Test
    void create_validInput_savesWithTodoStatusAndAuditFields() {
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
            Task t = invocation.getArgument(0);
            t.setId("mongo-id");
            return Mono.just(t);
        });

        StepVerifier.create(taskService.create("Buy milk", "seba"))
                .expectNextMatches(dto ->
                        dto.title().equals("Buy milk")
                        && dto.status() == TaskStatus.TODO
                        && dto.createdBy().equals("seba")
                        && dto.updatedBy().equals("seba")
                        && dto.createdAt() != null
                        && dto.updatedAt() != null
                        && dto.taskId() != null)
                .verifyComplete();

        verify(taskRepository).save(any(Task.class));
    }

    // --- getById ---

    @Test
    void getById_existingTaskId_returnsTaskDto() {
        UUID taskId = UUID.randomUUID();
        Task task = buildTestTask(taskId, "Buy milk");

        when(taskRepository.findByTaskId(taskId)).thenReturn(Mono.just(task));

        StepVerifier.create(taskService.getById(taskId))
                .expectNextMatches(dto ->
                        dto.taskId().equals(taskId)
                        && dto.title().equals("Buy milk"))
                .verifyComplete();
    }

    @Test
    void getById_nonExistingTaskId_throwsTaskNotFoundException() {
        UUID taskId = UUID.randomUUID();

        when(taskRepository.findByTaskId(taskId)).thenReturn(Mono.empty());

        StepVerifier.create(taskService.getById(taskId))
                .expectError(TaskNotFoundException.class)
                .verify();
    }

    // --- getAll ---

    @Test
    void getAll_multipleTasks_returnsAllMapped() {
        Task t1 = buildTestTask(UUID.randomUUID(), "Task 1");
        Task t2 = buildTestTask(UUID.randomUUID(), "Task 2");

        when(taskRepository.findAll()).thenReturn(Flux.just(t1, t2));

        StepVerifier.create(taskService.getAll())
                .expectNextMatches(dto -> dto.title().equals("Task 1"))
                .expectNextMatches(dto -> dto.title().equals("Task 2"))
                .verifyComplete();
    }

    @Test
    void getAll_empty_returnsEmptyFlux() {
        when(taskRepository.findAll()).thenReturn(Flux.empty());

        StepVerifier.create(taskService.getAll())
                .verifyComplete();
    }

    // --- update ---

    @Test
    void update_fullUpdate_updatesAllFieldsAndAudit() {
        UUID taskId = UUID.randomUUID();
        Task existing = buildTestTask(taskId, "Old title");

        when(taskRepository.findByTaskId(taskId)).thenReturn(Mono.just(existing));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(taskService.update(taskId, "New title", TaskStatus.IN_PROGRESS, "seba"))
                .expectNextMatches(dto ->
                        dto.title().equals("New title")
                        && dto.status() == TaskStatus.IN_PROGRESS
                        && dto.updatedBy().equals("seba")
                        && dto.updatedAt() != null)
                .verifyComplete();
    }

    @Test
    void update_onlyTitle_statusUnchanged() {
        UUID taskId = UUID.randomUUID();
        Task existing = buildTestTask(taskId, "Old title");
        existing.setStatus(TaskStatus.WAITING);

        when(taskRepository.findByTaskId(taskId)).thenReturn(Mono.just(existing));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(taskService.update(taskId, "New title", null, "seba"))
                .expectNextMatches(dto ->
                        dto.title().equals("New title")
                        && dto.status() == TaskStatus.WAITING)
                .verifyComplete();
    }

    @Test
    void update_onlyStatus_titleUnchanged() {
        UUID taskId = UUID.randomUUID();
        Task existing = buildTestTask(taskId, "Keep this title");

        when(taskRepository.findByTaskId(taskId)).thenReturn(Mono.just(existing));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(taskService.update(taskId, null, TaskStatus.DONE, "seba"))
                .expectNextMatches(dto ->
                        dto.title().equals("Keep this title")
                        && dto.status() == TaskStatus.DONE)
                .verifyComplete();
    }

    @Test
    void update_nonExistingTaskId_throwsTaskNotFoundException() {
        UUID taskId = UUID.randomUUID();

        when(taskRepository.findByTaskId(taskId)).thenReturn(Mono.empty());

        StepVerifier.create(taskService.update(taskId, "Title", TaskStatus.TODO, "seba"))
                .expectError(TaskNotFoundException.class)
                .verify();
    }

    // --- delete ---

    @Test
    void delete_existingTaskId_completes() {
        UUID taskId = UUID.randomUUID();

        when(taskRepository.existsByTaskId(taskId)).thenReturn(Mono.just(true));
        when(taskRepository.deleteByTaskId(taskId)).thenReturn(Mono.empty());

        StepVerifier.create(taskService.delete(taskId))
                .verifyComplete();

        verify(taskRepository).deleteByTaskId(taskId);
    }

    @Test
    void delete_nonExistingTaskId_throwsTaskNotFoundException() {
        UUID taskId = UUID.randomUUID();

        when(taskRepository.existsByTaskId(taskId)).thenReturn(Mono.just(false));

        StepVerifier.create(taskService.delete(taskId))
                .expectError(TaskNotFoundException.class)
                .verify();

        verify(taskRepository, never()).deleteByTaskId(any());
    }

    // --- helper ---

    private static Task buildTestTask(UUID taskId, String title) {
        Task task = new Task();
        task.setId("mongo-id");
        task.setTaskId(taskId);
        task.setTitle(title);
        task.setStatus(TaskStatus.TODO);
        task.setCreatedBy("seba");
        task.setUpdatedBy("seba");
        Instant now = Instant.now();
        task.setCreatedAt(now);
        task.setUpdatedAt(now);
        return task;
    }
}
