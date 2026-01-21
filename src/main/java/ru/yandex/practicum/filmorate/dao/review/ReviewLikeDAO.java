package ru.yandex.practicum.filmorate.dao.review;

public interface ReviewLikeDAO {

    // Ставим лайк отзыву.
    void addLikeToReview(Long reviewId, Long userId);

    // Ставим дизлайк отзыву.
    void addDislikeToReview(Long reviewId, Long userId);

    // Удаляем лайк у отзыва.
    void removeLikeFromReview(Long reviewId, Long userId);

    // Удаляем дизлайк у отзыва.
    void removeDislikeFromReview(Long reviewId, Long userId);

}