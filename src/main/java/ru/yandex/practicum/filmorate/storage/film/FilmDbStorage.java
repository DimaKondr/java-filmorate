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

        // Очищаем и обновляем жанры
        oldFilm.getGenres().clear();
        if (!updatedFilm.getGenres().isEmpty()) {
            genreDAO.removeFilmsGenres(oldFilm);
            genreDAO.addGenreToFilm(updatedFilm);
            oldFilm.getGenres().addAll(updatedFilm.getGenres());
        }

        // Обновляем рейтинг MPA
        if (updatedFilm.getMpa().getId() != null && !updatedFilm.getMpa().getId().equals(oldFilm.getMpa().getId())) {
            ratingDAO.removeFilmRating(oldFilm);
            ratingDAO.addRatingToFilm(updatedFilm);
            oldFilm.getMpa().setId(updatedFilm.getMpa().getId());
            oldFilm.getMpa().setName(updatedFilm.getMpa().getName());
        }

        // Обновляем режиссеров
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

            for (Film film : films) {
                fillFilmAdditionalData(film);
            }

            log.info("Список всех фильмов успешно предоставлен. Найдено {} фильмов.", films.size());
            return films;
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
        log.info("Получение фильмов режиссера {} отсортированных по лайкам", directorId);

        // Проверяем существование режиссера
        String checkDirectorQuery = "SELECT COUNT(*) FROM directors WHERE id = ?";
        Integer directorCount = jdbc.queryForObject(checkDirectorQuery, Integer.class, directorId);

        if (directorCount == null || directorCount == 0) {
            log.warn("Режиссер с ID {} не найден", directorId);
            return new ArrayList<>();
        }

        String query = "SELECT f.* " +
                "FROM films f " +
                "JOIN film_directors fd ON f.id = fd.film_id " +
                "LEFT JOIN film_likes fl ON f.id = fl.film_id " +
                "WHERE fd.director_id = ? " +
                "GROUP BY f.id " +
                "ORDER BY COUNT(fl.user_id) DESC";

        try {
            List<Film> films = jdbc.query(query, mapper, directorId);

            for (Film film : films) {
                fillFilmAdditionalData(film);
            }

            log.info("Найдено {} фильмов режиссера {}, отсортированных по лайкам", films.size(), directorId);
            return films;
        } catch (DataAccessException e) {
            log.error("Ошибка при получении фильмов режиссера {}: {}", directorId, e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public List<Film> getFilmsByDirectorSortedByYear(Long directorId) {
        log.info("Получение фильмов режиссера {} отсортированных по году", directorId);

        // Проверяем существование режиссера
        String checkDirectorQuery = "SELECT COUNT(*) FROM directors WHERE id = ?";
        Integer directorCount = jdbc.queryForObject(checkDirectorQuery, Integer.class, directorId);

        if (directorCount == null || directorCount == 0) {
            log.warn("Режиссер с ID {} не найден", directorId);
            return new ArrayList<>();
        }

        String query = "SELECT f.* FROM films f " +
                "JOIN film_directors fd ON f.id = fd.film_id " +
                "WHERE fd.director_id = ? " +
                "ORDER BY f.release_date ASC";

        try {
            List<Film> films = jdbc.query(query, mapper, directorId);

            for (Film film : films) {
                fillFilmAdditionalData(film);
            }

            log.info("Найдено {} фильмов режиссера {}, отсортированных по году", films.size(), directorId);
            return films;
        } catch (DataAccessException e) {
            log.error("Ошибка при получении фильмов режиссера {}: {}", directorId, e.getMessage());
            return new ArrayList<>();
        }
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
        if (film == null) {
            return;
        }
    public List<Film> getCommonFilms(Long userId, Long friendId) {
        String query = "SELECT f.id, f.name, f.description, f.release_date, f.duration " +
                "FROM films AS f " +
                "JOIN film_likes AS f_l ON f_l.film_id = f.id " +
                "WHERE f_l.user_id IN (?, ?) " +
                "GROUP BY f.id, f.name, f.description, f.release_date, f.duration " +
                "HAVING COUNT(DISTINCT f_l.user_id) = 2 " +
                "ORDER BY COUNT(f_l.user_id) DESC";

        List<Film> films = jdbc.query(query, mapper, userId, friendId);

        for (Film film : films) {
            List<FilmGenre> filmGenres = genreDAO.getGenresOfFilm(film);
            film.getGenres().addAll(filmGenres);
        }

        return films;
    }

    private void addDirectors(Film film) {
        String query = "INSERT INTO film_directors(film_id, director_id) VALUES (?, ?)";

        try {
            // Жанры - коллекция уже инициализирована в конструкторе Film
            List<FilmGenre> filmGenres = genreDAO.getGenresOfFilm(film);
            if (filmGenres != null && !filmGenres.isEmpty()) {
                film.getGenres().addAll(filmGenres);
            }

            // Рейтинг MPA - объект уже инициализирован в конструкторе Film
            FilmAgeRating filmAgeRating = ratingDAO.getRatingOfFilm(film);
            if (filmAgeRating != null) {
                film.getMpa().setId(filmAgeRating.getId());
                film.getMpa().setName(filmAgeRating.getName());
            }

            // Лайки - коллекция уже инициализирована в конструкторе Film
            Set<Long> filmLikes = likeDAO.getLikesOfFilm(film);
            if (filmLikes != null && !filmLikes.isEmpty()) {
                film.getFilmLikedUsersId().addAll(filmLikes);
            }

            // Режиссеры - коллекция уже инициализирована в конструкторе Film
            Set<Director> directors = loadDirector(film.getId());
            if (directors != null && !directors.isEmpty()) {
                film.getDirectors().addAll(directors);
            }
        } catch (Exception e) {
            log.warn("Ошибка при заполнении дополнительных данных фильма {}: {}", film.getId(), e.getMessage());
        }
    }

    private void addDirectors(Film film) {
        if (film == null || film.getId() == null || film.getDirectors().isEmpty()) {
            return;
        }

        String query = "INSERT INTO film_directors(film_id, director_id) VALUES (?, ?)";

        List<Object[]> batchArgs = film.getDirectors().stream()
                .filter(Objects::nonNull)
                .map(Director::getId)
                .filter(Objects::nonNull)
                .distinct()
                .map(directorId -> new Object[]{film.getId(), directorId})
                .toList();

        if (!batchArgs.isEmpty()) {
            try {
                jdbc.batchUpdate(query, batchArgs);
            } catch (DataAccessException e) {
                log.error("Ошибка при добавлении режиссеров к фильму {}: {}", film.getId(), e.getMessage());
            }
        }
    }

    private Set<Director> loadDirector(Long filmId) {
        if (filmId == null) {
            return new HashSet<>();
        }

        String query = "SELECT d.id, d.name " +
                "FROM directors d " +
                "JOIN film_directors fd ON d.id = fd.director_id " +
                "WHERE fd.film_id = ?";

        try {
            List<Director> directors = jdbc.query(query, directorRowMapper, filmId);
            return directors != null ? new HashSet<>(directors) : new HashSet<>();
        } catch (Exception e) {
            log.warn("Ошибка при загрузке режиссеров для фильма {}: {}", filmId, e.getMessage());
            return new HashSet<>();
        }
    }

    private void removeDirectorFromFilm(Long id) {
        if (id == null) {
            return;
        }

        String query = "DELETE FROM film_directors WHERE film_id = ?";
        try {
            jdbc.update(query, id);
        } catch (DataAccessException e) {
            log.error("Ошибка при удалении режиссеров из фильма {}: {}", id, e.getMessage());
        }
    }
}