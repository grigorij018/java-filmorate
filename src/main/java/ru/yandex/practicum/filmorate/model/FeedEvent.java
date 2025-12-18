package ru.yandex.practicum.filmorate.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeedEvent {
    private Long timestamp;
    private Integer userId;
    private EventType eventType;
    private Operation operation;
    private Integer eventId;
    private Integer entityId;

    public enum EventType {
        LIKE,
        REVIEW,
        FRIEND
    }

    public enum Operation {
        ADD,
        REMOVE,
        UPDATE
    }
}