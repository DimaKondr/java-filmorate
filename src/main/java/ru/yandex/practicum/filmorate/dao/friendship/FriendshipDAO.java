package ru.yandex.practicum.filmorate.dao.friendship;

import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.model.UserFriendship;

import java.util.List;

public interface FriendshipDAO {

    // Добавляем друга к пользователю.
    User addFriendToUser(User user, User addedFriend);

    // Удаляем друга у пользователя.
    User removeFriendFromUser(User user, User removedFriend);

    // При удалении пользователя удаляем его из друзей у всех оставшихся пользователей.
    User removeUserFromFriends(User user);

    // Получение статусы дружбы.
    UserFriendship getFriendshipStatus(User user1, User user2);

    // Изменение статуса дружбы.
    void changeFriendshipStatus(Long firstUserId, Long secondUserId, String status);

    // Получаем список друзей пользователя.
    List<User> getFriendsList(User user);

    // Получаем список общих друзей.
    List<User> getMutualFriendsList(User user1, User user2);

}