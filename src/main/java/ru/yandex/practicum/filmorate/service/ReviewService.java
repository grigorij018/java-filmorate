package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import ru.yandex.practicum.filmorate.model.FeedEvent;
import ru.yandex.practicum.filmorate.model.Review;
import ru.yandex.practicum.filmorate.storage.feed.FeedStorage;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.review.ReviewStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewStorage reviewStorage;
    private final UserStorage userStorage;
    private final FilmStorage filmStorage;
    private final FeedStorage feedStorage;

    private void validateReview(Review review) {
        if (review.getContent() == null || review.getContent().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Содержание отзыва не может быть пустым"
            );
        }

        if (review.getIsPositive() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Тип отзыва обязателен (true - положительный, false - отрицательный)"
            );
        }

        if (review.getUserId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "ID пользователя обязателен"
            );
        }

        if (review.getFilmId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "ID фильма обязателен"
            );
        }

        if (review.getContent().length() > 5000) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Отзыв не должен превышать 5000 символов"
            );
        }
    }

    public Review create(Review review) {
        log.info("Создание нового отзыва пользователем {} для фильма {}",
                review.getUserId(), review.getFilmId());

        validateReview(review);

        validateUserAndFilmExist(review.getUserId(), review.getFilmId());

        if (hasUserReviewedFilm(review.getUserId(), review.getFilmId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    String.format("Пользователь %d уже оставил отзыв на фильм %d",
                            review.getUserId(), review.getFilmId())
            );
        }

        review.setUseful(0);

        Review createdReview = reviewStorage.create(review);
        log.info("Создан отзыв с ID: {}", createdReview.getReviewId());

        feedStorage.createReviewEvent(review.getUserId(), createdReview.getReviewId(), FeedEvent.Operation.ADD);

        return createdReview;
    }

    public Review update(Review review) {
        log.info("Обновление отзыва с ID: {}", review.getReviewId());

        if (review.getReviewId() == null || review.getReviewId() <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Отзыв не найден"
            );
        }

        validateReview(review);

        Review existingReview = reviewStorage.findById(review.getReviewId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Отзыв не найден"
                ));

        if (!existingReview.getUserId().equals(review.getUserId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    String.format("Пользователь %d не может редактировать отзыв пользователя %d",
                            review.getUserId(), existingReview.getUserId())
            );
        }

        review.setUseful(existingReview.getUseful());

        Review updatedReview = reviewStorage.update(review);
        log.info("Отзыв с ID {} обновлен", updatedReview.getReviewId());

        feedStorage.createReviewEvent(review.getUserId(), review.getReviewId(), FeedEvent.Operation.UPDATE);

        return updatedReview;
    }

    public void delete(Integer id) {
        log.info("Удаление отзыва с ID: {}", id);

        if (id == null || id <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Отзыв не найден"
            );
        }

        if (!reviewStorage.existsById(id)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Отзыв не найден"
            );
        }

        // Получаем отзыв перед удалением, чтобы узнать userId
        Review review = findById(id);

        reviewStorage.delete(id);
        log.info("Отзыв с ID {} удален", id);

        feedStorage.createReviewEvent(review.getUserId(), id, FeedEvent.Operation.REMOVE);
    }

    public Review findById(Integer id) {
        log.debug("Поиск отзыва с ID: {}", id);

        if (id == null || id <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Отзыв не найден"
            );
        }

        return reviewStorage.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Отзыв не найден"
                ));
    }

    public List<Review> findByFilmId(Integer filmId, Integer count) {
        if (filmId != null) {
            log.info("Поиск отзывов для фильма ID: {}, лимит: {}", filmId, count);

            if (filmId <= 0) {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Фильм не найден"
                );
            }

            if (filmStorage.findById(filmId).isEmpty()) {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Фильм не найден"
                );
            }

            int limit = count != null ? count : 10;
            return reviewStorage.findByFilmId(filmId, limit);

        } else {
            log.info("Поиск всех отзывов, лимит: {}", count);

            int limit = count != null ? count : 10;
            return reviewStorage.findAllWithLimit(limit);
        }
    }

    public void addLike(Integer reviewId, Integer userId) {
        log.info("Добавление лайка отзыву {} пользователем {}", reviewId, userId);

        validateReviewAndUserExist(reviewId, userId);

        Review review = reviewStorage.findById(reviewId).orElseThrow();
        if (review.getDislikes().contains(userId)) {
            reviewStorage.removeDislike(reviewId, userId);
        }

        reviewStorage.addLike(reviewId, userId);
        log.info("Лайк добавлен к отзыву {} пользователем {}", reviewId, userId);
    }

    public void addDislike(Integer reviewId, Integer userId) {
        log.info("Добавление дизлайка отзыву {} пользователем {}", reviewId, userId);

        validateReviewAndUserExist(reviewId, userId);

        Review review = reviewStorage.findById(reviewId).orElseThrow();
        if (review.getLikes().contains(userId)) {
            reviewStorage.removeLike(reviewId, userId);
        }

        reviewStorage.addDislike(reviewId, userId);
        log.info("Дизлайк добавлен к отзыву {} пользователем {}", reviewId, userId);
    }

    public void removeLike(Integer reviewId, Integer userId) {
        log.info("Удаление лайка отзыва {} пользователем {}", reviewId, userId);

        validateReviewAndUserExist(reviewId, userId);
        reviewStorage.removeLike(reviewId, userId);
        log.info("Лайк удален у отзыва {} пользователем {}", reviewId, userId);
    }

    public void removeDislike(Integer reviewId, Integer userId) {
        log.info("Удаление дизлайка отзыва {} пользователем {}", reviewId, userId);

        validateReviewAndUserExist(reviewId, userId);
        reviewStorage.removeDislike(reviewId, userId);
        log.info("Дизлайк удален у отзыва {} пользователем {}", reviewId, userId);
    }

    private void validateUserAndFilmExist(Integer userId, Integer filmId) {
        if (userStorage.findById(userId).isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Пользователь не найден"
            );
        }

        if (filmStorage.findById(filmId).isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Фильм не найден"
            );
        }
    }

    private void validateReviewAndUserExist(Integer reviewId, Integer userId) {
        if (reviewId == null || reviewId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Отзыв не найден"
            );
        }

        if (userId == null || userId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Пользователь не найден"
            );
        }

        if (!reviewStorage.existsById(reviewId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Отзыв не найден"
            );
        }

        if (userStorage.findById(userId).isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Пользователь не найден"
            );
        }
    }

    private boolean hasUserReviewedFilm(Integer userId, Integer filmId) {
        List<Review> userReviews = reviewStorage.findAll().stream()
                .filter(review -> review.getUserId().equals(userId)
                        && review.getFilmId().equals(filmId))
                .toList();

        return !userReviews.isEmpty();
    }

    public List<Review> findAll() {
        log.debug("Получение всех отзывов");
        return reviewStorage.findAll();
    }
}