package ru.yandex.practicum.filmorate.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ErrorHandlerTest {

    @Autowired
    private ErrorHandler errorHandler;

    @Test
    void handleResponseStatusException_ReturnsCorrectResponse() {
        ResponseStatusException ex = new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Фильм не найден"
        );

        ResponseEntity<Map<String, String>> response = errorHandler.handleResponseStatusException(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("404 NOT_FOUND", response.getBody().get("error"));
        assertEquals("Фильм не найден", response.getBody().get("message"));
    }

    @Test
    void handleOtherExceptions_ReturnsInternalServerError() {
        Exception ex = new Exception("Test exception");

        Map<String, String> response = errorHandler.handleOtherExceptions(ex);

        assertNotNull(response);
        assertEquals("Внутренняя ошибка сервера", response.get("error"));
    }
}