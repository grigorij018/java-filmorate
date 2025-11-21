package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import ru.yandex.practicum.filmorate.model.User;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/users")
public class UserController {
    private final Map<Integer, User> users = new HashMap<>();
    private int nextId = 1;

    private void setDisplayName(User user) {
        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
            log.debug("Имя пользователя было установлено как логин: {}", user.getLogin());
        }
    }

    @GetMapping
    public Collection<User> findAll() {
        return users.values();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public User create(@Valid @RequestBody User user) {
        setDisplayName(user);
        user.setId(nextId++);
        users.put(user.getId(), user);
        log.info("Создан пользователь: {}", user);
        return user;
    }

    @PutMapping
    public User update(@Valid @RequestBody User user) {
        if (user.getId() == null || !users.containsKey(user.getId())) {
            log.warn("Попытка обновить несуществующего пользователя с id: {}", user.getId());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Пользователь не найден");
        }
        setDisplayName(user);
        users.put(user.getId(), user);
        log.info("Обновлен пользователь: {}", user);
        return user;
    }
}