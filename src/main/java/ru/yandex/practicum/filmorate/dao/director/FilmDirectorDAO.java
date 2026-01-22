package ru.yandex.practicum.filmorate.dao.director;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.DataBaseException;
import ru.yandex.practicum.filmorate.model.Director;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
@Slf4j
public class FilmDirectorDAO implements DirectorDAO {
    private final JdbcTemplate jdbc;
    private final DirectorRowMapper mapper;

    @Override
    public Director addDirector(Director director) {
        String query = "INSERT INTO directors(name) VALUES (?)";

        Long id = insert(query, director.getName());
        director.setId(id);
        return director;
    }

    @Override
    public Director updateDirector(Director director) {
        String query = "UPDATE directors SET name = ? WHERE id = ?";
        update(query, director.getName(), director.getId());
        return director;
    }

    @Override
    public List<Director> getAllDirectors() {
        String query = "SELECT id, name FROM directors";

        try {
            return jdbc.query(query, mapper);

        } catch (DataAccessException e) {
            throw new DataBaseException("Не удалось получить список всех режиссеров.");
        }
    }

    @Override
    public Optional<Director> getDirectorById(Long id) {
        String query = "SELECT id, name FROM directors WHERE id = ?";

        try {
            Director director = jdbc.queryForObject(query, mapper, id);
            return Optional.of(director);
        } catch (EmptyResultDataAccessException ignored) {
            return Optional.empty();
        }
    }

    @Override
    public boolean removeDirector(Long id) {
        String query = "DELETE FROM directors WHERE id = ?";
        return delete(query, id);
    }

    private Long insert(String query, Object... params) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection
                    .prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            for (int idx = 0; idx < params.length; idx++) {
                ps.setObject(idx + 1, params[idx]);
            }
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) {
            return key.longValue();
        } else {
            throw new DataBaseException("Не удалось сохранить данные");
        }
    }

    private void update(String query, Object... params) {
        int rowsUpdated = jdbc.update(query, params);
        if (rowsUpdated == 0) {
            throw new DataBaseException("Не удалось обновить данные");
        }
    }

    private boolean delete(String query, long id) {
        int rowsDeleted = jdbc.update(query, id);
        return rowsDeleted > 0;
    }
}
