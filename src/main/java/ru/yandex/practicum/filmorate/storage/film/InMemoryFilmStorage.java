package ru.yandex.practicum.filmorate.storage.film;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.Film;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class InMemoryFilmStorage implements FilmStorage {
    private final Map<Integer, Film> films = new HashMap<>();
    private final Map<Integer, Set<Integer>> likes = new HashMap<>();
    private int nextId = 1;

    @Override
    public List<Film> findAll() {
        return new ArrayList<>(films.values());
    }

    @Override
    public Film create(Film film) {
        film.setId(nextId++);
        films.put(film.getId(), film);
        likes.put(film.getId(), new HashSet<>());
        log.info("Создан фильм: {}", film);
        return film;
    }

    @Override
    public Film update(Film film) {
        films.put(film.getId(), film);
        if (!likes.containsKey(film.getId())) {
            likes.put(film.getId(), new HashSet<>());
        }
        log.info("Обновлен фильм: {}", film);
        return film;
    }

    @Override
    public Optional<Film> findById(Integer id) {
        return Optional.ofNullable(films.get(id));
    }

    @Override
    public void delete(Integer id) {
        films.remove(id);
        likes.remove(id);
        log.info("Удален фильм с id: {}", id);
    }

    @Override
    public Film addLike(Integer filmId, Integer userId) {
        Film film = films.get(filmId);
        if (film != null) {
            likes.get(filmId).add(userId);
            log.info("Пользователь {} поставил лайк фильму {}", userId, filmId);
        }
        return film;
    }

    @Override
    public Film removeLike(Integer filmId, Integer userId) {
        Film film = films.get(filmId);
        if (film != null) {
            likes.get(filmId).remove(userId);
            log.info("Пользователь {} удалил лайк с фильма {}", userId, filmId);
        }
        return film;
    }

    @Override
    public List<Film> getPopularFilms(int count) {
        return films.values().stream()
                .sorted((f1, f2) -> Integer.compare(
                        likes.getOrDefault(f2.getId(), Collections.emptySet()).size(),
                        likes.getOrDefault(f1.getId(), Collections.emptySet()).size()
                ))
                .limit(count)
                .collect(Collectors.toList());
    }

    public int getLikesCount(Integer filmId) {
        return likes.getOrDefault(filmId, Collections.emptySet()).size();
    }
}