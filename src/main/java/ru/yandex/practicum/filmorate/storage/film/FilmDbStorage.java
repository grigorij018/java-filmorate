package ru.yandex.practicum.filmorate.storage.film;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.MpaRating;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Repository
@Primary
@RequiredArgsConstructor
public class FilmDbStorage implements FilmStorage {

    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional(readOnly = true)
    public List<Film> findAll() {
        String filmsSql = "SELECT f.*, m.id as mpa_id, m.name as mpa_name, m.description as mpa_description " +
                "FROM films f " +
                "LEFT JOIN mpa_ratings m ON f.mpa_id = m.id " +
                "ORDER BY f.id";

        List<Film> films = jdbcTemplate.query(filmsSql, this::mapRowToFilm);

        if (!films.isEmpty()) {
            loadGenresForFilms(films);
            loadLikesForFilms(films);
        }

        return films;
    }

    @Override
    @Transactional
    public Film create(Film film) {
        try {
            // Сохраняем фильм
            String filmSql = "INSERT INTO films (name, description, release_date, duration, mpa_id) " +
                    "VALUES (?, ?, ?, ?, ?)";

            KeyHolder keyHolder = new GeneratedKeyHolder();

            jdbcTemplate.update(connection -> {
                PreparedStatement stmt = connection.prepareStatement(filmSql, new String[]{"id"});
                stmt.setString(1, film.getName());
                stmt.setString(2, film.getDescription());
                stmt.setDate(3, Date.valueOf(film.getReleaseDate()));
                stmt.setInt(4, film.getDuration());
                stmt.setInt(5, film.getMpa().getId());
                return stmt;
            }, keyHolder);

            Integer filmId = Objects.requireNonNull(keyHolder.getKey()).intValue();
            film.setId(filmId);

            // Сохраняем жанры с сохранением порядка
            saveGenresInTransaction(filmId, film.getGenres());

            log.info("Создан фильм с ID: {}", filmId);
            return findById(filmId).orElse(film);
        } catch (DataAccessException e) {
            log.error("Ошибка при создании фильма: {}", film.getName(), e);
            throw new RuntimeException("Не удалось создать фильм", e);
        }
    }

