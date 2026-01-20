package ru.yandex.practicum.filmorate.storage.film;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.filmorate.dao.genre.GenreDAO;
import ru.yandex.practicum.filmorate.dao.like.LikeDAO;
import ru.yandex.practicum.filmorate.dao.rating.RatingDAO;
import ru.yandex.practicum.filmorate.exception.DataBaseException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.FilmAgeRating;
import ru.yandex.practicum.filmorate.model.FilmGenre;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Set;

@Repository("filmDbStorage")
@RequiredArgsConstructor
@Slf4j
public class FilmDbStorage implements FilmStorage {
    private final JdbcTemplate jdbc;
    private final FilmRowMapper mapper;
    private final GenreDAO genreDAO;
    private final RatingDAO ratingDAO;
    private final LikeDAO likeDAO;

    @Override
    public Film addFilm(Film film) {
        if (film == null) {
            log.error("Запрос на добавление нового фильма поступил с пустым телом");
            throw new ValidationException("Запрос на добавление фильма поступил с пустым телом");
        }

        log.info("Начат процесс добавления нового фильма.");
        String query = "INSERT INTO films(name, description, release_Date, duration) " +
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
            log.info("Успешно добавлен новый фильм с ID: {}", film.getId());

            if (!film.getGenres().isEmpty()) {
                genreDAO.addGenreToFilm(film);
            }
            if (film.getMpa().getId() != null) {
                ratingDAO.addRatingToFilm(film);
            }
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

        Film film = getFilmById(filmId); // Сам выбросит NotFoundException

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

        log.info("Начат процесс обновления данных фильма. Проверяем ID пользователя");
        if (updatedFilm.getId() == null) {
            log.error("Фильм имеет ID со значением null");
            throw new ValidationException("ID фильма должен быть указан");
        }

        log.info("Начата проверка наличия фильма с ID: {}", updatedFilm.getId());
        Film oldFilm = null;
        try {
            oldFilm = getFilmById(updatedFilm.getId());
            if (oldFilm == null) {
                log.error("Обновление фильма. ID: {} Не найден", updatedFilm.getId());
                throw new NotFoundException("Обновление фильма. Фильм с ID: "
                        + updatedFilm.getId() + " не найден");
            }
        } catch (NotFoundException e) {
            log.error("Обновление фильма. Ошибка при проверке существования ID --> {}", e.getMessage());
            throw new NotFoundException("Обновление фильма. Ошибка при проверке существования ID");
        }

        LocalDate cinemaBirthDate = LocalDate.of(1895, Month.DECEMBER, 28);
        if (!oldFilm.getName().equals(updatedFilm.getName())) {
            log.debug("Устанавливаем обновленное название фильма: {}", updatedFilm.getName());
            oldFilm.setName(updatedFilm.getName());
        }
        if (!oldFilm.getDescription().equals(updatedFilm.getDescription())) {
            log.debug("Обновляем описание фильма: {}", updatedFilm.getDescription());
            oldFilm.setDescription(updatedFilm.getDescription());
        }
        if (!oldFilm.getReleaseDate().isEqual(updatedFilm.getReleaseDate())
                && updatedFilm.getReleaseDate().isAfter(cinemaBirthDate)) {
            log.debug("Устанавливаем обновленную дату релиза: {}", updatedFilm.getReleaseDate());
            oldFilm.setReleaseDate(updatedFilm.getReleaseDate());
        }
        if (!oldFilm.getDuration().equals(updatedFilm.getDuration())) {
            log.debug("Устанавливаем обновленную длительность фильма: {}", updatedFilm.getDuration());
            oldFilm.setDuration(updatedFilm.getDuration());
        }

        log.debug("Обновляем данные фильма с ID: {} ...", oldFilm.getId());
        String query = "UPDATE films SET name = ?, description = ?, release_Date = ?, duration = ? WHERE id = ?";
        int affectedRows = -1;

        try {
            affectedRows = jdbc.update(query,
                    oldFilm.getName(),
                    oldFilm.getDescription(),
                    oldFilm.getReleaseDate().toString(),
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
        log.info("Данные фильма с ID: {} успешно обновлены.", oldFilm.getId());

        if (!oldFilm.getGenres().equals(updatedFilm.getGenres())) {
            log.debug("Обновляем данные о жанрах фильма с ID: {}.", oldFilm.getId());
            genreDAO.removeFilmsGenres(oldFilm);
            genreDAO.addGenreToFilm(updatedFilm);
        }
        if (updatedFilm.getMpa().getId() != null && !updatedFilm.getMpa().getId().equals(oldFilm.getMpa().getId())) {
            log.debug("Обновляем данные о возрастном рейтинге фильма с ID: {}.", oldFilm.getId());
            ratingDAO.removeFilmRating(oldFilm);
            ratingDAO.addRatingToFilm(updatedFilm);
        }
        return oldFilm;
    }

    @Override
    public List<Film> getAllFilms() {
        String query = "SELECT * FROM films";

        try {
            log.info("Начат процесс предоставления списка всех фильмов.");
            List<Film> films = jdbc.query(query, mapper);
            if (!films.isEmpty()) {
                log.debug("Добавляем ID жанров в каждый фильм списка.");
                for (Film film : films) {
                    List<FilmGenre> filmGenres = genreDAO.getGenresOfFilm(film);
                    if (!filmGenres.isEmpty()) {
                        log.info("Список жанров фильма c ID: {} успешно предоставлен.", film.getId());
                        for (FilmGenre genre : filmGenres) {
                            film.getGenres().add(genre);
                        }
                    } else {
                        log.debug("Список жанров фильма с ID: {} пуст.", film.getId());
                    }

                    log.debug("Добавляем ID возрастного рейтинга в каждый фильм списка.");
                    FilmAgeRating filmAgeRating = ratingDAO.getRatingOfFilm(film);
                    if (filmAgeRating != null) {
                        film.getMpa().setId(filmAgeRating.getId());
                        film.getMpa().setName(filmAgeRating.getName());
                    }

                    log.debug("Добавляем имеющиеся лайки в каждый фильм списка.");
                    Set<Long> filmLikes = likeDAO.getLikesOfFilm(film);
                    if (!filmLikes.isEmpty()) {
                        log.info("Набор лайков фильма c ID: {} успешно предоставлен.", film.getId());
                        for (Long like : filmLikes) {
                            film.getFilmLikedUsersId().add(like);
                        }
                    } else {
                        log.debug("Список лайков фильма с ID: {} пуст.", film.getId());
                    }
                }
                log.info("Список всех фильмов успешно предоставлен.");
                return films;
            } else {
                log.error("Список всех фильмов пуст.");
                throw new DataBaseException("Список всех фильмов пуст.");
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
            log.info("Начата проверка наличия фильма с ID: {} для его предоставления по запросу", filmId);
            Film film = jdbc.queryForObject(query, mapper, filmId);
            if (film != null) {
                log.debug("Добавляем ID жанров в фильм.");
                List<FilmGenre> filmGenres = genreDAO.getGenresOfFilm(film);
                if (!filmGenres.isEmpty()) {
                    log.info("Получение фильма. Получен список жанров фильма c ID: {}", filmId);
                    for (FilmGenre genre : filmGenres) {
                        film.getGenres().add(genre);
                    }
                } else {
                    log.debug("Получение фильма. Список жанров фильма с ID: {} пуст.", film.getId());
                }

                log.debug("Добавляем ID возрастного рейтинга в фильм.");
                FilmAgeRating filmAgeRating = ratingDAO.getRatingOfFilm(film);
                if (filmAgeRating != null) {
                    film.getMpa().setId(filmAgeRating.getId());
                    film.getMpa().setName(filmAgeRating.getName());
                }

                log.debug("Добавляем ID пользователей, которые поставили лайк фильму.");
                Set<Long> filmLikes = likeDAO.getLikesOfFilm(film);
                if (!filmLikes.isEmpty()) {
                    log.info("Получение фильма. Получен список лайков фильма c ID: {}", filmId);
                    for (Long like : filmLikes) {
                        film.getFilmLikedUsersId().add(like);
                    }
                } else {
                    log.debug("Получение фильма. Список лайков фильма с ID: {} пуст.", film.getId());
                }

                log.info("Фильм с ID: {} найден и успешно предоставлен в ответ на запрос.", filmId);
                return film;
            } else {
                throw new DataBaseException("Не удалось получить фильм c ID: " + filmId);
            }
        } catch (DataAccessException e) {
            log.error("Неудачная попытка получения фильма по ID: {}. --> {}", filmId, e.getMessage());
            throw new NotFoundException("Попытка получения фильма. Фильм с ID: " + filmId + " не найден");
        }
    }

}