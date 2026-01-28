package ru.yandex.practicum.filmorate.storage.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.filmorate.dao.friendship.FriendshipDAO;
import ru.yandex.practicum.filmorate.exception.DataBaseException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Repository("userDbStorage")
@RequiredArgsConstructor
@Slf4j
public class UserDbStorage implements UserStorage {
    private final JdbcTemplate jdbc;
    private final UserRowMapper mapper;
    private final FriendshipDAO friendshipDAO;

    @Override
    public User addUser(User user) {
        if (user == null) {
            log.error("Запрос на добавление нового пользователя поступил с пустым телом");
            throw new ValidationException("Запрос на добавление пользователя поступил с пустым телом");
        }

        log.info("Начат процесс добавления нового пользователя. Проверяем уникальность E-mail");
        String queryForEmail = "SELECT * FROM users WHERE email = ?";
        try {
            List<User> result = jdbc.query(queryForEmail, mapper, user.getEmail());
            if (!result.isEmpty()) {
                log.error("Добавление пользователя. E-mail: {} уже используется", user.getEmail());
                throw new ValidationException("Добавление пользователя. Указанный E-mail: "
                        + user.getEmail() + " уже используется");
            }
        } catch (DataAccessException e) {
            log.error("Добавление пользователя. Ошибка при проверке существования E-mail --> {}", e.getMessage());
            throw new DataBaseException("Добавление пользователя. Ошибка при проверке существования E-mail");
        }

        if (user.getName() == null || user.getName().isBlank()) {
            log.debug("Добавление пользователя. Имя не указано, устанавливаем имя как логин: {}", user.getLogin());
            user.setName(user.getLogin());
        }

        String query = "INSERT INTO users(email, login, name, birthday) " +
                "VALUES (?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        int affectedRows = -1;

        try {
            affectedRows = jdbc.update(connection -> {
                PreparedStatement stmt = connection.prepareStatement(query, new String[]{"id"});
                stmt.setString(1, user.getEmail());
                stmt.setString(2, user.getLogin());
                stmt.setString(3, user.getName());
                stmt.setDate(4, Date.valueOf(user.getBirthday()));
                return stmt;
            }, keyHolder);
        } catch (DataAccessException e) {
            log.error("Неудачная попытка добавления нового пользователя. --> {}", e.getMessage());
            throw new DataBaseException("Не удалось добавить нового пользователя.");
        }

        if (affectedRows != 1) {
            log.error("При добавления нового пользователя должна быть обработана 1 строка, а обработано {} строк.",
                    affectedRows);
            throw new DataBaseException("Не удалось добавить нового пользователя.");
        }

        Long id = keyHolder.getKeyAs(Long.class);
        if (id != null) {
            user.setId(id);
            log.debug("Новому пользователю назначен ID: {}", user.getId());
            log.info("Успешно добавлен новый пользователь с ID: {}", user.getId());
            return user;
        } else {
            log.error("Не удалось добавить нового пользователя, так как ID имеет null-значение.");
            throw new DataBaseException("Не удалось добавить пользователя.");
        }
    }

    @Override
    @Transactional
    public User removeUser(Long userId) {
        log.info("Удаление пользователя с ID: {}", userId);

        User user = getUserById(userId);

        try {
            jdbc.update("DELETE FROM users WHERE id = ?", userId);
        } catch (DataAccessException e) {
            log.error("Ошибка БД при удалении пользователя {}: {}", userId, e.getMessage());
            throw new DataBaseException("Не удалось удалить пользователя");
        }

        log.info("Пользователь с ID: {} удален", userId);
        return user;
    }

