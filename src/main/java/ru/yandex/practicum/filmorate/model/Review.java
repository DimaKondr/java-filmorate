package ru.yandex.practicum.filmorate.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;

@Data
public class Review {
    private Long reviewId;

    @NotBlank(message = "Название не может быть null или пустым")
    private String content;

    @NotNull(message = "Тип отзыва не может быть null")
    private Boolean isPositive;

    @NotNull(message = "ID пользователя не может быть null")
    private Long userId;

    @NotNull(message = "ID фильма не может быть null")
    private Long filmId;
    private Long useful;

    @Autowired
    public Review(Long reviewId, String content, Boolean isPositive, Long userId, Long filmId, Long useful) {
        this.reviewId = reviewId;
        this.content = content;
        this.isPositive = isPositive;
        this.userId = userId;
        this.filmId = filmId;
        this.useful = useful;
    }
}