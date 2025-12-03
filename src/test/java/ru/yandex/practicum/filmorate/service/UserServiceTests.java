package ru.yandex.practicum.filmorate.service;

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

class UserServiceTests {
    UserStorage userStorage;
    UserService userService;
    User user1;
    User user2;

    @BeforeEach
    void setUp() {
        userStorage = new InMemoryUserStorage();
        userService = new UserService(userStorage);
        user1 = new User(null, "testemail1@testemail.com", "TestLogin1", "TestName1",
                LocalDate.of(2000, Month.JANUARY, 15));
        user2 = new User(null, "testemail2@testemail.com", "TestLogin2", "TestName2",
                LocalDate.of(2010, Month.OCTOBER, 22));
    }

    @Test
    void addValidFriendTesting() {
        User addedUser1 = userStorage.addUser(user1);
        User addedUser2 = userStorage.addUser(user2);

        userService.addFriend(addedUser1.getId(), addedUser2.getId());
        List<Long> friendsList1 = new ArrayList<>(addedUser1.getFriendsId());
        List<Long> friendsList2 = new ArrayList<>(addedUser2.getFriendsId());

        // Проверяем, что в обоих списках только один ID с нужным номером
        assertEquals(1, friendsList1.size(), "Количество элементов не совпадает");
        assertEquals(1, friendsList2.size(), "Количество элементов не совпадает");
        assertEquals(2, friendsList1.get(0), "ID не совпадает");
        assertEquals(1, friendsList2.get(0), "ID не совпадает");
    }

    @Test
    void addFriendWithSameIdTesting() {
        User addedUser1 = userStorage.addUser(user1);
        User addedUser2 = userStorage.addUser(user2);

        // Проверяем, что было выброшено необходимое исключение, так как предоставлены одинаковые ID
        ValidationException exception = assertThrows(ValidationException.class,
                () -> userService.addFriend(2L, addedUser2.getId()),
                "Исключение не выброшено, или выброшено неверное исключение");
        assertEquals("ID=" + 2L + " пользователя и ID= "
                        + addedUser2.getId() + " друга для добавления совпадают",
                exception.getMessage(), "Сообщения не совпадают");
    }

