package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final FilmStorage filmStorage;
    private final UserStorage userStorage;

    public List<Film> getRecommendations(Integer userId) {
        // Проверяем существование пользователя
        if (userStorage.findById(userId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Пользователь с ID " + userId + " не найден");
        }

        // Находим похожего пользователя
        Integer similarUserId = userStorage.findMostSimilarUser(userId);

        if (similarUserId == null) {
            log.info("Для пользователя {} не найдено похожих пользователей", userId);
            return List.of(); // Возвращаем пустой список
        }

        log.info("Для пользователя {} найден похожий пользователь {}",
                userId, similarUserId);

        // Получаем рекомендации
        return filmStorage.getRecommendedFilms(userId, similarUserId);
    }
}