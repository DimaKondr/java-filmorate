package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.dao.friendship.FriendshipRowMapper;
import ru.yandex.practicum.filmorate.dao.friendship.UserFriendshipDAO;
import ru.yandex.practicum.filmorate.dao.genre.FilmGenreDAO;
import ru.yandex.practicum.filmorate.dao.genre.GenreRowMapper;
import ru.yandex.practicum.filmorate.dao.like.FilmLikeDAO;
import ru.yandex.practicum.filmorate.dao.rating.FilmAgeRatingDAO;
import ru.yandex.practicum.filmorate.dao.rating.RatingRowMapper;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.film.FilmDbStorage;
import ru.yandex.practicum.filmorate.storage.film.FilmRowMapper;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.user.UserDbStorage;
import ru.yandex.practicum.filmorate.storage.user.UserRowMapper;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

@JdbcTest
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Import({
        FilmDbStorage.class,
        UserDbStorage.class,
        FilmLikeDAO.class,
        FilmGenreDAO.class,
        FilmAgeRatingDAO.class,
        UserFriendshipDAO.class,
        UserService.class,
        FilmService.class,
        FilmRowMapper.class,
        UserRowMapper.class,
        GenreRowMapper.class,
        RatingRowMapper.class,
        FriendshipRowMapper.class
        })
class FilmServiceTests {
    private final FilmService filmService;
    private final UserService userService;

    Film film;
    User user;
    FilmStorage filmStorage;
    UserStorage userStorage;

    @BeforeEach
    void setUp() {
        filmStorage = filmService.getFilmStorage();
        userStorage = userService.getUserStorage();

        film = new Film(null, "Name of the film", "Description of the film",
                LocalDate.of(1895, Month.DECEMBER, 28), 1L);
        user = new User(null, "testemail@testemail.com", "TestLogin", "TestName",
                LocalDate.of(2000, Month.JANUARY, 15));
    }

    @Test
    void addValidLikeTesting() {
        User addedUser = userStorage.addUser(user);
        Film addedFilm = filmStorage.addFilm(film);

        Film likedFilm = filmService.addLike(addedFilm.getId(),addedUser.getId());
        List<Long> test1 = new ArrayList<>(filmService.getLikeDAO().getLikesOfFilm(addedFilm));

        // Проверяем, что в списке только один ID с нужным номером
        assertEquals(1, test1.size(), "Количество элементов не совпадает");
        assertEquals(addedUser.getId(), test1.get(0), "ID не совпадает");
    }

