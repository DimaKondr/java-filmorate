package ru.yandex.practicum.filmorate.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.film.InMemoryFilmStorage;

import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryFilmStorageTests {
    FilmStorage filmStorage;
    Film film1;
    Film film2;

    @BeforeEach
    void setUp() {
        filmStorage = new InMemoryFilmStorage();
        film1 = new Film(null, "Name of the film1", "Description of the film1",
                LocalDate.of(1895, Month.DECEMBER, 28), 1L);
        film2 = new Film(null, "Name of the film2", "Description of the film2",
                LocalDate.of(1900, Month.MAY, 11), 5L);
    }

    @Test
    void addValidFilmTesting() {
        Film addedFilm = filmStorage.addFilm(film1);

        // Проверяем, что добавился фильм с корректными данными
        assertNotNull(addedFilm, "Фильм не существует");
        assertEquals(1, addedFilm.getId(), "ID не совпадает");
        assertEquals("Name of the film1", addedFilm.getName(), "Название не совпадает");
        assertEquals("Description of the film1", addedFilm.getDescription(), "Описание не совпадает");
        assertEquals(LocalDate.of(1895, 12, 28), addedFilm.getReleaseDate(), "Дата релиза не совпадает");
        assertEquals(1L, addedFilm.getDuration(), "Длительность не совпадает");
    }

    @Test
    void addFilmWithNullRequestTesting() {
        // Проверяем, что было выброшено необходимое исключение, так как пытаемся добавить null-объект
        ValidationException exception = assertThrows(ValidationException.class, () -> filmStorage.addFilm(null),
                "Исключение не выброшено, или выброшено неверное исключение");
        assertEquals("Запрос на добавление фильма поступил с пустым телом",
                exception.getMessage(), "Сообщения не совпадают");
    }

    @Test
    void removeValidFilmTesting() {
        Film addedFilm1 = filmStorage.addFilm(film1);
        Film addedFilm2 = filmStorage.addFilm(film2);

        // Проверяем, что в хранилище два элемента
        assertEquals(2, filmStorage.getAllFilms().size(), "Неверное количество элементов в списке");

        filmStorage.removeFilm(1L);

        // Проверяем, что удалился нужный фильм, и что в списке остался один фильм с верным ID
        assertEquals(1, filmStorage.getAllFilms().size(), "Неверное количество элементов в списке");
        assertEquals(2L, filmStorage.getAllFilms().get(0).getId(), "Неверный ID фильма");
    }

    @Test
    void removeFilmWithInvalidIdTesting() {
        Film addedFilm1 = filmStorage.addFilm(film1);
        Film addedFilm2 = filmStorage.addFilm(film2);

        // Проверяем, что в хранилище два элемента
        assertEquals(2, filmStorage.getAllFilms().size(), "Неверное количество элементов в списке");

        // Проверяем, что было выброшено необходимое исключение, так как фильма с таким ID нет
        NotFoundException exception = assertThrows(NotFoundException.class, () -> filmStorage.removeFilm(3L),
                "Исключение не выброшено, или выброшено неверное исключение");
        assertEquals("Попытка удаления фильма. Фильм с ID: " + 3L + " не найден",
                exception.getMessage(), "Сообщения не совпадают");
    }

    @Test
    void updateValidFilmTesting() {
        filmStorage.addFilm(film1);

        Film film = new Film(1L, "Updated name of the film", "Updated description of the film",
                LocalDate.of(1995, Month.DECEMBER, 28), 90L);
        Film updatedFilm = filmStorage.updateFilm(film);

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
                () -> filmStorage.updateFilm(null),
                "Исключение не выброшено, или выброшено неверное исключение");
        assertEquals("Запрос на обновление данных фильма поступил с пустым телом",
                exception.getMessage(), "Сообщения не совпадают");
    }

    @Test
    void updateFilmWithNonExistingIDTesting() {
        Film film3 = new Film(null, "Name of the film1", "Description of the film1",
                LocalDate.of(1997, Month.DECEMBER, 15), 60L);
        Film film4 = new Film(7L, "Name of the film2", "Description of the film2",
                LocalDate.of(2000, Month.DECEMBER, 29), 70L);

        // Проверяем, что было выброшено необходимое исключение, так как ID имеет значение null
        ValidationException exception1 = assertThrows(ValidationException.class,
                () -> filmStorage.updateFilm(film3),
                "Исключение не выброшено, или выброшено неверное исключение");
        assertEquals("ID фильма должен быть указан", exception1.getMessage(), "Сообщения не совпадают");

        // Проверяем, что было выброшено необходимое исключение, так как ID не найден
        NotFoundException exception2 = assertThrows(NotFoundException.class,
                () -> filmStorage.updateFilm(film4),
                "Исключение не выброшено, или выброшено неверное исключение");
        assertEquals("Попытка обновления данных фильма. Фильм с ID = " + film4.getId() + " не найден",
                exception2.getMessage(), "Сообщения не совпадают");
    }

    @Test
    void updateFilmWithInvalidReleaseDateTesting() {
        filmStorage.addFilm(film1);

        Film film = new Film(1L, "Updated name of the film", "Updated description of the film",
                LocalDate.of(1895, Month.DECEMBER, 27), 90L);
        Film updatedFilm = filmStorage.updateFilm(film);

        // Проверяем, что фильм не обновил данные на некорректные
        assertNotNull(updatedFilm, "Фильм не существует");
        assertEquals(LocalDate.of(1895, 12, 28), updatedFilm.getReleaseDate(), "Дата релиза некорректна");
    }

    @Test
    void getAllFilmsTesting() {
        Film film3 = new Film(null, "Name of the film3", "Description of the film3",
                LocalDate.of(2001, Month.DECEMBER, 29), 145L);
        filmStorage.addFilm(film1);
        filmStorage.addFilm(film2);
        filmStorage.addFilm(film3);
        List<Film> allFilms = filmStorage.getAllFilms();

        // Проверяем, что список существует, а также количество пользователей
        assertNotNull(allFilms);
        assertEquals(3, allFilms.size(), "Неверное количество элементов в списке");

        // Создаем тестовые фильмы
        Film testFilm1 = new Film(1L, "Name of the film1", "Description of the film1",
                LocalDate.of(1895, Month.DECEMBER, 28), 1L);
        Film testFilm2 = new Film(2L, "Name of the film2", "Description of the film2",
                LocalDate.of(1900, Month.MAY, 11), 5L);
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
    void getFilmByValidIdTesting() {
        Film addedFilm1 = filmStorage.addFilm(film1);
        Film addedFilm2 = filmStorage.addFilm(film2);

        // Проверяем, что в хранилище два элемента
        assertEquals(2, filmStorage.getAllFilms().size(), "Неверное количество элементов в списке");

        Film receivedFilm = filmStorage.getFilmById(2L);

        // Проверяем, что получили нужный фильм с верными данными
        assertNotNull(receivedFilm, "Фильм не существует");
        assertEquals(2, receivedFilm.getId(), "ID не совпадает");
        assertEquals("Name of the film2", receivedFilm.getName(), "Название не совпадает");
        assertEquals("Description of the film2", receivedFilm.getDescription(), "Описание не совпадает");
        assertEquals(LocalDate.of(1900, 5, 11), receivedFilm.getReleaseDate(), "Дата релиза не совпадает");
        assertEquals(5L, receivedFilm.getDuration(), "Длительность не совпадает");
    }

    @Test
    void getFilmByInvalidIdTesting() {
        Film addedFilm1 = filmStorage.addFilm(film1);
        Film addedFilm2 = filmStorage.addFilm(film2);

        // Проверяем, что в хранилище два элемента
        assertEquals(2, filmStorage.getAllFilms().size(), "Неверное количество элементов в списке");

        // Проверяем, что было выброшено необходимое исключение, так как фильма с таким ID нет
        NotFoundException exception = assertThrows(NotFoundException.class, () -> filmStorage.getFilmById(3L),
                "Исключение не выброшено, или выброшено неверное исключение");
        assertEquals("Попытка получения фильма. Фильм с ID: " + 3L + " не найден",
                exception.getMessage(), "Сообщения не совпадают");
    }

    @Test
    void getNextIdTesting() {
        Film film3 = new Film(null, "Name of the film3", "Description of the film3",
                LocalDate.of(2001, Month.DECEMBER, 29), 145L);
        Film addedFilm1 = filmStorage.addFilm(film1);
        Film addedFilm2 = filmStorage.addFilm(film2);
        Film addedFilm3 = filmStorage.addFilm(film3);

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
        Film addedTestFilm1 = filmStorage.addFilm(testFilm1);
        Film addedTestFilm2 = filmStorage.addFilm(testFilm2);
        Film addedTestFilm3 = filmStorage.addFilm(testFilm3);

        // Проверим, что был переназначен верный ID.
        assertEquals(4, addedTestFilm1.getId(), "Генерация ID не работает");
        assertEquals(5, addedTestFilm2.getId(), "Генерация ID не работает");
        assertEquals(6, addedTestFilm3.getId(), "Генерация ID не работает");
    }

}