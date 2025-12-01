package ru.yandex.practicum.filmorate.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Friendship {
    private Integer id;
    private Integer userId;
    private Integer friendId;
    private FriendshipStatus status;
    private LocalDateTime createdDate;
}