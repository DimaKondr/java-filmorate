package ru.yandex.practicum.filmorate.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FilmControllerTests {
    private FilmController filmController;

    @BeforeEach
    void setUp() {
        filmController = new FilmController();
    }

    @Test
    void addValidFilmTesting() {
        Film film = new Film(null, "Name of the film", "Description of the film",
                LocalDate.of(1895, Month.DECEMBER, 28), 1L);
        Film addedFilm = filmController.addFilm(film);

        // Проверяем, что добавился фильм с корректными данными
        assertNotNull(addedFilm, "Фильм не существует");
        assertEquals(1, addedFilm.getId(), "ID не совпадает");
        assertEquals("Name of the film", addedFilm.getName(), "Название не совпадает");
        assertEquals("Description of the film", addedFilm.getDescription(), "Описание не совпадает");
        assertEquals(LocalDate.of(1895, 12, 28), addedFilm.getReleaseDate(), "Дата релиза не совпадает");
        assertEquals(1L, addedFilm.getDuration(), "Длительность не совпадает");
    }

    @Test
    void addFilmWithNullRequestTesting() {
        // Проверяем, что было выброшено необходимое исключение, так как пытаемся добавить null-объект
        ValidationException exception = assertThrows(ValidationException.class, () -> filmController.addFilm(null),
                "Исключение не выброшено, или выброшено неверное исключение");
        assertEquals("Запрос на добавление фильма поступил с пустым телом",
                exception.getMessage(), "Сообщения не совпадают");
    }

    @Test
    void updateValidFilmTesting() {
        Film filmForUpdate = new Film(null, "Name of the film", "Description of the film",
                LocalDate.of(1895, Month.DECEMBER, 28), 1L);
        filmController.addFilm(filmForUpdate);

        Film film = new Film(1L, "Updated name of the film", "Updated description of the film",
                LocalDate.of(1995, Month.DECEMBER, 28), 90L);
        Film updatedFilm = filmController.updateFilm(film);

        // Проверяем, что пользователь обновился с корректными данными
        assertNotNull(updatedFilm, "Фильм не существует");
        assertEquals(1, updatedFilm.getId(), "Изменился ID в процессе обновления");
        assertEquals("Updated name of the film", updatedFilm.getName(), "Название не обновилось");
        assertEquals("Updated description of the film", updatedFilm.getDescription(), "Описание не обновилось");
        assertEquals(LocalDate.of(1995, 12, 28), updatedFilm.getReleaseDate(), "Дата релиза не обновилась");
        assertEquals(90L, updatedFilm.getDuration(), "Длительность не обновилась");
    }

    @Test
    void updateFilmWithNullRequestTesting() {
        // Проверяем, что было выброшено необходимое исключение, так как пытаемся добавить null-объект
        ValidationException exception = assertThrows(ValidationException.class,
                () -> filmController.updateFilm(null),
                "Исключение не выброшено, или выброшено неверное исключение");
        assertEquals("Запрос на обновление данных фильма поступил с пустым телом",
                exception.getMessage(), "Сообщения не совпадают");
    }

    @Test
    void updateFilmWithNonExistingIDTesting() {
        Film film1 = new Film(null, "Name of the film1", "Description of the film1",
                LocalDate.of(1997, Month.DECEMBER, 15), 60L);
        Film film2 = new Film(7L, "Name of the film2", "Description of the film2",
                LocalDate.of(2000, Month.DECEMBER, 29), 70L);

        // Проверяем, что было выброшено необходимое исключение, так как ID имеет значение null
        ValidationException exception1 = assertThrows(ValidationException.class,
                () -> filmController.updateFilm(film1),
                "Исключение не выброшено, или выброшено неверное исключение");
        assertEquals("ID фильма должен быть указан", exception1.getMessage(), "Сообщения не совпадают");

        // Проверяем, что было выброшено необходимое исключение, так как ID не найден
        ValidationException exception2 = assertThrows(ValidationException.class,
                () -> filmController.updateFilm(film2),
                "Исключение не выброшено, или выброшено неверное исключение");
        assertEquals("Фильм с ID = " + film2.getId() + " не найден",
                exception2.getMessage(), "Сообщения не совпадают");
    }

    @Test
    void updateFilmWithInvalidReleaseDateTesting() {
        Film filmForUpdate = new Film(null, "Name of the film", "Description of the film",
                LocalDate.of(1895, Month.DECEMBER, 28), 1L);
        filmController.addFilm(filmForUpdate);

        Film film = new Film(1L, "Updated name of the film", "Updated description of the film",
                LocalDate.of(1895, Month.DECEMBER, 27), 90L);
        Film updatedFilm = filmController.updateFilm(film);

        // Проверяем, что фильм не обновил данные на некорректные
        assertNotNull(updatedFilm, "Фильм не существует");
        assertEquals(LocalDate.of(1895, 12, 28), updatedFilm.getReleaseDate(), "Дата релиза некорректна");
    }

    @Test
    void getAllFilmsTesting() {
        Film film1 = new Film(null, "Name of the film1", "Description of the film1",
                LocalDate.of(1995, Month.DECEMBER, 29), 90L);
        Film film2 = new Film(null, "Name of the film2", "Description of the film2",
                LocalDate.of(1900, Month.DECEMBER, 29), 120L);
        Film film3 = new Film(null, "Name of the film3", "Description of the film3",
                LocalDate.of(2001, Month.DECEMBER, 29), 145L);
        filmController.addFilm(film1);
        filmController.addFilm(film2);
        filmController.addFilm(film3);
        List<Film> allFilms = filmController.getAllFilms();

        // Проверяем, что список существует, а также количество пользователей
        assertNotNull(allFilms);
        assertEquals(3, allFilms.size());

        // Создаем тестовых пользователей
        Film testFilm1 = new Film(1L, "Name of the film1", "Description of the film1",
                LocalDate.of(1995, Month.DECEMBER, 29), 90L);
        Film testFilm2 = new Film(2L, "Name of the film2", "Description of the film2",
                LocalDate.of(1900, Month.DECEMBER, 29), 120L);
        Film testFilm3 = new Film(3L, "Name of the film3", "Description of the film3",
                LocalDate.of(2001, Month.DECEMBER, 29), 145L);

        List<Film> testList = new ArrayList<>();
        testList.add(testFilm1);
        testList.add(testFilm2);
        testList.add(testFilm3);

        // Проверяем, что списки совпадают
        assertEquals(testList, allFilms, "Списки не совпадают");
    }

    @Test
    void getNextIdTesting() {
        Film film1 = new Film(null, "Name of the film1", "Description of the film1",
                LocalDate.of(1995, Month.DECEMBER, 29), 90L);
        Film film2 = new Film(null, "Name of the film2", "Description of the film2",
                LocalDate.of(1900, Month.DECEMBER, 29), 120L);
        Film film3 = new Film(null, "Name of the film3", "Description of the film3",
                LocalDate.of(2001, Month.DECEMBER, 29), 145L);
        Film addedFilm1 = filmController.addFilm(film1);
        Film addedFilm2 = filmController.addFilm(film2);
        Film addedFilm3 = filmController.addFilm(film3);

        // Проверяем, что назначен ожидаемый ID.
        assertEquals(1, addedFilm1.getId(), "Генерация ID не работает");
        assertEquals(2, addedFilm2.getId(), "Генерация ID не работает");
        assertEquals(3, addedFilm3.getId(), "Генерация ID не работает");

        Film testFilm1 = new Film(1L, "Name of the film1", "Description of the film1",
                LocalDate.of(1995, Month.DECEMBER, 29), 90L);
        Film testFilm2 = new Film(3L, "Name of the film2", "Description of the film2",
                LocalDate.of(1900, Month.DECEMBER, 29), 120L);
        Film testFilm3 = new Film(10L, "Name of the film3", "Description of the film3",
                LocalDate.of(2001, Month.DECEMBER, 29), 145L);
        Film addedTestFilm1 = filmController.addFilm(testFilm1);
        Film addedTestFilm2 = filmController.addFilm(testFilm2);
        Film addedTestFilm3 = filmController.addFilm(testFilm3);

        // Проверим, что был переназначен верный ID.
        assertEquals(4, addedTestFilm1.getId(), "Генерация ID не работает");
        assertEquals(5, addedTestFilm2.getId(), "Генерация ID не работает");
        assertEquals(6, addedTestFilm3.getId(), "Генерация ID не работает");
    }
}