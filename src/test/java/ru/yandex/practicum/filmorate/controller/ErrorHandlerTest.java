package ru.yandex.practicum.filmorate.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.server.ResponseStatusException;

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

        var response = errorHandler.handleResponseStatusException(ex);

        assertNotNull(response);
        assertEquals("404 NOT_FOUND", response.get("error"));
        assertEquals("Фильм не найден", response.get("message"));
    }

    @Test
    void handleOtherExceptions_ReturnsInternalServerError() {
        Exception ex = new Exception("Test exception");

        var response = errorHandler.handleOtherExceptions(ex);

        assertNotNull(response);
        assertEquals("Внутренняя ошибка сервера", response.get("error"));
    }
}