    @Override
    @Transactional
    public Film update(Film film) {
        try {
            // Обновляем фильм
            String filmSql = "UPDATE films SET name = ?, description = ?, release_date = ?, duration = ?, mpa_id = ? " +
                    "WHERE id = ?";

            int updated = jdbcTemplate.update(filmSql,
                    film.getName(),
                    film.getDescription(),
                    Date.valueOf(film.getReleaseDate()),
                    film.getDuration(),
                    film.getMpa().getId(),
                    film.getId());

            if (updated == 0) {
                throw new RuntimeException("Фильм с ID " + film.getId() + " не найден");
            }

            // Обновляем жанры в той же транзакции
            updateGenresInTransaction(film.getId(), film.getGenres());

            log.info("Обновлен фильм с ID: {}", film.getId());
            return findById(film.getId()).orElse(film);
        } catch (DataAccessException e) {
            log.error("Ошибка при обновлении фильма с ID: {}", film.getId(), e);
            throw new RuntimeException("Не удалось обновить фильм", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Film> findById(Integer id) {
        String filmsSql = "SELECT f.*, m.id as mpa_id, m.name as mpa_name, m.description as mpa_description " +
                "FROM films f " +
                "LEFT JOIN mpa_ratings m ON f.mpa_id = m.id " +
                "WHERE f.id = ?";

        try {
            Film film = jdbcTemplate.queryForObject(filmsSql, this::mapRowToFilm, id);
            if (film != null) {
                loadAdditionalDataForSingleFilm(film);
            }
            return Optional.ofNullable(film);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        try {
            // Удаление film_genres произойдет каскадно из-за ON DELETE CASCADE
            String sql = "DELETE FROM films WHERE id = ?";
            int deleted = jdbcTemplate.update(sql, id);

            if (deleted == 0) {
                throw new RuntimeException("Фильм с ID " + id + " не найден");
            }

            log.info("Удален фильм с ID: {}", id);
        } catch (DataAccessException e) {
            log.error("Ошибка при удалении фильма с ID: {}", id, e);
            throw new RuntimeException("Не удалось удалить фильм", e);
        }
    }

    @Override
    @Transactional
    public Film addLike(Integer filmId, Integer userId) {
        try {
            String sql = "MERGE INTO likes (film_id, user_id) KEY(film_id, user_id) VALUES (?, ?)";
            jdbcTemplate.update(sql, filmId, userId);
            log.info("Пользователь {} поставил лайк фильму {}", userId, filmId);

            return findById(filmId).orElseThrow(() ->
                    new RuntimeException("Фильм с ID " + filmId + " не найден после добавления лайка"));
        } catch (DataAccessException e) {
            log.error("Ошибка при добавлении лайка фильму {} пользователем {}", filmId, userId, e);
            throw new RuntimeException("Не удалось добавить лайк", e);
        }
    }

    @Override
    @Transactional
    public Film removeLike(Integer filmId, Integer userId) {
        try {
            String sql = "DELETE FROM likes WHERE film_id = ? AND user_id = ?";
            int deleted = jdbcTemplate.update(sql, filmId, userId);

            if (deleted == 0) {
                throw new RuntimeException("Лайк не найден для фильма " + filmId + " и пользователя " + userId);
            }

            log.info("Пользователь {} удалил лайк у фильма {}", userId, filmId);
            return findById(filmId).orElseThrow(() ->
                    new RuntimeException("Фильм с ID " + filmId + " не найден после удаления лайка"));
        } catch (DataAccessException e) {
            log.error("Ошибка при удаления лайка фильму {} пользователем {}", filmId, userId, e);
            throw new RuntimeException("Не удалось удалить лайк", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Film> getPopularFilms(int count) {
        String sql = """
            SELECT f.*, m.id as mpa_id, m.name as mpa_name, m.description as mpa_description,
                   COUNT(l.user_id) as like_count
            FROM films f
            LEFT JOIN mpa_ratings m ON f.mpa_id = m.id
            LEFT JOIN likes l ON f.id = l.film_id
            GROUP BY f.id, m.id, m.name, m.description
            ORDER BY COUNT(l.user_id) DESC, f.id
            LIMIT ?
            """;

        List<Film> films = jdbcTemplate.query(sql, this::mapRowToFilm, count);

        if (!films.isEmpty()) {
            loadGenresForFilms(films);
        }

        return films;
    }

    private Film mapRowToFilm(ResultSet rs, int rowNum) throws SQLException {
        Film film = new Film();
        film.setId(rs.getInt("id"));
        film.setName(rs.getString("name"));
        film.setDescription(rs.getString("description"));
        film.setReleaseDate(rs.getDate("release_date").toLocalDate());
        film.setDuration(rs.getInt("duration"));

        MpaRating mpa = new MpaRating();
        mpa.setId(rs.getInt("mpa_id"));
        mpa.setName(rs.getString("mpa_name"));
        mpa.setDescription(rs.getString("mpa_description"));
        film.setMpa(mpa);

        film.setGenres(new LinkedHashSet<>()); // Используем LinkedHashSet для сохранения порядка
        film.setLikes(new HashSet<>());

        return film;
    }

    private void loadAdditionalDataForSingleFilm(Film film) {
        loadGenresForSingleFilm(film);
        loadLikesForSingleFilm(film);
    }

    private void loadGenresForFilms(List<Film> films) {
        if (films.isEmpty()) {
            return;
        }

        List<Integer> filmIds = films.stream()
                .map(Film::getId)
                .collect(Collectors.toList());

        String placeholders = filmIds.stream()
                .map(id -> "?")
                .collect(Collectors.joining(", "));

        String sql = String.format("""
        SELECT fg.film_id, g.id as genre_id, g.name as genre_name
        FROM film_genres fg
        JOIN genres g ON fg.genre_id = g.id
        WHERE fg.film_id IN (%s)
        ORDER BY fg.film_id, fg.genre_id  -- Сортировка по ID фильма и ID жанра в порядке вставки
        """, placeholders);

        Map<Integer, LinkedHashSet<Genre>> genresByFilmId = jdbcTemplate.query(sql, filmIds.toArray(), rs -> {
            Map<Integer, LinkedHashSet<Genre>> result = new HashMap<>();
            while (rs.next()) {
                Integer filmId = rs.getInt("film_id");
                Genre genre = new Genre(
                        rs.getInt("genre_id"),
                        rs.getString("genre_name")
                );
                result.computeIfAbsent(filmId, k -> new LinkedHashSet<>()).add(genre);
            }
            return result;
        });

        for (Film film : films) {
            LinkedHashSet<Genre> genres = genresByFilmId.getOrDefault(film.getId(), new LinkedHashSet<>());
            film.setGenres(genres);
        }
    }

    private void loadLikesForFilms(List<Film> films) {
        if (films.isEmpty()) {
            return;
        }

        List<Integer> filmIds = films.stream()
                .map(Film::getId)
                .collect(Collectors.toList());

        String placeholders = filmIds.stream()
                .map(id -> "?")
                .collect(Collectors.joining(", "));

        String sql = String.format("""
            SELECT film_id, user_id
            FROM likes
            WHERE film_id IN (%s)
            ORDER BY film_id
            """, placeholders);

        Map<Integer, List<Integer>> likesByFilmId = jdbcTemplate.query(sql, filmIds.toArray(), rs -> {
            Map<Integer, List<Integer>> result = new HashMap<>();
            while (rs.next()) {
                Integer filmId = rs.getInt("film_id");
                Integer userId = rs.getInt("user_id");
                result.computeIfAbsent(filmId, k -> new ArrayList<>()).add(userId);
            }
            return result;
        });

        for (Film film : films) {
            List<Integer> likes = likesByFilmId.getOrDefault(film.getId(), new ArrayList<>());
            film.setLikes(new HashSet<>(likes));
        }
    }

    private void loadGenresForSingleFilm(Film film) {
        String sql = "SELECT g.id, g.name " +
                "FROM genres g " +
                "JOIN film_genres fg ON g.id = fg.genre_id " +
                "WHERE fg.film_id = ? " +
                "ORDER BY fg.genre_id";  // Сохраняем порядок вставки

        LinkedHashSet<Genre> genres = jdbcTemplate.query(sql, rs -> {
            LinkedHashSet<Genre> result = new LinkedHashSet<>();
            while (rs.next()) {
                Genre genre = new Genre();
                genre.setId(rs.getInt("id"));
                genre.setName(rs.getString("name"));
                result.add(genre);
            }
            return result;
        }, film.getId());

        film.setGenres(genres);
    }

    private void loadLikesForSingleFilm(Film film) {
        String sql = "SELECT user_id FROM likes WHERE film_id = ?";
        List<Integer> likes = jdbcTemplate.queryForList(sql, Integer.class, film.getId());
        film.setLikes(new HashSet<>(likes));
    }

    private void saveGenresInTransaction(Integer filmId, Set<Genre> genres) {
        if (genres != null && !genres.isEmpty()) {
            String insertSql = "INSERT INTO film_genres (film_id, genre_id) VALUES (?, ?)";
            List<Object[]> batchArgs = genres.stream()
                    .map(genre -> new Object[]{filmId, genre.getId()})
                    .collect(Collectors.toList());
            jdbcTemplate.batchUpdate(insertSql, batchArgs);
        }
    }

    private void updateGenresInTransaction(Integer filmId, Set<Genre> genres) {
        // Удаляем старые жанры
        String deleteSql = "DELETE FROM film_genres WHERE film_id = ?";
        jdbcTemplate.update(deleteSql, filmId);

        // Добавляем новые, если они есть
        if (genres != null && !genres.isEmpty()) {
            saveGenresInTransaction(filmId, genres);
        }
    }
}