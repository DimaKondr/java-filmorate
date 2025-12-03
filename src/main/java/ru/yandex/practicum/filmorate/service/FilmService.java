package ru.yandex.practicum.filmorate.service;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
@Getter
@Slf4j
public class FilmService {
    private final FilmStorage filmStorage;
    private final UserService userService;

    @Autowired
    public FilmService(FilmStorage filmStorage, UserService userService) {
        this.filmStorage = filmStorage;
        this.userService = userService;
    }

    public Film addLike(Long likedFilmId, Long userId) {
        log.info("Начат процесс добавления нового лайка.");
        try {
            userService.getUserStorage().getUserById(userId);
        } catch (NotFoundException e) {
            throw new NotFoundException("Пользователь с ID: "
                    + userId + " не найден. Невозможно поставить лайк фильму");
        }
        Film film = filmStorage.getFilmById(likedFilmId);
        film.addLike(userId);
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
        film.removeLike(userId);
        return film;
    }

    public List<Film> getMostPopularFilms(Long mostPopularFilmCount) {
        log.info("Начат процесс получения списка наиболее популярных фильмов.");
        Map<Long, Long> sortedByLikeFilms = new TreeMap<>(Comparator.reverseOrder());
        for (Film film : filmStorage.getAllFilms()) {
            if (!film.getFilmLikedUsersId().isEmpty()) {
                sortedByLikeFilms.put((long) film.getFilmLikedUsersId().size(), film.getId());
            }
        }
        return sortedByLikeFilms.values().stream()
                .map(filmStorage::getFilmById)
                .limit(mostPopularFilmCount)
                .toList();
    }

}