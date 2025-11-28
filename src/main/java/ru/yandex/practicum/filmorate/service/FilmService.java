package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FilmService {
    private static final LocalDate EARLY_RELEASE_DATE = LocalDate.of(1895, 12, 28);

    private final FilmStorage filmStorage;
    private final UserStorage userStorage;

    public List<Film> findAll() {
        return filmStorage.findAll();
    }

    public Film create(Film film) {
        validateReleaseDate(film);
        return filmStorage.create(film);
    }

    public Film update(Film film) {
        validateReleaseDate(film);
        if (film.getId() == null || filmStorage.findById(film.getId()).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Фильм не найден");
        }
        return filmStorage.update(film);
    }

    public Film findById(Integer id) {
        return filmStorage.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Фильм не найден"));
    }

    public Film addLike(Integer filmId, Integer userId) {
        validateFilmAndUserExist(filmId, userId);
        return filmStorage.addLike(filmId, userId);
    }

    public Film removeLike(Integer filmId, Integer userId) {
        validateFilmAndUserExist(filmId, userId);
        return filmStorage.removeLike(filmId, userId);
    }

    public List<Film> getPopularFilms(Integer count) {
        int filmsCount = count != null ? count : 10;
        return filmStorage.getPopularFilms(filmsCount);
    }

    private void validateReleaseDate(Film film) {
        if (film.getReleaseDate().isBefore(EARLY_RELEASE_DATE)) {
            log.warn("Ошибка валидации даты релиза для фильма: {}", film.getName());
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Дата релиза — не раньше 28 декабря 1895 года"
            );
        }
    }

    private void validateFilmAndUserExist(Integer filmId, Integer userId) {
        if (filmStorage.findById(filmId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Фильм не найден");
        }
        if (userStorage.findById(userId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Пользователь не найден");
        }
    }
}
