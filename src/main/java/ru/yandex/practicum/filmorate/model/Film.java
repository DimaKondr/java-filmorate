package ru.yandex.practicum.filmorate.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;

@Data
@AllArgsConstructor
public class Film {
    private Long id;

    @NotBlank(message = "Название не может быть null или пустым")
    private String name;

    @NotNull(message = "Описание не может быть null")
    @Size(max = 200, message = "Максимальная длина описания — 200 символов")
    private String description;

    @NotNull(message = "Дата релиза не может быть null")
    private LocalDate releaseDate;

    @NotNull(message = "Продолжительность не может быть null")
    @Min(value = 1, message = "Продолжительность фильма должна быть положительным числом")
    private Long duration;
}