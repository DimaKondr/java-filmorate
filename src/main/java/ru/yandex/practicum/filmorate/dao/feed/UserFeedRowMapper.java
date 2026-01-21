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
            return new UserFeed(
                    resultSet.getLong("event_id"),
                    resultSet.getLong("timestamp"),
                    resultSet.getLong("user_id"),
                    EventType.valueOf(resultSet.getString("event_type")),
                    Operation.valueOf(resultSet.getString("operation")),
                    resultSet.getLong("entity_id")
            );
        } catch (IllegalArgumentException e) {
            log.error("Ошибка при маппинге UserFeed: {}", e.getMessage());
            return null;
        }
    }
}