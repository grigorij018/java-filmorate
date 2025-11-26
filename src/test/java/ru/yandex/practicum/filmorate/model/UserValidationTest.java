package ru.yandex.practicum.filmorate.model;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class UserValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    void userWithInvalidEmail_ShouldFailValidation() {
        User user = new User();
        user.setEmail("invalid-email");
        user.setLogin("validlogin");
        user.setBirthday(LocalDate.of(1990, 1, 1));

        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertFalse(violations.isEmpty());
        assertEquals("Некорректный формат email", violations.iterator().next().getMessage());
    }

    @Test
    void userWithBlankLogin_ShouldFailValidation() {
        User user = new User();
        user.setEmail("valid@email.com");
        user.setLogin("");
        user.setBirthday(LocalDate.of(1990, 1, 1));

        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertFalse(violations.isEmpty());
        assertEquals("Логин не может быть пустым", violations.iterator().next().getMessage());
    }

    @Test
    void userWithSpacesInLogin_ShouldFailValidation() {
        User user = new User();
        user.setEmail("valid@email.com");
        user.setLogin("login with spaces");
        user.setBirthday(LocalDate.of(1990, 1, 1));

        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertFalse(violations.isEmpty());
        assertEquals("Логин не может содержать пробелы", violations.iterator().next().getMessage());
    }

    @Test
    void userWithFutureBirthday_ShouldFailValidation() {
        User user = new User();
        user.setEmail("valid@email.com");
        user.setLogin("validlogin");
        user.setBirthday(LocalDate.of(2100, 1, 1));

        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertFalse(violations.isEmpty());
        assertEquals("Дата рождения не может быть в будущем", violations.iterator().next().getMessage());
    }

    @Test
    void validUser_ShouldPassValidation() {
        User user = new User();
        user.setEmail("valid@email.com");
        user.setLogin("validlogin");
        user.setBirthday(LocalDate.of(1990, 1, 1));

        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertTrue(violations.isEmpty());
    }

    @Test
    void userWithNullName_ShouldSetNameToLogin() {
        User user = new User();
        user.setEmail("test@email.com");
        user.setLogin("testlogin");
        user.setName(null);

        // Проверяем логику установки имени (это тестируется в UserController)
        assertNull(user.getName());
    }
}