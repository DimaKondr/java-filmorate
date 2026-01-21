package ru.yandex.practicum.filmorate.dao.feed;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.EventType;
import ru.yandex.practicum.filmorate.model.Operation;
import ru.yandex.practicum.filmorate.model.UserFeed;

import java.sql.ResultSet;
import java.sql.SQLException;

@Component
@Slf4j
public class UserFeedRowMapper implements RowMapper<UserFeed> {

    @Override
    public UserFeed mapRow(ResultSet resultSet, int rowNum) throws SQLException {
        try {
            Long eventId = resultSet.getLong("event_id");
            if (resultSet.wasNull()) {
                eventId = null;
            }

            Long timestamp = resultSet.getLong("timestamp");
            Long userId = resultSet.getLong("user_id");
            String eventTypeStr = resultSet.getString("event_type");
            String operationStr = resultSet.getString("operation");
            Long entityId = resultSet.getLong("entity_id");

            if (eventTypeStr == null || operationStr == null) {
                log.error("Ошибка при маппинге UserFeed: eventType или operation null");
                return null;
            }

            EventType eventType = EventType.valueOf(eventTypeStr);
            Operation operation = Operation.valueOf(operationStr);

            return new UserFeed(eventId, timestamp, userId, eventType, operation, entityId);
        } catch (IllegalArgumentException e) {
            log.error("Ошибка при маппинге UserFeed: {}", e.getMessage());
            return null;
        } catch (SQLException e) {
            log.error("SQL ошибка при маппинге UserFeed: {}", e.getMessage());
            return null;
        }
    }
}