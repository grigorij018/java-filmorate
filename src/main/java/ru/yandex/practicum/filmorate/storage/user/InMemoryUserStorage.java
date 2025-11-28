package ru.yandex.practicum.filmorate.storage.user;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.User;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class InMemoryUserStorage implements UserStorage {
    private final Map<Integer, User> users = new HashMap<>();
    private final Map<Integer, Set<Integer>> friendships = new HashMap<>();
    private int nextId = 1;

    @Override
    public List<User> findAll() {
        return new ArrayList<>(users.values());
    }

    @Override
    public User create(User user) {
        user.setId(nextId++);
        users.put(user.getId(), user);
        friendships.put(user.getId(), new HashSet<>());
        log.info("Создан пользователь: {}", user);
        return user;
    }

    @Override
    public User update(User user) {
        users.put(user.getId(), user);
        if (!friendships.containsKey(user.getId())) {
            friendships.put(user.getId(), new HashSet<>());
        }
        log.info("Обновлен пользователь: {}", user);
        return user;
    }

    @Override
    public Optional<User> findById(Integer id) {
        return Optional.ofNullable(users.get(id));
    }

    @Override
    public void delete(Integer id) {
        users.remove(id);
        friendships.remove(id);
        // Удаляем пользователя из списков друзей других пользователей
        friendships.values().forEach(friends -> friends.remove(id));
        log.info("Удален пользователь с id: {}", id);
    }

    @Override
    public void addFriend(Integer userId, Integer friendId) {
        if (users.containsKey(userId) && users.containsKey(friendId)) {
            friendships.get(userId).add(friendId);
            friendships.get(friendId).add(userId);
            log.info("Пользователи {} и {} теперь друзья", userId, friendId);
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
        Set<Integer> friendIds = friendships.getOrDefault(userId, Collections.emptySet());
        return friendIds.stream()
                .map(users::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public List<User> getCommonFriends(Integer userId, Integer otherUserId) {
        Set<Integer> userFriends = friendships.getOrDefault(userId, Collections.emptySet());
        Set<Integer> otherUserFriends = friendships.getOrDefault(otherUserId, Collections.emptySet());

        return userFriends.stream()
                .filter(otherUserFriends::contains)
                .map(users::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}