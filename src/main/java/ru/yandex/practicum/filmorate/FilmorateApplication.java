package ru.yandex.practicum.filmorate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;


@SpringBootApplication
@EnableTransactionManagement
public class FilmorateApplication {
	public static void main(String[] args) {
		SpringApplication.run(FilmorateApplication.class, args);
	}

}
