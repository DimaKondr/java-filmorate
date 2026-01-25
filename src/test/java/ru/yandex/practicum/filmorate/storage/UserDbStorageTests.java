package ru.yandex.practicum.filmorate.storage;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.dao.friendship.FriendshipRowMapper;
import ru.yandex.practicum.filmorate.dao.friendship.UserFriendshipDAO;
import ru.yandex.practicum.filmorate.exception.DataBaseException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.UserDbStorage;
import ru.yandex.practicum.filmorate.storage.user.UserRowMapper;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

@JdbcTest
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Import({
        UserDbStorage.class,
        UserRowMapper.class,
        FriendshipRowMapper.class,
        UserFriendshipDAO.class
        })
class UserDbStorageTests {
    private final UserDbStorage userDbStorage;
    User user1;
    User user2;

    @BeforeEach
    void setUp() {
        user1 = new User(null, "testemail1@testemail.com", "TestLogin1", "TestName1",
                LocalDate.of(2000, Month.JANUARY, 15));
        user2 = new User(null, "testemail2@testemail.com", "TestLogin2", "TestName2",
                LocalDate.of(2005, Month.JUNE, 4));
    }

    @Test
    void addValidUserTesting() {
        User addedUser = userDbStorage.addUser(user1);
        User testUser = userDbStorage.getUserById(addedUser.getId());

        // Проверяем, что добавился пользователь с корректными данными
        assertNotNull(testUser, "Пользователь не существует");
        assertEquals(addedUser.getId(), testUser.getId(), "ID не совпадает");
        assertEquals("testemail1@testemail.com", testUser.getEmail(), "E-mail не совпадает");
        assertEquals("TestLogin1", testUser.getLogin(), "Логин не совпадает");
        assertEquals("TestName1", testUser.getName(), "Имя не совпадает");
        assertEquals(LocalDate.of(2000, 1, 15), testUser.getBirthday(), "Дата рождения не совпадает");
    }

    @Test
    void addUserWithNullRequestTesting() {
        // Проверяем, что было выброшено необходимое исключение, так как пытаемся добавить null-объект
        ValidationException exception = assertThrows(ValidationException.class, () -> userDbStorage.addUser(null),
                "Исключение не выброшено, или выброшено неверное исключение");
        assertEquals("Запрос на добавление пользователя поступил с пустым телом",
                exception.getMessage(), "Сообщения не совпадают");
    }

    @Test
    void addUserWithExistingEmailTesting() {
        User user3 = new User(null, "testemail1@testemail.com", "SecondTestLogin", "SecondTestName",
                LocalDate.of(2000, Month.JANUARY, 15));
        userDbStorage.addUser(user1);
        userDbStorage.addUser(user2);

        // Проверяем, что было выброшено необходимое исключение, так как E-mail уже используется
        ValidationException exception = assertThrows(ValidationException.class, () -> userDbStorage.addUser(user3),
                "Исключение не выброшено, или выброшено неверное исключение");
        assertEquals("Добавление пользователя. Указанный E-mail: " + user3.getEmail() + " уже используется",
                exception.getMessage(), "Сообщения не совпадают");
    }

    @Test
    void addValidUserWithNonNameTesting() {
        User user3 = new User(null, "testemail@testemail.com", "TestLogin1", null,
                LocalDate.of(2000, Month.JANUARY, 15));
        User user4 = new User(null, "secondtestemail@testemail.com", "TestLogin2", "",
                LocalDate.of(2000, Month.JANUARY, 15));
        User addedUser1 = userDbStorage.addUser(user3);
        User addedUser2 = userDbStorage.addUser(user4);
        User testUser1 = userDbStorage.getUserById(addedUser1.getId());
        User testUser2 = userDbStorage.getUserById(addedUser2.getId());

        // Проверяем, что пользователи существуют
        assertNotNull(testUser1, "Пользователь не существует");
        assertNotNull(testUser2, "Пользователь не существует");

        // Проверяем, что имя не сохранило null-значение или пустую строку, и приняло значение логина
        assertEquals("TestLogin1", testUser1.getName(), "Имя не совпадает с логином");
        assertEquals("TestLogin2", testUser2.getName(), "Имя не совпадает с логином");
    }

    @Test
    void removeValidUserTesting() {
        User addedUser1 = userDbStorage.addUser(user1);
        User addedUser2 = userDbStorage.addUser(user2);
        Long user2Id = addedUser2.getId();

        // Проверяем, что в хранилище два элемента
        assertEquals(2, userDbStorage.getAllUsers().size(), "Неверное количество элементов в списке");

        userDbStorage.removeUser(addedUser1.getId());

        // Проверяем, что удалился нужный пользователь, и что в списке остался один пользователь с верным ID
        assertEquals(1, userDbStorage.getAllUsers().size(), "Неверное количество элементов в списке");
        assertEquals(addedUser2.getId(), userDbStorage.getUserById(user2Id).getId(), "Неверный ID фильма");
    }

