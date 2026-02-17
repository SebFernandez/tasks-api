package com.seba.tasks.repository;

import com.seba.tasks.model.Task;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@Repository
public interface TaskRepository extends ReactiveMongoRepository<Task, String> {
    Mono<Task> findByTaskId(UUID taskId);

    Mono<Boolean> existsByTaskId(UUID taskId);

    Mono<Void> deleteByTaskId(UUID taskId);

    Flux<Task> findByDependsOnContaining(UUID taskId);
}
