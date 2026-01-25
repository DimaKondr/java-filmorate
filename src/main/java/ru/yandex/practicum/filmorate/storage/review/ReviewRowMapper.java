package ru.yandex.practicum.filmorate.storage.review;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.Review;

import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class ReviewRowMapper implements RowMapper<Review> {

    @Override
    public Review mapRow(ResultSet resultSet, int rowNum) throws SQLException {
        return new Review(resultSet.getLong("reviewId"),
                resultSet.getString("content"),
                resultSet.getBoolean("isPositive"),
                resultSet.getLong("userId"),
                resultSet.getLong("filmId"),
                resultSet.getLong("useful"));
    }

}