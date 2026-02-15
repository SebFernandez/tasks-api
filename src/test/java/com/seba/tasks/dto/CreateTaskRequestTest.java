package com.seba.tasks.dto;

import com.seba.tasks.error.exceptions.InvalidArgumentException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class CreateTaskRequestTest {

    @Test
    void validInput_createsRecord() {
        CreateTaskRequest request = new CreateTaskRequest("Buy milk", "seba");

        assertEquals("Buy milk", request.title());
        assertEquals("seba", request.createdBy());
    }

    @Test
    void nullTitle_throwsInvalidArgumentException() {
        assertThrows(InvalidArgumentException.class,
                () -> new CreateTaskRequest(null, "seba"));
    }

    @Test
    void blankTitle_throwsInvalidArgumentException() {
        assertThrows(InvalidArgumentException.class,
                () -> new CreateTaskRequest("  ", "seba"));
    }

    @Test
    void nullCreatedBy_throwsInvalidArgumentException() {
        assertThrows(InvalidArgumentException.class,
                () -> new CreateTaskRequest("Buy milk", null));
    }

    @Test
    void blankCreatedBy_throwsInvalidArgumentException() {
        assertThrows(InvalidArgumentException.class,
                () -> new CreateTaskRequest("Buy milk", "  "));
    }
}
