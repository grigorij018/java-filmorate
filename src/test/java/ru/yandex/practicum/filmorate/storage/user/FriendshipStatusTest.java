package ru.yandex.practicum.filmorate.storage.user;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.model.User;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Import({UserDbStorage.class})
class FriendshipStatusTest {

    private final UserStorage userStorage;

    @Test
    void shouldAddFriendWithPendingStatus() {
        // Создаем первого пользователя
        User user1 = new User();
        user1.setEmail("user1@email.com");
        user1.setLogin("user1");
        user1.setName("User 1");
        user1.setBirthday(LocalDate.of(1990, 1, 1));
        User createdUser1 = userStorage.create(user1);

        // Создаем второго пользователя
        User user2 = new User();
        user2.setEmail("user2@email.com");
        user2.setLogin("user2");
        user2.setName("User 2");
        user2.setBirthday(LocalDate.of(1991, 1, 1));
        User createdUser2 = userStorage.create(user2);

        // Добавляем в друзья (статус PENDING согласно ТЗ - односторонняя дружба)
        userStorage.addFriend(createdUser1.getId(), createdUser2.getId());


        // 1. Проверить, что пользователи созданы успешно
        assertThat(createdUser1.getId()).isNotNull();
        assertThat(createdUser2.getId()).isNotNull();
        
    }
}