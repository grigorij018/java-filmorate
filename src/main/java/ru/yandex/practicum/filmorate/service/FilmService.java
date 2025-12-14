package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.GenreStorage;
import ru.yandex.practicum.filmorate.storage.MpaStorage;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FilmService {
    private static final LocalDate EARLY_RELEASE_DATE = LocalDate.of(1895, 12, 28);

    private final FilmStorage filmStorage;
    private final UserStorage userStorage;
    private final MpaStorage mpaStorage;
    private final GenreStorage genreStorage;

    public List<Film> findAll() {
        return filmStorage.findAll();
    }

    public Film create(Film film) {
        validateReleaseDate(film);
        validateMpa(film);
        validateGenres(film);
        return filmStorage.create(film);
    }

    public Film update(Film film) {
        if (film.getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ID фильма обязателен");
        }
        validateReleaseDate(film);
        validateMpa(film);
        validateGenres(film);

        if (filmStorage.findById(film.getId()).isEmpty()) {
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

    public List<Film> searchFilms(String query, String by) {

        if (query == null || query.isBlank()) {
            return getPopularFilms(10);
        }

        if (by == null || by.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "При поиске по подстроке должен быть задан by");
        }

        Set<String> fields = Arrays.stream(by.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        boolean searchByDirector = fields.contains("director");
        boolean searchByTitle = fields.contains("title");

        if (!searchByDirector && !searchByTitle) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "by должен содержать director, title или оба");
        }

        return filmStorage.searchFilms(query, searchByDirector, searchByTitle);
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

    private void validateMpa(Film film) {
        if (film.getMpa() == null || film.getMpa().getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MPA рейтинг обязателен");
        }

        boolean mpaExists = mpaStorage.findById(film.getMpa().getId()).isPresent();
        if (!mpaExists) {
            log.warn("MPA рейтинг с ID {} не найден", film.getMpa().getId());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Рейтинг MPA не найден");
        }
    }

    private void validateGenres(Film film) {
        if (film.getGenres() != null && !film.getGenres().isEmpty()) {
            for (var genre : film.getGenres()) {
                boolean genreExists = genreStorage.findById(genre.getId()).isPresent();
                if (!genreExists) {
                    log.warn("Жанр с ID {} не найден", genre.getId());
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Жанр не найден");
                }
            }
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