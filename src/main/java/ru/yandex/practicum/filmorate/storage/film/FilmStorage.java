package ru.yandex.practicum.filmorate.storage.film;

import ru.yandex.practicum.filmorate.model.Film;

import java.util.List;

public interface FilmStorage {

    // Добавляем новый фильм.
    Film addFilm(Film film);

    // Удаляем имеющийся фильм.
    Film removeFilm(Long filmId);

    // Обновляем имеющийся фильм.
    Film updateFilm(Film updatedFilm);

    // Получаем список всех имеющихся фильмов.
    List<Film> getAllFilms();

    // Получаем фильм по ID.
    Film getFilmById(Long filmId);

}