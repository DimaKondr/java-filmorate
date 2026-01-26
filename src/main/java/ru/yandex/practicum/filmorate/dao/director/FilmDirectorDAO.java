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
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.film.FilmRowMapper;

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
    private final FilmRowMapper filmRowMapper; // Добавлен FilmRowMapper

    // Удалена дублирующая зависимость jdbcTemplate (уже есть jdbc)

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

    // ВАЖНО: Эти методы должны быть добавлены в интерфейс DirectorDAO
    @Override
    public List<Film> getFilmsByDirectorSortedByYear(Long directorId) {
        log.info("Получение фильмов режиссера {} отсортированных по году через DAO", directorId);

        String query = "SELECT f.* FROM films f " +
                "JOIN film_directors fd ON f.id = fd.film_id " +
                "WHERE fd.director_id = ? " +
                "ORDER BY f.release_date ASC";

        try {
            return jdbc.query(query, filmRowMapper, directorId);
        } catch (DataAccessException e) {
            log.error("Ошибка при получении фильмов режиссера {}: {}", directorId, e.getMessage());
            throw new DataBaseException("Не удалось получить фильмы режиссера");
        }
    }

    @Override
    public List<Film> getFilmsByDirectorSortedByLikes(Long directorId) {
        log.info("Получение фильмов режиссера {} отсортированных по лайкам через DAO", directorId);

        String query = "SELECT f.* " +
                "FROM films f " +
                "JOIN film_directors fd ON f.id = fd.film_id " +
                "LEFT JOIN film_likes fl ON f.id = fl.film_id " +
                "WHERE fd.director_id = ? " +
                "GROUP BY f.id " +
                "ORDER BY COUNT(fl.user_id) DESC";

        try {
            return jdbc.query(query, filmRowMapper, directorId);
        } catch (DataAccessException e) {
            log.error("Ошибка при получении фильмов режиссера {}: {}", directorId, e.getMessage());
            throw new DataBaseException("Не удалось получить фильмы режиссера");
        }
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
        String query1 = "DELETE FROM film_directors WHERE director_id = ?";
        jdbc.update(query1, id)
        ;
        String query2 = "DELETE FROM directors WHERE id = ?";
        return delete(query2, id);
    }

    @Override
    public List<Director> getDirectorsOfFilm(Long filmId) {
        String query = "SELECT id, name " +
                "FROM directors d " +
                "JOIN film_directors fd ON d.id = fd.director_id " +
                "WHERE fd.film_id = ?" +
                "ORDER BY d.id ASC";

        try {
            log.info("Начат процесс предоставления списка режиссеров фильма.");
            List<Director> directors = jdbc.query(query, mapper, filmId);
            log.info("Список режиссеров фильма успешно предоставлен.");
            return directors;
        } catch (DataAccessException e) {
            log.error("Неудачная попытка получения списка режиссеров фильма. --> {}", e.getMessage());
            throw new DataBaseException("Не удалось получить список режиссеров фильма.");
        }
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