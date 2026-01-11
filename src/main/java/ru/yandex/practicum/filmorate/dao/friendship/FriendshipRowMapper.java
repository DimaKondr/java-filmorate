package ru.yandex.practicum.filmorate.dao.friendship;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.UserFriendship;

import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class FriendshipRowMapper implements RowMapper<UserFriendship> {

    @Override
    public UserFriendship mapRow(ResultSet resultSet, int rowNum) throws SQLException {
        return new UserFriendship(resultSet.getLong("id"),
                resultSet.getLong("user_id"),
                resultSet.getLong("friend_id"),
                resultSet.getString("status"));
    }

}