package ru.yandex.practicum.filmorate.storage.review;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.filmorate.model.Review;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Repository
@Primary
@RequiredArgsConstructor
public class ReviewDbStorage implements ReviewStorage {

    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional(readOnly = true)
    public List<Review> findAll() {
        String sql = "SELECT * FROM reviews ORDER BY created_at DESC";
        List<Review> reviews = jdbcTemplate.query(sql, this::mapRowToReview);
        loadLikesDislikes(reviews);
        return reviews;
    }

    @Override
    @Transactional
    public Review create(Review review) {
        String sql = "INSERT INTO reviews (content, is_positive, user_id, film_id, created_at) " +
                "VALUES (?, ?, ?, ?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement stmt = connection.prepareStatement(sql, new String[]{"id"});
            stmt.setString(1, review.getContent());
            stmt.setBoolean(2, review.getIsPositive());
            stmt.setInt(3, review.getUserId());
            stmt.setInt(4, review.getFilmId());
            stmt.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
            return stmt;
        }, keyHolder);

        Integer reviewId = Objects.requireNonNull(keyHolder.getKey()).intValue();
        review.setReviewId(reviewId);

        log.info("Создан отзыв с ID: {}", reviewId);
        return findById(reviewId).orElse(review);
    }

    @Override
    @Transactional
    public Review update(Review review) {
        String sql = "UPDATE reviews SET content = ?, is_positive = ? WHERE id = ?";

        int updated = jdbcTemplate.update(sql,
                review.getContent(),
                review.getIsPositive(),
                review.getReviewId());

        if (updated == 0) {
            throw new RuntimeException("Отзыв с ID " + review.getReviewId() + " не найден");
        }

        log.info("Обновлен отзыв с ID: {}", review.getReviewId());
        return findById(review.getReviewId()).orElse(review);
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        String sql = "DELETE FROM reviews WHERE id = ?";
        int deleted = jdbcTemplate.update(sql, id);

        if (deleted == 0) {
            throw new RuntimeException("Отзыв с ID " + id + " не найден");
        }

        log.info("Удален отзыв с ID: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Review> findById(Integer id) {
        String sql = "SELECT * FROM reviews WHERE id = ?";

        try {
            Review review = jdbcTemplate.queryForObject(sql, this::mapRowToReview, id);
            if (review != null) {
                loadLikesDislikesForSingleReview(review);
            }
            return Optional.ofNullable(review);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Review> findByFilmId(Integer filmId, Integer count) {
        String sql = "SELECT * FROM reviews WHERE film_id = ? " +
                "ORDER BY (SELECT COUNT(*) FROM review_likes WHERE review_id = reviews.id AND is_like = true) - " +
                "(SELECT COUNT(*) FROM review_likes WHERE review_id = reviews.id AND is_like = false) DESC " +
                "LIMIT ?";

        List<Review> reviews = jdbcTemplate.query(sql, this::mapRowToReview, filmId, count);
        loadLikesDislikes(reviews);
        return reviews;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Review> findAllWithLimit(Integer count) {
        String sql = "SELECT * FROM reviews " +
                "ORDER BY (SELECT COUNT(*) FROM review_likes WHERE review_id = reviews.id AND is_like = true) - " +
                "(SELECT COUNT(*) FROM review_likes WHERE review_id = reviews.id AND is_like = false) DESC " +
                "LIMIT ?";

        List<Review> reviews = jdbcTemplate.query(sql, this::mapRowToReview, count);
        loadLikesDislikes(reviews);
        return reviews;
    }

    @Override
    @Transactional
    public void addLike(Integer reviewId, Integer userId) {
        removeReaction(reviewId, userId);
        String sql = "INSERT INTO review_likes (review_id, user_id, is_like) VALUES (?, ?, true)";
        jdbcTemplate.update(sql, reviewId, userId);
        log.info("Пользователь {} поставил лайк отзыву {}", userId, reviewId);
    }

    @Override
    @Transactional
    public void addDislike(Integer reviewId, Integer userId) {
        removeReaction(reviewId, userId);
        String sql = "INSERT INTO review_likes (review_id, user_id, is_like) VALUES (?, ?, false)";
        jdbcTemplate.update(sql, reviewId, userId);
        log.info("Пользователь {} поставил дизлайк отзыву {}", userId, reviewId);
    }

    @Override
    @Transactional
    public void removeLike(Integer reviewId, Integer userId) {
        String sql = "DELETE FROM review_likes WHERE review_id = ? AND user_id = ? AND is_like = true";
        jdbcTemplate.update(sql, reviewId, userId);
        log.info("Пользователь {} удалил лайк у отзыва {}", userId, reviewId);
    }

    @Override
    @Transactional
    public void removeDislike(Integer reviewId, Integer userId) {
        String sql = "DELETE FROM review_likes WHERE review_id = ? AND user_id = ? AND is_like = false";
        jdbcTemplate.update(sql, reviewId, userId);
        log.info("Пользователь {} удалил дизлайк у отзыва {}", userId, reviewId);
    }

    @Override
    @Transactional
    public void removeReaction(Integer reviewId, Integer userId) {
        String sql = "DELETE FROM review_likes WHERE review_id = ? AND user_id = ?";
        jdbcTemplate.update(sql, reviewId, userId);
    }

    @Override
    public boolean existsById(Integer id) {
        String sql = "SELECT COUNT(*) FROM reviews WHERE id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, id);
        return count != null && count > 0;
    }

    private Review mapRowToReview(ResultSet rs, int rowNum) throws SQLException {
        Review review = new Review();
        review.setReviewId(rs.getInt("id"));
        review.setContent(rs.getString("content"));
        review.setIsPositive(rs.getBoolean("is_positive"));
        review.setUserId(rs.getInt("user_id"));
        review.setFilmId(rs.getInt("film_id"));

        Timestamp timestamp = rs.getTimestamp("created_at");
        if (timestamp != null) {
            review.setCreatedAt(timestamp.toLocalDateTime());
        }

        review.setLikes(new HashSet<>());
        review.setDislikes(new HashSet<>());

        return review;
    }

    private void loadLikesDislikes(List<Review> reviews) {
        if (reviews.isEmpty()) return;

        List<Integer> reviewIds = reviews.stream()
                .map(Review::getReviewId)
                .collect(Collectors.toList());

        String placeholders = reviewIds.stream()
                .map(id -> "?")
                .collect(Collectors.joining(", "));

        String sql = String.format(
                "SELECT review_id, user_id, is_like FROM review_likes WHERE review_id IN (%s)",
                placeholders
        );

        Map<Integer, Review> reviewMap = reviews.stream()
                .collect(Collectors.toMap(Review::getReviewId, r -> r));

        jdbcTemplate.query(sql, reviewIds.toArray(), rs -> {
            Integer reviewId = rs.getInt("review_id");
            Integer userId = rs.getInt("user_id");
            Boolean isLike = rs.getBoolean("is_like");

            Review review = reviewMap.get(reviewId);
            if (review != null) {
                if (isLike) {
                    review.getLikes().add(userId);
                } else {
                    review.getDislikes().add(userId);
                }
            }
        });

        reviews.forEach(review -> review.setUseful(review.calculateUseful()));
    }

    private void loadLikesDislikesForSingleReview(Review review) {
        String sql = "SELECT user_id, is_like FROM review_likes WHERE review_id = ?";

        jdbcTemplate.query(sql, rs -> {
            Integer userId = rs.getInt("user_id");
            Boolean isLike = rs.getBoolean("is_like");

            if (isLike) {
                review.getLikes().add(userId);
            } else {
                review.getDislikes().add(userId);
            }
        }, review.getReviewId());

        review.setUseful(review.calculateUseful());
    }
}