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

        // НЕ настраиваем моки для MPA и жанров - они не будут использоваться,
        // так как валидация даты релиза выбросит исключение раньше
        assertThrows(ResponseStatusException.class, () -> filmService.create(validFilm));

        // Проверяем, что методы валидации MPA и жанров не вызывались
        verify(mpaStorage, never()).findById(anyInt());
        verify(genreStorage, never()).findById(anyInt());
        verify(filmStorage, never()).create(any(Film.class));
    }

    @Test
    void createFilm_InvalidMpa_ShouldThrowException() {
        // Устанавливаем несуществующий MPA
        validFilm.getMpa().setId(999);
        when(mpaStorage.findById(999)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> filmService.create(validFilm));

        verify(mpaStorage, times(1)).findById(999);
        verify(genreStorage, never()).findById(anyInt());
    }

    @Test
    void createFilm_InvalidGenre_ShouldThrowException() {
        // Устанавливаем несуществующий жанр
        validFilm.setGenres(Set.of(new Genre(999, "Несуществующий")));
        when(mpaStorage.findById(1)).thenReturn(Optional.of(validFilm.getMpa()));
        when(genreStorage.findById(999)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> filmService.create(validFilm));

        verify(mpaStorage, times(1)).findById(1);
        verify(genreStorage, times(1)).findById(999);
        verify(filmStorage, never()).create(any(Film.class));
    }

    @Test
    void getCommonFilms_ShouldReturnFilmsWhenUsersExist() {
        // Arrange
        Integer userId = 1;
        Integer friendId = 2;

        User user = new User();
        user.setId(userId);
        User friend = new User();
        friend.setId(friendId);

        Film film1 = new Film();
        film1.setId(1);
        Film film2 = new Film();
        film2.setId(2);

        when(userStorage.findById(userId)).thenReturn(Optional.of(user));
        when(userStorage.findById(friendId)).thenReturn(Optional.of(friend));
        when(filmStorage.getCommonFilms(userId, friendId)).thenReturn(List.of(film1, film2));

        // Act
        List<Film> result = filmService.getCommonFilms(userId, friendId);

        // Assert
        assertEquals(2, result.size());
        verify(userStorage, times(2)).findById(anyInt());
        verify(filmStorage, times(1)).getCommonFilms(userId, friendId);
    }

    @Test
    void getCommonFilms_ShouldThrowExceptionWhenUserNotFound() {
        // Arrange
        Integer userId = 1;
        Integer friendId = 2;

        when(userStorage.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResponseStatusException.class, () -> {
            filmService.getCommonFilms(userId, friendId);
        });

        verify(userStorage, times(1)).findById(userId);
        verify(userStorage, never()).findById(friendId);
        verify(filmStorage, never()).getCommonFilms(anyInt(), anyInt());
    }

    @Test
    void getCommonFilms_ShouldThrowExceptionWhenFriendNotFound() {
        // Arrange
        Integer userId = 1;
        Integer friendId = 2;

        User user = new User();
        user.setId(userId);

        when(userStorage.findById(userId)).thenReturn(Optional.of(user));
        when(userStorage.findById(friendId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResponseStatusException.class, () -> {
            filmService.getCommonFilms(userId, friendId);
        });

        verify(userStorage, times(1)).findById(userId);
        verify(userStorage, times(1)).findById(friendId);
        verify(filmStorage, never()).getCommonFilms(anyInt(), anyInt());
    }
}