package ru.yandex.practicum.filmorate.storage.review;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.DataBaseException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Review;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.FilmService;
import ru.yandex.practicum.filmorate.service.UserService;

import java.sql.PreparedStatement;
import java.util.List;

@Repository
@RequiredArgsConstructor
@Slf4j
public class ReviewDbStorage implements ReviewStorage {
    private final JdbcTemplate jdbc;
    private final ReviewRowMapper mapper;
    private final UserService userService;
    private final FilmService filmService;

    @Override
    public Review addReview(Review review) {
        if (review == null) {
            log.error("Запрос на добавление нового отзыва поступил с пустым телом");
            throw new ValidationException("Запрос на добавление отзыва поступил с пустым телом");
        }

        log.info("Проверяем, что пользователь с ID: {} существует.", review.getUserId());
        User user = userService.getUserStorage().getUserById(review.getUserId());

        log.info("Проверяем, что пользователь с ID: {} существует.", review.getUserId());
        Film film = filmService.getFilmStorage().getFilmById(review.getFilmId());

        log.info("Начат процесс добавления нового отзыва.");
        String query = "INSERT INTO reviews(content, isPositive, userId, filmId) " +
                "VALUES (?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        int affectedRows = -1;

        try {
            affectedRows = jdbc.update(connection -> {
                PreparedStatement stmt = connection.prepareStatement(query, new String[]{"reviewId"});
                stmt.setString(1, review.getContent());
                stmt.setBoolean(2, review.getIsPositive());
                stmt.setLong(3, review.getUserId());
                stmt.setLong(4, review.getFilmId());
                return stmt;
            }, keyHolder);
        } catch (DataAccessException e) {
            log.error("Неудачная попытка добавления нового отзыва. --> {}", e.getMessage());
            throw new DataBaseException("Не удалось добавить новый отзыв.");
        }

        if (affectedRows != 1) {
            log.error("При добавления нового отзыва должна быть обработана 1 строка, а обработано {} строк.",
                    affectedRows);
            throw new DataBaseException("Не удалось добавить новый отзыв.");
        }

        Long id = keyHolder.getKeyAs(Long.class);
        if (id != null) {
            review.setReviewId(id);
            log.debug("Новому отзыву назначен ID: {}", id);
            log.info("Успешно добавлен новый фильм с ID: {}", id);
            return review;
        } else {
            log.error("Не удалось добавить новый отзыв, так как ID имеет null-значение.");
            throw new DataBaseException("Не удалось добавить отзыв.");
        }
    }

    @Override
    public Review updateReview(Review updatedReview) {
        if (updatedReview == null) {
            log.error("Запрос на обновление данных фильма поступил с пустым телом");
            throw new ValidationException("Запрос на обновление данных фильма поступил с пустым телом");
        }

        log.info("Начата проверка наличия отзыва с ID: {}", updatedReview.getReviewId());
        Review oldReview = null;
        try {
            oldReview = getReviewById(updatedReview.getReviewId());
            if (oldReview == null) {
                log.error("Обновление отзыва. ID: {} не найден", updatedReview.getReviewId());
                throw new NotFoundException("Обновление отзыва. Отзыв с ID: "
                        + updatedReview.getReviewId() + " не найден");
            }
        } catch (NotFoundException e) {
            log.error("Обновление отзыва. Ошибка при проверке существования ID --> {}", e.getMessage());
            throw new NotFoundException("Обновление отзыва. Ошибка при проверке существования ID");
        }

        if (!oldReview.getContent().equals(updatedReview.getContent())) {
            log.debug("Устанавливаем обновленный текст отзыва.");
            oldReview.setContent(updatedReview.getContent());
        }
        if (!oldReview.getIsPositive().equals(updatedReview.getIsPositive())) {
            log.debug("Устанавливаем обновленный тип отзыва.");
            oldReview.setIsPositive(updatedReview.getIsPositive());
        }

        log.debug("Обновляем данные отзыва с ID: {} ...", oldReview.getReviewId());
        String query = "UPDATE reviews SET content = ?, isPositive = ? WHERE reviewId = ?";
        int affectedRows = -1;

        try {
            affectedRows = jdbc.update(query,
                    oldReview.getContent(),
                    oldReview.getIsPositive(),
                    oldReview.getReviewId());
        } catch (DataAccessException e) {
            log.error("Неудачная попытка обновления данных отзыва. --> {}", e.getMessage());
            throw new DataBaseException("Не удалось обновить данные отзыва.");
        }

        if (affectedRows != 1) {
            log.error("При обновлении данных отзыва должна быть обработана 1 строка, а обработано {} строк.",
                    affectedRows);
            throw new DataBaseException("Не удалось обновить данные отзыва.");
        }
        log.info("Данные отзыва с ID: {} успешно обновлены.", oldReview.getReviewId());
        return oldReview;
    }

    @Override
    public Review removeReview(Long reviewId) {
        String query = "DELETE FROM reviews WHERE reviewId = ?";

        try {
            log.info("Начат процесс удаления отзыва с ID: {}", reviewId);
            Review removedReview = null;
            try {
                removedReview = getReviewById(reviewId);
            } catch (NotFoundException e) {
                log.error("В процессе удаления отзыва не удалось его получить по ID: {}. --> {}",
                        reviewId, e.getMessage());
                throw new NotFoundException("Не удалось удалить отзыв.");
            }

            int affectedRows = jdbc.update(query, reviewId);
            if (affectedRows != 1) {
                log.error("При удалении отзыва должна быть обработана 1 строка, а обработано {} строк.",
                        affectedRows);
                throw new DataBaseException("Не удалось удалить отзыв.");
            }
            log.debug("Отзыв с ID: {} успешно удален.", reviewId);
            return removedReview;
        } catch (DataAccessException e) {
            log.error("Неудачная попытка удаления отзыва с ID: {}. --> {}", reviewId, e.getMessage());
            throw new DataBaseException("Не удалось удалить отзыв.");
        }
    }

    @Override
    public Review getReviewById(Long reviewId) {
        String query = "SELECT * FROM reviews WHERE reviewId = ?";

        try {
            log.info("Начата проверка наличия отзыва с ID: {} для его предоставления по запросу", reviewId);
            return jdbc.queryForObject(query, mapper, reviewId);
        } catch (DataAccessException e) {
            log.error("Неудачная попытка получения отзыва по ID: {}. --> {}", reviewId, e.getMessage());
            throw new NotFoundException("Попытка получения отзыва. Отзыв с ID: " + reviewId + " не найден");
        }
    }

    @Override
    public List<Review> getReviews(Long filmId, Long count) {
        String queryForAllReviews = "SELECT * " +
                            "FROM reviews " +
                            "ORDER BY useful DESC " +
                            "LIMIT ?";

        String queryForReviewsOfFilm = "SELECT * " +
                                "FROM reviews " +
                                "WHERE filmId = ? " +
                                "ORDER BY useful DESC " +
                                "LIMIT ?";

        try {
            log.info("Начат процесс предоставления списка отзывов.");
            List<Review> reviews = filmId == 0 ? jdbc.query(queryForAllReviews, mapper, count)
                    : jdbc.query(queryForReviewsOfFilm, mapper, filmId, count);
            log.info("Список отзывов успешно предоставлен.");
            return reviews;
        } catch (DataAccessException e) {
            log.error("Неудачная попытка получения списка отзывов. --> {}", e.getMessage());
            throw new DataBaseException("Не удалось получить список отзывов.");
        }
    }

}