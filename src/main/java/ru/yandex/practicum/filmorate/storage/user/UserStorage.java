package ru.yandex.practicum.filmorate.storage.user;

import ru.yandex.practicum.filmorate.model.User;
import java.util.List;
import java.util.Optional;

public interface UserStorage {

    List<User> findAll();

    User create(User user);

    User update(User user);

    Optional<User> findById(Integer id);

    void delete(Integer id);

    void addFriend(Integer userId, Integer friendId);

    void removeFriend(Integer userId, Integer friendId);

    List<User> getFriends(Integer userId);

    List<User> getCommonFriends(Integer userId, Integer otherUserId);

    // Новый метод для получения всех друзей (включая неподтвержденных)
    List<User> getAllFriends(Integer userId);
}