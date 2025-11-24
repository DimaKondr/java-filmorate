package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;
import java.time.Month;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/films")
@Slf4j
public class FilmController {
    private final Map<Long, Film> films = new HashMap<>();

    @PostMapping
    public Film addFilm(@Valid @RequestBody Film film) {
        if (film == null) {
            log.error("Запрос на добавление нового фильма поступил с пустым телом");
            throw new ValidationException("Запрос на добавление фильма поступил с пустым телом");
        }
        film.setId(getNextId());
        log.debug("Новому фильму назначен ID: {}", film.getId());
        films.put(film.getId(), film);
        log.info("Успешно добавлен новый фильм с ID: {}", film.getId());
        return film;
    }

    @PutMapping
    public Film updateFilm(@Valid @RequestBody Film updatedFilm) {
        if (updatedFilm == null) {
            log.error("Запрос на обновление данных фильма поступил с пустым телом");
            throw new ValidationException("Запрос на обновление данных фильма поступил с пустым телом");
        }
        log.info("Начат процесс обновления данных фильма. Проверяем ID фильма");
        if (updatedFilm.getId() == null) {
            log.error("Фильм имеет ID со значением null");
            throw new ValidationException("ID фильма должен быть указан");
        }
        log.info("Начата проверка наличия фильма с ID: {}", updatedFilm.getId());
        if (films.containsKey(updatedFilm.getId())) {
            Film oldFilm = films.get(updatedFilm.getId());
            LocalDate cinemaBirthDate = LocalDate.of(1895, Month.DECEMBER, 28);
            if (!oldFilm.getName().equals(updatedFilm.getName())) {
                log.debug("Устанавливаем обновленное название фильма: {}", updatedFilm.getName());
                oldFilm.setName(updatedFilm.getName());
            }
            if (!oldFilm.getDescription().equals(updatedFilm.getDescription())) {
                log.debug("Обновляем описание фильма: {}", updatedFilm.getDescription());
                oldFilm.setDescription(updatedFilm.getDescription());
            }
            if (!oldFilm.getReleaseDate().isEqual(updatedFilm.getReleaseDate())
                    && updatedFilm.getReleaseDate().isAfter(cinemaBirthDate)) {
                log.debug("Устанавливаем обновленную дату релиза: {}", updatedFilm.getReleaseDate());
                oldFilm.setReleaseDate(updatedFilm.getReleaseDate());
            }
            if (!oldFilm.getDuration().equals(updatedFilm.getDuration())) {
                log.debug("Устанавливаем обновленную длительность фильма: {}", updatedFilm.getDuration());
                oldFilm.setDuration(updatedFilm.getDuration());
            }
            log.info("Данные фильма с ID: {} успешно обновлены", oldFilm.getId());
            return oldFilm;
        }
        log.error("Фильм с ID: {} не найден", updatedFilm.getId());
        throw new ValidationException("Фильм с ID = " + updatedFilm.getId() + " не найден");
    }

    @GetMapping
    public List<Film> getAllFilms() {
        return films.values().stream().toList();
    }

    //Генерируем ID нового фильма
    private long getNextId() {
        long currentMaxId = films.keySet()
                .stream()
                .mapToLong(id -> id)
                .max()
                .orElse(0);
        return ++currentMaxId;
    }

}