    @Override
    public User updateUser(User updatedUser) {
        if (updatedUser == null) {
            log.error("Запрос на обновление данных пользователя поступил с пустым телом");
            throw new ValidationException("Запрос на обновление данных пользователя поступил с пустым телом");
        }

        log.info("Начат процесс обновления данных пользователя. Проверяем ID пользователя");
        if (updatedUser.getId() == null) {
            log.error("Пользователь имеет ID со значением null");
            throw new ValidationException("ID пользователя должен быть указан");
        }

        log.info("Начата проверка наличия пользователя с ID: {}", updatedUser.getId());
        String queryForId = "SELECT * FROM users WHERE id = ?";
        try {
            List<User> idResult = jdbc.query(queryForId, mapper, updatedUser.getId());
            if (idResult.isEmpty()) {
                log.error("Обновление пользователя. ID: {} Не найден", updatedUser.getId());
                throw new NotFoundException("Обновление пользователя. Пользователь с ID: "
                        + updatedUser.getId() + " не найден");
            }
        } catch (DataAccessException e) {
            log.error("Обновление пользователя. Ошибка при проверке существования ID --> {}", e.getMessage());
            throw new DataBaseException("Обновление пользователя. Ошибка при проверке существования ID");
        }

        log.info("Проверяем уникальность E-mail обновляемого пользователя");
        String queryForEmail = "SELECT * FROM users WHERE email = ?";
        try {
            List<User> emailResult = jdbc.query(queryForEmail, mapper, updatedUser.getEmail());
            if (!emailResult.isEmpty()) {
                log.error("Обновление пользователя. E-mail: {} уже используется", updatedUser.getEmail());
                throw new ValidationException("Обновление пользователя. Указанный E-mail: "
                        + updatedUser.getEmail() + " уже используется");
            }
        } catch (DataAccessException e) {
            log.error("Обновление пользователя. Ошибка при проверке существования E-mail --> {}", e.getMessage());
            throw new DataBaseException("Обновление пользователя. Ошибка при проверке существования E-mail");
        }

        if (updatedUser.getName() == null || updatedUser.getName().isBlank()) {
            log.debug("Обновление пользователя. Имя не указано. Оставляем имя без изменений");
            User oldUser = getUserById(updatedUser.getId());
            updatedUser.setName(oldUser.getName());
        }

        log.debug("Обновляем данные пользователя с ID: {} ...", updatedUser.getId());
        String query = "UPDATE users SET email = ?, login = ?, name = ?, birthday = ? WHERE id = ?";
        int affectedRows = -1;

        try {
            affectedRows = jdbc.update(query,
                    updatedUser.getEmail(),
                    updatedUser.getLogin(),
                    updatedUser.getName(),
                    updatedUser.getBirthday().toString(),
                    updatedUser.getId());
        } catch (DataAccessException e) {
            log.error("Неудачная попытка обновления данных пользователя. --> {}", e.getMessage());
            throw new DataBaseException("Не удалось обновить данные пользователя.");
        }

        if (affectedRows != 1) {
            log.error("При обновлении данных пользователя должна быть обработана 1 строка, а обработано {} строк.",
                    affectedRows);
            throw new DataBaseException("Не удалось обновить данные пользователя.");
        }
        log.info("Данные пользователя с ID: {} успешно обновлены.", updatedUser.getId());
        return updatedUser;
    }

    @Override
    public List<User> getAllUsers() {
        String query = "SELECT * FROM users";

        try {
            log.info("Начат процесс предоставления списка всех пользователей.");
            List<User> users = jdbc.query(query, mapper);
            if (!users.isEmpty()) {
                log.info("Список всех пользователей успешно предоставлен.");
                return users;
            } else {
                log.error("Список всех пользователей пуст.");
                throw new DataBaseException("Список всех пользователей пуст.");
            }
        } catch (DataAccessException e) {
            log.error("Неудачная попытка получения списка всех пользователей. --> {}", e.getMessage());
            throw new DataBaseException("Не удалось получить список всех пользователей.");
        }
    }

    @Override
    public User getUserById(Long userId) {
        String query = "SELECT * FROM users WHERE id = ?";

        try {
            log.info("Начата попытка выгрузки из БД пользователя с ID: {} для его предоставления по запросу", userId);
            User user = jdbc.queryForObject(query, mapper, userId);
            log.info("Пользователь с ID: {} найден и успешно предоставлен в ответ на запрос.", userId);
            return user;
        } catch (DataAccessException e) {
            log.error("Неудачная попытка получения пользователя по ID: {}. --> {}", userId, e.getMessage());
            throw new NotFoundException("Попытка получения пользователя. Пользователь с ID: " + userId + " не найден");
        }
    }

    public Set<Long> getUserLikes(Long userId) {
        String query = "SELECT film_id FROM film_likes WHERE user_id = ?";
        List<Long> filmIds = jdbc.queryForList(query, Long.class, userId);
        return new HashSet<>(filmIds);
    }
}