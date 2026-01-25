package ru.yandex.practicum.filmorate.dao.friendship;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.DataBaseException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.model.UserFriendship;
import ru.yandex.practicum.filmorate.storage.user.UserRowMapper;

import java.sql.PreparedStatement;
import java.util.List;

@Repository
@RequiredArgsConstructor
@Slf4j
public class UserFriendshipDAO implements FriendshipDAO {
    private final JdbcTemplate jdbc;
    private final FriendshipRowMapper friendshipRowMapper;
    private final UserRowMapper userRowMapper;

    @Override
    public User addFriendToUser(User user, User addedFriend) {
        log.info("Начат процесс добавления друга.");
        String query = "INSERT INTO friendship (user_id, friend_id, status) " +
                "SELECT " +
                  "? AS user_id, " +
                  "? AS friend_id, " +
                  "CASE " +
                    "WHEN EXISTS (" +
                      "SELECT 1 FROM friendship " +
                      "WHERE user_id = ? AND friend_id = ?" +
                    ") THEN 'CONFIRMED' " +
                    "ELSE 'UNCONFIRMED' " +
                  "END AS status";

        KeyHolder keyHolder = new GeneratedKeyHolder();
        int affectedRows = -1;

        try {
            affectedRows = jdbc.update(connection -> {
                PreparedStatement stmt = connection.prepareStatement(query, new String[]{"id"});
                stmt.setLong(1, user.getId());
                stmt.setLong(2, addedFriend.getId());
                stmt.setLong(3, addedFriend.getId());
                stmt.setLong(4, user.getId());
                return stmt;
            }, keyHolder);
        } catch (DataAccessException e) {
            log.error("Неудачная попытка добавления друга к пользователю. --> {}", e.getMessage());
            throw new DataBaseException("Не удалось добавить друга к пользователю.");
        }

        if (affectedRows != 1) {
            log.error("При добавлении друга к пользователю должна быть обработана 1 строка, а обработано {} строк.",
                    affectedRows);
            throw new DataBaseException("Обработана не одна строка. Не удалось добавить друга к пользователю.");
        }

        Long id = keyHolder.getKeyAs(Long.class);
        if (id != null) {
            log.debug("Пользователю с ID: {} добавлен друг с ID: {}", user.getId(), addedFriend.getId());
        } else {
            log.error("Не удалось добавить друга к пользователю, так как ID записи в БД имеет null-значение.");
            throw new DataBaseException("Не удалось добавить друга к пользователю.");
        }

        UserFriendship friendship = getFriendshipStatus(user, addedFriend);
        if ("CONFIRMED".equals(friendship.getStatus())) {
            changeFriendshipStatus(friendship.getFriendId(), friendship.getUserId(), "CONFIRMED");
        }

        user.getFriendships().add(new UserFriendship(id, user.getId(), addedFriend.getId(), friendship.getStatus()));
        return user;
    }

    @Override
    public User removeFriendFromUser(User user, User removedFriend) {
        log.info("Начат процесс удаления друга у пользователя.");
        String query = "DELETE FROM friendship WHERE user_id = ? AND friend_id = ?";

        int affectedRows = jdbc.update(query, user.getId(), removedFriend.getId());

        if (affectedRows == 0) {
            log.debug("Отсутствует запись пользователь(ID: {}) / друг (ID: {}).",
                    user.getId(), removedFriend.getId());
        } else if (affectedRows == 1) {
            log.debug("Запись пользователь(ID: {}) / друг (ID: {}) успешно найдена.",
                    user.getId(), removedFriend.getId());
        } else {
            log.error("При удалении друга у пользователя должна быть обработана 1 строка," +
                    " а обработано {} строк.", affectedRows);
            throw new DataBaseException("Не удалось удалить друга у пользователя.");
        }

        UserFriendship friendship = null;
        try {
            friendship = getFriendshipStatus(user, removedFriend);
            if ("CONFIRMED".equals(friendship.getStatus())) {
                changeFriendshipStatus(friendship.getUserId(), friendship.getFriendId(), "UNCONFIRMED");
            }
        } catch (NotFoundException e) {
            log.debug("Запись пользователь(ID: {}) / друг (ID: {}) отсутствует.",
                    user.getId(), removedFriend.getId());
        }

        user.getFriendships().remove(friendship);
        log.info("У пользователя с ID: {} успешно удален друг с ID: {}.", user.getId(), removedFriend.getId());
        return user;
    }

