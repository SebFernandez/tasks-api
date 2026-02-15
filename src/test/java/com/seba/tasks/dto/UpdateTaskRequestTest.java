package com.seba.tasks.dto;

import com.seba.tasks.error.exceptions.InvalidArgumentException;
import com.seba.tasks.model.TaskStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class UpdateTaskRequestTest {

    @Test
    void validInput_createsRecord() {
        UpdateTaskRequest request = new UpdateTaskRequest("New title", TaskStatus.DONE, "seba");

        assertEquals("New title", request.title());
        assertEquals(TaskStatus.DONE, request.status());
        assertEquals("seba", request.updatedBy());
    }

    @Test
    void nullTitleAndStatus_allowed() {
        UpdateTaskRequest request = new UpdateTaskRequest(null, null, "seba");

        assertNull(request.title());
        assertNull(request.status());
        assertEquals("seba", request.updatedBy());
    }

    @Test
    void nullUpdatedBy_throwsInvalidArgumentException() {
        assertThrows(InvalidArgumentException.class,
                () -> new UpdateTaskRequest("Title", TaskStatus.TODO, null));
    }

    @Test
    void blankUpdatedBy_throwsInvalidArgumentException() {
        assertThrows(InvalidArgumentException.class,
                () -> new UpdateTaskRequest("Title", TaskStatus.TODO, "  "));
    }
}
