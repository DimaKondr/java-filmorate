package ru.yandex.practicum.filmorate.dao.like;

import ru.yandex.practicum.filmorate.model.Film;

import java.util.List;
import java.util.Set;

public interface LikeDAO {

    // Добавляем лайк к фильму.
    void addLikeToFilm(Film film, Long userId);

    // Удаляем лайк у фильма.
    void removeLikeFromFilm(Film film, Long userId);

    // Удаляем все лайки фильма.
    void removeAllLikesFromFilm(Film film);

    // Получаем лайки фильма.
    Set<Long> getLikesOfFilm(Film film);

    // Получаем определенное количество ID самых популярных фильмов.
    List<Long> getIdOfMostPopularFilms(Long mostPopularFilmCount);

}