    @Test
    void addFriendWithInvalidUserIdTesting() {
        User addedUser1 = userStorage.addUser(user1);
        User addedUser2 = userStorage.addUser(user2);

        // Проверяем, что было выброшено необходимое исключение, так как ID пользователя не найден
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> userService.addFriend(3L, addedUser2.getId()),
                "Исключение не выброшено, или выброшено неверное исключение");
        assertEquals("Попытка получения пользователя. Пользователь с ID: " + 3L + " не найден",
                exception.getMessage(), "Сообщения не совпадают");
    }

    @Test
    void addFriendWithInvalidFriendIdTesting() {
        User addedUser1 = userStorage.addUser(user1);
        User addedUser2 = userStorage.addUser(user2);

        // Проверяем, что было выброшено необходимое исключение, так как ID друга не найден
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> userService.addFriend(addedUser1.getId(), 4L),
                "Исключение не выброшено, или выброшено неверное исключение");
        assertEquals("Попытка получения пользователя. Пользователь с ID: " + 4L + " не найден",
                exception.getMessage(), "Сообщения не совпадают");
    }

    @Test
    void removeValidFriendTesting() {
        User addedUser1 = userStorage.addUser(user1);
        User addedUser2 = userStorage.addUser(user2);

        userService.addFriend(addedUser1.getId(), addedUser2.getId());
        userService.removeFriend(addedUser1.getId(), addedUser2.getId());
        List<Long> friendsList1 = new ArrayList<>(addedUser1.getFriendsId());
        List<Long> friendsList2 = new ArrayList<>(addedUser2.getFriendsId());

        // Проверяем, что в оба списка пусты
        assertTrue(friendsList1.isEmpty(), "Список не пуст");
        assertTrue(friendsList2.isEmpty(), "Список не пуст");
    }

    @Test
    void removeFriendWithSameIdTesting() {
        User addedUser1 = userStorage.addUser(user1);
        User addedUser2 = userStorage.addUser(user2);
        userService.addFriend(addedUser1.getId(), addedUser2.getId());

        // Проверяем, что было выброшено необходимое исключение, так как предоставлены одинаковые ID
        ValidationException exception = assertThrows(ValidationException.class,
                () -> userService.removeFriend(2L, addedUser2.getId()),
                "Исключение не выброшено, или выброшено неверное исключение");
        assertEquals("ID=" + 2L + " пользователя и ID= "
                        + addedUser2.getId() + " друга для добавления совпадают",
                exception.getMessage(), "Сообщения не совпадают");
    }

    @Test
    void removeFriendWithInvalidUserIdTesting() {
        User addedUser1 = userStorage.addUser(user1);
        User addedUser2 = userStorage.addUser(user2);
        userService.addFriend(addedUser1.getId(), addedUser2.getId());

        // Проверяем, что было выброшено необходимое исключение, так как ID пользователя не найден
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> userService.removeFriend(3L, addedUser2.getId()),
                "Исключение не выброшено, или выброшено неверное исключение");
        assertEquals("Попытка получения пользователя. Пользователь с ID: " + 3L + " не найден",
                exception.getMessage(), "Сообщения не совпадают");
    }

    @Test
    void removeFriendWithInvalidFriendIdTesting() {
        User addedUser1 = userStorage.addUser(user1);
        User addedUser2 = userStorage.addUser(user2);
        userService.addFriend(addedUser1.getId(), addedUser2.getId());

        // Проверяем, что было выброшено необходимое исключение, так как ID друга не найден
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> userService.removeFriend(addedUser1.getId(), 4L),
                "Исключение не выброшено, или выброшено неверное исключение");
        assertEquals("Попытка получения пользователя. Пользователь с ID: " + 4L + " не найден",
                exception.getMessage(), "Сообщения не совпадают");
    }

    @Test
    void getFriendsListOfUserTesting() {
        User user3 = new User(null, "testemail3@testemail.com", "TestLogin3", "TestName3",
                LocalDate.of(2000, Month.JANUARY, 15));
        User user4 = new User(null, "testemail4@testemail.com", "TestLogin4", "TestName4",
                LocalDate.of(2000, Month.JANUARY, 15));
        User addedUser1 = userStorage.addUser(user1);
        User addedUser2 = userStorage.addUser(user2);
        User addedUser3 = userStorage.addUser(user3);
        User addedUser4 = userStorage.addUser(user4);

        userService.addFriend(addedUser1.getId(), addedUser2.getId());
        userService.addFriend(addedUser1.getId(), addedUser3.getId());
        userService.addFriend(addedUser1.getId(), addedUser4.getId());

        List<User> testList = new ArrayList<>();
        testList.add(addedUser2);
        testList.add(addedUser3);
        testList.add(addedUser4);
        List<User> friendsList = userService.getFriendsListOfUser(addedUser1.getId());

        // Проверяем, что в списке верное количество друзей и данные совпадают
        assertEquals(3, friendsList.size(), "Количество элементов не совпадает");
        assertEquals(testList, friendsList, "Списки не совпадают");
    }

    @Test
    void getMutualFriendsListTesting() {
        User user3 = new User(null, "testemail3@testemail.com", "TestLogin3", "TestName3",
                LocalDate.of(2000, Month.JANUARY, 15));
        User user4 = new User(null, "testemail4@testemail.com", "TestLogin4", "TestName4",
                LocalDate.of(2000, Month.JANUARY, 15));
        User addedUser1 = userStorage.addUser(user1);
        User addedUser2 = userStorage.addUser(user2);
        User addedUser3 = userStorage.addUser(user3);
        User addedUser4 = userStorage.addUser(user4);

        userService.addFriend(addedUser1.getId(), addedUser2.getId());
        userService.addFriend(addedUser2.getId(), addedUser3.getId());
        userService.addFriend(addedUser3.getId(), addedUser4.getId());

        List<User> mutualFriendsList1 = userService.getMutualFriendsList(addedUser1.getId(), addedUser3.getId());
        List<User> mutualFriendsList2 = userService.getMutualFriendsList(addedUser2.getId(), addedUser4.getId());

        // Проверяем, что в списках верное количество друзей и данные совпадают
        assertEquals(1, mutualFriendsList1.size(), "Количество элементов не совпадает");
        assertEquals(1, mutualFriendsList2.size(), "Количество элементов не совпадает");
        assertEquals(2, mutualFriendsList1.get(0).getId(), "ID не совпадают");
        assertEquals(3, mutualFriendsList2.get(0).getId(), "ID не совпадают");
    }

}