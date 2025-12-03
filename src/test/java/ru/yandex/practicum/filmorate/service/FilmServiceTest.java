package ru.yandex.practicum.filmorate.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.MpaRating;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.GenreStorage;
import ru.yandex.practicum.filmorate.storage.MpaStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FilmServiceTest {

    @Mock
    private FilmStorage filmStorage;

    @Mock
    private UserStorage userStorage;

    @Mock
    private MpaStorage mpaStorage;

    @Mock
    private GenreStorage genreStorage;

    @InjectMocks
    private FilmService filmService;

    private Film validFilm;

    @BeforeEach
    void setUp() {
        validFilm = new Film();
        validFilm.setName("Test Film");
        validFilm.setDescription("Test Description");
        validFilm.setReleaseDate(LocalDate.of(2000, 1, 1));
        validFilm.setDuration(120);

        MpaRating mpa = new MpaRating();
        mpa.setId(1);
        mpa.setName("G");
        mpa.setDescription("No restrictions");
        validFilm.setMpa(mpa);

        validFilm.setGenres(Set.of(new Genre(1, "Комедия")));
    }

    @Test
    void createFilm_ValidFilm_ShouldCallStorageCreate() {
        // Настраиваем моки
        when(mpaStorage.findById(1)).thenReturn(Optional.of(validFilm.getMpa()));
        when(genreStorage.findById(1)).thenReturn(Optional.of(new Genre(1, "Комедия")));
        when(filmStorage.create(any(Film.class))).thenReturn(validFilm);

        Film result = filmService.create(validFilm);

        assertNotNull(result);
        verify(filmStorage, times(1)).create(validFilm);
        verify(mpaStorage, times(1)).findById(1);
        verify(genreStorage, times(1)).findById(1);
    }

    @Test
    void createFilm_TooEarlyReleaseDate_ShouldThrowException() {
        validFilm.setReleaseDate(LocalDate.of(1890, 1, 1));

        // Настраиваем моки для валидации MPA и жанров
        when(mpaStorage.findById(1)).thenReturn(Optional.of(validFilm.getMpa()));
        when(genreStorage.findById(1)).thenReturn(Optional.of(new Genre(1, "Комедия")));

        assertThrows(ResponseStatusException.class, () -> filmService.create(validFilm));
    }

    @Test
    void createFilm_InvalidMpa_ShouldThrowException() {
        // Устанавливаем несуществующий MPA
        validFilm.getMpa().setId(999);
        when(mpaStorage.findById(999)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> filmService.create(validFilm));
    }

    @Test
    void createFilm_InvalidGenre_ShouldThrowException() {
        // Устанавливаем несуществующий жанр
        validFilm.setGenres(Set.of(new Genre(999, "Несуществующий")));
        when(mpaStorage.findById(1)).thenReturn(Optional.of(validFilm.getMpa()));
        when(genreStorage.findById(999)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> filmService.create(validFilm));
    }
}