package ru.yandex.practicum.filmorate.storage.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.filmorate.model.User;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Repository
@Primary
@RequiredArgsConstructor
public class UserDbStorage implements UserStorage {

    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional(readOnly = true)
    public List<User> findAll() {
        String sql = "SELECT * FROM users ORDER BY id";
        List<User> users = jdbcTemplate.query(sql, this::mapRowToUser);

        if (!users.isEmpty()) {
            loadFriendsForUsers(users);
        }

        return users;
    }

    @Override
    @Transactional
    public User create(User user) {
        try {
            String sql = "INSERT INTO users (email, login, name, birthday) VALUES (?, ?, ?, ?)";

            KeyHolder keyHolder = new GeneratedKeyHolder();

            jdbcTemplate.update(connection -> {
                PreparedStatement stmt = connection.prepareStatement(sql, new String[]{"id"});
                stmt.setString(1, user.getEmail());
                stmt.setString(2, user.getLogin());
                stmt.setString(3, user.getName());
                stmt.setDate(4, Date.valueOf(user.getBirthday()));
                return stmt;
            }, keyHolder);

            Integer id = Objects.requireNonNull(keyHolder.getKey()).intValue();
            user.setId(id);
            log.info("Создан пользователь с ID: {}", id);
            return user;
        } catch (DataAccessException e) {
            log.error("Ошибка при создании пользователя: {}", user.getEmail(), e);
            throw new RuntimeException("Не удалось создать пользователя", e);
        }
    }

