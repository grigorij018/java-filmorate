package ru.yandex.practicum.filmorate.storage.feed;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.filmorate.model.FeedEvent;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
public class FeedDbStorage implements FeedStorage {

    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional(readOnly = true)
    public List<FeedEvent> getUserFeed(Integer userId) {
        String sql = "SELECT * FROM feed_events WHERE user_id = ? ORDER BY event_id ASC";

        return jdbcTemplate.query(sql, this::mapRowToFeedEvent, userId);
    }

    @Override
    @Transactional
    public void deleteEventsByUserId(Integer userId) {
        String sql = "DELETE FROM feed_events WHERE user_id = ?";
        jdbcTemplate.update(sql, userId);
        log.info("Удалены события пользователя с ID: {}", userId);
    }

    @Override
    @Transactional
    public void deleteEventsByEntityId(Integer entityId, FeedEvent.EventType eventType) {
        String sql = "DELETE FROM feed_events WHERE entity_id = ? AND event_type = ?";
        jdbcTemplate.update(sql, entityId, eventType.name());
        log.info("Удалены события для сущности ID: {}, тип: {}", entityId, eventType);
    }

    private FeedEvent mapRowToFeedEvent(ResultSet rs, int rowNum) throws SQLException {
        return FeedEvent.builder()
                .eventId(rs.getInt("event_id"))
                .userId(rs.getInt("user_id"))
                .entityId(rs.getInt("entity_id"))
                .eventType(FeedEvent.EventType.valueOf(rs.getString("event_type")))
                .operation(FeedEvent.Operation.valueOf(rs.getString("operation")))
                .timestamp(rs.getLong("timestamp"))
                .build();
    }

    @Override
    @Transactional
    public FeedEvent createEvent(FeedEvent event) {
        String sql = "INSERT INTO feed_events (user_id, entity_id, event_type, operation, timestamp) " +
                "VALUES (?, ?, ?, ?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement stmt = connection.prepareStatement(sql, new String[]{"event_id"});
            stmt.setInt(1, event.getUserId());
            stmt.setInt(2, event.getEntityId());
            stmt.setString(3, event.getEventType().name());
            stmt.setString(4, event.getOperation().name());
            stmt.setLong(5, event.getTimestamp());
            return stmt;
        }, keyHolder);

        event.setEventId(keyHolder.getKey().intValue());
        return event;
    }

    @Override
    @Transactional
    public void createLikeEvent(Integer userId, Integer filmId, FeedEvent.Operation operation) {
        FeedEvent event = FeedEvent.builder()
                .userId(userId)
                .entityId(filmId)
                .eventType(FeedEvent.EventType.LIKE)
                .operation(operation)
                .timestamp(Instant.now().toEpochMilli())
                .build();
        createEvent(event);
        log.info("Создано событие LIKE: операция={}, пользователь={}, фильм={}, eventId={}",
                operation, userId, filmId, event.getEventId());
    }

    @Override
    @Transactional
    public void createFriendEvent(Integer userId, Integer friendId, FeedEvent.Operation operation) {
        FeedEvent event = FeedEvent.builder()
                .userId(userId)
                .entityId(friendId)
                .eventType(FeedEvent.EventType.FRIEND)
                .operation(operation)
                .timestamp(Instant.now().toEpochMilli())
                .build();
        createEvent(event);
        log.info("Создано событие FRIEND: операция={}, пользователь={}, друг={}",
                operation, userId, friendId);
    }

    @Override
    @Transactional
    public void createReviewEvent(Integer userId, Integer reviewId, FeedEvent.Operation operation) {
        FeedEvent event = FeedEvent.builder()
                .userId(userId)
                .entityId(reviewId)
                .eventType(FeedEvent.EventType.REVIEW)
                .operation(operation)
                .timestamp(Instant.now().toEpochMilli())
                .build();
        createEvent(event);
        log.info("Создано событие REVIEW: операция={}, пользователь={}, отзыв={}",
                operation, userId, reviewId);
    }
}