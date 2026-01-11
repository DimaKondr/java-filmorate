package ru.yandex.practicum.filmorate.dao.genre;

import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.FilmGenre;

import java.util.List;

public interface GenreDAO {

    // Получаем список всех имеющихся жанров.
    List<FilmGenre> getAllGenres();

    // Получаем жанр по ID.
    FilmGenre getGenreById(Long id);

    // Добавляем жанры к фильму.
    void addGenreToFilm(Film film);

    // Удаляем жанры фильма.
    void removeFilmsGenres(Film film);

    // Получаем жанры фильма.
    List<FilmGenre> getGenresOfFilm(Film film);

}