    @Test
    void removeUserWithInvalidIdTesting() {
        userDbStorage.addUser(user1);
        userDbStorage.addUser(user2);

        assertEquals(2, userDbStorage.getAllUsers().size(), "Неверное количество элементов в списке");

        Long invalidId = 999L;

        assertThrows(NotFoundException.class, () -> userDbStorage.removeUser(invalidId),
                "Исключение не выброшено, или выброшено неверное исключение");
    }

    @Test
    void updateValidUserTesting() {
        User addedUser1 = userDbStorage.addUser(user1);

        User user = new User(addedUser1.getId(), "newemail@testemail.com", "NewTestLogin", "NewTestName",
                LocalDate.of(2005, Month.JULY, 10));
        User updatedUser = userDbStorage.updateUser(user);

        // Проверяем, что пользователь обновился с корректными данными
        assertNotNull(updatedUser, "Пользователь не существует");
        assertEquals(addedUser1.getId(), updatedUser.getId(), "Изменился ID в процессе обновления");
        assertEquals("newemail@testemail.com", updatedUser.getEmail(), "E-mail не обновился");
        assertEquals("NewTestLogin", updatedUser.getLogin(), "Логин не обновился");
        assertEquals("NewTestName", updatedUser.getName(), "Имя не обновилось");
        assertEquals(LocalDate.of(2005, 7, 10), updatedUser.getBirthday(), "Дата рождения не обновилась");
    }

    @Test
    void updateUserWithNullRequestTesting() {
        // Проверяем, что было выброшено необходимое исключение, так как пытаемся обновить null-объект
        ValidationException exception = assertThrows(ValidationException.class,
                () -> userDbStorage.updateUser(null),
                "Исключение не выброшено, или выброшено неверное исключение");
        assertEquals("Запрос на обновление данных пользователя поступил с пустым телом",
                exception.getMessage(), "Сообщения не совпадают");
    }

    @Test
    void updateUserWithExistingEmailTesting() {
        User addedUser1 = userDbStorage.addUser(user1);
        User addedUser2 = userDbStorage.addUser(user2);

        User updatedUser = new User(addedUser2.getId(), "testemail1@testemail.com", "AnotherTestLogin",
                "AnotherNewTestName", LocalDate.of(2005, Month.JULY, 10));

        // Проверяем, что было выброшено необходимое исключение, так как E-mail уже используется
        ValidationException exception = assertThrows(ValidationException.class,
                () -> userDbStorage.updateUser(updatedUser),
                "Исключение не выброшено, или выброшено неверное исключение");
        assertEquals("Обновление пользователя. Указанный E-mail: " + updatedUser.getEmail() + " уже используется",
                exception.getMessage(), "Сообщения не совпадают");
    }

    @Test
    void updateUserWithNonExistingIDTesting() {
        User addedUser1 = userDbStorage.addUser(user1);
        User addedUser2 = userDbStorage.addUser(user2);

        // Сгенерируем случайный ID
        Long uniqueId = generateUniqueId(addedUser1, addedUser2);

        User user3 = new User(uniqueId, "testemail@testemail.com", "TestLogin", "TestName",
                LocalDate.of(2000, Month.JANUARY, 15));
        User user4 = new User(null, "testemail@testemail.com", "TestLogin", "TestName",
                LocalDate.of(2000, Month.JANUARY, 15));

        // Проверяем, что было выброшено необходимое исключение, так как ID имеет значение null
        ValidationException exception1 = assertThrows(ValidationException.class,
                () -> userDbStorage.updateUser(user4),
                "Исключение не выброшено, или выброшено неверное исключение");
        assertEquals("ID пользователя должен быть указан", exception1.getMessage(), "Сообщения не совпадают");

        // Проверяем, что было выброшено необходимое исключение, так как ID не найден
        NotFoundException exception2 = assertThrows(NotFoundException.class,
                () -> userDbStorage.updateUser(user3),
                "Исключение не выброшено, или выброшено неверное исключение");
        assertEquals("Обновление пользователя. Пользователь с ID: " + user3.getId() + " не найден",
                exception2.getMessage(), "Сообщения не совпадают");
    }

    @Test
    void updateUserWithNonNameTesting() {
        User addedUser1 = userDbStorage.addUser(user1);
        User addedUser2 = userDbStorage.addUser(user2);

        User user3 = new User(user1.getId(), "testemail@testemail.com", "TestLogin", null,
                LocalDate.of(2000, Month.JANUARY, 15));
        User user4 = new User(user2.getId(), "secondtestemail@testemail.com", "TestLogin", "",
                LocalDate.of(2000, Month.JANUARY, 15));

        User result1 = userDbStorage.updateUser(user3);
        User result2 = userDbStorage.updateUser(user4);

        // Проверяем, что имя не обновилось на null или пустую строку
        assertEquals("TestName1", result1.getName(), "Имя не совпадает с логином");
        assertEquals("TestName2", result2.getName(), "Имя не совпадает с логином");
    }

