package ru.yandex.practicum.filmorate.dao.rating;

import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.FilmAgeRating;

import java.util.List;

public interface RatingDAO {

    // Получаем список всех имеющихся рейтингов.
    List<FilmAgeRating> getAllRatings();

    // Получаем рейтинг по ID.
    FilmAgeRating getRatingById(Long id);

    // Добавляем возрастной рейтинг к фильму
    void addRatingToFilm(Film film);

    // Удаляем возрастной рейтинг фильма.
    void removeFilmRating(Film film);

    // Получаем возрастной рейтинг фильма.
    FilmAgeRating getRatingOfFilm(Film film);

}