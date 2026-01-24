package ru.yandex.practicum.filmorate.storage.film;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.dao.director.DirectorRowMapper;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.filmorate.dao.genre.GenreDAO;
import ru.yandex.practicum.filmorate.dao.like.LikeDAO;
import ru.yandex.practicum.filmorate.dao.rating.RatingDAO;
import ru.yandex.practicum.filmorate.exception.DataBaseException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.FilmAgeRating;
import ru.yandex.practicum.filmorate.model.FilmGenre;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.time.Month;
import java.util.*;

@Repository("filmDbStorage")
@RequiredArgsConstructor
@Slf4j
public class FilmDbStorage implements FilmStorage {
    private final JdbcTemplate jdbc;
    private final FilmRowMapper mapper;
    private final GenreDAO genreDAO;
    private final RatingDAO ratingDAO;
    private final LikeDAO likeDAO;
    private final DirectorRowMapper directorRowMapper;

    @Override
    public Film addFilm(Film film) {
        if (film == null) {
            log.error("Запрос на добавление нового фильма поступил с пустым телом");
            throw new ValidationException("Запрос на добавление фильма поступил с пустым телом");
        }

        log.info("Начат процесс добавления нового фильма.");
        String query = "INSERT INTO films(name, description, release_date, duration) " +
                "VALUES (?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        int affectedRows = -1;

        try {
            affectedRows = jdbc.update(connection -> {
                PreparedStatement stmt = connection.prepareStatement(query, new String[]{"id"});
                stmt.setString(1, film.getName());
                stmt.setString(2, film.getDescription());
                stmt.setDate(3, Date.valueOf(film.getReleaseDate()));
                stmt.setLong(4, film.getDuration());
                return stmt;
            }, keyHolder);
        } catch (DataAccessException e) {
            log.error("Неудачная попытка добавления нового фильма. --> {}", e.getMessage());
            throw new DataBaseException("Не удалось добавить новый фильм.");
        }

        if (affectedRows != 1) {
            log.error("При добавления нового фильма должна быть обработана 1 строка, а обработано {} строк.",
                    affectedRows);
            throw new DataBaseException("Не удалось добавить новый фильм.");
        }

        Long id = keyHolder.getKeyAs(Long.class);
        if (id != null) {
            film.setId(id);
            log.debug("Новому фильму назначен ID: {}", film.getId());

            if (!film.getGenres().isEmpty()) {
                genreDAO.addGenreToFilm(film);
            }
            if (film.getMpa().getId() != null) {
                ratingDAO.addRatingToFilm(film);
            }
            addDirectors(film);

            log.info("Успешно добавлен новый фильм с ID: {}", film.getId());
            return film;
        } else {
            log.error("Не удалось добавить новый фильм, так как ID имеет null-значение.");
            throw new DataBaseException("Не удалось добавить фильм.");
        }
    }

    @Override
    @Transactional
    public Film removeFilm(Long filmId) {
        log.info("Удаление фильма с ID: {}", filmId);

        Film film = getFilmById(filmId);

        try {
            jdbc.update("DELETE FROM films WHERE id = ?", filmId);
        } catch (DataAccessException e) {
            log.error("Ошибка БД при удалении фильма с ID: {}", filmId, e);
            throw new DataBaseException("Не удалось удалить фильм.");
        }

        log.info("Фильм с ID: {} успешно удален", filmId);
        return film;
    }

