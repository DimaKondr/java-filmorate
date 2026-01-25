package ru.yandex.practicum.filmorate.dao.director;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.Director;

import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class DirectorRowMapper implements RowMapper<Director> {

    @Override
    public Director mapRow(ResultSet resultSet, int rowNum) throws SQLException {
        return new Director(resultSet.getLong("id"),
                resultSet.getString("name"));
    }
}