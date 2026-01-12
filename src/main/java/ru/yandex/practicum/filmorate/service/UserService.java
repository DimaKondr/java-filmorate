package ru.yandex.practicum.filmorate.service;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.dao.friendship.FriendshipDAO;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.List;

@Service
@Getter
@Slf4j
public class UserService {
    private final UserStorage userStorage;
    private final FriendshipDAO friendshipDAO;

    @Autowired
    public UserService(@Qualifier("userDbStorage") UserStorage userStorage, FriendshipDAO friendshipDAO) {
        this.userStorage = userStorage;
        this.friendshipDAO = friendshipDAO;
    }

    public User addFriend(Long userId, Long addedFriendsId) {
        log.info("Начат процесс добавления нового друга.");
        if (userId.equals(addedFriendsId)) {
            log.error("ID {} пользователя и ID {} друга для добавления совпадают", userId, addedFriendsId);
            throw new ValidationException("ID=" + userId + " пользователя и ID= "
                    + addedFriendsId + " друга для добавления совпадают");
        }
        User user = userStorage.getUserById(userId);
        User addedFriend = userStorage.getUserById(addedFriendsId);
        log.info("Начат процесс добавления друга с ID: {} к пользователю с ID: {}.", addedFriendsId, userId);
        return friendshipDAO.addFriendToUser(user, addedFriend);
    }

    public User removeFriend(Long userId, Long removedFriendsId) {
        log.info("Начат процесс удаления из списка друзей.");
        if (userId.equals(removedFriendsId)) {
            log.error("ID {} пользователя и ID {} друга для удаления совпадают", userId, removedFriendsId);
            throw new ValidationException("ID=" + userId + " пользователя и ID= "
                    + removedFriendsId + " друга для добавления совпадают");
        }
        User user = userStorage.getUserById(userId);
        User removedFriend = userStorage.getUserById(removedFriendsId);
        log.info("Начат процесс удаления друга с ID: {} у пользователя с ID: {}.", removedFriendsId, userId);
        return friendshipDAO.removeFriendFromUser(user, removedFriend);
    }

    public List<User> getFriendsListOfUser(Long userId) {
        log.info("Начат процесс получения списка друзей пользователя с ID {}.", userId);
        User user = userStorage.getUserById(userId);
        return friendshipDAO.getFriendsList(user);
    }

    public List<User> getMutualFriendsList(Long firstUserId, Long secondUserId) {
        log.info("Начат процесс получения списка общих друзей пользователей с ID = {} и ID = {}",
                firstUserId, secondUserId);
        if (firstUserId.equals(secondUserId)) {
            log.error("ID {} и ID {} обоих пользователей совпадают", firstUserId, secondUserId);
            throw new ValidationException("ID обоих пользователей совпадают");
        }
        User firstUser = userStorage.getUserById(firstUserId);
        User secondUser = userStorage.getUserById(secondUserId);
        return friendshipDAO.getMutualFriendsList(firstUser, secondUser);
    }

}