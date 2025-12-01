package ru.yandex.practicum.filmorate.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MpaRating {
    private Integer id;
    private String name;
    private String description;

    public enum MpaName {
        G("G", "У фильма нет возрастных ограничений"),
        PG("PG", "Детям рекомендуется смотреть фильм с родителями"),
        PG_13("PG-13", "Детям до 13 лет просмотр не желателен"),
        R("R", "Лицам до 17 лет просматривать фильм можно только в присутствии взрослого"),
        NC_17("NC-17", "Лицам до 18 лет просмотр запрещён");

        private final String code;
        private final String description;

        MpaName(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public String getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }
    }
}