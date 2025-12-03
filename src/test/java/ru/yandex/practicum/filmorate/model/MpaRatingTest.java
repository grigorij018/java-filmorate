package ru.yandex.practicum.filmorate.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MpaRatingTest {

    @Test
    void shouldCreateMpaRatingWithAllFields() {
        MpaRating mpa = new MpaRating(1, "G", "У фильма нет возрастных ограничений");

        assertEquals(1, mpa.getId());
        assertEquals("G", mpa.getName());
        assertEquals("У фильма нет возрастных ограничений", mpa.getDescription());
    }

    @Test
    void shouldHaveAllMpaNames() {
        assertEquals("G", MpaRating.MpaName.G.getCode());
        assertEquals("У фильма нет возрастных ограничений", MpaRating.MpaName.G.getDescription());

        assertEquals("PG", MpaRating.MpaName.PG.getCode());
        assertEquals("Детям рекомендуется смотреть фильм с родителями", MpaRating.MpaName.PG.getDescription());
    }
}