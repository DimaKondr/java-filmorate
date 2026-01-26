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
        log.info("=== ПОЛУЧЕНИЕ ПОПУЛЯРНЫХ ФИЛЬМОВ ===");
        log.info("Параметры: count={}, genreId={}, year={}", count, genreId, year);

        // Используем метод из FilmStorage (ваш текущий FilmDbStorage уже имеет его)
        List<Film> allPopularFilms = filmStorage.getMostPopularFilms(count.intValue());

        // Если нет фильтрации, возвращаем как есть
        if (genreId == null && year == null) {
            log.info("Без фильтрации, возвращаем {} фильмов", allPopularFilms.size());
            return allPopularFilms;
        }

        // Фильтруем результаты
        List<Film> filteredFilms = new ArrayList<>();

        for (Film film : allPopularFilms) {
            boolean passesFilter = true;

            // Фильтрация по жанру
            if (genreId != null) {
                boolean hasGenre = film.getGenres().stream()
                        .anyMatch(genre -> genreId.equals(genre.getId()));
                if (!hasGenre) {
                    passesFilter = false;
                    log.debug("Фильм ID {} не имеет жанра ID {}, пропускаем", film.getId(), genreId);
                }
            }

            // Фильтрация по году
            if (year != null && passesFilter) {
                if (film.getReleaseDate().getYear() != year) {
                    passesFilter = false;
                    log.debug("Фильм ID {} имеет год {}, а нужен {}, пропускаем",
                            film.getId(), film.getReleaseDate().getYear(), year);
                }
            }

            if (passesFilter) {
                filteredFilms.add(film);
                log.debug("Фильм ID {} прошел фильтрацию", film.getId());
            }
        }

        log.info("=== ПОПУЛЯРНЫЕ ФИЛЬМЫ ПОЛУЧЕНЫ ===");
        log.info("Возвращаем {} отфильтрованных фильмов", filteredFilms.size());
        return filteredFilms;
    }

    public List<Film> getFilmsByDirector(Long directorId, SortType sortType) {
        log.info("Получение фильмов режиссера ID: {} с сортировкой: {}", directorId, sortType);
        return switch (sortType) {
            case LIKES -> filmStorage.getFilmsByDirectorSortedByLikes(directorId);
            case YEAR -> filmStorage.getFilmsByDirectorSortedByYear(directorId);
        };
    }

    public List<Film> getCommonFilms(Long userId, Long friendId) {
        return filmStorage.getCommonFilms(userId, friendId);
    }

    public List<Film> searchFilms(String query, String by) {
        log.info("=== ПОИСК ФИЛЬМОВ В СЕРВИСЕ ===");
        log.info("Запрос: '{}', критерии: '{}'", query, by);

        if (query == null || query.trim().isEmpty()) {
            log.warn("Пустой запрос поиска");
            return List.of();
        }

        List<String> criteria = new ArrayList<>();

        if (by != null) {
            String[] parts = by.split(",");
            for (String part : parts) {
                String trimmed = part.trim().toLowerCase();
                if (trimmed.equals("title") || trimmed.equals("director")) {
                    criteria.add(trimmed);
                }
            }
        }

        if (criteria.isEmpty()) {
            criteria = List.of("title", "director");
        }

        log.info("Критерии поиска: {}", criteria);

        List<Film> result = filmStorage.searchFilms(query, criteria);
        log.info("Найдено {} фильмов", result.size());

        return result;
    }
}