    @Override
    public User removeUserFromFriends(User user) {
        log.info("Начат процесс удаления пользователя из друзей остальных пользователей.");
        String query = "DELETE FROM friendship WHERE user_id = ? OR friend_id = ?";

        try {
            jdbc.update(query, user.getId(), user.getId());
        } catch (Exception e) {
            log.error("Неудачная попытка удаления пользователя из друзей остальных пользователей. --> {}",
                    e.getMessage());
            throw new DataBaseException("Не удалось удалить пользователя из друзей остальных пользователей.");
        }

        user.getFriendships().clear();
        log.info("Пользователь с ID: {} успешно удален из друзей остальных пользователей.", user.getId());
        return user;
    }

    @Override
    public UserFriendship getFriendshipStatus(User user1, User user2) {
        String query = "SELECT * FROM friendship WHERE user_id = ? AND friend_id = ?";

        try {
            log.info("Начата проверка наличия записи пользователь(ID: {}) / друг (ID: {}).",
                    user1.getId(), user2.getId());
            UserFriendship friendship = jdbc.queryForObject(query, friendshipRowMapper, user1.getId(), user2.getId());

            if (friendship != null) {
                log.info("Запись о дружбе с ID: {} найдена и успешно предоставлена в ответ на запрос.",
                        friendship.getId());
                return friendship;
            } else {
                log.error("Данные о статусу дружбы двух пользователей вернулись с null-значением.");
                throw new DataBaseException("Не удалось получить статус дружбы двух пользователей.");
            }
        } catch (DataAccessException e) {
            log.error("Неудачная попытка получения записи пользователь(ID: {}) / друг (ID: {}) --> {}",
                    user1.getId(), user2.getId(), e.getMessage());
            throw new NotFoundException("Не удалось получить данные о дружбе двух пользователей.");
        }
    }

    @Override
    public void changeFriendshipStatus(Long firstUserId, Long secondUserId, String status) {
        log.info("Начат процесс изменения статуса дружбы.");
        String query = "UPDATE friendship SET status = ? WHERE user_id = ? AND friend_id = ?";

        int affectedRows = -1;

        try {
            affectedRows = jdbc.update(connection -> {
                PreparedStatement stmt = connection.prepareStatement(query);
                stmt.setString(1, status);
                stmt.setLong(2, firstUserId);
                stmt.setLong(3, secondUserId);
                return stmt;
            });
        } catch (DataAccessException e) {
            log.error("Неудачная попытка изменения статуса дружбы. --> {}", e.getMessage());
            throw new DataBaseException("Не удалось обновить статус дружбы.");
        }

        if (affectedRows != 1) {
            log.error("При обновлении статуса дружбы должна быть обработана 1 строка, а обработано {} строк.",
                    affectedRows);
            throw new DataBaseException("Обработана не одна строка. Не удалось обновить статус дружбы.");
        }
    }

    @Override
    public List<User> getFriendsList(User user) {
        String query = "SELECT * " +
                "FROM users u " +
                "JOIN friendship fs ON u.id = fs.friend_id " +
                "WHERE fs.user_id = ?";

        try {
            log.info("Начат процесс предоставления списка друзей пользователя.");
            List<User> friends = jdbc.query(query, userRowMapper, user.getId());
            return friends;
        } catch (DataAccessException e) {
            log.error("Неудачная попытка получения списка друзей пользователя. --> {}", e.getMessage());
            throw new DataBaseException("Не удалось получить список друзей пользователя.");
        }
    }

    @Override
    public List<User> getMutualFriendsList(User user1, User user2) {
        String query = "SELECT u.id, u.email, u.login, u.name, u.birthday " +
                "FROM (" +
                  "SELECT f1.friend_id AS id " +
                  "FROM friendship f1 " +
                  "JOIN friendship f2 ON f1.friend_id = f2.friend_id " +
                  "WHERE f1.user_id = ? AND f2.user_id = ?" +
                ") AS mutual_friends " +
                "JOIN users u ON mutual_friends.id = u.id";

        try {
            log.info("Начат процесс предоставления списка общих друзей пользователей с ID: {} и ID: {}.",
                    user1.getId(), user2.getId());
            List<User> mutualFriends = jdbc.query(query, userRowMapper, user1.getId(), user2.getId());
            log.info("Список общих друзей успешно предоставлен.");
            return mutualFriends;
        } catch (DataAccessException e) {
            log.error("Неудачная попытка получения списка общих друзей пользователей с ID: {} и ID: {}. --> {}",
                    user1.getId(), user2.getId(), e.getMessage());
            throw new DataBaseException("Не удалось получить список общих друзей.");
        }
    }

}