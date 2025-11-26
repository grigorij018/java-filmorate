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
        validUser = new User();
        validUser.setEmail("valid@email.com");
        validUser.setLogin("validlogin");
        validUser.setBirthday(LocalDate.of(1990, 1, 1));

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
                .andExpect(jsonPath("$.email").value("valid@email.com"))
                .andExpect(jsonPath("$.login").value("validlogin"));
    }

    @Test
    void createUser_UserWithBlankName_NameSetToLogin() throws Exception {
        User user = new User();
        user.setEmail("test@email.com");
        user.setLogin("testlogin");
        user.setName(""); // Пустое имя

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("testlogin"));
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
        // Сначала создаем пользователя
        String response = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validUser)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        User createdUser = objectMapper.readValue(response, User.class);
        createdUser.setEmail("updated@email.com");

        mockMvc.perform(put("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createdUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("updated@email.com"));
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