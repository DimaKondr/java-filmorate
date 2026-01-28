package ru.yandex.practicum.filmorate.storage;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.dao.director.DirectorRowMapper;
import ru.yandex.practicum.filmorate.dao.like.FilmLikeDAO;
import ru.yandex.practicum.filmorate.dao.rating.FilmAgeRatingDAO;
import ru.yandex.practicum.filmorate.dao.genre.FilmGenreDAO;
import ru.yandex.practicum.filmorate.dao.genre.GenreRowMapper;
import ru.yandex.practicum.filmorate.dao.rating.RatingRowMapper;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.film.FilmDbStorage;
import ru.yandex.practicum.filmorate.storage.film.FilmRowMapper;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Random;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.*;

@JdbcTest
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Import({
        FilmDbStorage.class,
        FilmRowMapper.class,
        FilmGenreDAO.class,
        FilmLikeDAO.class,
        FilmAgeRatingDAO.class,
        GenreRowMapper.class,
        RatingRowMapper.class,
        DirectorRowMapper.class
        })
class FilmDbStorageTests {
    private final FilmDbStorage filmDbStorage;
    Film film1;
    Film film2;

    @BeforeEach
    void setUp() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        film1 = new Film(null, "Name of the film1", "Description of the film1",
                LocalDate.of(1895, Month.DECEMBER, 28), 1L);
        film2 = new Film(null, "Name of the film2", "Description of the film2",
                LocalDate.of(1900, Month.MAY, 11), 5L);
    }

    @Test
    void addValidFilmTesting() {
        Film addedFilm = filmDbStorage.addFilm(film1);
        Film testFilm = filmDbStorage.getFilmById(addedFilm.getId());

        // Проверяем, что добавился фильм с корректными данными
        assertNotNull(testFilm, "Фильм не существует");
        assertEquals(addedFilm.getId(), testFilm.getId(), "ID не совпадает");
        assertEquals("Name of the film1", testFilm.getName(), "Название не совпадает");
        assertEquals("Description of the film1", testFilm.getDescription(), "Описание не совпадает");
        assertEquals(LocalDate.of(1895, 12, 28), testFilm.getReleaseDate(), "Дата релиза не совпадает");
        assertEquals(1L, testFilm.getDuration(), "Длительность не совпадает");
    }

    @Test
    void addFilmWithNullRequestTesting() {
        // Проверяем, что было выброшено необходимое исключение, так как пытаемся добавить null-объект
        ValidationException exception = assertThrows(ValidationException.class, () -> filmDbStorage.addFilm(null),
                "Исключение не выброшено, или выброшено неверное исключение");
        assertEquals("Запрос на добавление фильма поступил с пустым телом",
                exception.getMessage(), "Сообщения не совпадают");
    }

    @Test
    void removeValidFilmTesting() {
        Film addedFilm1 = filmDbStorage.addFilm(film1);
        Film addedFilm2 = filmDbStorage.addFilm(film2);

        // Проверяем, что в хранилище два элемента
        assertEquals(2, filmDbStorage.getAllFilms().size(), "Неверное количество элементов в списке");

        filmDbStorage.removeFilm(addedFilm1.getId());

        // Проверяем, что удалился нужный фильм, и что в списке остался один фильм с верным ID
        assertEquals(1, filmDbStorage.getAllFilms().size(), "Неверное количество элементов в списке");
        assertEquals(addedFilm2.getId(), filmDbStorage.getAllFilms().get(0).getId(), "Неверный ID фильма");
    }

    @Test
    void removeFilmWithInvalidIdTesting() {
        Film addedFilm1 = filmDbStorage.addFilm(film1);
        Film addedFilm2 = filmDbStorage.addFilm(film2);

        // Проверяем, что в хранилище два элемента
        assertEquals(2, filmDbStorage.getAllFilms().size(), "Неверное количество элементов в списке");

        // Сгенерируем случайный ID
        Long uniqueId = generateUniqueId(addedFilm1, addedFilm2);

        // Проверяем, что было выброшено необходимое исключение
        assertThrows(NotFoundException.class, () -> filmDbStorage.removeFilm(uniqueId),
                "Исключение не выброшено, или выброшено неверное исключение");
    }

    @Test
    void updateValidFilmTesting() {
        Film addedFilm1 = filmDbStorage.addFilm(film1);

        Film film = new Film(addedFilm1.getId(), "Updated name of the film", "Updated description of the film",
                LocalDate.of(1995, Month.DECEMBER, 28), 90L);
        Film updatedFilm = filmDbStorage.updateFilm(film);

        // Проверяем, что пользователь обновился с корректными данными
        assertNotNull(updatedFilm, "Фильм не существует");
        assertEquals(addedFilm1.getId(), updatedFilm.getId(), "Изменился ID в процессе обновления");
        assertEquals("Updated name of the film", updatedFilm.getName(), "Название не обновилось");
        assertEquals("Updated description of the film", updatedFilm.getDescription(), "Описание не обновилось");
        assertEquals(LocalDate.of(1995, 12, 28), updatedFilm.getReleaseDate(), "Дата релиза не обновилась");
        assertEquals(90L, updatedFilm.getDuration(), "Длительность не обновилась");
    }

    @Test
    void updateFilmWithNullRequestTesting() {
        // Проверяем, что было выброшено необходимое исключение, так как пытаемся добавить null-объект
        ValidationException exception = assertThrows(ValidationException.class,
                () -> filmDbStorage.updateFilm(null),
                "Исключение не выброшено, или выброшено неверное исключение");
        assertEquals("Запрос на обновление данных фильма поступил с пустым телом",
                exception.getMessage(), "Сообщения не совпадают");
    }

    @Test
    void updateFilmWithNonExistingIDTesting() {
        Film addedFilm1 = filmDbStorage.addFilm(film1);
        Film addedFilm2 = filmDbStorage.addFilm(film2);

        // Сгенерируем случайный ID
        Long uniqueId = generateUniqueId(addedFilm1, addedFilm2);

        Film film3 = new Film(null, "Name of the film1", "Description of the film1",
                LocalDate.of(1997, Month.DECEMBER, 15), 60L);
        Film film4 = new Film(uniqueId, "Name of the film2", "Description of the film2",
                LocalDate.of(2000, Month.DECEMBER, 29), 70L);

        // Проверяем, что было выброшено необходимое исключение, так как ID имеет значение null
        ValidationException exception1 = assertThrows(ValidationException.class,
                () -> filmDbStorage.updateFilm(film3),
                "Исключение не выброшено, или выброшено неверное исключение");
        assertEquals("ID фильма должен быть указан", exception1.getMessage(), "Сообщения не совпадают");

        // Проверяем, что было выброшено необходимое исключение, так как ID не найден
        NotFoundException exception2 = assertThrows(NotFoundException.class,
                () -> filmDbStorage.updateFilm(film4),
                "Исключение не выброшено, или выброшено неверное исключение");
        assertEquals("Обновление фильма. Ошибка при проверке существования ID",
                exception2.getMessage(), "Сообщения не совпадают");
    }

    @Test
    void updateFilmWithInvalidReleaseDateTesting() {
        Film addedFilm1 = filmDbStorage.addFilm(film1);

        Film film = new Film(addedFilm1.getId(), "Updated name of the film", "Updated description of the film",
                LocalDate.of(1895, Month.DECEMBER, 27), 90L);
        Film updatedFilm = filmDbStorage.updateFilm(film);

        // Проверяем, что фильм не обновил данные на некорректные
        assertNotNull(updatedFilm, "Фильм не существует");
        assertEquals(LocalDate.of(1895, 12, 28), updatedFilm.getReleaseDate(), "Дата релиза некорректна");
    }

    @Test
    void getAllFilms() {
        // Проверяем, что список пуст.
        assertTrue(filmDbStorage.getAllFilms().isEmpty(), "Список не пуст!");

        Film film3 = new Film(null, "Name of the film3", "Description of the film3",
                LocalDate.of(2001, Month.DECEMBER, 29), 145L);
        Film addedFilm1 = filmDbStorage.addFilm(film1);
        Film addedFilm2 = filmDbStorage.addFilm(film2);
        Film addedFilm3 = filmDbStorage.addFilm(film3);
        List<Film> allFilmsTest = filmDbStorage.getAllFilms();

        // Проверяем, что список существует, а также количество фильмов
        assertNotNull(allFilmsTest);
        assertEquals(3, allFilmsTest.size(), "Неверное количество элементов в списке");

        for (Film film : allFilmsTest) {
            if (film.getId().equals(addedFilm1.getId())) {
                assertEquals("Name of the film1", film.getName(), "Название не совпадает");
                assertEquals("Description of the film1", film.getDescription(), "Описание не совпадает");
                assertEquals(LocalDate.of(1895, Month.DECEMBER, 28), film.getReleaseDate(), "Дата релиза не совпадает");
                assertEquals(1L, film.getDuration(), "Длительность не совпадает");
                break;
            }
        }

        for (Film film : allFilmsTest) {
            if (film.getId().equals(addedFilm2.getId())) {
                assertEquals("Name of the film2", film.getName(), "Название не совпадает");
                assertEquals("Description of the film2", film.getDescription(), "Описание не совпадает");
                assertEquals(LocalDate.of(1900, Month.MAY, 11), film.getReleaseDate(), "Дата релиза не совпадает");
                assertEquals(5L, film.getDuration(), "Длительность не совпадает");
                break;
            }
        }

        for (Film film : allFilmsTest) {
            if (film.getId().equals(addedFilm3.getId())) {
                assertEquals("Name of the film3", film.getName(), "Название не совпадает");
                assertEquals("Description of the film3", film.getDescription(), "Описание не совпадает");
                assertEquals(LocalDate.of(2001, Month.DECEMBER, 29), film.getReleaseDate(), "Дата релиза не совпадает");
                assertEquals(145L, film.getDuration(), "Длительность не совпадает");
                break;
            }
        }
    }

    @Test
    void getFilmByValidIdTesting() {
        Film addedFilm1 = filmDbStorage.addFilm(film1);
        Film addedFilm2 = filmDbStorage.addFilm(film2);

        // Проверяем, что в хранилище два элемента
        assertEquals(2, filmDbStorage.getAllFilms().size(), "Неверное количество элементов в списке");

        Film receivedFilm = filmDbStorage.getFilmById(addedFilm2.getId());

        // Проверяем, что получили нужный фильм с верными данными
        assertNotNull(receivedFilm, "Фильм не существует");
        assertEquals(addedFilm2.getId(), receivedFilm.getId(), "ID не совпадает");
        assertEquals("Name of the film2", receivedFilm.getName(), "Название не совпадает");
        assertEquals("Description of the film2", receivedFilm.getDescription(), "Описание не совпадает");
        assertEquals(LocalDate.of(1900, 5, 11), receivedFilm.getReleaseDate(), "Дата релиза не совпадает");
        assertEquals(5L, receivedFilm.getDuration(), "Длительность не совпадает");
    }

    @Test
    void getFilmByInvalidIdTesting() {
        Film addedFilm1 = filmDbStorage.addFilm(film1);
        Film addedFilm2 = filmDbStorage.addFilm(film2);

        // Проверяем, что в хранилище два элемента
        assertEquals(2, filmDbStorage.getAllFilms().size(), "Неверное количество элементов в списке");

        // Сгенерируем случайный ID
        Long uniqueId = generateUniqueId(addedFilm1, addedFilm2);

        // Проверяем, что было выброшено необходимое исключение, так как фильма с таким ID нет
        NotFoundException exception = assertThrows(NotFoundException.class, () -> filmDbStorage.getFilmById(uniqueId),
                "Исключение не выброшено, или выброшено неверное исключение");
        assertEquals("Попытка получения фильма. Фильм с ID: " + uniqueId + " не найден",
                exception.getMessage(), "Сообщения не совпадают");
    }

    // Вспомогательный метод для генерации случайного ID, которого не должно быть в базе
    Long generateUniqueId(Film addedFilm1, Film addedFilm2) {
        Random random = new Random();
        long uniqueId;
        while (true) {
            long result = random.nextLong();
            if (result != addedFilm1.getId() && result != addedFilm2.getId()) {
                uniqueId = result;
                return uniqueId;
            }
        }
    }

}