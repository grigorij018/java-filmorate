package ru.yandex.practicum.filmorate.storage.feed;

import ru.yandex.practicum.filmorate.model.FeedEvent;
import java.util.List;

public interface FeedStorage {

    FeedEvent createEvent(FeedEvent event);

    List<FeedEvent> getUserFeed(Integer userId);

    void deleteEventsByUserId(Integer userId);

    void deleteEventsByEntityId(Integer entityId, FeedEvent.EventType eventType);

    void createLikeEvent(Integer userId, Integer filmId, FeedEvent.Operation operation);

    void createFriendEvent(Integer userId, Integer friendId, FeedEvent.Operation operation);

    void createReviewEvent(Integer userId, Integer reviewId, FeedEvent.Operation operation);
}