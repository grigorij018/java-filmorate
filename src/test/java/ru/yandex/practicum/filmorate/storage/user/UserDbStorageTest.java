package ru.yandex.practicum.filmorate.storage.user;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.model.User;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Import({UserDbStorage.class})
class UserDbStorageTest {

    private final UserStorage userStorage;
    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setEmail("test@email.com");
        testUser.setLogin("testlogin");
        testUser.setName("Test User");
        testUser.setBirthday(LocalDate.of(1990, 1, 1));
    }

    @Test
    void shouldCreateUser() {
        User createdUser = userStorage.create(testUser);

        assertThat(createdUser.getId()).isNotNull();
        assertThat(createdUser.getEmail()).isEqualTo("test@email.com");
        assertThat(createdUser.getLogin()).isEqualTo("testlogin");
    }

    @Test
    void shouldFindUserById() {
        User createdUser = userStorage.create(testUser);
        Optional<User> foundUser = userStorage.findById(createdUser.getId());

        assertThat(foundUser)
                .isPresent()
                .hasValueSatisfying(user -> {
                    assertThat(user.getId()).isEqualTo(createdUser.getId());
                    assertThat(user.getEmail()).isEqualTo("test@email.com");
                });
    }

    @Test
    void shouldUpdateUser() {
        User createdUser = userStorage.create(testUser);
        createdUser.setEmail("updated@email.com");
        createdUser.setName("Updated Name");

        User updatedUser = userStorage.update(createdUser);

        assertThat(updatedUser.getEmail()).isEqualTo("updated@email.com");
        assertThat(updatedUser.getName()).isEqualTo("Updated Name");

        Optional<User> foundUser = userStorage.findById(createdUser.getId());
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getEmail()).isEqualTo("updated@email.com");
    }

    @Test
    void shouldFindAllUsers() {
        userStorage.create(testUser);

        User secondUser = new User();
        secondUser.setEmail("second@email.com");
        secondUser.setLogin("secondlogin");
        secondUser.setName("Second User");
        secondUser.setBirthday(LocalDate.of(1991, 1, 1));
        userStorage.create(secondUser);

        List<User> users = userStorage.findAll();

        assertThat(users).hasSize(2);
        assertThat(users).extracting(User::getEmail)
                .containsExactlyInAnyOrder("test@email.com", "second@email.com");
    }

    @Test
    void shouldAddFriend() {
        User user1 = userStorage.create(testUser);

        User user2 = new User();
        user2.setEmail("friend@email.com");
        user2.setLogin("friendlogin");
        user2.setName("Friend User");
        user2.setBirthday(LocalDate.of(1992, 1, 1));
        User friend = userStorage.create(user2);

        userStorage.addFriend(user1.getId(), friend.getId());

        List<User> friends = userStorage.getFriends(user1.getId());
        assertThat(friends).hasSize(1);
        assertThat(friends.get(0).getId()).isEqualTo(friend.getId());
    }

    @Test
    void shouldRemoveFriend() {
        User user1 = userStorage.create(testUser);

        User user2 = new User();
        user2.setEmail("friend@email.com");
        user2.setLogin("friendlogin");
        user2.setName("Friend User");
        user2.setBirthday(LocalDate.of(1992, 1, 1));
        User friend = userStorage.create(user2);

        userStorage.addFriend(user1.getId(), friend.getId());
        userStorage.removeFriend(user1.getId(), friend.getId());

        List<User> friends = userStorage.getFriends(user1.getId());
        assertThat(friends).isEmpty();
    }
}