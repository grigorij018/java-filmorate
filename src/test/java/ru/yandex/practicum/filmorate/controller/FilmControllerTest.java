package ru.yandex.practicum.filmorate.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.MpaRating;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class FilmControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private Film validFilm;
    private Film invalidFilm;

    @BeforeEach
    void setUp() {
        // Создаем валидный MPA рейтинг
        MpaRating mpa = new MpaRating();
        mpa.setId(1);
        mpa.setName("G");
        mpa.setDescription("У фильма нет возрастных ограничений");

        // Валидный фильм
        validFilm = new Film();
        validFilm.setName("Valid Film");
        validFilm.setDescription("Valid description");
        validFilm.setReleaseDate(LocalDate.of(2000, 1, 1));
        validFilm.setDuration(120);
        validFilm.setMpa(mpa);

        // Невалидный фильм
        invalidFilm = new Film();
        invalidFilm.setName("");
        invalidFilm.setDescription("A".repeat(201)); // Слишком длинное описание
        invalidFilm.setReleaseDate(LocalDate.of(1800, 1, 1)); // Слишком ранняя дата
        invalidFilm.setDuration(-10); // Отрицательная продолжительность
        invalidFilm.setMpa(mpa);
    }

    @Test
    void createFilm_ValidFilm_ReturnsCreated() throws Exception {
        mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validFilm)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Valid Film"))
                .andExpect(jsonPath("$.duration").value(120));
    }

    @Test
    void createFilm_InvalidName_ReturnsBadRequest() throws Exception {
        Film film = new Film();
        film.setName("");
        film.setDescription("Description");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(120);
        film.setMpa(validFilm.getMpa());

        mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(film)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createFilm_EarlyReleaseDate_ReturnsBadRequest() throws Exception {
        Film film = new Film();
        film.setName("Early Film");
        film.setDescription("Description");
        film.setReleaseDate(LocalDate.of(1890, 1, 1));
        film.setDuration(120);
        film.setMpa(validFilm.getMpa());

        mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(film)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateFilm_ValidFilm_ReturnsOk() throws Exception {
        // Сначала создаем фильм
        String response = mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validFilm)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Film createdFilm = objectMapper.readValue(response, Film.class);
        createdFilm.setName("Updated Film Name");

        mockMvc.perform(put("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createdFilm)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Film Name"));
    }

    @Test
    void updateFilm_NonExistentFilm_ReturnsNotFound() throws Exception {
        Film film = new Film();
        film.setId(999);
        film.setName("Non-existent Film");
        film.setDescription("Description");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(120);
        film.setMpa(validFilm.getMpa());

        mockMvc.perform(put("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(film)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAllFilms_ReturnsOk() throws Exception {
        mockMvc.perform(get("/films"))
                .andExpect(status().isOk());
    }
}