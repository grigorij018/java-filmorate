package ru.yandex.practicum.filmorate.storage.user;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.model.FriendshipStatus;
import java.util.*;
import java.util.stream.Collectors;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;
import ru.yandex.practicum.filmorate.model.User;

@Deprecated
@Slf4j
@Component
public abstract class InMemoryUserStorage implements UserStorage {
    private final Map<Integer, User> users = new HashMap<>();
    private final Map<Integer, Map<Integer, FriendshipStatus>> friendships = new HashMap<>();
    private int nextId = 1;

    @Override
    public User create(User user) {
        user.setId(nextId++);
        users.put(user.getId(), user);
        friendships.put(user.getId(), new HashMap<>());
        log.info("Создан пользователь: {}", user);
        return user;
    }

    @Override
    public User update(User user) {
        users.put(user.getId(), user);
        if (!friendships.containsKey(user.getId())) {
            friendships.put(user.getId(), new HashMap<>());
        }
        log.info("Обновлен пользователь: {}", user);
        return user;
    }

    @Override
    public void addFriend(Integer userId, Integer friendId) {
        if (users.containsKey(userId) && users.containsKey(friendId)) {
            // Устанавливаем статус PENDING для инициатора
            friendships.get(userId).put(friendId, FriendshipStatus.PENDING);
            log.info("Пользователь {} отправил запрос на дружбу пользователю {}", userId, friendId);
        }
    }

    // Новый метод для подтверждения дружбы
    public void confirmFriendship(Integer userId, Integer friendId) {
        if (friendships.containsKey(userId) && friendships.containsKey(friendId)) {
            friendships.get(userId).put(friendId, FriendshipStatus.CONFIRMED);
            friendships.get(friendId).put(userId, FriendshipStatus.CONFIRMED);
            log.info("Пользователи {} и {} теперь друзья (подтверждено)", userId, friendId);
        }
    }

    @Override
    public void removeFriend(Integer userId, Integer friendId) {
        if (friendships.containsKey(userId)) {
            friendships.get(userId).remove(friendId);
        }
        if (friendships.containsKey(friendId)) {
            friendships.get(friendId).remove(userId);
        }
        log.info("Пользователи {} и {} больше не друзья", userId, friendId);
    }

    @Override
    public List<User> getFriends(Integer userId) {
        Map<Integer, FriendshipStatus> userFriends = friendships.getOrDefault(userId, Collections.emptyMap());
        return userFriends.keySet().stream()
                .map(users::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    // Новый метод для получения статуса дружбы
    public FriendshipStatus getFriendshipStatus(Integer userId, Integer friendId) {
        if (friendships.containsKey(userId)) {
            return friendships.get(userId).getOrDefault(friendId, null);
        }
        return null;
    }
}