package ru.yandex.practicum.filmorate.dao.feed;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.DataBaseException;
import ru.yandex.practicum.filmorate.model.EventType;
import ru.yandex.practicum.filmorate.model.Operation;
import ru.yandex.practicum.filmorate.model.UserFeed;

import java.sql.PreparedStatement;
import java.time.Instant;
import java.util.List;

@Repository
@RequiredArgsConstructor
@Slf4j
public class UserFeedDAOImpl implements UserFeedDAO {
    private final JdbcTemplate jdbc;
    private final UserFeedRowMapper rowMapper;

    @Override
    public void addEvent(UserFeed event) {
        String query = "INSERT INTO user_feeds (timestamp, user_id, event_type, operation, entity_id) " +
                "VALUES (?, ?, ?, ?, ?)";

        try {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbc.update(connection -> {
                PreparedStatement stmt = connection.prepareStatement(query, new String[]{"event_id"});
                stmt.setLong(1, event.getTimestamp());
                stmt.setLong(2, event.getUserId());
                stmt.setString(3, event.getEventType().name());
                stmt.setString(4, event.getOperation().name());
                stmt.setLong(5, event.getEntityId());
                return stmt;
            }, keyHolder);

            Long eventId = keyHolder.getKeyAs(Long.class);
            if (eventId != null) {
                event.setEventId(eventId);
                log.debug("Добавлено событие с ID: {}", eventId);
            }
        } catch (DataAccessException e) {
            log.error("Ошибка при добавлении события в ленту: {}", e.getMessage());
            throw new DataBaseException("Не удалось добавить событие в ленту");
        }
    }

    @Override
    public List<UserFeed> getFeedByUserId(Long userId) {
        String query = "SELECT * FROM user_feeds WHERE user_id = ? ORDER BY timestamp DESC";

        try {
            log.info("Получение ленты событий пользователя с ID: {}", userId);
            return jdbc.query(query, rowMapper, userId);
        } catch (DataAccessException e) {
            log.error("Ошибка при получении ленты событий пользователя с ID: {}", userId);
            throw new DataBaseException("Не удалось получить ленту событий");
        }
    }

    @Override
    public void addLikeEvent(Long userId, Long entityId, Operation operation) {
        UserFeed event = new UserFeed();
        event.setTimestamp(Instant.now().toEpochMilli());
        event.setUserId(userId);
        event.setEventType(EventType.LIKE);
        event.setOperation(operation);
        event.setEntityId(entityId);

        addEvent(event);
        log.info("Добавлено событие LIKE: пользователь {} {} лайк фильму {}",
                userId, operation, entityId);
    }

    @Override
    public void addFriendEvent(Long userId, Long entityId, Operation operation) {
        UserFeed event = new UserFeed();
        event.setTimestamp(Instant.now().toEpochMilli());
        event.setUserId(userId);
        event.setEventType(EventType.FRIEND);
        event.setOperation(operation);
        event.setEntityId(entityId);

        addEvent(event);
        log.info("Добавлено событие FRIEND: пользователь {} {} друга {}",
                userId, operation, entityId);

    }
}