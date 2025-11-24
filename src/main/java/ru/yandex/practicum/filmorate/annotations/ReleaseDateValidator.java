package ru.yandex.practicum.filmorate.annotations;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDate;
import java.time.Month;

public class ReleaseDateValidator implements ConstraintValidator<ValidReleaseDate, LocalDate> {
    private LocalDate cinemaBirthDate;

    @Override
    public void initialize(ValidReleaseDate annotation) {
        cinemaBirthDate = LocalDate.of(1895, Month.DECEMBER, 28);
    }

    @Override
    public boolean isValid(LocalDate releaseDate, ConstraintValidatorContext context) {
        if (releaseDate == null) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Дата релиза не может быть null")
                    .addConstraintViolation();
            return false;
        }
        if (releaseDate.isBefore(cinemaBirthDate)) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Дата релиза не может быть раньше 28 декабря 1895 года")
                    .addConstraintViolation();
            return false;
        }
        return true;
    }

}