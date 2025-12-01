package ru.yandex.practicum.filmorate.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;
import java.time.LocalDate;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserStorage userStorage;

    @InjectMocks
    private UserService userService;

    private User validUser;

    @BeforeEach
    void setUp() {
        validUser = new User();
        validUser.setId(1);
        validUser.setEmail("test@email.com");
        validUser.setLogin("testlogin");
        validUser.setName("Test User");
        validUser.setBirthday(LocalDate.of(1990, 1, 1));
    }

    @Test
    void createUser_WhenNameIsEmpty_ShouldSetNameToLogin() {
        validUser.setName("");
        when(userStorage.create(any(User.class))).thenReturn(validUser);

        User result = userService.create(validUser);

        assertEquals("testlogin", result.getName());
    }

    @Test
    void createUser_WhenNameIsNull_ShouldSetNameToLogin() {
        validUser.setName(null);
        when(userStorage.create(any(User.class))).thenReturn(validUser);

        User result = userService.create(validUser);

        assertEquals("testlogin", result.getName());
    }

    @Test
    void updateUser_NonExistentUser_ShouldThrowException() {
        when(userStorage.findById(1)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> userService.update(validUser));
    }
}