package ru.yandex.practicum.filmorate.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.InMemoryUserStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryUserStorageTests {
    UserStorage userStorage;
    User user1;
    User user2;

    @BeforeEach
    void setUp() {
        userStorage = new InMemoryUserStorage();
        user1 = new User(null, "testemail1@testemail.com", "TestLogin1", "TestName1",
                LocalDate.of(2000, Month.JANUARY, 15));
        user2 = new User(null, "testemail2@testemail.com", "TestLogin2", "TestName2",
                LocalDate.of(2005, Month.JUNE, 4));
    }

    @Test
    void addValidUserTesting() {
        User addedUser = userStorage.addUser(user1);

        // Проверяем, что добавился пользователь с корректными данными
        assertNotNull(addedUser, "Пользователь не существует");
        assertEquals(1, addedUser.getId(), "ID не совпадает");
        assertEquals("testemail1@testemail.com", addedUser.getEmail(), "E-mail не совпадает");
        assertEquals("TestLogin1", addedUser.getLogin(), "Логин не совпадает");
        assertEquals("TestName1", addedUser.getName(), "Имя не совпадает");
        assertEquals(LocalDate.of(2000, 1, 15), addedUser.getBirthday(), "Дата рождения не совпадает");
    }

    @Test
    void addUserWithNullRequestTesting() {
        // Проверяем, что было выброшено необходимое исключение, так как пытаемся добавить null-объект
        ValidationException exception = assertThrows(ValidationException.class, () -> userStorage.addUser(null),
                "Исключение не выброшено, или выброшено неверное исключение");
        assertEquals("Запрос на добавление пользователя поступил с пустым телом",
                exception.getMessage(), "Сообщения не совпадают");
    }

    @Test
    void addUserWithExistingEmailTesting() {
        User user3 = new User(null, "testemail1@testemail.com", "SecondTestLogin", "SecondTestName",
                LocalDate.of(2000, Month.JANUARY, 15));
        userStorage.addUser(user1);

        // Проверяем, что было выброшено необходимое исключение, так как E-mail уже используется
        ValidationException exception = assertThrows(ValidationException.class, () -> userStorage.addUser(user3),
                "Исключение не выброшено, или выброшено неверное исключение");
        assertEquals("Указанный E-mail: " + user3.getEmail() + " уже используется",
                exception.getMessage(), "Сообщения не совпадают");
    }

    @Test
    void addValidUserWithNonNameTesting() {
        User user1 = new User(null, "testemail@testemail.com", "TestLogin", null,
                LocalDate.of(2000, Month.JANUARY, 15));
        User user2 = new User(null, "secondtestemail@testemail.com", "TestLogin", "",
                LocalDate.of(2000, Month.JANUARY, 15));
        User addedUser1 = userStorage.addUser(user1);
        User addedUser2 = userStorage.addUser(user2);

        // Проверяем, что пользователи существуют
        assertNotNull(addedUser1, "Пользователь не существует");
        assertNotNull(addedUser2, "Пользователь не существует");

        // Проверяем, что имя не сохранило null-значение или пустую строку, и приняло значение логина
        assertEquals("TestLogin", addedUser1.getName(), "Имя не совпадает с логином");
        assertEquals("TestLogin", addedUser2.getName(), "Имя не совпадает с логином");
    }

    @Test
    void removeValidUserTesting() {
        User addedUser1 = userStorage.addUser(user1);
        User addedUser2 = userStorage.addUser(user2);

        // Проверяем, что в хранилище два элемента
        assertEquals(2, userStorage.getAllUsers().size(), "Неверное количество элементов в списке");

        userStorage.removeUser(1L);

        // Проверяем, что удалился нужный пользователь, и что в списке остался один пользователь с верным ID
        assertEquals(1, userStorage.getAllUsers().size(), "Неверное количество элементов в списке");
        assertEquals(2L, userStorage.getAllUsers().get(0).getId(), "Неверный ID фильма");
    }

    @Test
    void removeUserWithInvalidIdTesting() {
        User user1 = new User(null, "testemail@testemail.com", "TestLogin", "TestName",
                LocalDate.of(2000, Month.JANUARY, 15));
        User user2 = new User(null, "testemail1@testemail.com", "TestLogin1", "TestName1",
                LocalDate.of(2005, Month.JUNE, 4));
        User addedUser1 = userStorage.addUser(user1);
        User addedUser2 = userStorage.addUser(user2);

        // Проверяем, что в хранилище два элемента
        assertEquals(2, userStorage.getAllUsers().size(), "Неверное количество элементов в списке");

        // Проверяем, что было выброшено необходимое исключение, так как пользователя с таким ID нет
        NotFoundException exception = assertThrows(NotFoundException.class, () -> userStorage.removeUser(3L),
                "Исключение не выброшено, или выброшено неверное исключение");
        assertEquals("Попытка удаления пользователя. Пользователь с ID: " + 3L + " не найден",
                exception.getMessage(), "Сообщения не совпадают");
    }

    @Test
    void updateValidUserTesting() {
        userStorage.addUser(user1);

        User user = new User(1L, "newemail@testemail.com", "NewTestLogin", "NewTestName",
                LocalDate.of(2005, Month.JULY, 10));
        User updatedUser = userStorage.updateUser(user);

        // Проверяем, что пользователь обновился с корректными данными
        assertNotNull(updatedUser, "Пользователь не существует");
        assertEquals(1, updatedUser.getId(), "Изменился ID в процессе обновления");
        assertEquals("newemail@testemail.com", updatedUser.getEmail(), "E-mail не обновился");
        assertEquals("NewTestLogin", updatedUser.getLogin(), "Логин не обновился");
        assertEquals("NewTestName", updatedUser.getName(), "Имя не обновилось");
        assertEquals(LocalDate.of(2005, 7, 10), updatedUser.getBirthday(), "Дата рождения не обновилась");
    }

    @Test
    void updateUserWithNullRequestTesting() {
        // Проверяем, что было выброшено необходимое исключение, так как пытаемся обновить null-объект
        ValidationException exception = assertThrows(ValidationException.class,
                () -> userStorage.updateUser(null),
                "Исключение не выброшено, или выброшено неверное исключение");
        assertEquals("Запрос на обновление данных пользователя поступил с пустым телом",
                exception.getMessage(), "Сообщения не совпадают");
    }

    @Test
    void updateUserWithExistingEmailTesting() {
        userStorage.addUser(user1);
        userStorage.addUser(user2);

        User updatedUser = new User(2L, "testemail1@testemail.com", "AnotherTestLogin", "AnotherNewTestName",
                LocalDate.of(2005, Month.JULY, 10));

        // Проверяем, что было выброшено необходимое исключение, так как E-mail уже используется
        ValidationException exception = assertThrows(ValidationException.class,
                () -> userStorage.updateUser(updatedUser),
                "Исключение не выброшено, или выброшено неверное исключение");
        assertEquals("Обновляемый E-mail: " + updatedUser.getEmail() + " уже используется",
                exception.getMessage(), "Сообщения не совпадают");
    }

    @Test
    void updateUserWithNonExistingIDTesting() {
        User user3 = new User(14L, "testemail@testemail.com", "TestLogin", "TestName",
                LocalDate.of(2000, Month.JANUARY, 15));

        // Проверяем, что было выброшено необходимое исключение, так как ID имеет значение null
        ValidationException exception1 = assertThrows(ValidationException.class,
                () -> userStorage.updateUser(user1),
                "Исключение не выброшено, или выброшено неверное исключение");
        assertEquals("ID пользователя должен быть указан", exception1.getMessage(), "Сообщения не совпадают");

        // Проверяем, что было выброшено необходимое исключение, так как ID не найден
        NotFoundException exception2 = assertThrows(NotFoundException.class,
                () -> userStorage.updateUser(user3),
                "Исключение не выброшено, или выброшено неверное исключение");
        assertEquals("Попытка обновления данных пользователя. Пользователь с ID: " + user3.getId() + " не найден",
                exception2.getMessage(), "Сообщения не совпадают");
    }

    @Test
    void updateUserWithNonNameTesting() {
        User userForUpdate = new User(null, "testemail@testemail.com", "TestLogin", "NameForUpdate",
                LocalDate.of(2000, Month.JANUARY, 15));
        User user3 = new User(1L, "testemail@testemail.com", "TestLogin", null,
                LocalDate.of(2000, Month.JANUARY, 15));
        User user4 = new User(1L, "secondtestemail@testemail.com", "TestLogin", "",
                LocalDate.of(2000, Month.JANUARY, 15));
        User testUser = userStorage.addUser(userForUpdate);


        // Проверяем, что пользователь существует
        assertNotNull(testUser, "Пользователь не существует");

        User result1 = userStorage.updateUser(user3);
        User result2 = userStorage.updateUser(user4);

        // Проверяем, что имя не обновилось на null или пустую строку
        assertEquals("NameForUpdate", result1.getName(), "Имя не совпадает с логином");
        assertEquals("NameForUpdate", result2.getName(), "Имя не совпадает с логином");
    }

    @Test
    void getAllUsersTesting() {
        User user3 = new User(null, "testemail3@testemail.com", "TestLogin3", "TestName3",
                LocalDate.of(2003, Month.NOVEMBER, 8));
        userStorage.addUser(user1);
        userStorage.addUser(user2);
        userStorage.addUser(user3);
        List<User> allUsers = userStorage.getAllUsers();

        // Проверяем, что список существует, а также количество пользователей
        assertNotNull(allUsers);
        assertEquals(3, allUsers.size());

        // Создаем тестовых пользователей
        User testUser1 = new User(1L, "testemail1@testemail.com", "TestLogin1", "TestName1",
                LocalDate.of(2000, Month.JANUARY, 15));
        User testUser2 = new User(2L, "testemail2@testemail.com", "TestLogin2", "TestName2",
                LocalDate.of(2005, Month.JUNE, 4));
        User testUser3 = new User(3L, "testemail3@testemail.com", "TestLogin3", "TestName3",
                LocalDate.of(2003, Month.NOVEMBER, 8));

        List<User> testList = new ArrayList<>();
        testList.add(testUser1);
        testList.add(testUser2);
        testList.add(testUser3);

        // Проверяем, что списки совпадают
        assertEquals(testList, allUsers, "Списки не совпадают");
    }

    @Test
    void getUserByValidIdTesting() {
        User addedUser1 = userStorage.addUser(user1);
        User addedUser2 = userStorage.addUser(user2);

        // Проверяем, что в хранилище два элемента
        assertEquals(2, userStorage.getAllUsers().size(), "Неверное количество элементов в списке");

        User receivedUser = userStorage.getUserById(2L);

        // Проверяем, что получили нужного пользователя с верными данными
        assertNotNull(receivedUser, "Пользователь не существует");
        assertEquals(2, receivedUser.getId(), "ID не совпадает");
        assertEquals("testemail2@testemail.com", receivedUser.getEmail(), "E-mail не совпадает");
        assertEquals("TestLogin2", receivedUser.getLogin(), "Логин не совпадает");
        assertEquals("TestName2", receivedUser.getName(), "Имя не совпадает");
        assertEquals(LocalDate.of(2005, 6, 4), receivedUser.getBirthday(), "Дата рождения не совпадает");
    }

    @Test
    void getUserByInvalidIdTesting() {
        User user1 = new User(null, "testemail@testemail.com", "TestLogin", "TestName",
                LocalDate.of(2000, Month.JANUARY, 15));
        User user2 = new User(null, "testemail1@testemail.com", "TestLogin1", "TestName1",
                LocalDate.of(2005, Month.JUNE, 4));
        User addedUser1 = userStorage.addUser(user1);
        User addedUser2 = userStorage.addUser(user2);

        // Проверяем, что в хранилище два элемента
        assertEquals(2, userStorage.getAllUsers().size(), "Неверное количество элементов в списке");

        // Проверяем, что было выброшено необходимое исключение, так как пользователя с таким ID нет
        NotFoundException exception = assertThrows(NotFoundException.class, () -> userStorage.getUserById(3L),
                "Исключение не выброшено, или выброшено неверное исключение");
        assertEquals("Попытка получения пользователя. Пользователь с ID: " + 3L + " не найден",
                exception.getMessage(), "Сообщения не совпадают");
    }

    @Test
    void getNextIdTesting() {
        User user1 = new User(null, "testemail@testemail.com", "TestLogin", "TestName",
                LocalDate.of(2000, Month.JANUARY, 15));
        User user2 = new User(null, "testemail2@testemail.com", "TestLogin2", "TestName2",
                LocalDate.of(1995, Month.MAY, 20));
        User user3 = new User(null, "testemail3@testemail.com", "TestLogin3", "TestName3",
                LocalDate.of(2003, Month.NOVEMBER, 8));
        User addedUser1 = userStorage.addUser(user1);
        User addedUser2 = userStorage.addUser(user2);
        User addedUser3 = userStorage.addUser(user3);

        // Проверяем, что назначен ожидаемый ID.
        assertEquals(1, addedUser1.getId(), "Генерация ID не работает");
        assertEquals(2, addedUser2.getId(), "Генерация ID не работает");
        assertEquals(3, addedUser3.getId(), "Генерация ID не работает");

        User testUser1 = new User(1L, "testemail4@testemail.com", "TestLogin", "TestName",
                LocalDate.of(2000, Month.JANUARY, 15));
        User testUser2 = new User(3L, "testemail5@testemail.com", "TestLogin2", "TestName2",
                LocalDate.of(1995, Month.MAY, 20));
        User testUser3 = new User(10L, "testemail6@testemail.com", "TestLogin3", "TestName3",
                LocalDate.of(2003, Month.NOVEMBER, 8));
        User addedTestUser1 = userStorage.addUser(testUser1);
        User addedTestUser2 = userStorage.addUser(testUser2);
        User addedTestUser3 = userStorage.addUser(testUser3);

        // Проверим, что был переназначен верный ID.
        assertEquals(4, addedTestUser1.getId(), "Генерация ID не работает");
        assertEquals(5, addedTestUser2.getId(), "Генерация ID не работает");
        assertEquals(6, addedTestUser3.getId(), "Генерация ID не работает");
    }

}