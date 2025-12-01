package ru.yandex.practicum.filmorate.storage.film;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.MpaRating;
import java.util.*;
import java.util.stream.Collectors;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.model.Film;

@Slf4j
@Component
public abstract class InMemoryFilmStorage implements FilmStorage {
    private final Map<Integer, Film> films = new HashMap<>();
    private final Map<Integer, Set<Integer>> likes = new HashMap<>();
    private final Map<Integer, Set<Genre>> filmGenres = new HashMap<>();
    private int nextId = 1;

    // Пример данных для MPA рейтингов
    private final Map<Integer, MpaRating> mpaRatings = Map.of(
            1, new MpaRating(1, "G", "У фильма нет возрастных ограничений"),
            2, new MpaRating(2, "PG", "Детям рекомендуется смотреть фильм с родителями"),
            3, new MpaRating(3, "PG-13", "Детям до 13 лет просмотр не желателен"),
            4, new MpaRating(4, "R", "Лицам до 17 лет просматривать фильм можно только в присутствии взрослого"),
            5, new MpaRating(5, "NC-17", "Лицам до 18 лет просмотр запрещён")
    );

    @Override
    public Film create(Film film) {
        film.setId(nextId++);
        films.put(film.getId(), film);
        likes.put(film.getId(), new HashSet<>());
        filmGenres.put(film.getId(), new HashSet<>());

        // Если у фильма есть жанры, сохраняем их
        if (film.getGenres() != null) {
            filmGenres.put(film.getId(), new HashSet<>(film.getGenres()));
        }

        log.info("Создан фильм: {}", film);
        return film;
    }

    @Override
    public Film update(Film film) {
        films.put(film.getId(), film);
        if (!likes.containsKey(film.getId())) {
            likes.put(film.getId(), new HashSet<>());
        }

        // Обновляем жанры фильма
        if (film.getGenres() != null) {
            filmGenres.put(film.getId(), new HashSet<>(film.getGenres()));
        } else {
            filmGenres.put(film.getId(), new HashSet<>());
        }

        log.info("Обновлен фильм: {}", film);
        return film;
    }

    // Добавьте метод для получения жанров фильма
    public Set<Genre> getFilmGenres(Integer filmId) {
        return filmGenres.getOrDefault(filmId, Collections.emptySet());
    }

    // Добавьте метод для добавления жанра к фильму
    public void addGenreToFilm(Integer filmId, Genre genre) {
        if (films.containsKey(filmId)) {
            filmGenres.computeIfAbsent(filmId, k -> new HashSet<>()).add(genre);
        }
    }
}