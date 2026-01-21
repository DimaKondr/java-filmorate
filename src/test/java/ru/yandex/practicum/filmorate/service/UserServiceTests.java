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
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.model.UserFeed;
import ru.yandex.practicum.filmorate.model.UserFriendship;
import ru.yandex.practicum.filmorate.storage.user.UserDbStorage;
import ru.yandex.practicum.filmorate.storage.user.UserRowMapper;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

@JdbcTest
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Import({
        UserDbStorage.class,
        UserFriendshipDAO.class,
        UserFeedDAOImpl.class,
        UserFeedRowMapper.class,
        UserService.class,
        UserRowMapper.class,
        FriendshipRowMapper.class
})
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

        // Исправленные проверки ленты событий:
        // У каждого пользователя должно быть по 1 событию (оба добавили друг друга)
        List<UserFeed> feed1 = userService.getFeedByUserId(addedUser1.getId());
        List<UserFeed> feed2 = userService.getFeedByUserId(addedUser2.getId());

        assertEquals(1, feed1.size(), "У первого пользователя должно быть 1 событие в ленте");
        assertEquals(1, feed2.size(), "У второго пользователя должно быть 1 событие в ленте");
        assertEquals("FRIEND", feed1.get(0).getEventType().name(), "Тип события должен быть FRIEND");
        assertEquals("ADD", feed1.get(0).getOperation().name(), "Операция должна быть ADD");
        assertEquals(addedUser2.getId(), feed1.get(0).getEntityId(), "ID друга в событии не совпадает");
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

        // Исправленные проверки ленты событий:
        // У каждого пользователя должно быть по 2 события (добавление + удаление)
        List<UserFeed> feed1 = userService.getFeedByUserId(addedUser1.getId());
        List<UserFeed> feed2 = userService.getFeedByUserId(addedUser2.getId());

        assertEquals(2, feed1.size(), "У первого пользователя должно быть 2 события в ленте");
        assertEquals(2, feed2.size(), "У второго пользователя должно быть 2 события в ленте");

        // Проверяем, что последние события - REMOVE
        assertEquals("REMOVE", feed1.get(0).getOperation().name(), "Последняя операция должна быть REMOVE");
        assertEquals("REMOVE", feed2.get(0).getOperation().name(), "Последняя операция должна быть REMOVE");
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

        // Проверяем, что было выброшено необходимое исключение, так как ID друга не найден
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> userService.removeFriend(addedUser1.getId(), 999L),
                "Исключение не выброшено, или выброшено неверное исключение");
        assertEquals("Попытка получения пользователя. Пользователь с ID: " + 999L + " не найден",
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

        // Исправленная проверка ленты событий:
        List<UserFeed> feed = userService.getFeedByUserId(addedUser1.getId());
        assertEquals(3, feed.size(), "Должно быть 3 события в ленте после добавления 3 друзей");
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

    @Test
    void testGetUserFeed() {
        User addedUser1 = userStorage.addUser(user1);
        User addedUser2 = userStorage.addUser(user2);

        // Добавляем друга
        userService.addFriend(addedUser1.getId(), addedUser2.getId());

        // Получаем ленту событий
        List<UserFeed> feed = userService.getFeedByUserId(addedUser1.getId());

        // Проверяем
        assertNotNull(feed, "Лента событий не должна быть null");
        assertEquals(1, feed.size(), "Должно быть 1 событие в ленте");

        UserFeed event = feed.get(0);
        assertEquals(addedUser1.getId(), event.getUserId(), "ID пользователя в событии не совпадает");
        assertEquals("FRIEND", event.getEventType().name(), "Тип события должен быть FRIEND");
        assertEquals("ADD", event.getOperation().name(), "Операция должна быть ADD");
        assertEquals(addedUser2.getId(), event.getEntityId(), "ID друга в событии не совпадает");
        assertNotNull(event.getTimestamp(), "Timestamp не должен быть null");
    }

    @Test
    void testGetUserFeedForNonExistentUser() {
        // Проверяем, что было выброшено исключение для несуществующего пользователя
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> userService.getFeedByUserId(999L),
                "Должно быть выброшено исключение для несуществующего пользователя");
        assertEquals("Попытка получения пользователя. Пользователь с ID: 999 не найден",
                exception.getMessage(), "Сообщения не совпадают");
    }

    @Test
    void testGetEmptyUserFeed() {
        User addedUser1 = userStorage.addUser(user1);

        // Получаем ленту событий для пользователя без событий
        List<UserFeed> feed = userService.getFeedByUserId(addedUser1.getId());

        // Проверяем
        assertNotNull(feed, "Лента событий не должна быть null");
        assertTrue(feed.isEmpty(), "Лента должна быть пустой для нового пользователя");
    }

    @Test
    void testFeedEventsAreSortedByTimestampDesc() {
        User user1 = new User(null, "user1@mail.ru", "user1", "User One",
                LocalDate.of(1990, 1, 1));
        User user2 = new User(null, "user2@mail.ru", "user2", "User Two",
                LocalDate.of(1992, 2, 2));
        User user3 = new User(null, "user3@mail.ru", "user3", "User Three",
                LocalDate.of(1993, 3, 3));

        User savedUser1 = userStorage.addUser(user1);
        User savedUser2 = userStorage.addUser(user2);
        User savedUser3 = userStorage.addUser(user3);

        // Добавляем события
        userService.addFriend(savedUser1.getId(), savedUser2.getId());
        // Небольшая задержка для разницы во времени
        try { Thread.sleep(10); } catch (InterruptedException e) { /* ignore */ }
        userService.addFriend(savedUser1.getId(), savedUser3.getId());

        // Получаем ленту
        List<UserFeed> feed = userService.getFeedByUserId(savedUser1.getId());

        // Проверяем
        assertEquals(2, feed.size(), "Должно быть 2 события в ленте");

        // Проверяем сортировку по времени (новые первыми)
        // Второй добавленный друг должен быть первым в списке
        assertEquals(savedUser3.getId(), feed.get(0).getEntityId(),
                "Последнее добавление должно быть первым в ленте");
        assertEquals(savedUser2.getId(), feed.get(1).getEntityId(),
                "Первое добавление должно быть вторым в ленте");

        // Проверяем, что timestamp убывает
        assertTrue(feed.get(0).getTimestamp() > feed.get(1).getTimestamp(),
                "События должны быть отсортированы по убыванию timestamp");
    }

    // Вспомогательный метод для генерации случайного ID, которого не должно быть в базе
    Long generateUniqueId(Long id1, Long id2) {
        Random random = new Random();
        long uniqueId;
        while (true) {
            // Используем более безопасный способ генерации ID
            long result = Math.abs(random.nextLong() % 10000) + 10000;
            if (result != id1 && result != id2) {
                uniqueId = result;
                return uniqueId;
            }
        }
    }
}