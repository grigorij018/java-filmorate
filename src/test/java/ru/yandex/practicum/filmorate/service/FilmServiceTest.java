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
import java.time.LocalDate;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FilmServiceTest {

    @Mock
    private FilmStorage filmStorage;

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
        validFilm.setMpa(new MpaRating(1, "G", "No restrictions"));
        validFilm.setGenres(Set.of(new Genre(1, "Комедия")));
    }

    @Test
    void createFilm_ValidFilm_ShouldCallStorageCreate() {
        when(filmStorage.create(any(Film.class))).thenReturn(validFilm);

        Film result = filmService.create(validFilm);

        assertNotNull(result);
        verify(filmStorage, times(1)).create(validFilm);
    }

    @Test
    void createFilm_TooEarlyReleaseDate_ShouldThrowException() {
        validFilm.setReleaseDate(LocalDate.of(1890, 1, 1));

        assertThrows(ResponseStatusException.class, () -> filmService.create(validFilm));
    }
}