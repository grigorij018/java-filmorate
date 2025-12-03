package ru.yandex.practicum.filmorate.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private User validUser;
    private User invalidUser;

    @BeforeEach
    void setUp() {
        // Валидный пользователь
        validUser = new User();
        validUser.setEmail("valid" + UUID.randomUUID() + "@email.com");
        validUser.setLogin("validlogin" + UUID.randomUUID().toString().substring(0, 8));
        validUser.setBirthday(LocalDate.of(1990, 1, 1));

        // Невалидный пользователь
        invalidUser = new User();
        invalidUser.setEmail("invalid-email");
        invalidUser.setLogin("invalid login"); // Пробелы в логине
        invalidUser.setBirthday(LocalDate.of(2100, 1, 1)); // Дата в будущем
    }

    @Test
    void createUser_ValidUser_ReturnsCreated() throws Exception {
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validUser)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.email").value(validUser.getEmail()))
                .andExpect(jsonPath("$.login").value(validUser.getLogin()));
    }

    @Test
    void createUser_UserWithBlankName_NameSetToLogin() throws Exception {
        User user = new User();
        user.setEmail("test" + UUID.randomUUID() + "@email.com");
        user.setLogin("testlogin" + UUID.randomUUID().toString().substring(0, 8));
        user.setName(""); // Пустое имя
        user.setBirthday(LocalDate.of(1990, 1, 1));

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(user.getLogin()));
    }

    @Test
    void createUser_InvalidEmail_ReturnsBadRequest() throws Exception {
        User user = new User();
        user.setEmail("invalid-email");
        user.setLogin("validlogin");
        user.setBirthday(LocalDate.of(1990, 1, 1));

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createUser_LoginWithSpaces_ReturnsBadRequest() throws Exception {
        User user = new User();
        user.setEmail("test@email.com");
        user.setLogin("login with spaces");
        user.setBirthday(LocalDate.of(1990, 1, 1));

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateUser_ValidUser_ReturnsOk() throws Exception {
        // Сначала создаем пользователя с уникальными данными
        User uniqueUser = new User();
        uniqueUser.setEmail("update" + UUID.randomUUID() + "@email.com");
        uniqueUser.setLogin("updatelogin" + UUID.randomUUID().toString().substring(0, 8));
        uniqueUser.setBirthday(LocalDate.of(1990, 1, 1));

        String response = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(uniqueUser)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        User createdUser = objectMapper.readValue(response, User.class);
        createdUser.setEmail("updated" + UUID.randomUUID() + "@email.com");

        mockMvc.perform(put("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createdUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(createdUser.getEmail()));
    }

    @Test
    void updateUser_NonExistentUser_ReturnsNotFound() throws Exception {
        User user = new User();
        user.setId(999);
        user.setEmail("nonexistent@email.com");
        user.setLogin("nonexistent");
        user.setBirthday(LocalDate.of(1990, 1, 1));

        mockMvc.perform(put("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAllUsers_ReturnsOk() throws Exception {
        mockMvc.perform(get("/users"))
                .andExpect(status().isOk());
    }
}