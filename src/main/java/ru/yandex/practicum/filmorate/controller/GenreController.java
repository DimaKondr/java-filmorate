package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.filmorate.dao.genre.FilmGenreDAO;
import ru.yandex.practicum.filmorate.model.FilmGenre;

import java.util.List;

@RestController
@RequestMapping("/genres")
@Validated
public class GenreController {
    private final FilmGenreDAO dao;

    @Autowired
    public GenreController(FilmGenreDAO dao) {
        this.dao = dao;
    }

    @GetMapping
    public List<FilmGenre> getAllGenres() {
        return dao.getAllGenres();
    }

    @GetMapping("/{id}")
    public FilmGenre getGenre(@PathVariable("id")
                        @NotNull(message = "id не может быть null")
                        @Min(value = 1, message = "id должен быть положительным целым числом")
                        @Valid Long id) {
        return dao.getGenreById(id);
    }

}