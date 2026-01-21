package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.dao.feed.UserFeedDAOImpl;
import ru.yandex.practicum.filmorate.dao.feed.UserFeedRowMapper;
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
import ru.yandex.practicum.filmorate.model.UserFeed;
import ru.yandex.practicum.filmorate.storage.film.FilmDbStorage;
import ru.yandex.practicum.filmorate.storage.film.FilmRowMapper;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.user.UserDbStorage;
import ru.yandex.practicum.filmorate.storage.user.UserRowMapper;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;

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
        UserFeedDAOImpl.class,
        UserFeedRowMapper.class,
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
    private FilmStorage filmStorage;
    private UserStorage userStorage;

    @BeforeEach
    void setUp() {
        filmStorage = filmService.getFilmStorage();
        userStorage = userService.getUserStorage();
    }

    @Test
    void addValidLike_shouldAddLikeAndCreateFeedEvent() {
        User user = new User(null, "testemail@testemail.com", "TestLogin", "TestName",
                LocalDate.of(2000, Month.JANUARY, 15));
        Film film = new Film(null, "Name of the film", "Description of the film",
                LocalDate.of(1895, Month.DECEMBER, 28), 1L);

        User addedUser = userStorage.addUser(user);
        Film addedFilm = filmStorage.addFilm(film);

        Film likedFilm = filmService.addLike(addedFilm.getId(), addedUser.getId());
        List<Long> likes = new ArrayList<>(filmService.getLikeDAO().getLikesOfFilm(addedFilm));

        assertEquals(1, likes.size());
        assertEquals(addedUser.getId(), likes.get(0));

        List<UserFeed> feed = userService.getFeedByUserId(addedUser.getId());
        assertEquals(1, feed.size());
        assertEquals(addedFilm.getId(), feed.get(0).getEntityId());
        assertEquals("LIKE", feed.get(0).getEventType().name());
        assertEquals("ADD", feed.get(0).getOperation().name());
    }

    @Test
    void addLikeToNonExistentFilm_shouldThrowNotFoundException() {
        Film film = new Film(null, "Name of the film", "Description of the film",
                LocalDate.of(1895, Month.DECEMBER, 28), 1L);
        User user = new User(null, "testemail@testemail.com", "TestLogin", "TestName",
                LocalDate.of(2000, Month.JANUARY, 15));

        Film addedFilm = filmStorage.addFilm(film);
        User addedUser = userStorage.addUser(user);
        Long nonExistentId = 999L;

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> filmService.addLike(nonExistentId, addedUser.getId()));

        assertEquals("Попытка получения фильма. Фильм с ID: " + nonExistentId + " не найден",
                exception.getMessage());
    }

    @Test
    void addLikeWithNonExistentUser_shouldThrowNotFoundException() {
        Film film = new Film(null, "Name of the film", "Description of the film",
                LocalDate.of(1895, Month.DECEMBER, 28), 1L);
        User user = new User(null, "testemail@testemail.com", "TestLogin", "TestName",
                LocalDate.of(2000, Month.JANUARY, 15));

        Film addedFilm = filmStorage.addFilm(film);
        User addedUser = userStorage.addUser(user);
        Long nonExistentId = 999L;

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> filmService.addLike(addedFilm.getId(), nonExistentId));

        assertEquals("Попытка получения пользователя. Пользователь с ID: " + nonExistentId + " не найден",
                exception.getMessage());
    }

    @Test
    void removeLike_shouldRemoveLikeAndCreateFeedEvent() {
        Film film = new Film(null, "Name of the film", "Description of the film",
                LocalDate.of(1895, Month.DECEMBER, 28), 1L);
        User user = new User(null, "testemail@testemail.com", "TestLogin", "TestName",
                LocalDate.of(2000, Month.JANUARY, 15));

        Film addedFilm = filmStorage.addFilm(film);
        User addedUser = userStorage.addUser(user);

        filmService.addLike(addedFilm.getId(), addedUser.getId());

        List<Long> likesBefore = new ArrayList<>(filmService.getLikeDAO().getLikesOfFilm(addedFilm));
        assertEquals(1, likesBefore.size());

        filmService.removeLike(addedFilm.getId(), addedUser.getId());

        List<Long> likesAfter = new ArrayList<>(filmService.getLikeDAO().getLikesOfFilm(addedFilm));
        assertTrue(likesAfter.isEmpty());

        List<UserFeed> feed = userService.getFeedByUserId(addedUser.getId());
        assertEquals(2, feed.size());

        // Проверяем, что первое событие - ADD, второе - REMOVE
        assertEquals("ADD", feed.get(0).getOperation().name());
        assertEquals("REMOVE", feed.get(1).getOperation().name());
    }

    @Test
    void removeLikeFromNonExistentFilm_shouldThrowNotFoundException() {
        Film film = new Film(null, "Name of the film", "Description of the film",
                LocalDate.of(1895, Month.DECEMBER, 28), 1L);
        User user = new User(null, "testemail@testemail.com", "TestLogin", "TestName",
                LocalDate.of(2000, Month.JANUARY, 15));

        Film addedFilm = filmStorage.addFilm(film);
        User addedUser = userStorage.addUser(user);
        Long nonExistentId = 999L;

        filmService.addLike(addedFilm.getId(), addedUser.getId());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> filmService.removeLike(nonExistentId, addedUser.getId()));

        assertEquals("Попытка получения фильма. Фильм с ID: " + nonExistentId + " не найден",
                exception.getMessage());
    }

    @Test
    void removeLikeWithNonExistentUser_shouldThrowNotFoundException() {
        Film film = new Film(null, "Name of the film", "Description of the film",
                LocalDate.of(1895, Month.DECEMBER, 28), 1L);
        User user = new User(null, "testemail@testemail.com", "TestLogin", "TestName",
                LocalDate.of(2000, Month.JANUARY, 15));

        Film addedFilm = filmStorage.addFilm(film);
        User addedUser = userStorage.addUser(user);
        Long nonExistentId = 999L;

        filmService.addLike(addedFilm.getId(), addedUser.getId());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> filmService.removeLike(addedFilm.getId(), nonExistentId));

        assertTrue(exception.getMessage().contains("не найден"));
    }

    @Test
    void getMostPopularFilms_shouldReturnFilmsSortedByLikes() {
        List<Film> films = new ArrayList<>();
        List<User> users = new ArrayList<>();

        for (int i = 1; i <= 3; i++) {
            Film film = filmStorage.addFilm(new Film(null, "Film " + i,
                    "Description " + i, LocalDate.of(2000, Month.JANUARY, i), 1L));
            films.add(film);
        }

        for (int i = 1; i <= 3; i++) {
            User user = userStorage.addUser(new User(null, "user" + i + "@mail.ru",
                    "login" + i, "Name " + i, LocalDate.of(1990, Month.JANUARY, i)));
            users.add(user);
        }

        List<Film> popularFilms = filmService.getMostPopularFilms(10L);
        assertTrue(popularFilms.isEmpty(), "Должен быть пустым, пока нет лайков");

        filmService.addLike(films.get(0).getId(), users.get(0).getId());
        filmService.addLike(films.get(0).getId(), users.get(1).getId());

        filmService.addLike(films.get(1).getId(), users.get(0).getId());

        filmService.addLike(films.get(2).getId(), users.get(0).getId());
        filmService.addLike(films.get(2).getId(), users.get(1).getId());
        filmService.addLike(films.get(2).getId(), users.get(2).getId());

        popularFilms = filmService.getMostPopularFilms(10L);

        assertEquals(3, popularFilms.size());
        assertEquals(films.get(2).getId(), popularFilms.get(0).getId(), "Фильм с 3 лайками должен быть первым");
        assertEquals(films.get(0).getId(), popularFilms.get(1).getId(), "Фильм с 2 лайками должен быть вторым");
        assertEquals(films.get(1).getId(), popularFilms.get(2).getId(), "Фильм с 1 лайком должен быть третьим");
    }

    @Test
    void userFeedEvents_shouldRecordLikeAndFriendEvents() {
        User user1 = userStorage.addUser(new User(null, "user1@mail.ru", "user1", "User One",
                LocalDate.of(1990, 1, 1)));
        User user2 = userStorage.addUser(new User(null, "user2@mail.ru", "user2", "User Two",
                LocalDate.of(1992, 2, 2)));
        Film film1 = filmStorage.addFilm(new Film(null, "Name of the film", "Description of the film",
                LocalDate.of(1895, Month.DECEMBER, 28), 1L));

        userService.addFriend(user1.getId(), user2.getId());
        filmService.addLike(film1.getId(), user1.getId());

        List<UserFeed> feed1 = userService.getFeedByUserId(user1.getId());
        List<UserFeed> feed2 = userService.getFeedByUserId(user2.getId());

        assertEquals(2, feed1.size(), "У user1 должно быть 2 события");
        assertEquals(0, feed2.size(), "У user2 не должно быть событий (дружба записывается только у инициатора)");

        // Проверяем порядок событий (в порядке создания - старые первыми)
        assertEquals("FRIEND", feed1.get(0).getEventType().name(), "Первое событие должно быть добавлением друга");
        assertEquals("LIKE", feed1.get(1).getEventType().name(), "Второе событие должно быть лайком");
    }

    @Test
    void feedEventsAreSortedChronologically() throws InterruptedException {
        User user1 = userStorage.addUser(new User(null, "user1@mail.ru", "user1", "User One",
                LocalDate.of(1990, 1, 1)));
        Film film1 = filmStorage.addFilm(new Film(null, "Name of the film", "Description of the film",
                LocalDate.of(1895, Month.DECEMBER, 28), 1L));

        // Добавляем первое событие
        filmService.addLike(film1.getId(), user1.getId());
        Thread.sleep(10);

        // Удаляем лайк
        filmService.removeLike(film1.getId(), user1.getId());
        Thread.sleep(10);

        // Добавляем снова
        filmService.addLike(film1.getId(), user1.getId());

        List<UserFeed> feed = userService.getFeedByUserId(user1.getId());

        assertEquals(3, feed.size());

        // События должны быть в хронологическом порядке (старые первыми)
        assertEquals("ADD", feed.get(0).getOperation().name(), "Первое событие - ADD");
        assertEquals("REMOVE", feed.get(1).getOperation().name(), "Второе событие - REMOVE");
        assertEquals("ADD", feed.get(2).getOperation().name(), "Третье событие - ADD");

        // Проверяем, что timestamp возрастает (старые события имеют меньший timestamp)
        assertTrue(feed.get(0).getTimestamp() < feed.get(1).getTimestamp(),
                "Первое событие должно быть раньше второго");
        assertTrue(feed.get(1).getTimestamp() < feed.get(2).getTimestamp(),
                "Второе событие должно быть раньше третьего");
    }
}