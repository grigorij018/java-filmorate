package ru.yandex.practicum.filmorate.storage.review;

import ru.yandex.practicum.filmorate.model.Review;

import java.util.List;
import java.util.Optional;

public interface ReviewStorage {
    List<Review> findAll();

    Review create(Review review);

    Review update(Review review);

    void delete(Integer id);

    Optional<Review> findById(Integer id);

    List<Review> findByFilmId(Integer filmId, Integer count);

    List<Review> findAllWithLimit(Integer count);

    void addLike(Integer reviewId, Integer userId);

    void addDislike(Integer reviewId, Integer userId);

    void removeLike(Integer reviewId, Integer userId);

    void removeDislike(Integer reviewId, Integer userId);

    void removeReaction(Integer reviewId, Integer userId);

    boolean existsById(Integer id);

    boolean existsByUserIdAndFilmId(Integer userId, Integer filmId);
}