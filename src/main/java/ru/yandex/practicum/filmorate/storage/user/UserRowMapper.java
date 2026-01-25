package ru.yandex.practicum.filmorate.storage.user;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.User;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

@Component
public class UserRowMapper implements RowMapper<User> {

    @Override
    public User mapRow(ResultSet resultSet, int rowNum) throws SQLException {
        return new User(resultSet.getLong("id"),
                resultSet.getString("email"),
                resultSet.getString("login"),
                resultSet.getString("name"),
                resultSet.getObject("birthday", LocalDate.class));
    }

}