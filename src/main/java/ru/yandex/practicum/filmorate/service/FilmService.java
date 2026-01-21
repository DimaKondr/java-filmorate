package ru.yandex.practicum.filmorate.service;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.dao.feed.UserFeedDAO;
import ru.yandex.practicum.filmorate.dao.like.LikeDAO;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.SortType;
import ru.yandex.practicum.filmorate.model.Operation;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;

import java.util.*;

@Service
@Getter
@Slf4j
public class FilmService {
    private final FilmStorage filmStorage;
    private final UserService userService;
    private final LikeDAO likeDAO;
    private final UserFeedDAO userFeedDAO;

    @Autowired
    public FilmService(@Qualifier("filmDbStorage") FilmStorage filmStorage,
                       UserService userService,
                       LikeDAO likeDAO,
                       UserFeedDAO userFeedDAO) {
        this.filmStorage = filmStorage;
        this.userService = userService;
        this.likeDAO = likeDAO;
        this.userFeedDAO = userFeedDAO;
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

    public List<Film> getMostPopularFilms(Long mostPopularFilmCount) {
        List<Long> idOfMostPopularFilms = likeDAO.getIdOfMostPopularFilms(mostPopularFilmCount);
        List<Film> mostPopularFilms = new ArrayList<>();

        for (Long id : idOfMostPopularFilms) {
            mostPopularFilms.add(filmStorage.getFilmById(id));
        }
        return mostPopularFilms;
    }

    public List<Film> getFilmsByDirector(Long directorId, SortType sortType) {
       return switch (sortType) {
           case LIKES -> filmStorage.getFilmsByDirectorSortedByLikes(directorId);
           case YEAR -> filmStorage.getFilmsByDirectorSortedByYear(directorId);
       };
    }
}