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
import java.util.Map;
import java.util.UUID;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
final class TasksControllerIT {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private TaskRepository taskRepository;

    @BeforeEach
    void setUp() {
        taskRepository.deleteAll().block();
    }

    // --- create ---

    @Test
    void create_validInput_returns201WithTodoStatus() {
        webTestClient.post().uri("/tasks")
                .bodyValue(Map.of("title", "Buy milk", "createdBy", "seba"))
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.taskId").isNotEmpty()
                .jsonPath("$.title").isEqualTo("Buy milk")
                .jsonPath("$.status").isEqualTo("TODO")
                .jsonPath("$.createdBy").isEqualTo("seba")
                .jsonPath("$.updatedBy").isEqualTo("seba")
                .jsonPath("$.createdAt").isNotEmpty()
                .jsonPath("$.updatedAt").isNotEmpty();
    }

    // --- getAll ---

    @Test
    void getAll_returnsAllTasks() {
        saveTask("Task 1");

        webTestClient.get().uri("/tasks")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(1)
                .jsonPath("$[0].title").isEqualTo("Task 1")
                .jsonPath("$[0].status").isEqualTo("TODO");
    }

    @Test
    void getAll_empty_returnsEmptyList() {
        webTestClient.get().uri("/tasks")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(0);
    }

    // --- getById ---

    @Test
    void getById_existingTask_returnsTask() {
        Task saved = saveTask("Task 1");

        webTestClient.get().uri("/tasks/{taskId}", saved.getTaskId())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.taskId").isEqualTo(saved.getTaskId().toString())
                .jsonPath("$.title").isEqualTo("Task 1");
    }

    @Test
    void getById_nonExisting_returns404() {
        webTestClient.get().uri("/tasks/{taskId}", UUID.randomUUID())
                .exchange()
                .expectStatus().isNotFound();
    }

    // --- update ---

    @Test
    void update_existingTask_returnsUpdatedFields() {
        Task saved = saveTask("Old title");

        webTestClient.patch().uri("/tasks/{taskId}", saved.getTaskId())
                .bodyValue(Map.of(
                        "title", "New title",
                        "status", "IN_PROGRESS",
                        "updatedBy", "seba"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.title").isEqualTo("New title")
                .jsonPath("$.status").isEqualTo("IN_PROGRESS")
                .jsonPath("$.updatedBy").isEqualTo("seba")
                .jsonPath("$.updatedAt").isNotEmpty();
    }

    @Test
    void update_nonExisting_returns404() {
        webTestClient.patch().uri("/tasks/{taskId}", UUID.randomUUID())
                .bodyValue(Map.of("title", "New", "updatedBy", "seba"))
                .exchange()
                .expectStatus().isNotFound();
    }

    // --- delete ---

    @Test
    void delete_existingTask_returns204() {
        Task saved = saveTask("To delete");

        webTestClient.delete().uri("/tasks/{taskId}", saved.getTaskId())
                .exchange()
                .expectStatus().isNoContent();

        webTestClient.get().uri("/tasks/{taskId}", saved.getTaskId())
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void delete_nonExisting_returns404() {
        webTestClient.delete().uri("/tasks/{taskId}", UUID.randomUUID())
                .exchange()
                .expectStatus().isNotFound();
    }

    // --- helper ---

    private Task saveTask(String title) {
        Task task = new Task();
        task.setTaskId(UUID.randomUUID());
        task.setTitle(title);
        task.setStatus(TaskStatus.TODO);
        task.setCreatedBy("seba");
        task.setUpdatedBy("seba");
        Instant now = Instant.now();
        task.setCreatedAt(now);
        task.setUpdatedAt(now);
        return taskRepository.save(task).block();
    }
}
