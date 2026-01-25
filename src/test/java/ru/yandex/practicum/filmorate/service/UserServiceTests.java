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
import ru.yandex.practicum.filmorate.model.UserFeed;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class UserServiceTests {
    private final UserService userService;
    private UserStorage userStorage;
    private User user1;
    private User user2;

    @BeforeEach
    void setUp() {
        userStorage = userService.getUserStorage();
        user1 = new User(null, "testemail1@testemail.com", "TestLogin1", "TestName1",
                LocalDate.of(2000, Month.JANUARY, 15));
        user2 = new User(null, "testemail2@testemail.com", "TestLogin2", "TestName2",
                LocalDate.of(2010, Month.OCTOBER, 22));
    }

    @Test
    void addValidFriend_shouldAddFriendAndCreateFeedEvent() {
        User addedUser1 = userStorage.addUser(user1);
        User addedUser2 = userStorage.addUser(user2);

        userService.addFriend(addedUser1.getId(), addedUser2.getId());
        userService.addFriend(addedUser2.getId(), addedUser1.getId());

        List<User> friendsList1 = userService.getFriendsListOfUser(addedUser1.getId());
        List<User> friendsList2 = userService.getFriendsListOfUser(addedUser2.getId());

        assertEquals(1, friendsList1.size());
        assertEquals(1, friendsList2.size());
        assertEquals(addedUser2.getId(), friendsList1.get(0).getId());
        assertEquals(addedUser1.getId(), friendsList2.get(0).getId());

        List<UserFeed> feed1 = userService.getFeedByUserId(addedUser1.getId());
        List<UserFeed> feed2 = userService.getFeedByUserId(addedUser2.getId());

        assertEquals(1, feed1.size());
        assertEquals(1, feed2.size());
        assertEquals("FRIEND", feed1.get(0).getEventType().name());
        assertEquals("ADD", feed1.get(0).getOperation().name());
        assertEquals(addedUser2.getId(), feed1.get(0).getEntityId());
        assertEquals("FRIEND", feed2.get(0).getEventType().name());
        assertEquals("ADD", feed2.get(0).getOperation().name());
        assertEquals(addedUser1.getId(), feed2.get(0).getEntityId());
    }

    @Test
    void addFriend_withSameIds_shouldThrowValidationException() {
        User addedUser = userStorage.addUser(user1);

        ValidationException exception = assertThrows(ValidationException.class,
                () -> userService.addFriend(addedUser.getId(), addedUser.getId()));

        assertEquals("ID=" + addedUser.getId() + " пользователя и ID= " +
                addedUser.getId() + " друга для добавления совпадают", exception.getMessage());
    }

    @Test
    void addFriend_withNonExistentUserId_shouldThrowNotFoundException() {
        User addedUser = userStorage.addUser(user1);
        Long nonExistentId = 999L;

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> userService.addFriend(nonExistentId, addedUser.getId()));

        assertEquals("Попытка получения пользователя. Пользователь с ID: " + nonExistentId + " не найден",
                exception.getMessage());
    }

    @Test
    void addFriend_withNonExistentFriendId_shouldThrowNotFoundException() {
        User addedUser = userStorage.addUser(user1);
        Long nonExistentId = 999L;

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> userService.addFriend(addedUser.getId(), nonExistentId));

        assertEquals("Попытка получения пользователя. Пользователь с ID: " + nonExistentId + " не найден",
                exception.getMessage());
    }

    @Test
    void removeValidFriend_shouldRemoveFriendAndCreateFeedEvent() {
        User addedUser1 = userStorage.addUser(user1);
        User addedUser2 = userStorage.addUser(user2);

        userService.addFriend(addedUser1.getId(), addedUser2.getId());
        userService.addFriend(addedUser2.getId(), addedUser1.getId());

        List<User> friendsList1 = userService.getFriendsListOfUser(addedUser1.getId());
        List<User> friendsList2 = userService.getFriendsListOfUser(addedUser2.getId());
        assertEquals(1, friendsList1.size());
        assertEquals(1, friendsList2.size());

        userService.removeFriend(addedUser1.getId(), addedUser2.getId());
        userService.removeFriend(addedUser2.getId(), addedUser1.getId());

        List<User> friendsList3 = userService.getFriendsListOfUser(addedUser1.getId());
        List<User> friendsList4 = userService.getFriendsListOfUser(addedUser2.getId());
        assertTrue(friendsList3.isEmpty());
        assertTrue(friendsList4.isEmpty());

        List<UserFeed> feed1 = userService.getFeedByUserId(addedUser1.getId());
        List<UserFeed> feed2 = userService.getFeedByUserId(addedUser2.getId());

        assertEquals(2, feed1.size());
        assertEquals(2, feed2.size());

        // Проверяем порядок событий (старые первыми)
        assertEquals("ADD", feed1.get(0).getOperation().name(), "Первое событие должно быть ADD");
        assertEquals("REMOVE", feed1.get(1).getOperation().name(), "Второе событие должно быть REMOVE");

        assertEquals("ADD", feed2.get(0).getOperation().name(), "Первое событие должно быть ADD");
        assertEquals("REMOVE", feed2.get(1).getOperation().name(), "Второе событие должно быть REMOVE");

        // Проверяем, что timestamp возрастает
        assertTrue(feed1.get(0).getTimestamp() < feed1.get(1).getTimestamp(),
                "Второе событие должно иметь больший timestamp");
    }

    @Test
    void removeFriend_withSameIds_shouldThrowValidationException() {
        User addedUser = userStorage.addUser(user1);

        ValidationException exception = assertThrows(ValidationException.class,
                () -> userService.removeFriend(addedUser.getId(), addedUser.getId()));

        assertEquals("ID=" + addedUser.getId() + " пользователя и ID= " +
                addedUser.getId() + " друга для добавления совпадают", exception.getMessage());
    }

    @Test
    void removeFriend_withNonExistentUserId_shouldThrowNotFoundException() {
        User addedUser1 = userStorage.addUser(user1);
        User addedUser2 = userStorage.addUser(user2);
        userService.addFriend(addedUser1.getId(), addedUser2.getId());

        Long nonExistentId = 999L;

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> userService.removeFriend(nonExistentId, addedUser2.getId()));

        assertEquals("Попытка получения пользователя. Пользователь с ID: " + nonExistentId + " не найден",
                exception.getMessage());
    }

    @Test
    void removeFriend_withNonExistentFriendId_shouldThrowNotFoundException() {
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
    void getFriendsListOfUser_shouldReturnAllFriends() {
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

        List<User> friendsList = userService.getFriendsListOfUser(addedUser1.getId());
        assertEquals(3, friendsList.size());

        List<UserFeed> feed = userService.getFeedByUserId(addedUser1.getId());
        assertEquals(3, feed.size(), "Должно быть 3 события в ленте после добавления 3 друзей");
    }

    @Test
    void getMutualFriends_shouldReturnCommonFriends() {
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

        List<User> mutualFriends1 = userService.getMutualFriendsList(addedUser1.getId(), addedUser4.getId());
        List<User> mutualFriends2 = userService.getMutualFriendsList(addedUser2.getId(), addedUser4.getId());

        assertEquals(1, mutualFriends1.size());
        assertEquals(1, mutualFriends2.size());
        assertEquals(addedUser2.getId(), mutualFriends1.get(0).getId());
        assertEquals(addedUser3.getId(), mutualFriends2.get(0).getId());
    }

    @Test
    void getUserFeed_shouldReturnEventsForUser() {
        User addedUser1 = userStorage.addUser(user1);
        User addedUser2 = userStorage.addUser(user2);

        userService.addFriend(addedUser1.getId(), addedUser2.getId());

        List<UserFeed> feed = userService.getFeedByUserId(addedUser1.getId());

        assertNotNull(feed);
        assertEquals(1, feed.size());

        UserFeed event = feed.get(0);
        assertEquals(addedUser1.getId(), event.getUserId());
        assertEquals("FRIEND", event.getEventType().name());
        assertEquals("ADD", event.getOperation().name());
        assertEquals(addedUser2.getId(), event.getEntityId());
        assertNotNull(event.getTimestamp());
    }

    @Test
    void getUserFeed_withNonExistentUser_shouldThrowNotFoundException() {
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> userService.getFeedByUserId(999L));

        assertEquals("Попытка получения пользователя. Пользователь с ID: 999 не найден",
                exception.getMessage());
    }

    @Test
    void getEmptyUserFeed_shouldReturnEmptyList() {
        User addedUser1 = userStorage.addUser(user1);

        List<UserFeed> feed = userService.getFeedByUserId(addedUser1.getId());

        assertNotNull(feed);
        assertTrue(feed.isEmpty());
    }

    @Test
    void feedEventsAreSortedChronologically() throws InterruptedException {
        User user1 = new User(null, "user1@mail.ru", "user1", "User One",
                LocalDate.of(1990, 1, 1));
        User user2 = new User(null, "user2@mail.ru", "user2", "User Two",
                LocalDate.of(1992, 2, 2));
        User user3 = new User(null, "user3@mail.ru", "user3", "User Three",
                LocalDate.of(1993, 3, 3));

        User savedUser1 = userStorage.addUser(user1);
        User savedUser2 = userStorage.addUser(user2);
        User savedUser3 = userStorage.addUser(user3);

        userService.addFriend(savedUser1.getId(), savedUser2.getId());
        Thread.sleep(10);
        userService.addFriend(savedUser1.getId(), savedUser3.getId());

        List<UserFeed> feed = userService.getFeedByUserId(savedUser1.getId());

        assertEquals(2, feed.size());

        // События должны быть в хронологическом порядке (старые первыми)
        assertEquals(savedUser2.getId(), feed.get(0).getEntityId(),
                "Первое добавление должно быть первым в ленте");
        assertEquals(savedUser3.getId(), feed.get(1).getEntityId(),
                "Второе добавление должно быть вторым в ленте");

        // Проверяем, что timestamp возрастает
        assertTrue(feed.get(0).getTimestamp() < feed.get(1).getTimestamp(),
                "События должны быть отсортированы по возрастанию timestamp");
    }
}