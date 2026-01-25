package ru.yandex.practicum.filmorate.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.practicum.filmorate.annotations.ValidReleaseDate;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Data
public class Film {
    private final Set<FilmGenre> genres = new HashSet<>();
    private final Set<Long> filmLikedUsersId = new HashSet<>();
    private final FilmAgeRating mpa = new FilmAgeRating();
    private final Set<Director> directors = new HashSet<>();
    private Long id;

    @NotBlank(message = "Название не может быть null или пустым")
    private String name;

    @NotNull(message = "Описание не может быть null")
    @Size(max = 200, message = "Максимальная длина описания — 200 символов")
    private String description;

    @ValidReleaseDate
    private LocalDate releaseDate;

    @NotNull(message = "Продолжительность не может быть null")
    @Min(value = 1, message = "Продолжительность фильма должна быть положительным числом")
    private Long duration;

    @Autowired
    public Film(Long id, String name, String description, LocalDate releaseDate, Long duration) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.releaseDate = releaseDate;
        this.duration = duration;
    }
}