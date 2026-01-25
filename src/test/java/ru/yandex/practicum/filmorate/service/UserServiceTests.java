package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.model.UserFriendship;

import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class UserServiceTests {
    private final UserService userService;
    User user1;
    User user2;
    UserStorage userStorage;

    @BeforeEach
    void setUp() {
        userStorage = userService.getUserStorage();
        user1 = new User(null, "testemail1@testemail.com", "TestLogin1", "TestName1",
                LocalDate.of(2000, Month.JANUARY, 15));
        user2 = new User(null, "testemail2@testemail.com", "TestLogin2", "TestName2",
                LocalDate.of(2010, Month.OCTOBER, 22));
    }

    @Test
    void addValidFriendTesting() {
        User addedUser1 = userStorage.addUser(user1);
        User addedUser2 = userStorage.addUser(user2);

        User updatedUser1 = userService.addFriend(addedUser1.getId(), addedUser2.getId());
        User updatedUser2 = userService.addFriend(addedUser2.getId(), addedUser1.getId());

        List<User> friendsList1 = new ArrayList<>(userService.getFriendsListOfUser(addedUser1.getId()));
        List<User> friendsList2 = new ArrayList<>(userService.getFriendsListOfUser(addedUser2.getId()));

        UserFriendship test1 = userService.getFriendshipDAO().getFriendshipStatus(addedUser1, addedUser2);
        UserFriendship test2 = userService.getFriendshipDAO().getFriendshipStatus(addedUser2, addedUser1);

        // Проверяем, что в обоих списках нужные данные о дружбе
        assertEquals(1, friendsList1.size(), "Количество элементов не совпадает");
        assertEquals(1, friendsList2.size(), "Количество элементов не совпадает");
        assertEquals(test1.getFriendId(), friendsList1.get(0).getId(), "Данные не совпадают");
        assertEquals(test2.getFriendId(), friendsList2.get(0).getId(), "Данные не совпадают");
    }

    @Test
    void addFriendWithSameIdTesting() {
        User addedUser1 = userStorage.addUser(user1);
        User addedUser2 = userStorage.addUser(user2);

        // Проверяем, что было выброшено необходимое исключение, так как предоставлены одинаковые ID
        ValidationException exception = assertThrows(ValidationException.class,
                () -> userService.addFriend(addedUser2.getId(), addedUser2.getId()),
                "Исключение не выброшено, или выброшено неверное исключение");
        assertEquals("ID=" + addedUser2.getId() + " пользователя и ID= "
                        + addedUser2.getId() + " друга для добавления совпадают",
                exception.getMessage(), "Сообщения не совпадают");
    }

    @Test
    void addFriendWithInvalidUserIdTesting() {
        User addedUser1 = userStorage.addUser(user1);
        User addedUser2 = userStorage.addUser(user2);

        // Сгенерируем случайный ID
        Long uniqueId = generateUniqueId(addedUser1.getId(), addedUser2.getId());

        // Проверяем, что было выброшено необходимое исключение, так как ID пользователя не найден
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> userService.addFriend(uniqueId, addedUser2.getId()),
                "Исключение не выброшено, или выброшено неверное исключение");
        assertEquals("Попытка получения пользователя. Пользователь с ID: " + uniqueId + " не найден",
                exception.getMessage(), "Сообщения не совпадают");
    }

    @Test
    void addFriendWithInvalidFriendIdTesting() {
        User addedUser1 = userStorage.addUser(user1);
        User addedUser2 = userStorage.addUser(user2);

        // Сгенерируем случайный ID
        Long uniqueId = generateUniqueId(addedUser1.getId(), addedUser2.getId());

        // Проверяем, что было выброшено необходимое исключение, так как ID друга не найден
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> userService.addFriend(addedUser1.getId(), uniqueId),
                "Исключение не выброшено, или выброшено неверное исключение");
        assertEquals("Попытка получения пользователя. Пользователь с ID: " + uniqueId + " не найден",
                exception.getMessage(), "Сообщения не совпадают");
    }

    @Test
    void removeValidFriendTesting() {
        User addedUser1 = userStorage.addUser(user1);
        User addedUser2 = userStorage.addUser(user2);

        userService.addFriend(addedUser1.getId(), addedUser2.getId());
        userService.addFriend(addedUser2.getId(), addedUser1.getId());
        List<User> friendsList1 = new ArrayList<>(userService.getFriendsListOfUser(addedUser1.getId()));
        List<User> friendsList2 = new ArrayList<>(userService.getFriendsListOfUser(addedUser2.getId()));

        // Проверяем, что в списках по одному элементу
        assertEquals(1, friendsList1.size(), "В списке не один элемент");
        assertEquals(1, friendsList2.size(), "В списке не один элемент");

        userService.removeFriend(addedUser1.getId(), addedUser2.getId());
        userService.removeFriend(addedUser2.getId(), addedUser1.getId());
        List<User> friendsList3 = new ArrayList<>(userService.getFriendsListOfUser(addedUser1.getId()));
        List<User> friendsList4 = new ArrayList<>(userService.getFriendsListOfUser(addedUser2.getId()));

        // Проверяем, что в оба списка пусты
        assertTrue(friendsList3.isEmpty(), "Список не пуст");
        assertTrue(friendsList4.isEmpty(), "Список не пуст");
    }

    @Test
    void removeFriendWithSameIdTesting() {
        User addedUser1 = userStorage.addUser(user1);
        User addedUser2 = userStorage.addUser(user2);
        userService.addFriend(addedUser1.getId(), addedUser2.getId());

        // Проверяем, что было выброшено необходимое исключение, так как предоставлены одинаковые ID
        ValidationException exception = assertThrows(ValidationException.class,
                () -> userService.removeFriend(addedUser2.getId(), addedUser2.getId()),
                "Исключение не выброшено, или выброшено неверное исключение");
        assertEquals("ID=" + addedUser2.getId() + " пользователя и ID= "
                        + addedUser2.getId() + " друга для добавления совпадают",
                exception.getMessage(), "Сообщения не совпадают");
    }

    @Test
    void removeFriendWithInvalidUserIdTesting() {
        User addedUser1 = userStorage.addUser(user1);
        User addedUser2 = userStorage.addUser(user2);
        userService.addFriend(addedUser1.getId(), addedUser2.getId());

        // Сгенерируем случайный ID
        Long uniqueId = generateUniqueId(addedUser1.getId(), addedUser2.getId());

        // Проверяем, что было выброшено необходимое исключение, так как ID пользователя не найден
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> userService.removeFriend(uniqueId, addedUser2.getId()),
                "Исключение не выброшено, или выброшено неверное исключение");
        assertEquals("Попытка получения пользователя. Пользователь с ID: " + uniqueId + " не найден",
                exception.getMessage(), "Сообщения не совпадают");
    }

    @Test
    void removeFriendWithInvalidFriendIdTesting() {
        User addedUser1 = userStorage.addUser(user1);
        User addedUser2 = userStorage.addUser(user2);
        userService.addFriend(addedUser1.getId(), addedUser2.getId());
        Long invalidId = 999999L;
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> userService.removeFriend(addedUser1.getId(), invalidId),
                "Исключение не выброшено, или выброшено неверное исключение");
        assertEquals("Попытка получения пользователя. Пользователь с ID: " + invalidId + " не найден",
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
        userService.addFriend(addedUser4.getId(), addedUser3.getId());
        userService.addFriend(addedUser4.getId(), addedUser2.getId());

        List<User> mutualFriendsList1 = userService.getMutualFriendsList(addedUser1.getId(), addedUser4.getId());
        List<User> mutualFriendsList2 = userService.getMutualFriendsList(addedUser2.getId(), addedUser4.getId());

        // Проверяем, что в списках верное количество элементов и данные совпадают
        assertEquals(1, mutualFriendsList1.size(), "Количество элементов не совпадает");
        assertEquals(1, mutualFriendsList2.size(), "Количество элементов не совпадает");
        assertEquals(addedUser2.getId(), mutualFriendsList1.get(0).getId(), "ID не совпадают");
        assertEquals(addedUser3.getId(), mutualFriendsList2.get(0).getId(), "ID не совпадают");
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