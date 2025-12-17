package ru.yandex.practicum.filmorate.model;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
public class Review {
    private Integer reviewId;
    private String content;
    private Boolean isPositive;

    @NotNull(message = "ID пользователя обязателен")
    private Integer userId;

    @NotNull(message = "ID фильма обязателен")
    private Integer filmId;
    private Integer useful = 0;
    private LocalDateTime createdAt;
    private Set<Integer> likes = new HashSet<>();
    private Set<Integer> dislikes = new HashSet<>();

    public Integer calculateUseful() {
        return (likes != null ? likes.size() : 0) - (dislikes != null ? dislikes.size() : 0);
    }
}