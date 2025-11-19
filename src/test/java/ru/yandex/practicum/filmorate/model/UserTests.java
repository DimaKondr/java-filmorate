package ru.yandex.practicum.filmorate.model;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.*;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class UserTests {
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
    void validUserTesting() {
        User user = new User(1L, "testemail@testemail.com", "TestLogin", "TestName",
                LocalDate.of(2000, Month.JANUARY, 15));
        Set<ConstraintViolation<User>> violations = validator.validate(user);

        assertTrue(violations.isEmpty());
    }

    @Test
    void invalidEmailTesting() {
        User user1 = new User(1L, null, "TestLogin", "TestName",
                LocalDate.of(2000, Month.JANUARY, 15));
        User user2 = new User(1L, "testemail   @testemail.com", "TestLogin", "TestName",
                LocalDate.of(2000, Month.JANUARY, 15));

        Set<ConstraintViolation<User>> violations1 = validator.validate(user1);
        Set<ConstraintViolation<User>> violations2 = validator.validate(user2);

        assertFalse(violations1.isEmpty(), "Должны быть нарушения");
        assertFalse(violations2.isEmpty(), "Должны быть нарушения");

        List<ConstraintViolation<User>> violationsList1= violations1.stream().toList();
        List<ConstraintViolation<User>> violationsList2= violations2.stream().toList();
        String message1 = violationsList1.get(0).getMessage();
        String message2 = violationsList2.get(0).getMessage();

        assertEquals("E-mail не может быть null", message1, "Неверное сообщение об ошибке");
        assertEquals("Указанный E-mail не соответствует формату", message2, "Неверное сообщение об ошибке");
    }

    @Test
    void invalidLoginTesting() {
        User user1 = new User(1L, "testemail@testemail.com", null, "TestName",
                LocalDate.of(2000, Month.JANUARY, 15));
        User user2 = new User(1L, "testemail@testemail.com", "", "TestName",
                LocalDate.of(2000, Month.JANUARY, 15));
        User user3 = new User(1L, "testemail@testemail.com", "Test Login", "TestName",
                LocalDate.of(2000, Month.JANUARY, 15));

        Set<ConstraintViolation<User>> violations1 = validator.validate(user1);
        Set<ConstraintViolation<User>> violations2 = validator.validate(user2);
        Set<ConstraintViolation<User>> violations3 = validator.validate(user3);

        assertFalse(violations1.isEmpty(), "Должны быть нарушения");
        assertFalse(violations2.isEmpty(), "Должны быть нарушения");
        assertFalse(violations3.isEmpty(), "Должны быть нарушения");

        List<ConstraintViolation<User>> violationsList1= violations1.stream().toList();
        List<ConstraintViolation<User>> violationsList2= violations2.stream().toList();
        List<ConstraintViolation<User>> violationsList3= violations3.stream().toList();
        String message1 = violationsList1.get(0).getMessage();
        String message2 = violationsList2.get(0).getMessage();
        String message3 = violationsList3.get(0).getMessage();

        assertEquals("Логин не может быть null", message1, "Неверное сообщение об ошибке");
        assertEquals("Логин не может быть пустым и не должен содержать пробелы",
                message2, "Неверное сообщение об ошибке");
        assertEquals("Логин не может быть пустым и не должен содержать пробелы",
                message3, "Неверное сообщение об ошибке");
    }

    @Test
    void invalidBirthdayTesting() {
        User user1 = new User(1L, "testemail@testemail.com", "TestLogin", "TestName", null);
        User user2 = new User(1L, "testemail@testemail.com", "TestLogin", "TestName",
                LocalDate.of(2030, Month.JANUARY, 15));

        Set<ConstraintViolation<User>> violations1 = validator.validate(user1);
        Set<ConstraintViolation<User>> violations2 = validator.validate(user2);

        assertFalse(violations1.isEmpty(), "Должны быть нарушения");
        assertFalse(violations2.isEmpty(), "Должны быть нарушения");

        List<ConstraintViolation<User>> violationsList1= violations1.stream().toList();
        List<ConstraintViolation<User>> violationsList2= violations2.stream().toList();
        String message1 = violationsList1.get(0).getMessage();
        String message2 = violationsList2.get(0).getMessage();

        assertEquals("Дата рождения не может быть null", message1, "Неверное сообщение об ошибке");
        assertEquals("Дата рождения не может быть в будущем", message2, "Неверное сообщение об ошибке");
    }
}