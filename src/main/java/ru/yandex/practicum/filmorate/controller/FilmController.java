package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/films")
public class FilmController {
    private final Map<Integer, Film> films = new HashMap<>();
    private int nextId = 1;
    private static final LocalDate EARLY_RELEASE_DATE = LocalDate.of(1895, 12, 28);

    private void validateReleaseDate(Film film) {
        if (film.getReleaseDate().isBefore(EARLY_RELEASE_DATE)) {
            log.warn("Ошибка валидации даты релиза для фильма: {}", film.getName());
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Дата релиза — не раньше 28 декабря 1895 года"
            );
        }
    }

    @GetMapping
    public Collection<Film> findAll() {
        return films.values();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Film create(@Valid @RequestBody Film film) {
        validateReleaseDate(film);
        film.setId(nextId++);
        films.put(film.getId(), film);
        log.info("Создан фильм: {}", film);
        return film;
    }

    @PutMapping
    public Film update(@Valid @RequestBody Film film) {
        if (film.getId() == null) {
            log.warn("Попытка обновить фильм без ID");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ID фильма обязателен");
        }

        if (!films.containsKey(film.getId())) {
            log.warn("Попытка обновить несуществующий фильм с id: {}", film.getId());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Фильм не найден");
        }
        validateReleaseDate(film);
        films.put(film.getId(), film);
        log.info("Обновлен фильм: {}", film);
        return film;
    }
}