package ru.yandex.practicum.filmorate.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock
    private FilmStorage filmStorage;

    @Mock
    private UserStorage userStorage;

    @InjectMocks
    private RecommendationService recommendationService;

    @Test
    void getRecommendations_WhenUserNotFound_ShouldThrowException() {
        when(userStorage.findById(1)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class,
                () -> recommendationService.getRecommendations(1));
    }

    @Test
    void getRecommendations_WhenNoSimilarUser_ShouldReturnEmptyList() {
        when(userStorage.findById(1)).thenReturn(Optional.of(new User()));
        when(userStorage.findMostSimilarUser(1)).thenReturn(null);

        List<Film> result = recommendationService.getRecommendations(1);

        assertTrue(result.isEmpty());
        verify(filmStorage, never()).getRecommendedFilms(anyInt(), anyInt());
    }
}