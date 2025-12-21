package ru.yandex.practicum.filmorate.service;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.storage.director.DirectorStorage;

import java.util.Collection;

@Slf4j
@Service
@RequiredArgsConstructor
public class DirectorService {
    private final DirectorStorage directorStorage;

    public Collection<Director> findAll() {
        return directorStorage.findAll();
    }

    public Director findById(Integer id) {
        return directorStorage.getById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Режиссёр не найден"));
    }

    public Director create(@Valid Director director) {
        log.info("Добавляем режиссера в коллекцию");

        if (director.getId() != null && directorStorage.getById(director.getId()).isPresent()) {
            return directorStorage.update(director);
        }

        return directorStorage.create(director);
    }

    public Director update(@Valid Director director) {
        log.info("Обновляем режиссера в коллекции");
        findById(director.getId());
        return directorStorage.update(director);
    }

    public void delete(@Valid Integer id) {
        log.info("Удаляем режиссера с id: {}", id);
        if (!directorStorage.delete(id))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Режиссер с таким id отсутствует в базе");
    }
}
