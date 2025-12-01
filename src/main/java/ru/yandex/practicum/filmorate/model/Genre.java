package ru.yandex.practicum.filmorate.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Genre {
    private Integer id;
    private String name;

    public enum GenreName {
        COMEDY("Комедия"),
        DRAMA("Драма"),
        CARTOON("Мультфильм"),
        THRILLER("Триллер"),
        DOCUMENTARY("Документальный"),
        ACTION("Боевик");

        private final String name;

        GenreName(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }
}