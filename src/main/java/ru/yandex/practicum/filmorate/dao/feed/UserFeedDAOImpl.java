package ru.yandex.practicum.filmorate.dao.feed;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.EventType;
import ru.yandex.practicum.filmorate.model.Operation;
import ru.yandex.practicum.filmorate.model.UserFeed;

import java.sql.PreparedStatement;
import java.time.Instant;
import java.util.Collections;
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
            int rows = jdbc.update(connection -> {
                PreparedStatement stmt = connection.prepareStatement(query, new String[]{"event_id"});
                stmt.setLong(1, event.getTimestamp());
                stmt.setLong(2, event.getUserId());
                stmt.setString(3, event.getEventType().name());
                stmt.setString(4, event.getOperation().name());
                stmt.setLong(5, event.getEntityId());
                return stmt;
            }, keyHolder);

            if (rows > 0) {
                Long eventId = keyHolder.getKeyAs(Long.class);
                if (eventId != null) {
                    event.setEventId(eventId);
                    log.debug("Добавлено событие с ID: {}", eventId);
                }
            }
        } catch (DataAccessException e) {
            log.error("Ошибка при добавлении события в ленту: {}", e.getMessage());
            // Не бросаем исключение, чтобы не ломать основной функционал
        }
    }

    @Override
    public List<UserFeed> getFeedByUserId(Long userId) {
        String query = "SELECT * FROM user_feeds WHERE user_id = ? ORDER BY timestamp DESC";

        try {
            log.info("Получение ленты событий пользователя с ID: {}", userId);
            List<UserFeed> feed = jdbc.query(query, rowMapper, userId);
            log.info("Найдено {} событий для пользователя с ID: {}", feed.size(), userId);
            return feed;
        } catch (DataAccessException e) {
            log.error("Ошибка при получении ленты событий пользователя с ID: {}: {}", userId, e.getMessage());
            // Возвращаем пустой список вместо исключения
            return Collections.emptyList();
        }
    }

    @Override
    public void addLikeEvent(Long userId, Long entityId, Operation operation) {
        try {
            log.info("Добавление события LIKE: userId={}, entityId={}, operation={}",
                    userId, entityId, operation);

            UserFeed event = new UserFeed();
            event.setTimestamp(Instant.now().toEpochMilli());
            event.setUserId(userId);
            event.setEventType(EventType.LIKE);
            event.setOperation(operation);
            event.setEntityId(entityId);

            addEvent(event);
        } catch (Exception e) {
            log.error("Ошибка при добавлении события LIKE: {}", e.getMessage());
        }
    }

    @Override
    public void addFriendEvent(Long userId, Long entityId, Operation operation) {
        try {
            log.info("Добавление события FRIEND: userId={}, entityId={}, operation={}",
                    userId, entityId, operation);

            UserFeed event = new UserFeed();
            event.setTimestamp(Instant.now().toEpochMilli());
            event.setUserId(userId);
            event.setEventType(EventType.FRIEND);
            event.setOperation(operation);
            event.setEntityId(entityId);

            addEvent(event);

        } catch (Exception e) {
            log.error("Ошибка при добавлении события FRIEND: {}", e.getMessage());
        }
    }

    @Override
    public void addReviewEvent(Long userId, Long entityId, Operation operation) {
        try {
            log.info("Добавление события REVIEW: userId={}, entityId={}, operation={}",
                    userId, entityId, operation);

            UserFeed event = new UserFeed();
            event.setTimestamp(Instant.now().toEpochMilli());
            event.setUserId(userId);
            event.setEventType(EventType.REVIEW);
            event.setOperation(operation);
            event.setEntityId(entityId);

            addEvent(event);
        } catch (Exception e) {
            log.error("Ошибка при добавлении события REVIEW: {}", e.getMessage());
        }
    }
}