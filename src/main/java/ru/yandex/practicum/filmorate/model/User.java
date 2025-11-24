package ru.yandex.practicum.filmorate.model;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;

@Data
@AllArgsConstructor
public class User {
    private Long id;

    @NotNull(message = "E-mail не может быть null")
    @Email(message = "Указанный E-mail не соответствует формату")
    private String email;

    @NotNull(message = "Логин не может быть null")
    @Pattern(regexp = "^\\S+$", message = "Логин не может быть пустым и не должен содержать пробелы")
    private String login;
    private String name;

    @NotNull(message = "Дата рождения не может быть null")
    @Past(message = "Дата рождения не может быть в будущем")
    private LocalDate birthday;
}