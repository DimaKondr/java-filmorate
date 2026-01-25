package ru.yandex.practicum.filmorate.dao.like;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.DataBaseException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.film.FilmRowMapper;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Repository
@RequiredArgsConstructor
@Slf4j
public class FilmLikeDAO implements LikeDAO {
    private final JdbcTemplate jdbc;
    private final FilmRowMapper mapper;

    @Override
    public void addLikeToFilm(Film film, Long userId) {
        log.info("Начат процесс добавления лайков к фильму.");
        String query = "INSERT INTO film_likes(film_id, user_id) VALUES (?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();
        int affectedRows = -1;

        try {
            affectedRows = jdbc.update(connection -> {
                PreparedStatement stmt = connection.prepareStatement(query, new String[]{"id"});
                stmt.setLong(1, film.getId());
                stmt.setLong(2, userId);
                return stmt;
            }, keyHolder);
        } catch (DataAccessException e) {
            log.error("Неудачная попытка присвоения рейтинга фильму. --> {}", e.getMessage());
            throw new DataBaseException("Не удалось добавить лайк фильму.");
        }

        if (affectedRows != 1) {
            log.error("При добавлении лайка фильму должна быть обработана 1 строка, а обработано {} строк.",
                    affectedRows);
            throw new DataBaseException("Обработана не одна строка. Не удалось добавить лайк фильму.");
        }

        Long id = keyHolder.getKeyAs(Long.class);
        if (id != null) {
            log.debug("Фильму с ID: {} добавлен лайк пользователя с ID: {}", film.getId(), userId);
        } else {
            log.error("Не удалось добавить лайк фильму, так как ID записи в БД имеет null-значение.");
            throw new DataBaseException("Не удалось добавить лайк фильму.");
        }
    }

    @Override
    public void removeLikeFromFilm(Film film, Long userId) {
        log.info("Начат процесс удаления лайка у фильма.");
        String query = "DELETE FROM film_likes WHERE film_id = ? AND user_id = ?";

        int affectedRows = jdbc.update(query, film.getId(), userId);
        if (affectedRows != 1) {
            log.error("При удалении лайка (ID пользователя: {}) у фильма (ID фильма: {}) " +
                    "должна быть обработана одна строка, а обработано {} строк.", userId, film.getId(), affectedRows);
            throw new DataBaseException("Не удалось удалить лайк у фильма.");
        }
        log.info("У фильма с ID: {} успешно удален лайк пользователя с ID: {}.", film.getId(), userId);
    }

    @Override
    public void removeAllLikesFromFilm(Film film) {
        log.info("Начат процесс удаления всех лайков фильма.");
        String query = "DELETE FROM film_likes WHERE film_id = ?";

        int affectedRows = jdbc.update(query, film.getId());
        log.info("Все лайки фильма с ID: {} успешно удалены. Из БД удалено {} записей.", film.getId(), affectedRows);
    }

    @Override
    public Set<Long> getLikesOfFilm(Film film) {
        String query = "SELECT user_id FROM film_likes WHERE film_id = ?";

        try {
            log.info("Начат процесс получения лайков фильма с ID: {}.", film.getId());
            List<Long> likes = jdbc.queryForList(query, Long.class, film.getId());

            if (likes.isEmpty()) {
                log.info("Список лайков фильма с ID: {} пуст.", film.getId());
            } else {
                log.debug("Список лайков фильма с ID: {} успешно предоставлен.", film.getId());
            }
            return new HashSet<>(likes);
        } catch (DataAccessException e) {
            log.error("Неудачная попытка получения лайков фильма с ID: {}. --> {}", film.getId(), e.getMessage());
            throw new DataBaseException("Не удалось получить список лайков фильма с ID: " + film.getId());
        }
    }

    @Override
    public List<Film> getMostPopularFilms(Long count, Long genreId, Long year) {
        StringBuilder sb = new StringBuilder("SELECT f.id, f.name, f.description, f.release_date, f.duration " +
                "FROM films AS f " +
                "LEFT JOIN film_likes AS fl ON f.id = fl.film_id ");

        List<Long> params = new ArrayList<>();
        List<String> whereConditions = new ArrayList<>();

        if (genreId != null) {
            sb.append("JOIN film_genres AS fg ON f.id = fg.film_id ");
        }

        if (year != null) {
            whereConditions.add("EXTRACT(YEAR FROM f.release_date) = ?");
            params.add(year);
        }

        if (genreId != null) {
            whereConditions.add("fg.genre_id = ?");
            params.add(genreId);
        }

        if (!whereConditions.isEmpty()) {
            sb.append("WHERE ");
            sb.append(String.join(" AND ", whereConditions));
        }

        sb.append("GROUP BY f.id " +
                "ORDER BY COUNT(fl.user_id) DESC " +
                "LIMIT ?");
        params.add(count);
        String querySB = sb.toString();

        try {
            log.info("Начат процесс получения {} самых популярных фильмов за {} год в жанре с ID: {}.",
                    count, year, genreId);

            List<Film> mostPopularFilms = jdbc.query(querySB, new PreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps) throws SQLException {
                    for (int i = 0; i < params.size(); i++) {
                        ps.setLong(i + 1, params.get(i));
                    }
                }
            }, mapper);

            if (mostPopularFilms.isEmpty()) {
                log.info("Список {} самых популярных фильмов за {} год в жанре с ID: {} пуст.", count, year, genreId);
            }
            if (mostPopularFilms.size() != count) {
                log.debug("Запрошено {} самых популярных фильмов за {} год в жанре с ID: {}. " +
                                "В базе найдено {} фильмов с подходящими параметрами.", count, year, genreId,
                        mostPopularFilms.size());
            }
            log.debug("Список {} самых популярных фильмов за {} год в жанре с ID: {} предоставлен.",
                        count, year, genreId);
            return mostPopularFilms;
        } catch (DataAccessException e) {
            log.error("Неудачная попытка получения списка {} самых популярных фильмов за {} год в жанре с ID: {}." +
                    " --> {}", count, year, genreId, e.getMessage());
            throw new DataBaseException("Не удалось получить список самых популярных фильмов.");
        }
    }

}