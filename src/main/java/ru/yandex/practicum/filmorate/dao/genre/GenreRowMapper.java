package ru.yandex.practicum.filmorate.dao.genre;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.FilmGenre;

import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class GenreRowMapper implements RowMapper<FilmGenre> {

    @Override
    public FilmGenre mapRow(ResultSet resultSet, int rowNum) throws SQLException {
        return new FilmGenre(resultSet.getLong("id"),
                resultSet.getString("name"));
    }

}