package ru.yandex.practicum.filmorate.dao.like;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.DataBaseException;
import ru.yandex.practicum.filmorate.model.Film;

import java.sql.PreparedStatement;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Repository
@RequiredArgsConstructor
@Slf4j
public class FilmLikeDAO implements LikeDAO {
    private final JdbcTemplate jdbc;

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
    public List<Long> getIdOfMostPopularFilms(Long mostPopularFilmCount) {
        String query = "SELECT film_id " +
                "FROM film_likes " +
                "GROUP BY film_id " +
                "ORDER BY COUNT(user_id) DESC " +
                "LIMIT ?";

        try {
            log.info("Начат процесс получения ID у {} самых популярных фильмов.", mostPopularFilmCount);
            List<Long> idOfMostPopularFilms = jdbc.queryForList(query, Long.class, mostPopularFilmCount);

            if (idOfMostPopularFilms.isEmpty()) {
                log.info("Набор ID у {} самых популярных фильмов пуст.", mostPopularFilmCount);
            } else if (idOfMostPopularFilms.size() != mostPopularFilmCount) {
                log.debug("Запрошено {} самых популярных фильмов. В базе найдено {} фильмов с лайками. " +
                        "Предоставлен набор ID у {} самых популярных фильмов.", mostPopularFilmCount,
                        idOfMostPopularFilms.size(), mostPopularFilmCount);
            } else {
                log.debug("Набор ID у {} самых популярных фильмов успешно предоставлен.", mostPopularFilmCount);
            }
            return idOfMostPopularFilms;
        } catch (DataAccessException e) {
            log.error("Неудачная попытка получения ID у {} самых популярных фильмов. --> {}",
                    mostPopularFilmCount, e.getMessage());
            throw new DataBaseException("Не удалось получить ID у " + mostPopularFilmCount +
                    " самых популярных фильмов.");
        }
    }

}