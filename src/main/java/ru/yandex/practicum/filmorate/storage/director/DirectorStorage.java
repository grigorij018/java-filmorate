package ru.yandex.practicum.filmorate.storage.director;

import jakarta.validation.Valid;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.filmorate.model.Director;

import java.util.List;
import java.util.Optional;

public interface DirectorStorage {

    @Transactional(readOnly = true)
    List<Director> findAll();

    @Transactional(readOnly = true)
    Optional<Director> getById(Integer id);

    @Transactional
    Director create(@Valid Director director);

    @Transactional
    Director update(@Valid Director director);

    @Transactional
    void delete(@Valid Integer id);
}
