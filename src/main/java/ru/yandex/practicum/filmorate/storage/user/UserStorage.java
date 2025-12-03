package ru.yandex.practicum.filmorate.storage.user;

import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.filmorate.model.User;
import java.util.List;
import java.util.Optional;

public interface UserStorage {

    @Transactional(readOnly = true)
    List<User> findAll();

    @Transactional
    User create(User user);

    @Transactional
    User update(User user);

    @Transactional(readOnly = true)
    Optional<User> findById(Integer id);

    @Transactional
    void delete(Integer id);

    @Transactional
    void addFriend(Integer userId, Integer friendId);

    @Transactional
    void removeFriend(Integer userId, Integer friendId);

    @Transactional(readOnly = true)
    List<User> getFriends(Integer userId);

    @Transactional(readOnly = true)
    default List<User> getAllFriends(Integer userId) {
        return getFriends(userId);
    }

    @Transactional(readOnly = true)
    List<User> getCommonFriends(Integer userId, Integer otherUserId);
}