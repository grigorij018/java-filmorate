package ru.yandex.practicum.filmorate.model;

import lombok.Data;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

@Data
public class Film {
    private Integer id;

    @NotBlank(message = "Название не может быть пустым")
    private String name;

    @Size(max = 200, message = "Описание не должно превышать 200 символов")
    private String description;

    @NotNull(message = "Дата релиза обязательна")
    private LocalDate releaseDate;

    @Positive(message = "Продолжительность должна быть положительной")
    private Integer duration;

    private MpaRating mpa;
    private Set<Genre> genres = new LinkedHashSet<>(); // Используем LinkedHashSet вместо HashSet
    private Set<Integer> likes = new LinkedHashSet<>();
    private Set<Director> directors = new LinkedHashSet<>();
}