package ru.yandex.practicum.filmorate.model;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class FilmTests {
    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        if (validatorFactory != null) {
            validatorFactory.close();
        }
    }

    @Test
    void testValidFilm() {
        StringBuilder sb = new StringBuilder();
        String description200 = sb.repeat("Y", 200).toString();

        assertEquals(200, description200.length(), "Неверная длина описания");

        Film film1 = new Film(1L, "Тестовый фильм", "Очень интересное кино",
                LocalDate.of(2000, Month.JANUARY, 15), 90L);
        Film film2 = new Film(1L, "Тестовый фильм", "",
                LocalDate.of(2000, Month.JANUARY, 15), 90L);
        Film film3 = new Film(1L, "Тестовый фильм", description200,
                LocalDate.of(2000, Month.JANUARY, 15), 90L);
        Set<ConstraintViolation<Film>> violations1 = validator.validate(film1);
        Set<ConstraintViolation<Film>> violations2 = validator.validate(film2);
        Set<ConstraintViolation<Film>> violations3 = validator.validate(film3);

        assertTrue(violations1.isEmpty());
        assertTrue(violations2.isEmpty());
        assertTrue(violations3.isEmpty());
    }

    @Test
    void testInvalidName() {
        Film film1 = new Film(1L, null, "Очень интересное кино",
                LocalDate.of(2000, Month.JANUARY, 15), 90L);
        Film film2 = new Film(1L, "", "Очень интересное кино",
                LocalDate.of(2000, Month.JANUARY, 15), 90L);
        Set<ConstraintViolation<Film>> violations1 = validator.validate(film1);
        Set<ConstraintViolation<Film>> violations2 = validator.validate(film2);

        assertFalse(violations1.isEmpty(), "Должны быть нарушения");
        assertFalse(violations2.isEmpty(), "Должны быть нарушения");

        List<ConstraintViolation<Film>> violationsList1 = violations1.stream().toList();
        List<ConstraintViolation<Film>> violationsList2 = violations2.stream().toList();
        String message1 = violationsList1.get(0).getMessage();
        String message2 = violationsList2.get(0).getMessage();

        assertEquals("Название не может быть null или пустым", message1, "Неверное сообщение об ошибке");
        assertEquals("Название не может быть null или пустым", message2, "Неверное сообщение об ошибке");
    }

    @Test
    void testInvalidDescription() {
        StringBuilder sb = new StringBuilder();
        String description201 = sb.repeat("Y", 201).toString();

        assertTrue(description201.length() > 200, "Короткое описание");

        Film film1 = new Film(1L, "Тестовый фильм", null,
                LocalDate.of(2000, Month.JANUARY, 15), 90L);
        Film film2 = new Film(1L, "Тестовый фильм", description201,
                LocalDate.of(2000, Month.JANUARY, 15), 90L);
        Set<ConstraintViolation<Film>> violations1 = validator.validate(film1);
        Set<ConstraintViolation<Film>> violations2 = validator.validate(film2);

        assertFalse(violations1.isEmpty(), "Должны быть нарушения");
        assertFalse(violations2.isEmpty(), "Должны быть нарушения");

        List<ConstraintViolation<Film>> violationsList1 = violations1.stream().toList();
        List<ConstraintViolation<Film>> violationsList2 = violations2.stream().toList();
        String message1 = violationsList1.get(0).getMessage();
        String message2 = violationsList2.get(0).getMessage();

        assertEquals("Описание не может быть null", message1, "Неверное сообщение об ошибке");
        assertEquals("Максимальная длина описания — 200 символов", message2, "Неверное сообщение об ошибке");
    }

    @Test
    void testInvalidReleaseDate() {
        Film film = new Film(1L, "Тестовый фильм", "Очень интересное кино",
                null, 90L);
        Set<ConstraintViolation<Film>> violations = validator.validate(film);

        assertFalse(violations.isEmpty(), "Должны быть нарушения");

        List<ConstraintViolation<Film>> violationsList = violations.stream().toList();
        String message = violationsList.get(0).getMessage();

        assertEquals("Дата релиза не может быть null", message, "Неверное сообщение об ошибке");
    }

    @Test
    void testInvalidDuration() {
        Film film1 = new Film(1L, "Тестовый фильм", "Очень интересное кино",
                LocalDate.of(2000, Month.JANUARY, 15), null);
        Film film2 = new Film(1L, "Тестовый фильм", "Очень интересное кино",
                LocalDate.of(2000, Month.JANUARY, 15), 0L);
        Film film3 = new Film(1L, "Тестовый фильм", "Очень интересное кино",
                LocalDate.of(2000, Month.JANUARY, 15), -1L);

        Set<ConstraintViolation<Film>> violations1 = validator.validate(film1);
        Set<ConstraintViolation<Film>> violations2 = validator.validate(film2);
        Set<ConstraintViolation<Film>> violations3 = validator.validate(film3);

        assertFalse(violations1.isEmpty(), "Должны быть нарушения");
        assertFalse(violations2.isEmpty(), "Должны быть нарушения");
        assertFalse(violations3.isEmpty(), "Должны быть нарушения");

        List<ConstraintViolation<Film>> violationsList1 = violations1.stream().toList();
        List<ConstraintViolation<Film>> violationsList2 = violations2.stream().toList();
        List<ConstraintViolation<Film>> violationsList3 = violations3.stream().toList();
        String message1 = violationsList1.get(0).getMessage();
        String message2 = violationsList2.get(0).getMessage();
        String message3 = violationsList3.get(0).getMessage();

        assertEquals("Продолжительность не может быть null", message1, "Неверное сообщение об ошибке");
        assertEquals("Продолжительность фильма должна быть положительным числом",
                message2, "Неверное сообщение об ошибке");
        assertEquals("Продолжительность фильма должна быть положительным числом",
                message3, "Неверное сообщение об ошибке");
    }
}