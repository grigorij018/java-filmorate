package ru.yandex.practicum.filmorate.storage.director;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.yandex.practicum.filmorate.model.Director;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Repository
@Primary
@RequiredArgsConstructor
public class DirectorDbStorage implements DirectorStorage {
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional(readOnly = true)
    public Collection<Director> findAll() {
        String sqlQuery = "SELECT * FROM director ORDER BY id";
        return jdbcTemplate.query(sqlQuery, this::mapRowToDirector);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Director> getById(Integer id) {
        String sqlQuery = "SELECT * FROM director WHERE id = ?";
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sqlQuery, this::mapRowToDirector, id));
        } catch (DataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    @Transactional
    public Director create(Director director) {
        try {
            // ВСЕГДА используем автоинкремент, игнорируя переданный ID
            String sqlQuery = "INSERT INTO director (name) VALUES (?)";

            KeyHolder keyHolder = new GeneratedKeyHolder();

            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(sqlQuery, Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, director.getName());
                return ps;
            }, keyHolder);

            Integer id = Objects.requireNonNull(keyHolder.getKey()).intValue();
            director.setId(id);

            log.info("Создан режиссёр с ID: {}", id);
            return director;
        } catch (DataIntegrityViolationException e) {
            log.error("Ошибка при добавлении режиссёра с именем: {}", director.getName(), e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Режиссёр с таким именем уже существует", e);
        } catch (DataAccessException e) {
            log.error("Ошибка при добавлении режиссёра: {}", director.getName(), e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Не удалось добавить режиссёра", e);
        }
    }

    @Override
    @Transactional
    public Director update(Director director) {
        try {
            // Проверяем, существует ли режиссер
            if (getById(director.getId()).isEmpty()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Режиссёр не найден");
            }

            String sqlQuery = "UPDATE director SET name = ? WHERE id = ?";
            int updated = jdbcTemplate.update(sqlQuery, director.getName(), director.getId());

            if (updated == 0) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Режиссёр не найден");
            }

            log.info("Обновлен режиссёр с ID: {}", director.getId());
            return director;
        } catch (DataIntegrityViolationException e) {
            log.error("Ошибка при обновлении режиссёра с ID: {}", director.getId(), e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Режиссёр с таким именем уже существует", e);
        } catch (DataAccessException e) {
            log.error("Ошибка при обновлении режиссёра с ID: {}", director.getId(), e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Не удалось обновить режиссёра", e);
        }
    }

    @Override
    @Transactional
    public boolean delete(Integer id) {
        try {
            // Проверяем, существует ли режиссер
            if (getById(id).isEmpty()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Режиссёр не найден");
            }

            // Сначала удаляем связи с фильмами
            String deleteFilmDirectorSql = "DELETE FROM film_director WHERE director_id = ?";
            jdbcTemplate.update(deleteFilmDirectorSql, id);

            // Затем удаляем режиссера
            String sqlQuery = "DELETE FROM director WHERE id = ?";
            int deleted = jdbcTemplate.update(sqlQuery, id);

            if (deleted > 0) {
                log.info("Удален режиссёр с ID: {}", id);
                return true;
            }
            return false;
        } catch (DataAccessException e) {
            log.error("Ошибка при удалении режиссёра с ID: {}", id, e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Не удалось удалить режиссёра", e);
        }
    }

    private Director mapRowToDirector(ResultSet rs, int rowNum) throws SQLException {
        return Director.builder()
                .id(rs.getInt("id"))
                .name(rs.getString("name"))
                .build();
    }
}