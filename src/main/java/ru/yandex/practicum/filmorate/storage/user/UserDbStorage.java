package ru.yandex.practicum.filmorate.storage.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.User;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.*;

@Slf4j
@Repository
@Primary
@RequiredArgsConstructor
public class UserDbStorage implements UserStorage {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<User> findAll() {
        String sql = "SELECT * FROM users ORDER BY id";
        List<User> users = jdbcTemplate.query(sql, this::mapRowToUser);
        users.forEach(this::loadFriends);
        return users;
    }

    @Override
    public User create(User user) {
        String sql = "INSERT INTO users (email, login, name, birthday) VALUES (?, ?, ?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement stmt = connection.prepareStatement(sql, new String[]{"id"});
            stmt.setString(1, user.getEmail());
            stmt.setString(2, user.getLogin());
            stmt.setString(3, user.getName());

            // Безопасная проверка birthday
            if (user.getBirthday() != null) {
                stmt.setDate(4, Date.valueOf(user.getBirthday()));
            } else {
                stmt.setNull(4, Types.DATE);
            }
            return stmt;
        }, keyHolder);

        Integer id = Objects.requireNonNull(keyHolder.getKey()).intValue();
        user.setId(id);
        log.info("Создан пользователь с ID: {}", id);
        return user;
    }

    @Override
    public User update(User user) {
        String sql = "UPDATE users SET email = ?, login = ?, name = ?, birthday = ? WHERE id = ?";

        int updated = jdbcTemplate.update(sql,
                user.getEmail(),
                user.getLogin(),
                user.getName(),
                user.getBirthday() != null ? Date.valueOf(user.getBirthday()) : null,
                user.getId());

        if (updated == 0) {
            throw new RuntimeException("Пользователь с ID " + user.getId() + " не найден");
        }

        log.info("Обновлен пользователь с ID: {}", user.getId());
        return user;
    }

    @Override
    public Optional<User> findById(Integer id) {
        String sql = "SELECT * FROM users WHERE id = ?";

        try {
            User user = jdbcTemplate.queryForObject(sql, this::mapRowToUser, id);
            loadFriends(user);
            return Optional.of(user);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public void delete(Integer id) {
        String sql = "DELETE FROM users WHERE id = ?";
        jdbcTemplate.update(sql, id);
        log.info("Удален пользователь с ID: {}", id);
    }

    @Override
    public void addFriend(Integer userId, Integer friendId) {
        // Проверяем существование пользователей
        if (findById(userId).isEmpty()) {
            throw new RuntimeException("Пользователь с ID " + userId + " не найден");
        }
        if (findById(friendId).isEmpty()) {
            throw new RuntimeException("Пользователь с ID " + friendId + " не найден");
        }

        // В ТЗ указано, что дружба должна быть односторонней по умолчанию
        // Друг подтверждает дружбу, вызывая addFriend со своей стороны
        String checkSql = "SELECT COUNT(*) FROM friendships WHERE user_id = ? AND friend_id = ?";
        Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, userId, friendId);

        if (count == 0) {
            // Добавляем запись о дружбе со статусом PENDING
            String sql = "INSERT INTO friendships (user_id, friend_id, status) VALUES (?, ?, 'PENDING')";
            jdbcTemplate.update(sql, userId, friendId);
            log.info("Пользователь {} добавил в друзья пользователя {} (статус: PENDING)", userId, friendId);
        }
    }

    @Override
    public void removeFriend(Integer userId, Integer friendId) {
        // Удаляем запись о дружбе только для данного пользователя
        String sql = "DELETE FROM friendships WHERE user_id = ? AND friend_id = ?";
        int deleted = jdbcTemplate.update(sql, userId, friendId);

        if (deleted > 0) {
            log.info("Пользователь {} удалил из друзей пользователя {}", userId, friendId);
        }
    }

    @Override
    public List<User> getFriends(Integer userId) {
        if (findById(userId).isEmpty()) {
            throw new RuntimeException("Пользователь с ID " + userId + " не найден");
        }

        // Получаем всех, кого пользователь добавил в друзья (независимо от статуса)
        String sql = "SELECT u.* FROM users u " +
                "JOIN friendships f ON u.id = f.friend_id " +
                "WHERE f.user_id = ?";

        List<User> friends = jdbcTemplate.query(sql, this::mapRowToUser, userId);
        friends.forEach(this::loadFriends);
        return friends;
    }

    @Override
    public List<User> getCommonFriends(Integer userId, Integer otherUserId) {
        // Проверяем существование пользователей
        if (findById(userId).isEmpty()) {
            throw new RuntimeException("Пользователь с ID " + userId + " не найден");
        }
        if (findById(otherUserId).isEmpty()) {
            throw new RuntimeException("Пользователь с ID " + otherUserId + " не найден");
        }

        // Находим общих друзей (тех, кто есть в друзьях у обоих пользователей)
        String sql = "SELECT u.* FROM users u " +
                "WHERE u.id IN (" +
                "  SELECT f1.friend_id FROM friendships f1 " +
                "  WHERE f1.user_id = ?" +
                "  INTERSECT " +
                "  SELECT f2.friend_id FROM friendships f2 " +
                "  WHERE f2.user_id = ?" +
                ")";

        List<User> commonFriends = jdbcTemplate.query(sql, this::mapRowToUser, userId, otherUserId);
        commonFriends.forEach(this::loadFriends);
        return commonFriends;
    }

    private User mapRowToUser(ResultSet rs, int rowNum) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setEmail(rs.getString("email"));
        user.setLogin(rs.getString("login"));
        user.setName(rs.getString("name"));

        Date birthday = rs.getDate("birthday");
        if (birthday != null) {
            user.setBirthday(birthday.toLocalDate());
        }

        return user;
    }

    private void loadFriends(User user) {
        if (user.getId() != null) {
            // Загружаем ID всех друзей (тех, кого пользователь добавил в друзья)
            String sql = "SELECT friend_id FROM friendships WHERE user_id = ?";
            List<Integer> friendIds = jdbcTemplate.queryForList(sql, Integer.class, user.getId());
            user.setFriends(new HashSet<>(friendIds));
        }
    }

    // Метод для проверки взаимной дружбы
    public boolean isMutualFriendship(Integer userId, Integer friendId) {
        String sql = "SELECT COUNT(*) FROM friendships f1 " +
                "JOIN friendships f2 ON f1.user_id = f2.friend_id AND f1.friend_id = f2.user_id " +
                "WHERE f1.user_id = ? AND f1.friend_id = ?";

        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, userId, friendId);
        return count != null && count > 0;
    }

    // Метод для подтверждения дружбы (другая сторона также добавляет в друзья)
    public void confirmFriendship(Integer userId, Integer friendId) {
        // Проверяем, есть ли обратная запись
        String checkReverseSql = "SELECT COUNT(*) FROM friendships WHERE user_id = ? AND friend_id = ?";
        Integer reverseCount = jdbcTemplate.queryForObject(checkReverseSql, Integer.class, friendId, userId);

        if (reverseCount == 0) {
            // Добавляем обратную запись
            String insertSql = "INSERT INTO friendships (user_id, friend_id, status) VALUES (?, ?, 'CONFIRMED')";
            jdbcTemplate.update(insertSql, friendId, userId);
            log.info("Пользователь {} подтвердил дружбу с пользователем {}", friendId, userId);
        }
    }
}