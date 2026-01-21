package ru.yandex.practicum.filmorate.storage.review;

import ru.yandex.practicum.filmorate.model.Review;

import java.util.List;

public interface ReviewStorage {

    // Добавляем новый отзыв.
    Review addReview(Review review);

    // Обновляем имеющийся отзыв.
    Review updateReview(Review updatedReview);

    // Удаляем имеющийся отзыв.
    Review removeReview(Long reviewId);

    // Получаем отзыв по ID.
    Review getReviewById(Long reviewId);

    // Получаем список отзывов фильма.
    List<Review> getReviews(Long filmId, Long count);

}