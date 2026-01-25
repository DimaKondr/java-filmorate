package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.SortType;
import ru.yandex.practicum.filmorate.service.FilmService;

import java.util.List;

@RestController
@RequestMapping("/films")
@Validated
@Slf4j
public class FilmController {
    private final FilmService filmService;

    @Autowired
    public FilmController(FilmService filmService) {
        this.filmService = filmService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Film addFilm(@Valid @RequestBody Film film) {
        return filmService.getFilmStorage().addFilm(film);
    }

    @GetMapping("/{id}")
    public Film getFilm(@PathVariable("id")
                        @NotNull(message = "id не может быть null")
                        @Min(value = 1, message = "id должен быть положительным целым числом")
                        @Valid Long filmId) {
        return filmService.getFilmStorage().getFilmById(filmId);
    }

    @DeleteMapping("/{id}")
    public Film deleteFilm(@PathVariable("id")
                           @NotNull(message = "id не может быть null")
                           @Min(value = 1, message = "id должен быть положительным целым числом")
                           @Valid Long deletedFilmId) {
        return filmService.getFilmStorage().removeFilm(deletedFilmId);
    }

    @PutMapping
    public Film updateFilm(@Valid @RequestBody Film updatedFilm) {
        return filmService.getFilmStorage().updateFilm(updatedFilm);
    }

    @GetMapping
    public List<Film> getAllFilms() {
        return filmService.getFilmStorage().getAllFilms();
    }

    @PutMapping("/{id}/like/{userId}")
    public Film addLike(@PathVariable("id")
                        @NotNull(message = "id не может быть null")
                        @Min(value = 1, message = "id должен быть положительным целым числом")
                        @Valid Long likedFilmId,
                        @PathVariable("userId")
                        @NotNull(message = "userId не может быть null")
                        @Min(value = 1, message = "userId должен быть положительным целым числом")
                        @Valid Long userId) {
        return filmService.addLike(likedFilmId, userId);
    }

    @DeleteMapping("/{id}/like/{userId}")
    public Film removeLike(@PathVariable("id")
                           @NotNull(message = "id не может быть null")
                           @Min(value = 1, message = "id должен быть положительным целым числом")
                           @Valid Long likedFilmId,
                           @PathVariable("userId")
                           @NotNull(message = "userId не может быть null")
                           @Min(value = 1, message = "userId должен быть положительным целым числом")
                           @Valid Long userId) {
        return filmService.removeLike(likedFilmId, userId);
    }

    @GetMapping("/popular")
    public List<Film> getMostPopularFilms(
            @RequestParam(name = "count", defaultValue = "10")
            @Positive(message = "count должен быть больше 0")
            @Valid Long count,

            @RequestParam(name = "genreId", required = false)
            Long genreId,

            @RequestParam(name = "year", required = false)
            Long year) {

        log.info("Запрос популярных фильмов: count={}, genreId={}, year={}", count, genreId, year);
        return filmService.getMostPopularFilms(count, genreId, year);
    }

    @GetMapping("/director/{directorId}")
    public List<Film> getFilmsByDirector(@PathVariable Long directorId,
                                         @RequestParam(name = "sortBy", defaultValue = "year") String sortBy) {
        log.info("Запрос фильмов режиссера: directorId={}, sortBy={}", directorId, sortBy);

        try {
            SortType sortType = SortType.valueOf(sortBy.toUpperCase());
            return filmService.getFilmsByDirector(directorId, sortType);
        } catch (IllegalArgumentException e) {
            log.error("Некорректное значение sortBy: {}. Допустимые значения: likes, year", sortBy);
            throw new IllegalArgumentException("Параметр sortBy должен быть 'likes' или 'year'");
        }
    }

    @GetMapping("/search")
    public List<Film> searchFilms(
            @RequestParam(name = "query", required = false) String query,
            @RequestParam(name = "by", required = false) String by) {

        log.info("=== ЗАПРОС ПОИСКА ФИЛЬМОВ ===");
        log.info("Параметры: query='{}', by='{}'", query, by);

        // Если query не указан, возвращаем пустой список (как в ТЗ)
        if (query == null || query.trim().isEmpty()) {
            log.info("Запрос поиска пустой, возвращаем пустой список");
            return List.of();
        }

        try {
            List<Film> result = filmService.searchFilms(query, by);
            log.info("Поиск завершен, найдено {} фильмов", result.size());
            return result;
        } catch (Exception e) {
            log.error("Ошибка в контроллере поиска: ", e);
            throw e;
        }
    }

    @GetMapping("/common")
    public List<Film> getCommonFilms(@RequestParam Long userId,
                                     @RequestParam Long friendId) {
        return filmService.getCommonFilms(userId, friendId);
    }
}