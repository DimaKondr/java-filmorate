package ru.yandex.practicum.filmorate.service;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.dao.director.DirectorDAO;
import ru.yandex.practicum.filmorate.dao.feed.UserFeedDAO;
import ru.yandex.practicum.filmorate.dao.genre.GenreDAO;
import ru.yandex.practicum.filmorate.dao.like.LikeDAO;
import ru.yandex.practicum.filmorate.dao.rating.RatingDAO;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.*;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;

import java.util.*;

@Service
@Getter
@Slf4j
public class FilmService {
    private final FilmStorage filmStorage;
    private final UserService userService;
    private final LikeDAO likeDAO;
    private final GenreDAO genreDAO;
    private final RatingDAO ratingDAO;
    private final UserFeedDAO userFeedDAO;
    private final DirectorDAO directorDAO;
    private final DirectorService directorService;

    @Autowired
    public FilmService(@Qualifier("filmDbStorage") FilmStorage filmStorage,
                       UserService userService,
                       LikeDAO likeDAO,
                       GenreDAO genreDAO,
                       RatingDAO ratingDAO,
                       UserFeedDAO userFeedDAO,
                       DirectorDAO directorDAO,
                       DirectorService directorService) {
        this.filmStorage = filmStorage;
        this.userService = userService;
        this.likeDAO = likeDAO;
        this.genreDAO = genreDAO;
        this.ratingDAO = ratingDAO;
        this.userFeedDAO = userFeedDAO;
        this.directorDAO = directorDAO;
        this.directorService = directorService;
    }

    public Film addLike(Long likedFilmId, Long userId) {
        log.info("Начат процесс добавления нового лайка.");
        User user = userService.getUserStorage().getUserById(userId);
        Film film = filmStorage.getFilmById(likedFilmId);
        likeDAO.addLikeToFilm(film, user.getId());

        userFeedDAO.addLikeEvent(userId, likedFilmId, Operation.ADD);

        return film;
    }

    public Film removeLike(Long unlikedFilmId, Long userId) {
        log.info("Начат процесс удаления лайка фильма.");
        try {
            userService.getUserStorage().getUserById(userId);
        } catch (NotFoundException e) {
            throw new NotFoundException("Пользователь с ID: "
                    + userId + " не найден. Невозможно удалить лайк у фильма");
        }
        Film film = filmStorage.getFilmById(unlikedFilmId);
        likeDAO.removeLikeFromFilm(film, userId);

        userFeedDAO.addLikeEvent(userId, unlikedFilmId, Operation.REMOVE);

        return film;
    }

    public List<Film> getMostPopularFilms(Long count, Long genreId, Long year) {
        List<Film> mostPopularFilms = likeDAO.getMostPopularFilms(count, genreId, year);

        for (Film film : mostPopularFilms) {
            film.getGenres().addAll(genreDAO.getGenresOfFilm(film));

            FilmAgeRating rating = ratingDAO.getRatingOfFilm(film);
            if (rating != null) {
                film.getMpa().setId(rating.getId());
                film.getMpa().setName(rating.getName());
            }

            film.getDirectors().addAll(directorDAO.getDirectorsOfFilm(film.getId()));
        }
        return mostPopularFilms;
    }

    public List<Film> getFilmsByDirector(Long directorId, SortType sortType) {
        directorService.getDirectorById(directorId);
        return switch (sortType) {
            case LIKES -> filmStorage.getFilmsByDirectorSortedByLikes(directorId);
            case YEAR -> filmStorage.getFilmsByDirectorSortedByYear(directorId);
        };
    }

    public List<Film> getCommonFilms(Long userId, Long friendId) {
        return filmStorage.getCommonFilms(userId, friendId);
    }
}