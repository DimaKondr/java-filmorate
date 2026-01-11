package ru.yandex.practicum.filmorate.dao.genre;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.DataBaseException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.FilmGenre;

import java.sql.PreparedStatement;
import java.util.List;

@Repository
@RequiredArgsConstructor
@Slf4j
public class FilmGenreDAO implements GenreDAO {
    private final JdbcTemplate jdbc;
    private final GenreRowMapper mapper;

    @Override
    public List<FilmGenre> getAllGenres() {
        String query = "SELECT * FROM genres";

        try {
            log.info("Начат процесс предоставления списка всех жанров.");
            List<FilmGenre> genres = jdbc.query(query, mapper);
            if (!genres.isEmpty()) {
                log.info("Список всех жанров успешно предоставлен.");
                return genres;
            } else {
                log.error("Список всех жанров пуст.");
                throw new DataBaseException("Список всех жанров пуст.");
            }
        } catch (DataAccessException e) {
            log.error("Неудачная попытка получения списка всех жанров. --> {}", e.getMessage());
            throw new DataBaseException("Не удалось получить список всех жанров.");
        }
    }

    @Override
    public FilmGenre getGenreById(Long id) {
        String query = "SELECT * FROM genres WHERE id = ?";

        try {
            log.info("Начата проверка наличия жанра с ID: {} для его предоставления по запросу", id);
            FilmGenre genre = jdbc.queryForObject(query, mapper, id);
            log.info("Жанр с ID: {} найден и успешно предоставлен в ответ на запрос.", id);
            return genre;
        } catch (DataAccessException e) {
            log.error("Неудачная попытка получения жанра по ID: {}. --> {}", id, e.getMessage());
            throw new NotFoundException("Попытка получения жанра. Жанр с ID: " + id + " не найден");
        }
    }

    @Override
    public void addGenreToFilm(Film film) {
        for (FilmGenre filmGenre : film.getGenres()) {
            log.info("Начат процесс добавления жанра к фильму.");
            FilmGenre genre = getGenreById(filmGenre.getId());

            String query = "INSERT INTO film_genres(film_id, genre_id) VALUES (?, ?)";
            KeyHolder keyHolder = new GeneratedKeyHolder();
            int affectedRows = -1;

            try {
                affectedRows = jdbc.update(connection -> {
                    PreparedStatement stmt = connection.prepareStatement(query, new String[]{"id"});
                    stmt.setLong(1, film.getId());
                    stmt.setLong(2, genre.getId());
                    return stmt;
                }, keyHolder);
            } catch (DataAccessException e) {
                log.error("Неудачная попытка добавления жанра к фильму. --> {}", e.getMessage());
                throw new DataBaseException("Не удалось добавить жанр к фильму.");
            }

            if (affectedRows != 1) {
                log.error("При добавления жанра к фильму должна быть обработана 1 строка, а обработано {} строк.",
                        affectedRows);
                throw new DataBaseException("Жанр не найден. Не удалось добавить жанр к фильму.");
            }

            Long id = keyHolder.getKeyAs(Long.class);
            if (id != null) {
                log.debug("Фильму с ID: {} присвоен жанр с ID: {}", film.getId(), genre);
            } else {
                log.error("Не удалось добавить жанр к фильму, так как ID записи в БД имеет null-значение.");
                throw new DataBaseException("Не удалось добавить жанр к фильму.");
            }
        }
    }

    @Override
    public void removeFilmsGenres(Film film) {
        log.info("Начат процесс удаления всех жанров фильма.");
        String query = "DELETE FROM film_genres WHERE film_id = ?";

        int numberOfGenres = film.getGenres().size();
        int affectedRows = jdbc.update(query, film.getId());
        if (affectedRows != numberOfGenres) {
            log.error("При удалении жанров фильма должно быть обработано {} строк, а обработано {} строк.",
                    numberOfGenres, affectedRows);
            throw new DataBaseException("Не удалось удалить жанры фильма при удалении фильма.");
        }
        log.info("Жанры фильма с ID: {} успешно удалены.", film.getId());
    }

    @Override
    public List<FilmGenre> getGenresOfFilm(Film film) {
        String query = "SELECT * " +
                "FROM genres g " +
                "JOIN film_genres fg ON g.id = fg.genre_id " +
                "WHERE fg.film_id = ?" +
                "ORDER BY g.id ASC";

        try {
            log.info("Начат процесс предоставления списка жанров фильма.");
            List<FilmGenre> genres = jdbc.query(query, mapper, film.getId());
            log.info("Список жанров фильма успешно предоставлен.");
            return genres;
        } catch (DataAccessException e) {
            log.error("Неудачная попытка получения списка жанров фильма. --> {}", e.getMessage());
            throw new DataBaseException("Не удалось получить список жанров фильма.");
        }
    }

}