    @Override
    @Transactional
    public User update(User user) {
        try {
            String sql = "UPDATE users SET email = ?, login = ?, name = ?, birthday = ? WHERE id = ?";

            int updated = jdbcTemplate.update(sql,
                    user.getEmail(),
                    user.getLogin(),
                    user.getName(),
                    Date.valueOf(user.getBirthday()),
                    user.getId());

            if (updated == 0) {
                throw new RuntimeException("Пользователь с ID " + user.getId() + " не найден");
            }

            log.info("Обновлен пользователь с ID: {}", user.getId());
            return user;
        } catch (DataAccessException e) {
            log.error("Ошибка при обновлении пользователя с ID: {}", user.getId(), e);
            throw new RuntimeException("Не удалось обновить пользователя", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findById(Integer id) {
        String sql = "SELECT * FROM users WHERE id = ?";

        try {
            User user = jdbcTemplate.queryForObject(sql, this::mapRowToUser, id);
            if (user != null) {
                loadFriendsForSingleUser(user);
            }
            return Optional.of(user);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        try {
            String sql = "DELETE FROM users WHERE id = ?";
            int deleted = jdbcTemplate.update(sql, id);

            if (deleted == 0) {
                throw new RuntimeException("Пользователь с ID " + id + " не найден");
            }

            log.info("Удален пользователь с ID: {}", id);
        } catch (DataAccessException e) {
            log.error("Ошибка при удалении пользователя с ID: {}", id, e);
            throw new RuntimeException("Не удалось удалить пользователя", e);
        }
    }

    @Override
    @Transactional
    public void addFriend(Integer userId, Integer friendId) {
        try {
            String sql = "MERGE INTO friendships (user_id, friend_id, status) KEY(user_id, friend_id) VALUES (?, ?, 'PENDING')";
            int updated = jdbcTemplate.update(sql, userId, friendId);

            if (updated == 0) {
                throw new RuntimeException("Не удалось добавить в друзья");
            }

            log.info("Пользователь {} добавил в друзья пользователя {}", userId, friendId);
        } catch (DataAccessException e) {
            log.error("Ошибка при добавлении в друзья пользователей {} и {}", userId, friendId, e);
            throw new RuntimeException("Не удалось добавить в друзья", e);
        }
    }

    @Override
    @Transactional
    public void removeFriend(Integer userId, Integer friendId) {
        try {
            String sql = "DELETE FROM friendships WHERE user_id = ? AND friend_id = ?";
            int deleted = jdbcTemplate.update(sql, userId, friendId);

            // Если ничего не удалено - это нормально (дружбы не было)
            if (deleted == 0) {
                log.info("Дружбы между пользователями {} и {} не существовало", userId, friendId);
                return; // Не бросаем исключение
            }

            log.info("Пользователь {} удалил из друзей пользователя {}", userId, friendId);
        } catch (DataAccessException e) {
            log.error("Ошибка при удалении из друзей пользователей {} и {}", userId, friendId, e);
            throw new RuntimeException("Не удалось удалить из друзей", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> getFriends(Integer userId) {
        String sql = """
        SELECT u.*
        FROM users u
        JOIN friendships f ON u.id = f.friend_id
        WHERE f.user_id = ?
        ORDER BY u.id
        """;

        List<User> friends = jdbcTemplate.query(sql, this::mapRowToUser, userId);

        if (!friends.isEmpty()) {
            loadFriendsForUsers(friends);
        }

        return friends;
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> getCommonFriends(Integer userId, Integer otherUserId) {
        String sql = """
            SELECT u.* FROM users u
            JOIN friendships f1 ON u.id = f1.friend_id
            JOIN friendships f2 ON u.id = f2.friend_id
            WHERE f1.user_id = ? AND f2.user_id = ?
            AND f1.status = 'CONFIRMED' AND f2.status = 'CONFIRMED'
            ORDER BY u.id
            """;

        List<User> commonFriends = jdbcTemplate.query(sql, this::mapRowToUser, userId, otherUserId);

        if (!commonFriends.isEmpty()) {
            loadFriendsForUsers(commonFriends);
        }

        return commonFriends;
    }

    @Transactional
    public void confirmFriendship(Integer userId, Integer friendId) {
        try {
            String sql = "UPDATE friendships SET status = 'CONFIRMED' WHERE user_id = ? AND friend_id = ?";
            int updated = jdbcTemplate.update(sql, userId, friendId);

            if (updated == 0) {
                throw new RuntimeException("Заявка в друзья не найдена");
            }

            log.info("Дружба между пользователями {} и {} подтверждена", userId, friendId);
        } catch (DataAccessException e) {
            log.error("Ошибка при подтверждении дружбы пользователей {} и {}", userId, friendId, e);
            throw new RuntimeException("Не удалось подтвердить дружбу", e);
        }
    }

    private User mapRowToUser(ResultSet rs, int rowNum) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setEmail(rs.getString("email"));
        user.setLogin(rs.getString("login"));
        user.setName(rs.getString("name"));
        user.setBirthday(rs.getDate("birthday").toLocalDate());
        user.setFriends(new HashSet<>());
        return user;
    }

    private void loadFriendsForUsers(List<User> users) {
        if (users.isEmpty()) {
            return;
        }

        List<Integer> userIds = users.stream()
                .map(User::getId)
                .collect(Collectors.toList());

        String placeholders = userIds.stream()
                .map(id -> "?")
                .collect(Collectors.joining(", "));

        String sql = String.format("""
            SELECT user_id, friend_id
            FROM friendships
            WHERE user_id IN (%s) AND status = 'CONFIRMED'
            ORDER BY user_id
            """, placeholders);

        Map<Integer, List<Integer>> friendsByUserId = jdbcTemplate.query(sql, userIds.toArray(), rs -> {
            Map<Integer, List<Integer>> result = new HashMap<>();
            while (rs.next()) {
                Integer userId = rs.getInt("user_id");
                Integer friendId = rs.getInt("friend_id");
                result.computeIfAbsent(userId, k -> new ArrayList<>()).add(friendId);
            }
            return result;
        });

        for (User user : users) {
            List<Integer> friendIds = friendsByUserId.getOrDefault(user.getId(), new ArrayList<>());
            user.setFriends(new HashSet<>(friendIds));
        }
    }

    private void loadFriendsForSingleUser(User user) {
        String sql = "SELECT friend_id FROM friendships WHERE user_id = ? AND status = 'CONFIRMED'";
        List<Integer> friendIds = jdbcTemplate.queryForList(sql, Integer.class, user.getId());
        user.setFriends(new HashSet<>(friendIds));
    }
}