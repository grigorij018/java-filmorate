package ru.yandex.practicum.filmorate.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GenreTest {

    @Test
    void shouldCreateGenreWithIdAndName() {
        Genre genre = new Genre(1, "Комедия");

        assertEquals(1, genre.getId());
        assertEquals("Комедия", genre.getName());
    }

    @Test
    void shouldHaveAllGenreNames() {
        assertEquals("Комедия", Genre.GenreName.COMEDY.getName());
        assertEquals("Драма", Genre.GenreName.DRAMA.getName());
        assertEquals("Мультфильм", Genre.GenreName.CARTOON.getName());
        assertEquals("Триллер", Genre.GenreName.THRILLER.getName());
        assertEquals("Документальный", Genre.GenreName.DOCUMENTARY.getName());
        assertEquals("Боевик", Genre.GenreName.ACTION.getName());
    }
}
