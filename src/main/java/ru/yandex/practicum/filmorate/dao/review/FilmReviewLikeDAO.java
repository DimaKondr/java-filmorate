package ru.yandex.practicum.filmorate.dao.review;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.DataBaseException;

import java.sql.PreparedStatement;

@Repository
@RequiredArgsConstructor
@Slf4j
public class FilmReviewLikeDAO implements ReviewLikeDAO {
    private final JdbcTemplate jdbc;

    @Override
    public void addLikeToReview(Long reviewId, Long userId) {
        log.info("Начат процесс добавления лайка отзыву.");
        String queryForUpdateTrying = "UPDATE reviews_likes SET status = 'LIKED' " +
                            "WHERE review_id = ? AND user_id = ?";

        String queryForInsert = "INSERT INTO reviews_likes(review_id, user_id, status) VALUES (?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        int affectedRows = -1;

        try {
            affectedRows = jdbc.update(queryForUpdateTrying, reviewId, userId);
            if (affectedRows == 1) {
                increaseUsefulByOne(reviewId);
                increaseUsefulByOne(reviewId);
            }
            if (affectedRows == 0) {
                affectedRows = jdbc.update(connection -> {
                    PreparedStatement stmt = connection.prepareStatement(queryForInsert, new String[]{"id"});
                    stmt.setLong(1, reviewId);
                    stmt.setLong(2, userId);
                    stmt.setString(3, "LIKED");
                    return stmt;
                }, keyHolder);

                Long id = keyHolder.getKeyAs(Long.class);
                if (id != null) {
                    log.debug("Отзыву с ID: {} добавлен лайк с ID: {}", reviewId, id);
                } else {
                    log.error("Не удалось добавить лайк отзыву, так как ID записи в БД имеет null-значение.");
                    throw new DataBaseException("Не удалось добавить лайк отзыву.");
                }
                increaseUsefulByOne(reviewId);
            }
        } catch (DataAccessException e) {
            log.error("Неудачная попытка добавления лайка отзыву. --> {}", e.getMessage());
            throw new DataBaseException("Не удалось добавить лайк отзыву.");
        }

        if (affectedRows != 1) {
            log.error("При добавлении лайка отзыву должна быть обработана 1 строка, а обработано {} строк.",
                    affectedRows);
            throw new DataBaseException("Не удалось добавить лайк отзыву.");
        }
    }

    @Override
    public void addDislikeToReview(Long reviewId, Long userId) {
        log.info("Начат процесс добавления дизлайка отзыву.");
        String queryForUpdateTrying = "UPDATE reviews_likes SET status = 'DISLIKED' " +
                "WHERE review_id = ? AND user_id = ?";

        String queryForInsert = "INSERT INTO reviews_likes(review_id, user_id, status) VALUES (?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        int affectedRows = -1;

        try {
            affectedRows = jdbc.update(queryForUpdateTrying, reviewId, userId);
            if (affectedRows == 1) {
                decreaseUsefulByOne(reviewId);
                decreaseUsefulByOne(reviewId);
            }
            if (affectedRows == 0) {
                affectedRows = jdbc.update(connection -> {
                    PreparedStatement stmt = connection.prepareStatement(queryForInsert, new String[]{"id"});
                    stmt.setLong(1, reviewId);
                    stmt.setLong(2, userId);
                    stmt.setString(3, "DISLIKED");
                    return stmt;
                }, keyHolder);

                Long id = keyHolder.getKeyAs(Long.class);
                if (id != null) {
                    log.debug("Отзыву с ID: {} добавлен дизлайк с ID: {}", reviewId, id);
                } else {
                    log.error("Не удалось добавить дизлайк отзыву, так как ID записи в БД имеет null-значение.");
                    throw new DataBaseException("Не удалось добавить дизлайк отзыву.");
                }
                decreaseUsefulByOne(reviewId);
            }
        } catch (DataAccessException e) {
            log.error("Неудачная попытка добавления дизлайка отзыву. --> {}", e.getMessage());
            throw new DataBaseException("Не удалось добавить дизлайк отзыву.");
        }

        if (affectedRows != 1) {
            log.error("При добавлении дизлайка отзыву должна быть обработана 1 строка, а обработано {} строк.",
                    affectedRows);
            throw new DataBaseException("Не удалось добавить дизлайк отзыву.");
        }
    }

