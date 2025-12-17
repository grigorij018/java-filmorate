package ru.yandex.practicum.filmorate.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.model.FeedEvent;
import ru.yandex.practicum.filmorate.service.FeedService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class FeedController {

    private final FeedService feedService;

    @GetMapping("/{id}/feed")
    public List<FeedEvent> getUserFeed(@PathVariable Integer id) {
        log.info("GET /users/{}/feed - получение ленты событий пользователя", id);
        return feedService.getUserFeed(id);
    }
}