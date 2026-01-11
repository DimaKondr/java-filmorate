package ru.yandex.practicum.filmorate.dao.rating;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.DataBaseException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.FilmAgeRating;

import java.sql.PreparedStatement;
import java.util.Comparator;
import java.util.List;

@Repository
@RequiredArgsConstructor
@Slf4j
public class FilmAgeRatingDAO implements RatingDAO {
    private final JdbcTemplate jdbc;
    private final RatingRowMapper mapper;

    @Override
    public List<FilmAgeRating> getAllRatings() {
        String query = "SELECT * FROM ratings";

        try {
            log.info("Начат процесс предоставления списка всех рейтингов.");
            List<FilmAgeRating> ratings = jdbc.query(query, mapper);
            if (ratings.size() == 5) {
                log.info("Сортируем список всех рейтингов по возрастанию ID рейтинга.");
                ratings.sort(Comparator.comparing(FilmAgeRating::getId));
                log.info("Список всех рейтингов успешно предоставлен.");
                return ratings;
            } else {
                log.error("Список всех рейтингов содержит неверное количество элементов.");
                throw new DataBaseException("Не удалось сформировать список всех рейтингов.");
            }
        } catch (DataAccessException e) {
            log.error("Неудачная попытка получения списка всех рейтингов. --> {}", e.getMessage());
            throw new DataBaseException("Не удалось получить список всех рейтингов.");
        }
    }

    @Override
    public FilmAgeRating getRatingById(Long id) {
        String query = "SELECT * FROM ratings WHERE id = ?";

        try {
            log.info("Начата проверка наличия рейтинга с ID: {} для его предоставления по запросу", id);
            FilmAgeRating rating = jdbc.queryForObject(query, mapper, id);
            log.info("Рейтинг с ID: {} найден и успешно предоставлен в ответ на запрос.", id);
            return rating;
        } catch (DataAccessException e) {
            log.error("Неудачная попытка получения рейтинга по ID: {}. --> {}", id, e.getMessage());
            throw new NotFoundException("Попытка получения рейтинга. Рейтинг с ID: " + id + " не найден");
        }
    }

    @Override
    public void addRatingToFilm(Film film) {
        log.info("Начат процесс добавления рейтинга к фильму.");
        FilmAgeRating rating = getRatingById(film.getMpa().getId());

        String query = "INSERT INTO age_ratings(film_id, rating_id) VALUES (?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        int affectedRows = -1;

        try {
            affectedRows = jdbc.update(connection -> {
                PreparedStatement stmt = connection.prepareStatement(query, new String[]{"id"});
                stmt.setLong(1, film.getId());
                stmt.setLong(2, film.getMpa().getId());
                return stmt;
            }, keyHolder);
        } catch (DataAccessException e) {
            log.error("Неудачная попытка присвоения рейтинга фильму. --> {}", e.getMessage());
            throw new DataBaseException("Не удалось присвоить рейтинг фильму.");
        }

        if (affectedRows != 1) {
            log.error("При присвоении рейтинга фильму должна быть обработана 1 строка, а обработано {} строк.",
                    affectedRows);
            throw new NotFoundException("Рейтинг не найден. Не удалось присвоить рейтинг фильму.");
        }

        Long id = keyHolder.getKeyAs(Long.class);
        if (id != null) {
            log.debug("Фильму с ID: {} присвоен возрастной рейтинг с ID: {}", film.getId(), film.getMpa().getId());
        } else {
            log.error("Не удалось присвоить возрастной рейтинг фильму, так как ID записи в БД имеет null-значение.");
            throw new DataBaseException("Не удалось присвоить рейтинг фильму.");
        }
    }

    @Override
    public void removeFilmRating(Film film) {
        log.info("Начат процесс удаления возрастного рейтинга фильма.");
        String query = "DELETE FROM age_ratings WHERE film_id = ?";

        int affectedRows = jdbc.update(query, film.getId());
        if (affectedRows != 1) {
            log.error("При удалении возрастного рейтинга фильма должна быть обработана 1 строка," +
                    " а обработано {} строк.", affectedRows);
            throw new DataBaseException("Не удалось удалить возрастной рейтинг фильма при удалении фильма.");
        }
        log.info("Возрастной рейтинг фильма с ID: {} успешно удален.", film.getId());
    }

    @Override
    public FilmAgeRating getRatingOfFilm(Film film) {
        String query = "SELECT * " +
                "FROM ratings r " +
                "JOIN age_ratings ar ON r.id = ar.rating_id " +
                "WHERE ar.film_id = ?";

        try {
            log.info("Начат процесс получения возрастного рейтинга фильма с ID: {}.", film.getId());
            return jdbc.queryForObject(query, mapper, film.getId());
        } catch (EmptyResultDataAccessException e) {
            log.debug("Фильму с ID: {} не присвоен рейтинг. --> {}", film.getId(), e.getMessage());
            return null;
        } catch (DataAccessException e) {
            log.error("Неудачная попытка получения рейтинга фильма с ID: {}. --> {}", film.getId(), e.getMessage());
            throw new DataBaseException("Не удалось получить возрастной рейтинг фильма с ID: " + film.getId());
        }
    }

}