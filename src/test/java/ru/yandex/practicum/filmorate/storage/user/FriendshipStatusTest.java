// src/test/java/ru/yandex/practicum/filmorate/storage/user/FriendshipStatusTest.java
package ru.yandex.practicum.filmorate.storage.user;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.model.User;
import java.time.LocalDate;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Import({UserDbStorage.class})
class FriendshipStatusTest {

    private final UserDbStorage userStorage;

    @Test
    void shouldAddFriendWithPendingStatus() {
        // Создаем двух пользователей
        User user1 = new User();
        user1.setEmail("user1@email.com");
        user1.setLogin("user1");
        user1.setBirthday(LocalDate.of(1990, 1, 1));
        User createdUser1 = userStorage.create(user1);

        User user2 = new User();
        user2.setEmail("user2@email.com");
        user2.setLogin("user2");
        user2.setBirthday(LocalDate.of(1991, 1, 1));
        User createdUser2 = userStorage.create(user2);

        // Добавляем друга (статус PENDING по умолчанию)
        userStorage.addFriend(createdUser1.getId(), createdUser2.getId());

        // Получаем всех друзей (включая неподтвержденных)
        List<User> allFriends = userStorage.getAllFriends(createdUser1.getId());
        assertThat(allFriends).hasSize(1);
        assertThat(allFriends.get(0).getId()).isEqualTo(createdUser2.getId());

        // Получаем подтвержденных друзей (должен быть пустой)
        List<User> confirmedFriends = userStorage.getFriends(createdUser1.getId());
        assertThat(confirmedFriends).isEmpty();

        // Подтверждаем дружбу
        userStorage.confirmFriendship(createdUser1.getId(), createdUser2.getId());

        // Теперь должны получить подтвержденного друга
        List<User> confirmedFriendsAfter = userStorage.getFriends(createdUser1.getId());
        assertThat(confirmedFriendsAfter).hasSize(1);
        assertThat(confirmedFriendsAfter.get(0).getId()).isEqualTo(createdUser2.getId());
    }
}
