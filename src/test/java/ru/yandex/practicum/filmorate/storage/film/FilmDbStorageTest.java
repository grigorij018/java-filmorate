package ru.yandex.practicum.filmorate.storage.film;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.MpaRating;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Import({FilmDbStorage.class})
class FilmDbStorageTest {

    private final FilmStorage filmStorage;

    private Film testFilm;

    @BeforeEach
    void setUp() {
        testFilm = new Film();
        testFilm.setName("Test Film");
        testFilm.setDescription("Test Description");
        testFilm.setReleaseDate(LocalDate.of(2000, 1, 1));
        testFilm.setDuration(120);

        MpaRating mpa = new MpaRating();
        mpa.setId(1);
        testFilm.setMpa(mpa);
    }

    @Test
    void testCreateAndFindFilm() {
        Film createdFilm = filmStorage.create(testFilm);

        assertThat(createdFilm.getId()).isNotNull();
        assertThat(createdFilm.getName()).isEqualTo("Test Film");

        Optional<Film> foundFilm = filmStorage.findById(createdFilm.getId());
        assertThat(foundFilm).isPresent();
        assertThat(foundFilm.get().getName()).isEqualTo("Test Film");
    }

    @Test
    void testFindAllFilms() {
        Film film1 = new Film();
        film1.setName("Film 1");
        film1.setDescription("Description 1");
        film1.setReleaseDate(LocalDate.of(2000, 1, 1));
        film1.setDuration(120);
        film1.setMpa(new MpaRating(1, "G", null));

        Film film2 = new Film();
        film2.setName("Film 2");
        film2.setDescription("Description 2");
        film2.setReleaseDate(LocalDate.of(2001, 1, 1));
        film2.setDuration(90);
        film2.setMpa(new MpaRating(2, "PG", null));

        filmStorage.create(film1);
        filmStorage.create(film2);

        List<Film> films = filmStorage.findAll();
        assertThat(films).hasSize(2);
    }
}