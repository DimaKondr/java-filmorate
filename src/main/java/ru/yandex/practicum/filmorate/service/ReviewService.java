package ru.yandex.practicum.filmorate.service;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.dao.review.ReviewLikeDAO;
import ru.yandex.practicum.filmorate.model.Review;
import ru.yandex.practicum.filmorate.storage.review.ReviewStorage;

@Service
@Getter
@Slf4j
public class ReviewService {
    private final ReviewStorage reviewStorage;
    private final ReviewLikeDAO reviewDAO;

    @Autowired
    public ReviewService(ReviewStorage reviewStorage, ReviewLikeDAO reviewDAO) {
        this.reviewStorage = reviewStorage;
        this.reviewDAO = reviewDAO;
    }

    public Review addLike(Long reviewId, Long userId) {
        reviewDAO.addLikeToReview(reviewId, userId);
        return reviewStorage.getReviewById(reviewId);
    }

    public Review addDislike(Long reviewId, Long userId) {
        reviewDAO.addDislikeToReview(reviewId, userId);
        return reviewStorage.getReviewById(reviewId);
    }

    public Review removeLike(Long reviewId, Long userId) {
        reviewDAO.removeLikeFromReview(reviewId, userId);
        return reviewStorage.getReviewById(reviewId);
    }

    public Review removeDislike(Long reviewId, Long userId) {
        reviewDAO.removeDislikeFromReview(reviewId, userId);
        return reviewStorage.getReviewById(reviewId);
    }

}