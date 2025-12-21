package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import ru.yandex.practicum.filmorate.model.FeedEvent;
import ru.yandex.practicum.filmorate.storage.feed.FeedStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeedService {

    private final FeedStorage feedStorage;
    private final UserStorage userStorage;

    public List<FeedEvent> getUserFeed(Integer userId) {
        validateUserExists(userId);
        log.info("Получение ленты событий пользователя с ID: {}", userId);
        return feedStorage.getUserFeed(userId);
    }

    public void addLikeEvent(Integer userId, Integer filmId) {
        FeedEvent event = FeedEvent.builder()
                .userId(userId)
                .entityId(filmId)
                .eventType(FeedEvent.EventType.LIKE)
                .operation(FeedEvent.Operation.ADD)
                .timestamp(Instant.now().toEpochMilli())
                .build();
        feedStorage.createEvent(event);
    }

    public void removeLikeEvent(Integer userId, Integer filmId) {
        FeedEvent event = FeedEvent.builder()
                .userId(userId)
                .entityId(filmId)
                .eventType(FeedEvent.EventType.LIKE)
                .operation(FeedEvent.Operation.REMOVE)
                .timestamp(Instant.now().toEpochMilli())
                .build();
        feedStorage.createEvent(event);
    }

    public void addFriendEvent(Integer userId, Integer friendId) {
        FeedEvent event = FeedEvent.builder()
                .userId(userId)
                .entityId(friendId)
                .eventType(FeedEvent.EventType.FRIEND)
                .operation(FeedEvent.Operation.ADD)
                .timestamp(Instant.now().toEpochMilli())
                .build();
        feedStorage.createEvent(event);
    }

    public void removeFriendEvent(Integer userId, Integer friendId) {
        FeedEvent event = FeedEvent.builder()
                .userId(userId)
                .entityId(friendId)
                .eventType(FeedEvent.EventType.FRIEND)
                .operation(FeedEvent.Operation.REMOVE)
                .timestamp(Instant.now().toEpochMilli())
                .build();
        feedStorage.createEvent(event);
    }

    public void addReviewEvent(Integer userId, Integer reviewId) {
        FeedEvent event = FeedEvent.builder()
                .userId(userId)
                .entityId(reviewId)
                .eventType(FeedEvent.EventType.REVIEW)
                .operation(FeedEvent.Operation.ADD)
                .timestamp(Instant.now().toEpochMilli())
                .build();
        feedStorage.createEvent(event);
    }

    public void updateReviewEvent(Integer userId, Integer reviewId) {
        FeedEvent event = FeedEvent.builder()
                .userId(userId)
                .entityId(reviewId)
                .eventType(FeedEvent.EventType.REVIEW)
                .operation(FeedEvent.Operation.UPDATE)
                .timestamp(Instant.now().toEpochMilli())
                .build();
        feedStorage.createEvent(event);
    }

    public void removeReviewEvent(Integer userId, Integer reviewId) {
        FeedEvent event = FeedEvent.builder()
                .userId(userId)
                .entityId(reviewId)
                .eventType(FeedEvent.EventType.REVIEW)
                .operation(FeedEvent.Operation.REMOVE)
                .timestamp(Instant.now().toEpochMilli())
                .build();
        log.info("Создание события REVIEW REMOVE: userId={}, reviewId={}, timestamp={}",
                userId, reviewId, event.getTimestamp());
        feedStorage.createEvent(event);
    }

    private void validateUserExists(Integer userId) {
        if (userStorage.findById(userId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Пользователь не найден");
        }
    }
}