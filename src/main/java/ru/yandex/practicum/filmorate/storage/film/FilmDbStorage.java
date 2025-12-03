package ru.yandex.practicum.filmorate.storage.film;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.MpaRating;

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
public class FilmDbStorage implements FilmStorage {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<Film> findAll() {
        String sql = "SELECT f.*, m.name as mpa_name, m.description as mpa_description " +
                "FROM films f " +
                "LEFT JOIN mpa_ratings m ON f.mpa_id = m.id";

        List<Film> films = jdbcTemplate.query(sql, this::mapRowToFilm);
        films.forEach(this::loadAdditionalData);
        return films;
    }

    @Override
    public Film create(Film film) {
        String sql = "INSERT INTO films (name, description, release_date, duration, mpa_id) " +
                "VALUES (?, ?, ?, ?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement stmt = connection.prepareStatement(sql, new String[]{"id"});
            stmt.setString(1, film.getName());
            stmt.setString(2, film.getDescription());
            stmt.setDate(3, Date.valueOf(film.getReleaseDate()));
            stmt.setInt(4, film.getDuration());

            // Безопасная проверка MPA
            if (film.getMpa() != null && film.getMpa().getId() != null) {
                stmt.setInt(5, film.getMpa().getId());
            } else {
                stmt.setNull(5, Types.INTEGER);
            }
            return stmt;
        }, keyHolder);

        Integer id = Objects.requireNonNull(keyHolder.getKey()).intValue();
        film.setId(id);

        saveGenres(film);
        log.info("Создан фильм с ID: {}", id);
        return findById(id).orElse(film);
    }

    @Override
    public Film update(Film film) {
        String sql = "UPDATE films SET name = ?, description = ?, release_date = ?, duration = ?, mpa_id = ? " +
                "WHERE id = ?";

        int updated = jdbcTemplate.update(sql,
                film.getName(),
                film.getDescription(),
                Date.valueOf(film.getReleaseDate()),
                film.getDuration(),
                film.getMpa() != null ? film.getMpa().getId() : null,
                film.getId());

        if (updated == 0) {
            throw new RuntimeException("Фильм с ID " + film.getId() + " не найден");
        }

        updateGenres(film);
        log.info("Обновлен фильм с ID: {}", film.getId());
        return findById(film.getId()).orElse(film);
    }

    @Override
    public Optional<Film> findById(Integer id) {
        String sql = "SELECT f.*, m.name as mpa_name, m.description as mpa_description " +
                "FROM films f " +
                "LEFT JOIN mpa_ratings m ON f.mpa_id = m.id " +
                "WHERE f.id = ?";

        try {
            Film film = jdbcTemplate.queryForObject(sql, this::mapRowToFilm, id);
            loadAdditionalData(film);
            return Optional.of(film);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public void delete(Integer id) {
        String sql = "DELETE FROM films WHERE id = ?";
        jdbcTemplate.update(sql, id);
        log.info("Удален фильм с ID: {}", id);
    }

    @Override
    public Film addLike(Integer filmId, Integer userId) {
        String sql = "MERGE INTO likes (film_id, user_id) KEY(film_id, user_id) VALUES (?, ?)";
        jdbcTemplate.update(sql, filmId, userId);
        log.info("Пользователь {} поставил лайк фильму {}", userId, filmId);
        return findById(filmId).orElseThrow(() ->
                new RuntimeException("Фильм с ID " + filmId + " не найден"));
    }

    @Override
    public Film removeLike(Integer filmId, Integer userId) {
        String sql = "DELETE FROM likes WHERE film_id = ? AND user_id = ?";
        jdbcTemplate.update(sql, filmId, userId);
        log.info("Пользователь {} удалил лайк у фильма {}", userId, filmId);
        return findById(filmId).orElseThrow(() ->
                new RuntimeException("Фильм с ID " + filmId + " не найден"));
    }

    @Override
    public List<Film> getPopularFilms(int count) {
        String sql = "SELECT f.*, m.name as mpa_name, m.description as mpa_description, " +
                "COUNT(l.user_id) as likes_count " +
                "FROM films f " +
                "LEFT JOIN mpa_ratings m ON f.mpa_id = m.id " +
                "LEFT JOIN likes l ON f.id = l.film_id " +
                "GROUP BY f.id, m.name, m.description " +
                "ORDER BY likes_count DESC " +
                "LIMIT ?";

        List<Film> films = jdbcTemplate.query(sql, this::mapRowToFilm, count);
        films.forEach(this::loadAdditionalData);
        return films;
    }

    private Film mapRowToFilm(ResultSet rs, int rowNum) throws SQLException {
        Film film = new Film();
        film.setId(rs.getInt("id"));
        film.setName(rs.getString("name"));
        film.setDescription(rs.getString("description"));
        film.setReleaseDate(rs.getDate("release_date").toLocalDate());
        film.setDuration(rs.getInt("duration"));

        // MPA рейтинг
        MpaRating mpa = new MpaRating();
        mpa.setId(rs.getInt("mpa_id"));
        mpa.setName(rs.getString("mpa_name"));
        mpa.setDescription(rs.getString("mpa_description"));
        film.setMpa(mpa);

        return film;
    }

    private void loadAdditionalData(Film film) {
        loadGenres(film);
        loadLikes(film);
    }

    private void saveGenres(Film film) {
        if (film.getId() != null) {
            String deleteSql = "DELETE FROM film_genres WHERE film_id = ?";
            jdbcTemplate.update(deleteSql, film.getId());

            if (film.getGenres() != null && !film.getGenres().isEmpty()) {
                // Сортируем жанры по ID перед сохранением
                List<Genre> sortedGenres = film.getGenres().stream()
                        .sorted(Comparator.comparing(Genre::getId))
                        .toList();

                String insertSql = "INSERT INTO film_genres (film_id, genre_id) VALUES (?, ?)";
                for (Genre genre : sortedGenres) {
                    jdbcTemplate.update(insertSql, film.getId(), genre.getId());
                }
            }
        }
    }

    private void updateGenres(Film film) {
        saveGenres(film);
    }

    private void loadGenres(Film film) {
        if (film.getId() != null) {
            String sql = "SELECT g.id, g.name FROM genres g " +
                    "JOIN film_genres fg ON g.id = fg.genre_id " +
                    "WHERE fg.film_id = ? " +
                    "ORDER BY g.id ASC"; // Добавляем сортировку по ID

            List<Genre> genres = jdbcTemplate.query(sql, (rs, rowNum) -> {
                Genre genre = new Genre();
                genre.setId(rs.getInt("id"));
                genre.setName(rs.getString("name"));
                return genre;
            }, film.getId());

            film.setGenres(new HashSet<>(genres));
        }
    }

    private void loadLikes(Film film) {
        if (film.getId() != null) {
            String sql = "SELECT user_id FROM likes WHERE film_id = ?";
            List<Integer> likes = jdbcTemplate.queryForList(sql, Integer.class, film.getId());
            film.setLikes(new HashSet<>(likes));
        }
    }
}