    @Override
    public Film updateFilm(Film updatedFilm) {
        if (updatedFilm == null) {
            log.error("Запрос на обновление данных фильма поступил с пустым телом");
            throw new ValidationException("Запрос на обновление данных фильма поступил с пустым телом");
        }

        log.info("Начат процесс обновления данных фильма.");
        if (updatedFilm.getId() == null) {
            log.error("Фильм имеет ID со значением null");
            throw new ValidationException("ID фильма должен быть указан");
        }

        log.info("Начата проверка наличия фильма с ID: {}", updatedFilm.getId());
        Film oldFilm = getFilmById(updatedFilm.getId());
        if (oldFilm == null) {
            log.error("Обновление фильма. ID: {} Не найден", updatedFilm.getId());
            throw new NotFoundException("Обновление фильма. Фильм с ID: " + updatedFilm.getId() + " не найден");
        }

        LocalDate cinemaBirthDate = LocalDate.of(1895, Month.DECEMBER, 28);
        if (!oldFilm.getName().equals(updatedFilm.getName())) {
            oldFilm.setName(updatedFilm.getName());
        }
        if (!oldFilm.getDescription().equals(updatedFilm.getDescription())) {
            oldFilm.setDescription(updatedFilm.getDescription());
        }
        if (!oldFilm.getReleaseDate().isEqual(updatedFilm.getReleaseDate())
                && updatedFilm.getReleaseDate().isAfter(cinemaBirthDate)) {
            oldFilm.setReleaseDate(updatedFilm.getReleaseDate());
        }
        if (!oldFilm.getDuration().equals(updatedFilm.getDuration())) {
            oldFilm.setDuration(updatedFilm.getDuration());
        }

        String query = "UPDATE films SET name = ?, description = ?, release_date = ?, duration = ? WHERE id = ?";
        int affectedRows = -1;

        try {
            affectedRows = jdbc.update(query,
                    oldFilm.getName(),
                    oldFilm.getDescription(),
                    oldFilm.getReleaseDate(),
                    oldFilm.getDuration(),
                    oldFilm.getId());
        } catch (DataAccessException e) {
            log.error("Неудачная попытка обновления данных фильма. --> {}", e.getMessage());
            throw new DataBaseException("Не удалось обновить данные фильма.");
        }

        if (affectedRows != 1) {
            log.error("При обновлении данных фильма должна быть обработана 1 строка, а обработано {} строк.",
                    affectedRows);
            throw new DataBaseException("Не удалось обновить данные фильма.");
        }

        if (!oldFilm.getGenres().equals(updatedFilm.getGenres())) {
            genreDAO.removeFilmsGenres(oldFilm);
            genreDAO.addGenreToFilm(updatedFilm);
        }
        if (updatedFilm.getMpa().getId() != null && !updatedFilm.getMpa().getId().equals(oldFilm.getMpa().getId())) {
            ratingDAO.removeFilmRating(oldFilm);
            ratingDAO.addRatingToFilm(updatedFilm);
        }

        if (!oldFilm.getDirectors().equals(updatedFilm.getDirectors())) {
            removeDirectorFromFilm(oldFilm.getId());
            addDirectors(updatedFilm);
            oldFilm.getDirectors().clear();
            oldFilm.getDirectors().addAll(loadDirector(oldFilm.getId()));
        }

        log.info("Данные фильма с ID: {} успешно обновлены.", oldFilm.getId());
        return oldFilm;
    }

    @Override
    public List<Film> getAllFilms() {
        String query = "SELECT * FROM films";

        try {
            log.info("Начат процесс предоставления списка всех фильмов.");
            List<Film> films = jdbc.query(query, mapper);

            if (!films.isEmpty()) {
                for (Film film : films) {
                    fillFilmAdditionalData(film);
                }
                log.info("Список всех фильмов успешно предоставлен.");
                return films;
            } else {
                log.info("Список всех фильмов пуст.");
                return films;
            }
        } catch (DataAccessException e) {
            log.error("Неудачная попытка получения списка всех фильмов. --> {}", e.getMessage());
            throw new DataBaseException("Не удалось получить список всех фильмов.");
        }
    }

    @Override
    public Film getFilmById(Long filmId) {
        String query = "SELECT * FROM films WHERE id = ?";

        try {
            log.info("Начата проверка наличия фильма с ID: {}", filmId);
            Film film = jdbc.queryForObject(query, mapper, filmId);

            fillFilmAdditionalData(film);

            log.info("Фильм с ID: {} найден и успешно предоставлен.", filmId);
            return film;
        } catch (DataAccessException e) {
            log.error("Неудачная попытка получения фильма по ID: {}. --> {}", filmId, e.getMessage());
            throw new NotFoundException("Фильм с ID: " + filmId + " не найден");
        }
    }

    @Override
    public List<Film> getFilmsByDirectorSortedByLikes(Long directorId) {
        String query = "SELECT f.* " +
                "FROM films f " +
                "JOIN film_directors fd ON f.id = fd.film_id " +
                "LEFT JOIN film_likes fl ON f.id = fl.film_id " +
                "WHERE fd.director_id = ? " +
                "GROUP BY f.id " +
                "ORDER BY COUNT(fl.user_id) DESC";

        List<Film> films = jdbc.query(query, mapper, directorId);

        for (Film film : films) {
            fillFilmAdditionalData(film);
        }

        return films;
    }

    @Override
    public List<Film> getFilmsByDirectorSortedByYear(Long directorId) {
        String query = "SELECT f.* FROM films f " +
                "JOIN film_directors fd ON f.id = fd.film_id " +
                "WHERE fd.director_id = ? " +
                "ORDER BY f.release_date";

        List<Film> films = jdbc.query(query, mapper, directorId);

        for (Film film : films) {
            fillFilmAdditionalData(film);
        }

        return films;
    }

