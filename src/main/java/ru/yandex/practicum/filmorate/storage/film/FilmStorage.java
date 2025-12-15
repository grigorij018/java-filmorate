package ru.yandex.practicum.filmorate.storage.film;

import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.filmorate.model.Film;

import java.util.List;
import java.util.Optional;

public interface FilmStorage {

    @Transactional(readOnly = true)
    List<Film> findAll();

    @Transactional
    Film create(Film film);

    @Transactional
    Film update(Film film);

    @Transactional(readOnly = true)
    Optional<Film> findById(Integer id);

    @Transactional
    void delete(Integer id);

    @Transactional
    Film addLike(Integer filmId, Integer userId);

    @Transactional
    Film removeLike(Integer filmId, Integer userId);

    @Transactional(readOnly = true)
    List<Film> searchFilms(String query, boolean searchByDirector, boolean searchByTitle);

    @Transactional(readOnly = true)
    List<Film> getPopularFilms(int count, Integer genreId, Integer year);

    @Transactional
    Film addDirector(Integer filmId, Integer directorId);

    @Transactional
    List<Film> getDirectorsFilms(Integer directorId);
}