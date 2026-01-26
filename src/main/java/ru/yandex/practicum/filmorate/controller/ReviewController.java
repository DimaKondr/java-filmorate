package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.model.Review;
import ru.yandex.practicum.filmorate.service.ReviewService;

import java.util.List;

@RestController
@RequestMapping("/reviews")
@Validated
public class ReviewController {
    private final ReviewService reviewService;

    @Autowired
    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Review addReviewToFilm(@Valid @RequestBody Review review) {
        return reviewService.addReview(review);
    }

    @PutMapping
    public Review updateReviewOfFilm(@Valid @RequestBody Review updatedReview) {
        return reviewService.updateReview(updatedReview);
    }

    @DeleteMapping("/{id}")
    public Review removeReviewFromFilm(@PathVariable("id")
                                       @NotNull(message = "id не может быть null")
                                       @Min(value = 1, message = "id должен быть положительным целым числом")
                                       @Valid Long removedReviewId) {
        return reviewService.removeReview(removedReviewId);
    }

    @GetMapping("/{id}")
    public Review getReview(@PathVariable("id")
                            @NotNull(message = "id не может быть null")
                            @Min(value = 1, message = "id должен быть положительным целым числом")
                            @Valid Long reviewId) {
        return reviewService.getReviewById(reviewId);
    }

    @GetMapping
    public List<Review> getReviewsOfFilm(@RequestParam(name = "filmId", required = false, defaultValue = "0")
                                         Long filmId,
                                         @RequestParam(name = "count", required = false, defaultValue = "10")
                                         @Positive(message = "count должен быть больше 0")
                                         @Valid
                                         Long count) {
        return reviewService.getReviews(filmId, count);
    }

    @PutMapping("/{id}/like/{userId}")
    public Review addLikeToReview(@PathVariable("id")
                                  @NotNull(message = "id не может быть null")
                                  @Valid Long reviewId,
                                  @PathVariable("userId")
                                  @NotNull(message = "userId не может быть null")
                                  @Valid Long userId) {
        return reviewService.addLike(reviewId, userId);
    }

    @PutMapping("/{id}/dislike/{userId}")
    public Review addDislikeToReview(@PathVariable("id")
                                     @NotNull(message = "id не может быть null")
                                     @Valid Long reviewId,
                                     @PathVariable("userId")
                                     @NotNull(message = "userId не может быть null")
                                     @Valid Long userId) {
        return reviewService.addDislike(reviewId, userId);
    }

    @DeleteMapping("/{id}/like/{userId}")
    public Review removeLikeFromReview(@PathVariable("id")
                                       @NotNull(message = "id не может быть null")
                                       @Valid Long reviewId,
                                       @PathVariable("userId")
                                       @NotNull(message = "userId не может быть null")
                                       @Valid Long userId) {
        return reviewService.removeLike(reviewId, userId);
    }

    @DeleteMapping("/{id}/dislike/{userId}")
    public Review removeDislikeFromReview(@PathVariable("id")
                                          @NotNull(message = "id не может быть null")
                                          @Valid Long reviewId,
                                          @PathVariable("userId")
                                          @NotNull(message = "userId не может быть null")
                                          @Valid Long userId) {
        return reviewService.removeDislike(reviewId, userId);
    }
}