    @Test
    void addLikeToInvalidFilmsIdTesting() {
        Film addedFilm = filmStorage.addFilm(film);
        User addedUser = userStorage.addUser(user);

        // Сгенерируем случайный ID
        Long uniqueId = generateUniqueId(addedFilm.getId(), addedFilm.getId());

        // Проверяем, что было выброшено необходимое исключение, так как ID фильма не найден
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> filmService.addLike(uniqueId,addedUser.getId()),
                "Исключение не выброшено, или выброшено неверное исключение");
        assertEquals("Попытка получения фильма. Фильм с ID: " + uniqueId + " не найден",
                exception.getMessage(), "Сообщения не совпадают");
    }

    @Test
    void addLikeWithInvalidUserIdTesting() {
        Film addedFilm = filmStorage.addFilm(film);
        User addedUser = userStorage.addUser(user);

        // Сгенерируем случайный ID
        Long uniqueId = generateUniqueId(addedUser.getId(), addedUser.getId());

        // Проверяем, что было выброшено необходимое исключение, так как ID пользователя не найден
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> filmService.addLike(addedFilm.getId(),uniqueId),
                "Исключение не выброшено, или выброшено неверное исключение");
        assertEquals("Попытка получения пользователя. Пользователь с ID: " + uniqueId + " не найден",
                exception.getMessage(), "Сообщения не совпадают");
    }

    @Test
    void removeLikeTesting() {
        Film addedFilm = filmStorage.addFilm(film);
        User addedUser = userStorage.addUser(user);

        Film likedFilm = filmService.addLike(addedFilm.getId(), addedUser.getId());
        List<Long> test1 = new ArrayList<>(filmService.getLikeDAO().getLikesOfFilm(addedFilm));

        // Проверяем, что в списке только один ID с нужным номером
        assertEquals(1, test1.size(), "Количество элементов не совпадает");
        assertEquals(addedUser.getId(), test1.get(0), "ID не совпадает");

        filmService.removeLike(addedFilm.getId(), addedUser.getId());
        List<Long> test2 = new ArrayList<>(filmService.getLikeDAO().getLikesOfFilm(addedFilm));

        // Проверяем, что в список лайков пуст
        assertTrue(test2.isEmpty(), "Список не пуст");
    }

    @Test
    void removeLikeFromInvalidFilmsIdTesting() {
        Film addedFilm = filmStorage.addFilm(film);
        User addedUser = userStorage.addUser(user);

        Film likedFilm = filmService.addLike(addedFilm.getId(), addedUser.getId());

        // Сгенерируем случайный ID
        Long uniqueId = generateUniqueId(addedFilm.getId(), addedFilm.getId());

        // Проверяем, что было выброшено необходимое исключение, так как ID фильма не найден
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> filmService.removeLike(uniqueId,addedUser.getId()),
                "Исключение не выброшено, или выброшено неверное исключение");
        assertEquals("Попытка получения фильма. Фильм с ID: " + uniqueId + " не найден",
                exception.getMessage(), "Сообщения не совпадают");
    }

    @Test
    void removeLikeWithInvalidUserIdTesting() {
        Film addedFilm = filmStorage.addFilm(film);
        User addedUser = userStorage.addUser(user);

        Film likedFilm = filmService.addLike(addedFilm.getId(), addedUser.getId());

        Long uniqueId = generateUniqueId(addedUser.getId(), addedUser.getId());

        // Проверяем, что было выброшено необходимое исключение, так как ID пользователя не найден
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> filmService.removeLike(likedFilm.getId(),uniqueId),
                "Исключение не выброшено, или выброшено неверное исключение");
        assertEquals("Пользователь с ID: " + uniqueId + " не найден. Невозможно удалить лайк у фильма",
                exception.getMessage(), "Сообщения не совпадают");
    }

    @Test
    void getMostPopularFilms() {
        List<Long> addedFilmsId = new ArrayList<>();
        List<Long> addedUsersId = new ArrayList<>();

        for (long i = 1; i < 4; i++) {
            Film addedFilm = filmStorage.addFilm(new Film(null, "Name of the film" + i,
                    "Description of the film" + i, LocalDate.of(1895, Month.DECEMBER, 28), 1L));
            addedFilmsId.add(addedFilm.getId());
        }
        addedFilmsId.sort(Comparator.naturalOrder());

        for (long i = 1; i < 4; i++) {
            User addedUser = userStorage.addUser(new User(null, "testEmail" + i + "@mail.ru",
                    "Login" + i, "Name" + i, LocalDate.of(1895, Month.DECEMBER, 28)));
            addedUsersId.add(addedUser.getId());
        }
        addedUsersId.sort(Comparator.naturalOrder());

        List<Film> result = filmService.getMostPopularFilms(100L);

        // Проверяем, что в списке изначально нет элементов, так как у всех фильмов списки лайков пустые
        assertTrue(result.isEmpty(), "Список не пуст");

        Film film1 = filmStorage.getFilmById(addedFilmsId.get(0));
        for (int i = 0; i < 2; i++) {
            filmService.addLike(film1.getId(), addedUsersId.get(i));
        }

        Film film2 = filmStorage.getFilmById(addedFilmsId.get(1));
        for (int i = 0; i < 1; i++) {
            filmService.addLike(film2.getId(), addedUsersId.get(i));
        }

        Film film3 = filmStorage.getFilmById(addedFilmsId.get(2));
        for (int i = 0; i < 3; i++) {
            filmService.addLike(film3.getId(), addedUsersId.get(i));
        }

        List<Film> result1 = filmService.getMostPopularFilms(12L);

        // Проверяем, что в списке верное количество фильмов
        assertEquals(3, result1.size(), "Количество элементов не совпадает");

        // Проверяем, что в списке фильмы в верном порядке (с наибольшего количества лайков по убыванию)
        assertEquals(addedFilmsId.get(2), result1.get(0).getId(), "ID не совпадают");
        assertEquals(addedFilmsId.get(0), result1.get(1).getId(), "ID не совпадают");
        assertEquals(addedFilmsId.get(1), result1.get(2).getId(), "ID не совпадают");
    }

    // Вспомогательный метод для генерации случайного ID, которого не должно быть в базе
    Long generateUniqueId(Long id1, Long id2) {
        Random random = new Random();
        long uniqueId;
        while (true) {
            long result = random.nextLong();
            if (result != id1 && result != id2) {
                uniqueId = result;
                return uniqueId;
            }
        }
    }

}