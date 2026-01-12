package ru.yandex.practicum.filmorate.dao.rating;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.FilmAgeRating;

import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class RatingRowMapper implements RowMapper<FilmAgeRating> {

    @Override
    public FilmAgeRating mapRow(ResultSet resultSet, int rowNum) throws SQLException {
        return new FilmAgeRating(resultSet.getLong("id"),
                resultSet.getString("name"));
    }

}