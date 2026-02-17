package com.seba.tasks.controller;

import com.seba.tasks.TestcontainersConfiguration;
import com.seba.tasks.model.Task;
import com.seba.tasks.model.TaskStatus;
import com.seba.tasks.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
final class DependencyControllerIT {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private TaskRepository taskRepository;

    @BeforeEach
    void setUp() {
        taskRepository.deleteAll().block();
    }

    @Test
    void addDependency_validTasks_returns200WithBlockedStatus() {
        Task blocker = saveTask("Blocker", TaskStatus.TODO);
        Task dependent = saveTask("Dependent", TaskStatus.TODO);

        webTestClient.put()
                .uri("/tasks/{taskId}/dependencies/{blockerId}", dependent.getTaskId(), blocker.getTaskId())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("BLOCKED")
                .jsonPath("$.dependsOn.length()").isEqualTo(1)
                .jsonPath("$.dependsOn[0]").isEqualTo(blocker.getTaskId().toString());
    }

    @Test
    void addDependency_selfDependency_returns400() {
        Task task = saveTask("Task", TaskStatus.TODO);

        webTestClient.put()
                .uri("/tasks/{taskId}/dependencies/{blockerId}", task.getTaskId(), task.getTaskId())
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void addDependency_circularDependency_returns409() {
        Task taskA = saveTask("Task A", TaskStatus.TODO);
        Task taskB = saveTask("Task B", TaskStatus.TODO);

        // A depends on B
        webTestClient.put()
                .uri("/tasks/{taskId}/dependencies/{blockerId}", taskA.getTaskId(), taskB.getTaskId())
                .exchange()
                .expectStatus().isOk();

        // B depends on A -> cycle
        webTestClient.put()
                .uri("/tasks/{taskId}/dependencies/{blockerId}", taskB.getTaskId(), taskA.getTaskId())
                .exchange()
                .expectStatus().isEqualTo(409);
    }

    @Test
    void removeDependency_existing_returns200() {
        Task blocker = saveTask("Blocker", TaskStatus.TODO);
        Task dependent = saveTask("Dependent", TaskStatus.TODO);

        // Add dependency
        webTestClient.put()
                .uri("/tasks/{taskId}/dependencies/{blockerId}", dependent.getTaskId(), blocker.getTaskId())
                .exchange()
                .expectStatus().isOk();

        // Remove it
        webTestClient.delete()
                .uri("/tasks/{taskId}/dependencies/{blockerId}", dependent.getTaskId(), blocker.getTaskId())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("TODO")
                .jsonPath("$.dependsOn.length()").isEqualTo(0);
    }

    @Test
    void removeDependency_nonExistent_returns404() {
        Task taskA = saveTask("Task A", TaskStatus.TODO);
        Task taskB = saveTask("Task B", TaskStatus.TODO);

        webTestClient.delete()
                .uri("/tasks/{taskId}/dependencies/{blockerId}", taskA.getTaskId(), taskB.getTaskId())
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void cascadeUnblock_blockerCompleted_dependentMovesToTodo() {
        Task blocker = saveTask("Blocker", TaskStatus.TODO);
        Task dependent = saveTask("Dependent", TaskStatus.TODO);

        // Add dependency
        webTestClient.put()
                .uri("/tasks/{taskId}/dependencies/{blockerId}", dependent.getTaskId(), blocker.getTaskId())
                .exchange()
                .expectStatus().isOk();

        // Complete the blocker
        webTestClient.patch()
                .uri("/tasks/{taskId}", blocker.getTaskId())
                .bodyValue(Map.of("status", "DONE", "updatedBy", "seba"))
                .exchange()
                .expectStatus().isOk();

        // Verify dependent is now TODO
        webTestClient.get()
                .uri("/tasks/{taskId}", dependent.getTaskId())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("TODO");
    }

    @Test
    void statusGuard_blockedTask_rejectsStatusChange() {
        Task blocker = saveTask("Blocker", TaskStatus.TODO);
        Task dependent = saveTask("Dependent", TaskStatus.TODO);

        // Add dependency -> dependent becomes BLOCKED
        webTestClient.put()
                .uri("/tasks/{taskId}/dependencies/{blockerId}", dependent.getTaskId(), blocker.getTaskId())
                .exchange()
                .expectStatus().isOk();

        // Try to change status of blocked task
        webTestClient.patch()
                .uri("/tasks/{taskId}", dependent.getTaskId())
                .bodyValue(Map.of("status", "IN_PROGRESS", "updatedBy", "seba"))
                .exchange()
                .expectStatus().isEqualTo(409);
    }

    // --- helper ---

    private Task saveTask(String title, TaskStatus status) {
        Task task = new Task();
        task.setTaskId(UUID.randomUUID());
        task.setTitle(title);
        task.setStatus(status);
        task.setDependsOn(List.of());
        task.setCreatedBy("seba");
        task.setUpdatedBy("seba");
        Instant now = Instant.now();
        task.setCreatedAt(now);
        task.setUpdatedAt(now);
        return taskRepository.save(task).block();
    }
}