    @Override
    public void removeLikeFromReview(Long reviewId, Long userId) {
        log.info("Начат процесс удаления лайка отзыва.");
        String query = "DELETE FROM reviews_likes WHERE review_id = ? AND user_id = ? AND status = 'LIKED'";

        int affectedRows = -1;
        try {
            affectedRows = jdbc.update(query, reviewId, userId);
        } catch (DataAccessException e) {
            log.error("Неудачная попытка удаления лайка отзыва. --> {}", e.getMessage());
            throw new DataBaseException("Не удалось удалить лайк отзыва.");
        }

        if (affectedRows != 1) {
            log.error("При удалении лайка отзыва должна быть обработана 1 строка, а обработано {} строк.",
                    affectedRows);
            throw new DataBaseException("Не удалось удалить лайк отзыва.");
        }
        log.debug("Лайк отзыва с ID: {} успешно удален.", reviewId);
        decreaseUsefulByOne(reviewId);
    }

    @Override
    public void removeDislikeFromReview(Long reviewId, Long userId) {
        log.info("Начат процесс удаления дизлайка отзыва.");
        String query = "DELETE FROM reviews_likes WHERE review_id = ? AND user_id = ? AND status = 'DISLIKED'";

        int affectedRows = -1;
        try {
            affectedRows = jdbc.update(query, reviewId, userId);
        } catch (DataAccessException e) {
            log.error("Неудачная попытка удаления дизлайка отзыва. --> {}", e.getMessage());
            throw new DataBaseException("Не удалось удалить дизлайк отзыва.");
        }

        if (affectedRows != 1) {
            log.error("При удалении дизлайка отзыва должна быть обработана 1 строка, а обработано {} строк.",
                    affectedRows);
            throw new DataBaseException("Не удалось удалить дизлайк отзыва.");
        }
        log.debug("Дизлайк отзыва с ID: {} успешно удален.", reviewId);
        increaseUsefulByOne(reviewId);
    }

    public void updateUseful(Long reviewId) {
        log.info("Начат процесс пересчета рейтинга отзыва c ID: {}.", reviewId);
        String queryForUpdateReviewUseful = "UPDATE reviews SET status = ? WHERE reviewId = ?";

        Long likesCount = getLikesCount(reviewId);
        Long dislikesCount = getDislikesCount(reviewId);

        int affectedRows = -1;

        try {
            affectedRows = jdbc.update(queryForUpdateReviewUseful, (likesCount + dislikesCount), reviewId);
        } catch (DataAccessException e) {
            log.error("Неудачная попытка обновления рейтинга отзыва. --> {}", e.getMessage());
            throw new DataBaseException("Не удалось обновить рейтинг отзыва.");
        }

        if (affectedRows != 1) {
            log.error("При обновлении рейтинга отзыва должна быть обработана 1 строка, а обработано {} строк.",
                    affectedRows);
            throw new DataBaseException("Не удалось обновить рейтинг отзыва.");
        }
        log.info("Рейтинг отзыва с ID: {} успешно обновлен.", reviewId);
    }

    private Long getLikesCount(Long reviewId) {
        String queryForLikesCount = "SELECT COUNT(review_id) " +
                            "FROM reviews_likes " +
                            "WHERE review_id = ? AND status = 'LIKED'";

        try {
            Long likesCount = jdbc.queryForObject(queryForLikesCount, Long.class, reviewId);
            log.debug("Отзыв с ID: {} имеет {} лайков.", reviewId, likesCount);
            return likesCount;
        } catch (EmptyResultDataAccessException e) {
            log.debug("Отзыв с ID: {} имеет 0 лайков.", reviewId);
            return 0L;
        } catch (DataAccessException e) {
            log.error("Не удалось посчитать количество лайков отзыва c ID: {}. --> {}",
                    reviewId, e.getMessage());
            throw new DataBaseException("Не удалось обновить данные о рейтинге отзыва.");
        }
    }

