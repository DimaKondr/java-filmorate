package ru.yandex.practicum.filmorate.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.film.InMemoryFilmStorage;
import ru.yandex.practicum.filmorate.storage.user.InMemoryUserStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FilmServiceTests {
    FilmService filmService;
    FilmStorage filmStorage;
    UserStorage userStorage;
    UserService userService;
    Film film;
    User user;

    @BeforeEach
    void setUp() {
        filmStorage = new InMemoryFilmStorage();
        userStorage = new InMemoryUserStorage();
        userService = new UserService(userStorage);
        filmService = new FilmService(filmStorage, userService);
        film = new Film(null, "Name of the film", "Description of the film",
                LocalDate.of(1895, Month.DECEMBER, 28), 1L);
        user = new User(null, "testemail@testemail.com", "TestLogin", "TestName",
                LocalDate.of(2000, Month.JANUARY, 15));
    }

    @Test
    void addValidLikeTesting() {
        User addedUser = userStorage.addUser(user);
        Film addedFilm = filmStorage.addFilm(film);

        Film likedFilm = filmService.addLike(1L,1L);
        List<Long> filmLikedUsersId = new ArrayList<>(likedFilm.getFilmLikedUsersId());

        // Проверяем, что в списке только один ID с нужным номером
        assertEquals(1, filmLikedUsersId.size(), "Количество элементов не совпадает");
        assertEquals(1, filmLikedUsersId.get(0), "ID не совпадает");
    }

    @Test
    void addLikeToInvalidFilmsIdTesting() {
        Film addedFilm = filmStorage.addFilm(film);
        User addedUser = userStorage.addUser(user);

        // Проверяем, что было выброшено необходимое исключение, так как ID фильма не найден
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> filmService.addLike(2L,addedUser.getId()),
                "Исключение не выброшено, или выброшено неверное исключение");
        assertEquals("Попытка получения фильма. Фильм с ID: " + 2L + " не найден",
                exception.getMessage(), "Сообщения не совпадают");
    }

    @Test
    void addLikeWithInvalidUserIdTesting() {
        Film addedFilm = filmStorage.addFilm(film);
        User addedUser = userStorage.addUser(user);

        // Проверяем, что было выброшено необходимое исключение, так как ID пользователя не найден
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> filmService.addLike(addedFilm.getId(),2L),
                "Исключение не выброшено, или выброшено неверное исключение");
        assertEquals("Пользователь с ID: " + 2L + " не найден. Невозможно поставить лайк фильму",
                exception.getMessage(), "Сообщения не совпадают");
    }

    @Test
    void removeLikeTesting() {
        Film addedFilm = filmStorage.addFilm(film);
        User addedUser = userStorage.addUser(user);

        Film likedFilm = filmService.addLike(1L,1L);
        List<Long> filmLikedUsersId = new ArrayList<>(likedFilm.getFilmLikedUsersId());

        // Проверяем, что в списке только один ID с нужным номером
        assertEquals(1, likedFilm.getFilmLikedUsersId().size(), "Количество элементов не совпадает");
        assertEquals(1L, filmLikedUsersId.get(0), "ID не совпадает");

        filmService.removeLike(1L,1L);

        // Проверяем, что в список лайков пуст
        assertTrue(addedFilm.getFilmLikedUsersId().isEmpty(), "Список не пуст");
    }

    @Test
    void removeLikeFromInvalidFilmsIdTesting() {
        Film addedFilm = filmStorage.addFilm(film);
        User addedUser = userStorage.addUser(user);

        Film likedFilm = filmService.addLike(1L,1L);

        // Проверяем, что было выброшено необходимое исключение, так как ID фильма не найден
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> filmService.removeLike(2L,addedUser.getId()),
                "Исключение не выброшено, или выброшено неверное исключение");
        assertEquals("Попытка получения фильма. Фильм с ID: " + 2L + " не найден",
                exception.getMessage(), "Сообщения не совпадают");
    }

    @Test
    void removeLikeWithInvalidUserIdTesting() {
        Film addedFilm = filmStorage.addFilm(film);
        User addedUser = userStorage.addUser(user);

        Film likedFilm = filmService.addLike(1L,1L);

        // Проверяем, что было выброшено необходимое исключение, так как ID пользователя не найден
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> filmService.removeLike(likedFilm.getId(),2L),
                "Исключение не выброшено, или выброшено неверное исключение");
        assertEquals("Пользователь с ID: " + 2L + " не найден. Невозможно удалить лайк у фильма",
                exception.getMessage(), "Сообщения не совпадают");
    }

    @Test
    void getMostPopularFilms() {
        for (long i = 1; i < 13; i++) {
            filmStorage.addFilm(new Film(null, "Name of the film" + i,
                    "Description of the film" + i, LocalDate.of(1895, Month.DECEMBER, 28), 1L));
        }

        List<Film> result = filmService.getMostPopularFilms(100L);

        // Проверяем, что в списке изначально нет элементов, так как у всех фильмов списки лайков пустые
        assertTrue(result.isEmpty(), "Список не пуст");

        System.out.println(filmStorage.getAllFilms().size());

        Film film1 = filmStorage.getFilmById(1L);
        for (long i = 1; i < 7; i++) {
            film1.getFilmLikedUsersId().add(i);
        }

        Film film2 = filmStorage.getFilmById(2L);
        for (long i = 1; i < 10; i++) {
            film2.getFilmLikedUsersId().add(i);
        }

        Film film3 = filmStorage.getFilmById(3L);
        for (long i = 1; i < 2; i++) {
            film3.getFilmLikedUsersId().add(i);
        }

        Film film4 = filmStorage.getFilmById(4L);
        for (long i = 1; i < 3; i++) {
            film4.getFilmLikedUsersId().add(i);
        }

        Film film5 = filmStorage.getFilmById(5L);
        for (long i = 1; i < 8; i++) {
            film5.getFilmLikedUsersId().add(i);
        }

        Film film6 = filmStorage.getFilmById(6L);
        for (long i = 1; i < 13; i++) {
            film6.getFilmLikedUsersId().add(i);
        }

        Film film7 = filmStorage.getFilmById(7L);
        for (long i = 1; i < 9; i++) {
            film7.getFilmLikedUsersId().add(i);
        }

        Film film8 = filmStorage.getFilmById(8L);
        for (long i = 1; i < 6; i++) {
            film8.getFilmLikedUsersId().add(i);
        }

        Film film9 = filmStorage.getFilmById(9L);
        for (long i = 1; i < 12; i++) {
            film9.getFilmLikedUsersId().add(i);
        }

        Film film10 = filmStorage.getFilmById(10L);
        for (long i = 1; i < 5; i++) {
            film10.getFilmLikedUsersId().add(i);
        }

        Film film11 = filmStorage.getFilmById(11L);
        for (long i = 1; i < 11; i++) {
            film11.getFilmLikedUsersId().add(i);
        }

        Film film12 = filmStorage.getFilmById(12L);
        for (long i = 1; i < 4; i++) {
            film12.getFilmLikedUsersId().add(i);
        }

        List<Film> result1 = filmService.getMostPopularFilms(12L);

        // Проверяем, что в списке верное количество фильмов
        assertEquals(12, result1.size(), "Количество элементов не совпадает");

        // Проверяем, что в списке фильмы в верном порядке (с наибольшего количества лайков по убыванию)
        assertEquals(6L, result1.get(0).getId(), "ID не совпадают");
        assertEquals(9L, result1.get(1).getId(), "ID не совпадают");
        assertEquals(11L, result1.get(2).getId(), "ID не совпадают");
        assertEquals(2L, result1.get(3).getId(), "ID не совпадают");
        assertEquals(7L, result1.get(4).getId(), "ID не совпадают");
        assertEquals(5L, result1.get(5).getId(), "ID не совпадают");
        assertEquals(1L, result1.get(6).getId(), "ID не совпадают");
        assertEquals(8L, result1.get(7).getId(), "ID не совпадают");
        assertEquals(10L, result1.get(8).getId(), "ID не совпадают");
        assertEquals(12L, result1.get(9).getId(), "ID не совпадают");
        assertEquals(4L, result1.get(10).getId(), "ID не совпадают");
        assertEquals(3L, result1.get(11).getId(), "ID не совпадают");
    }

}