    @Override
    public List<Film> searchFilms(String query, List<String> criteria) {
        log.info("Поиск фильмов по запросу: '{}' с критериями: {}", query, criteria);

        if (query == null || query.trim().isEmpty()) {
            log.warn("Запрос поиска пустой");
            return new ArrayList<>();
        }

        String searchQuery = query.toLowerCase().trim();

        try {
            StringBuilder sql = new StringBuilder(
                    "SELECT DISTINCT f.* FROM films f " +
                            "LEFT JOIN film_directors fd ON f.id = fd.film_id " +
                            "LEFT JOIN directors d ON fd.director_id = d.id "
            );

            List<String> conditions = new ArrayList<>();
            List<Object> params = new ArrayList<>();

            if (criteria.contains("title")) {
                conditions.add("LOWER(f.name) LIKE ?");
                params.add("%" + searchQuery + "%");
            }

            if (criteria.contains("director")) {
                conditions.add("LOWER(d.name) LIKE ?");
                params.add("%" + searchQuery + "%");
            }

            if (conditions.isEmpty()) {
                log.warn("Нет критериев поиска, возвращаем пустой список");
                return new ArrayList<>();
            }

            if (!conditions.isEmpty()) {
                sql.append("WHERE ");
                if (conditions.size() == 2) {
                    sql.append("(").append(conditions.get(0)).append(" OR ").append(conditions.get(1)).append(")");
                } else {
                    sql.append(conditions.get(0));
                }
            }

            sql.append(" ORDER BY f.id");

            List<Film> films = jdbc.query(sql.toString(), mapper, params.toArray());
            log.info("Найдено {} фильмов по запросу '{}'", films.size(), query);

            for (Film film : films) {
                fillFilmAdditionalData(film);
            }

            return films;

        } catch (DataAccessException e) {
            log.error("Ошибка БД при поиске фильмов по запросу '{}': {}", query, e.getMessage(), e);
            throw new DataBaseException("Не удалось выполнить поиск фильмов.");
        }
    }

    @Override
    public List<Film> getMostPopularFilms(int count) {
        log.info("Получение {} самых популярных фильмов", count);

        String sql = "SELECT f.* FROM films f " +
                "LEFT JOIN film_likes fl ON f.id = fl.film_id " +
                "GROUP BY f.id " +
                "ORDER BY COUNT(fl.user_id) DESC " +
                "LIMIT ?";

        try {
            List<Film> films = jdbc.query(sql, mapper, count);

            for (Film film : films) {
                fillFilmAdditionalData(film);
            }

            log.info("Успешно получено {} популярных фильмов", films.size());
            return films;

        } catch (DataAccessException e) {
            log.error("Ошибка при получении популярных фильмов: {}", e.getMessage(), e);
            throw new DataBaseException("Не удалось получить популярные фильмы");
        }
    }

    private void fillFilmAdditionalData(Film film) {
        // Жанры
        List<FilmGenre> filmGenres = genreDAO.getGenresOfFilm(film);
        if (!filmGenres.isEmpty()) {
            film.getGenres().addAll(filmGenres);
        }

        // Рейтинг MPA
        FilmAgeRating filmAgeRating = ratingDAO.getRatingOfFilm(film);
        if (filmAgeRating != null) {
            film.getMpa().setId(filmAgeRating.getId());
            film.getMpa().setName(filmAgeRating.getName());
        }

        // Лайки
        Set<Long> filmLikes = likeDAO.getLikesOfFilm(film);
        if (!filmLikes.isEmpty()) {
            film.getFilmLikedUsersId().addAll(filmLikes);
        }

        // Режиссеры
        Set<Director> directors = loadDirector(film.getId());
        if (!directors.isEmpty()) {
            film.getDirectors().addAll(directors);
        }
    }

    private void addDirectors(Film film) {
        String query = "INSERT INTO film_directors(film_id, director_id) VALUES (?, ?)";

        if (film.getDirectors() == null || film.getDirectors().isEmpty()) {
            return;
        }

        List<Object[]> batchArgs = film.getDirectors().stream()
                .map(Director::getId)
                .filter(Objects::nonNull)
                .distinct()
                .map(directorId -> new Object[]{film.getId(), directorId})
                .toList();

        if (!batchArgs.isEmpty()) {
            jdbc.batchUpdate(query, batchArgs);
        }
    }

    private Set<Director> loadDirector(Long filmId) {
        String query = "SELECT d.id, d.name " +
                "FROM directors d " +
                "JOIN film_directors fd ON d.id = fd.director_id " +
                "WHERE fd.film_id = ?";

        return new HashSet<>(jdbc.query(query, directorRowMapper, filmId));
    }

    private void removeDirectorFromFilm(Long id) {
        String query = "DELETE FROM film_directors WHERE film_id = ?";
        jdbc.update(query, id);
    }
}