    private Long getDislikesCount(Long reviewId) {
        String queryForDislikesCount = "SELECT COUNT(review_id) " +
                                "FROM reviews_likes " +
                                "WHERE review_id = ? AND status = 'DISLIKED'";

        try {
            Long dislikesCount = jdbc.queryForObject(queryForDislikesCount, Long.class, reviewId);
            log.debug("Отзыв с ID: {} имеет {} дизлайков.", reviewId, dislikesCount);
            return dislikesCount;
        } catch (EmptyResultDataAccessException e) {
            log.debug("Отзыв с ID: {} имеет 0 дизлайков.", reviewId);
            return 0L;
        } catch (DataAccessException e) {
            log.error("Не удалось посчитать количество дизлайков отзыва c ID: {}. --> {}",
                    reviewId, e.getMessage());
            throw new DataBaseException("Не удалось обновить данные о рейтинге отзыва.");
        }
    }

    private void increaseUsefulByOne(Long reviewId) {
        String queryForUsefulValue = "SELECT useful FROM reviews WHERE reviewId = ?";

        Long usefulValue;
        try {
            usefulValue = jdbc.queryForObject(queryForUsefulValue, Long.class, reviewId);
            log.debug("Повышение рейтинга. Отзыв с ID: {} имеет рейтинг {}.", reviewId, usefulValue);
        } catch (DataAccessException e) {
            log.error("Повышение рейтинга. Не удалось получить рейтинг отзыва c ID: {}. --> {}", reviewId, e.getMessage());
            throw new DataBaseException("Не удалось получить рейтинг отзыва.");
        }

        String queryForIncreaseUsefulByOne = "UPDATE reviews SET useful = ? WHERE reviewId = ?";

        int affectedRows = -1;
        try {
            affectedRows = jdbc.update(queryForIncreaseUsefulByOne, (usefulValue + 1), reviewId);
        } catch (DataAccessException e) {
            log.error("Неудачная попытка увеличения рейтинга на один. --> {}", e.getMessage());
            throw new DataBaseException("Не удалось увеличить рейтинг отзыва.");
        }

        if (affectedRows != 1) {
            log.error("При повышении рейтинга отзыва должна быть обработана 1 строка, а обработано {} строк.",
                    affectedRows);
            throw new DataBaseException("Не удалось повысить рейтинг отзыва.");
        }
        log.info("Рейтинг отзыва с ID: {} успешно повышен на одну единицу.", reviewId);
    }

    private void decreaseUsefulByOne(Long reviewId) {
        String queryForUsefulValue = "SELECT useful FROM reviews WHERE reviewId = ?";

        Long usefulValue;
        try {
            usefulValue = jdbc.queryForObject(queryForUsefulValue, Long.class, reviewId);
            log.debug("Понижение рейтинга. Отзыв с ID: {} имеет рейтинг {}.", reviewId, usefulValue);
        } catch (DataAccessException e) {
            log.error("Понижение рейтинга. Не удалось получить рейтинг отзыва c ID: {}. --> {}",
                    reviewId, e.getMessage());
            throw new DataBaseException("Не удалось получить рейтинг отзыва.");
        }

        String queryForIncreaseUsefulByOne = "UPDATE reviews SET useful = ? WHERE reviewId = ?";

        int affectedRows = -1;
        try {
            affectedRows = jdbc.update(queryForIncreaseUsefulByOne, (usefulValue - 1), reviewId);
        } catch (DataAccessException e) {
            log.error("Неудачная попытка понижения рейтинга на один. --> {}", e.getMessage());
            throw new DataBaseException("Не удалось понизить рейтинг отзыва.");
        }

        if (affectedRows != 1) {
            log.error("При понижении рейтинга отзыва должна быть обработана 1 строка, а обработано {} строк.",
                    affectedRows);
            throw new DataBaseException("Не удалось понизить рейтинг отзыва.");
        }
        log.info("Рейтинг отзыва с ID: {} успешно понижен на одну единицу.", reviewId);
    }

}