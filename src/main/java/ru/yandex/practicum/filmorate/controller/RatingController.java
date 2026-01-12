package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.dao.rating.FilmAgeRatingDAO;
import ru.yandex.practicum.filmorate.model.FilmAgeRating;

import java.util.List;

@RestController
@RequestMapping("/mpa")
@Validated
public class RatingController {
    private final FilmAgeRatingDAO dao;

    @Autowired
    public RatingController(FilmAgeRatingDAO dao) {
        this.dao = dao;
    }

    @GetMapping
    public List<FilmAgeRating> getAllRatings() {
        return dao.getAllRatings();
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public FilmAgeRating getRating(@PathVariable("id")
                              @NotNull(message = "id не может быть null")
                              @Min(value = 1, message = "id должен быть положительным целым числом")
                              @Valid Long id) {
        return dao.getRatingById(id);
    }

}