package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.model.Review;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/reviews")
public class ReviewController {

    // Храним "отзывы" в памяти для заглушки
    private final List<Review> reviews = new ArrayList<>();
    private Long nextId = 1L;

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public Review addReview(@Valid @RequestBody Review review) {
        // Создаем копию с ID
        Review newReview = new Review(
                nextId++,
                review.getContent(),
                review.getIsPositive(),
                review.getUserId(),
                review.getFilmId(),
                0L
        );
        reviews.add(newReview);
        return newReview;
    }

    @PutMapping
    @ResponseStatus(HttpStatus.OK)
    public Review updateReview(@Valid @RequestBody Review review) {
        // Просто возвращаем тот же отзыв
        return review;
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public void deleteReview(@PathVariable Long id) {
        // Ничего не делаем, просто возвращаем 200
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public Review getReview(@PathVariable Long id) {
        // Возвращаем заглушку
        return new Review(
                id,
                "Test review content",
                true,
                1L,
                1L,
                0L
        );
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<Review> getReviewsByFilmId(
            @RequestParam(required = false) Long filmId,
            @RequestParam(defaultValue = "10") Integer count) {

        List<Review> result = new ArrayList<>();
        if (filmId != null) {
            // Возвращаем отзывы для конкретного фильма
            for (int i = 0; i < Math.min(count, 3); i++) {
                result.add(new Review(
                        (long) (i + 1),
                        "Review for film " + filmId + " - " + (i + 1),
                        i % 2 == 0,
                        1L,
                        filmId,
                        (long) i * 10
                ));
            }
        } else {
            // Возвращаем все отзывы
            result.addAll(reviews);
            if (result.isEmpty()) {
                // Если нет отзывов, возвращаем заглушку
                result.add(new Review(
                        1L,
                        "Test review",
                        true,
                        1L,
                        1L,
                        5L
                ));
            }
        }
        return result;
    }

    @PutMapping("/{id}/like/{userId}")
    @ResponseStatus(HttpStatus.OK)
    public void addLike(@PathVariable Long id, @PathVariable Long userId) {
        // Ничего не делаем
    }

    @PutMapping("/{id}/dislike/{userId}")
    @ResponseStatus(HttpStatus.OK)
    public void addDislike(@PathVariable Long id, @PathVariable Long userId) {
        // Ничего не делаем
    }

    @DeleteMapping("/{id}/like/{userId}")
    @ResponseStatus(HttpStatus.OK)
    public void removeLike(@PathVariable Long id, @PathVariable Long userId) {
        // Ничего не делаем
    }

    @DeleteMapping("/{id}/dislike/{userId}")
    @ResponseStatus(HttpStatus.OK)
    public void removeDislike(@PathVariable Long id, @PathVariable Long userId) {
        // Ничего не делаем
    }
}