package ru.yandex.practicum.filmorate.service;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.storage.director.DirectorStorage;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DirectorService {
    private final DirectorStorage directorStorage;

    public List<Director> findAll() {
        return directorStorage.findAll();
    }

    public Director findById(Integer id) {
        return directorStorage.getById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Режиссёр не найден"));
    }

    public Director create(@Valid Director director) {
        return directorStorage.create(director);
    }

    public Director update(@Valid Director director) {
        return directorStorage.update(director);
    }

    public void delete(@Valid Integer id) {
        directorStorage.delete(id);
    }
}