    @Test
    void getAllUsersTesting() {
        // Проверяем, что было выброшено необходимое исключение, так как список пуст
        DataBaseException exception = assertThrows(DataBaseException.class, () -> userDbStorage.getAllUsers(),
                "Исключение не выброшено, или выброшено неверное исключение");
        assertEquals("Список всех пользователей пуст.",
                exception.getMessage(), "Сообщения не совпадают");

        User user3 = new User(null, "testemail3@testemail.com", "TestLogin3", "TestName3",
                LocalDate.of(2003, Month.NOVEMBER, 8));
        User addedUser1 = userDbStorage.addUser(user1);
        User addedUser2 = userDbStorage.addUser(user2);
        User addedUser3 = userDbStorage.addUser(user3);
        List<User> allUsersTest = userDbStorage.getAllUsers();

        // Проверяем, что список существует, а также проверяем количество пользователей
        assertNotNull(allUsersTest);
        assertEquals(3, allUsersTest.size(), "Неверное количество элементов в списке");

        for (User user : allUsersTest) {
            if (user.getId().equals(addedUser1.getId())) {
                assertEquals("testemail1@testemail.com", user.getEmail(), "E-mail не совпадает");
                assertEquals("TestLogin1", user.getLogin(), "Логин не совпадает");
                assertEquals("TestName1", user.getName(), "Имя не совпадает");
                assertEquals(LocalDate.of(2000, 1, 15), user.getBirthday(), "Дата рождения не совпадает");
                break;
            }
        }

        for (User user : allUsersTest) {
            if (user.getId().equals(addedUser2.getId())) {
                assertEquals("testemail2@testemail.com", user.getEmail(), "E-mail не совпадает");
                assertEquals("TestLogin2", user.getLogin(), "Логин не совпадает");
                assertEquals("TestName2", user.getName(), "Имя не совпадает");
                assertEquals(LocalDate.of(2005, 6, 4), user.getBirthday(), "Дата рождения не совпадает");
                break;
            }
        }

        for (User user : allUsersTest) {
            if (user.getId().equals(addedUser3.getId())) {
                assertEquals("testemail3@testemail.com", user.getEmail(), "E-mail не совпадает");
                assertEquals("TestLogin3", user.getLogin(), "Логин не совпадает");
                assertEquals("TestName3", user.getName(), "Имя не совпадает");
                assertEquals(LocalDate.of(2003, 11, 8), user.getBirthday(), "Дата рождения не совпадает");
                break;
            }
        }
    }

    @Test
    void getUserByValidIdTesting() {
        User addedUser1 = userDbStorage.addUser(user1);
        User addedUser2 = userDbStorage.addUser(user2);

        // Проверяем, что в хранилище два элемента
        assertEquals(2, userDbStorage.getAllUsers().size(), "Неверное количество элементов в списке");

        User receivedUser = userDbStorage.getUserById(addedUser2.getId());

        // Проверяем, что получили нужного пользователя с верными данными
        assertNotNull(receivedUser, "Пользователь не существует");
        assertEquals(addedUser2.getId(), receivedUser.getId(), "ID не совпадает");
        assertEquals("testemail2@testemail.com", receivedUser.getEmail(), "E-mail не совпадает");
        assertEquals("TestLogin2", receivedUser.getLogin(), "Логин не совпадает");
        assertEquals("TestName2", receivedUser.getName(), "Имя не совпадает");
        assertEquals(LocalDate.of(2005, 6, 4), receivedUser.getBirthday(), "Дата рождения не совпадает");
    }

    @Test
    void getUserByInvalidIdTesting() {
        User addedUser1 = userDbStorage.addUser(user1);
        User addedUser2 = userDbStorage.addUser(user2);

        // Проверяем, что в хранилище два элемента
        assertEquals(2, userDbStorage.getAllUsers().size(), "Неверное количество элементов в списке");

        // Сгенерируем случайный ID
        Long uniqueId = generateUniqueId(addedUser1, addedUser2);

        // Проверяем, что было выброшено необходимое исключение, так как пользователя с таким ID нет
        NotFoundException exception = assertThrows(NotFoundException.class, () -> userDbStorage.getUserById(uniqueId),
                "Исключение не выброшено, или выброшено неверное исключение");
        assertEquals("Попытка получения пользователя. Пользователь с ID: " + uniqueId + " не найден",
                exception.getMessage(), "Сообщения не совпадают");
    }

    // Вспомогательный метод для генерации случайного ID, которого не должно быть в базе.
    Long generateUniqueId(User addedUser1, User addedUser2) {
        Random random = new Random();
        long uniqueId;
        while (true) {
            long result = random.nextLong();
            if (result != addedUser1.getId() && result != addedUser2.getId()) {
                uniqueId = result;
                return uniqueId;
            }
        }
    }

}