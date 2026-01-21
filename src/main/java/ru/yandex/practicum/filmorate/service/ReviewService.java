package ru.yandex.practicum.filmorate.service;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.dao.feed.UserFeedDAO;
import ru.yandex.practicum.filmorate.dao.review.ReviewLikeDAO;
import ru.yandex.practicum.filmorate.model.Operation;
import ru.yandex.practicum.filmorate.model.Review;
import ru.yandex.practicum.filmorate.storage.review.ReviewStorage;

import java.util.List;

@Service
@Getter
@Slf4j
public class ReviewService {
    private final ReviewStorage reviewStorage;
    private final ReviewLikeDAO reviewDAO;
    private final UserFeedDAO userFeedDAO;

    @Autowired
    public ReviewService(ReviewStorage reviewStorage, ReviewLikeDAO reviewDAO, UserFeedDAO userFeedDAO) {
        this.reviewStorage = reviewStorage;
        this.reviewDAO = reviewDAO;
        this.userFeedDAO = userFeedDAO;
    }

    public Review addReview(Review review) {
        Review createdReview = reviewStorage.addReview(review);
        userFeedDAO.addReviewEvent(review.getUserId(), createdReview.getReviewId(), Operation.ADD);
        return createdReview;
    }

    public Review updateReview(Review review) {
        Review updatedReview = reviewStorage.updateReview(review);
        userFeedDAO.addReviewEvent(review.getUserId(), updatedReview.getReviewId(), Operation.UPDATE);
        return updatedReview;
    }

    public Review removeReview(Long reviewId) {
        Review review = reviewStorage.getReviewById(reviewId);
        Review removedReview = reviewStorage.removeReview(reviewId);
        userFeedDAO.addReviewEvent(review.getUserId(), reviewId, Operation.REMOVE);
        return removedReview;
    }

    public Review getReviewById(Long reviewId) {
        return reviewStorage.getReviewById(reviewId);
    }

    public List<Review> getReviews(Long filmId, Long count) {
        return reviewStorage.getReviews(filmId, count);
    }

    public Review addLike(Long reviewId, Long userId) {
        reviewDAO.addLikeToReview(reviewId, userId);
        userFeedDAO.addReviewEvent(userId, reviewId, Operation.ADD);
        return reviewStorage.getReviewById(reviewId);
    }

    public Review addDislike(Long reviewId, Long userId) {
        reviewDAO.addDislikeToReview(reviewId, userId);
        userFeedDAO.addReviewEvent(userId, reviewId, Operation.ADD);
        return reviewStorage.getReviewById(reviewId);
    }

    public Review removeLike(Long reviewId, Long userId) {
        reviewDAO.removeLikeFromReview(reviewId, userId);
        userFeedDAO.addReviewEvent(userId, reviewId, Operation.REMOVE);
        return reviewStorage.getReviewById(reviewId);
    }

    public Review removeDislike(Long reviewId, Long userId) {
        reviewDAO.removeDislikeFromReview(reviewId, userId);
        userFeedDAO.addReviewEvent(userId, reviewId, Operation.REMOVE);
        return reviewStorage.getReviewById(reviewId);
    }
}