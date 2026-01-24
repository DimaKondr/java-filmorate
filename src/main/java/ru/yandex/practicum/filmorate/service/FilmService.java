package ru.yandex.practicum.filmorate.service;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.dao.feed.UserFeedDAO;
import ru.yandex.practicum.filmorate.dao.like.LikeDAO;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.SortType;
import ru.yandex.practicum.filmorate.model.Operation;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;

import java.util.*;

@Service
@Getter
@Slf4j
public class FilmService {
    private final FilmStorage filmStorage;
    private final UserService userService;
    private final LikeDAO likeDAO;
    private final UserFeedDAO userFeedDAO;

    @Autowired
    public FilmService(@Qualifier("filmDbStorage") FilmStorage filmStorage,
                       UserService userService,
                       LikeDAO likeDAO,
                       UserFeedDAO userFeedDAO) {
        this.filmStorage = filmStorage;
        this.userService = userService;
        this.likeDAO = likeDAO;
        this.userFeedDAO = userFeedDAO;
    }

    public Film addLike(Long likedFilmId, Long userId) {
        log.info("Начат процесс добавления нового лайка.");
        User user = userService.getUserStorage().getUserById(userId);
        Film film = filmStorage.getFilmById(likedFilmId);
        likeDAO.addLikeToFilm(film, user.getId());

        userFeedDAO.addLikeEvent(userId, likedFilmId, Operation.ADD);

        return film;
    }

    public Film removeLike(Long unlikedFilmId, Long userId) {
        log.info("Начат процесс удаления лайка фильма.");
        try {
            userService.getUserStorage().getUserById(userId);
        } catch (NotFoundException e) {
            throw new NotFoundException("Пользователь с ID: "
                    + userId + " не найден. Невозможно удалить лайк у фильма");
        }
        Film film = filmStorage.getFilmById(unlikedFilmId);
        likeDAO.removeLikeFromFilm(film, userId);

        userFeedDAO.addLikeEvent(userId, unlikedFilmId, Operation.REMOVE);

        return film;
    }

    public List<Film> getMostPopularFilms(Long count, Long genreId, Long year) {
        log.info("=== ПОЛУЧЕНИЕ ПОПУЛЯРНЫХ ФИЛЬМОВ ===");
        log.info("Параметры: count={}, genreId={}, year={}", count, genreId, year);

        // Используем метод из FilmStorage (ваш текущий FilmDbStorage уже имеет его)
        List<Film> allPopularFilms = filmStorage.getMostPopularFilms(count.intValue());

        // Если нет фильтрации, возвращаем как есть
        if (genreId == null && year == null) {
            log.info("Без фильтрации, возвращаем {} фильмов", allPopularFilms.size());
            return allPopularFilms;
        }

        // Фильтруем результаты
        List<Film> filteredFilms = new ArrayList<>();

        for (Film film : allPopularFilms) {
            boolean passesFilter = true;

            // Фильтрация по жанру
            if (genreId != null) {
                boolean hasGenre = film.getGenres().stream()
                        .anyMatch(genre -> genreId.equals(genre.getId()));
                if (!hasGenre) {
                    passesFilter = false;
                    log.debug("Фильм ID {} не имеет жанра ID {}, пропускаем", film.getId(), genreId);
                }
            }

            // Фильтрация по году
            if (year != null && passesFilter) {
                if (film.getReleaseDate().getYear() != year) {
                    passesFilter = false;
                    log.debug("Фильм ID {} имеет год {}, а нужен {}, пропускаем",
                            film.getId(), film.getReleaseDate().getYear(), year);
                }
            }

            if (passesFilter) {
                filteredFilms.add(film);
                log.debug("Фильм ID {} прошел фильтрацию", film.getId());
            }
        }

        log.info("=== ПОПУЛЯРНЫЕ ФИЛЬМЫ ПОЛУЧЕНЫ ===");
        log.info("Возвращаем {} отфильтрованных фильмов", filteredFilms.size());
        return filteredFilms;
    }

    public List<Film> getFilmsByDirector(Long directorId, SortType sortType) {
        log.info("Получение фильмов режиссера ID: {} с сортировкой: {}", directorId, sortType);
        return switch (sortType) {
            case LIKES -> filmStorage.getFilmsByDirectorSortedByLikes(directorId);
            case YEAR -> filmStorage.getFilmsByDirectorSortedByYear(directorId);
        };
    }

    public List<Film> searchFilms(String query, String by) {
        log.info("=== НАЧАЛО ПОИСКА ФИЛЬМОВ ===");
        log.info("Входные параметры: query='{}', by='{}'", query, by);

        if (query == null) {
            log.warn("Запрос поиска null, возвращаем пустой список");
            return Collections.emptyList();
        }

        String trimmedQuery = query.trim();
        log.debug("Обрезанный запрос: '{}'", trimmedQuery);

        if (trimmedQuery.isEmpty()) {
            log.warn("Запрос поиска пустой, возвращаем пустой список");
            return Collections.emptyList();
        }

        // Проверка на минимальную длину запроса
        if (trimmedQuery.length() < 1) {
            log.warn("Слишком короткий запрос поиска: '{}'", trimmedQuery);
            return Collections.emptyList();
        }

        try {
            // Нормализация параметра by
            String normalizedBy = normalizeSearchByParameter(by);
            log.info("Нормализованный параметр by: '{}'", normalizedBy);

            // Парсинг критериев
            List<String> criteria = parseSearchCriteria(normalizedBy);
            log.info("Критерии поиска после парсинга: {}", criteria);

            if (criteria.isEmpty()) {
                log.warn("Нет допустимых критериев поиска, используем оба");
                criteria.add("title");
                criteria.add("director");
            }

            // Выполнение поиска
            log.info("Вызываем filmStorage.searchFilms('{}', {})", trimmedQuery, criteria);
            List<Film> result = filmStorage.searchFilms(trimmedQuery, criteria);

            log.info("=== РЕЗУЛЬТАТ ПОИСКА ===");
            log.info("Найдено фильмов: {}", result.size());

            if (result.isEmpty()) {
                log.info("Фильмы по запросу '{}' не найдены", trimmedQuery);
            } else {
                log.debug("Пример найденных фильмов:");
                for (int i = 0; i < Math.min(result.size(), 3); i++) {
                    Film film = result.get(i);
                    log.debug("  {}. ID: {}, Название: '{}', Режиссеры: {}",
                            i + 1, film.getId(), film.getName(),
                            film.getDirectors().stream().map(d -> d.getName()).toList());
                }
            }

            return result;

        } catch (ValidationException e) {
            log.error("Ошибка валидации при поиске: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("=== ОШИБКА ПОИСКА ===");
            log.error("Тип ошибки: {}", e.getClass().getName());
            log.error("Сообщение: {}", e.getMessage());
            log.error("Трассировка стека:", e);

            // Пробрасываем более информативное исключение
            throw new ru.yandex.practicum.filmorate.exception.DataBaseException(
                    "Не удалось выполнить поиск фильмов. Ошибка: " +
                            (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
        }
    }

    private String normalizeSearchByParameter(String by) {
        log.debug("Нормализация параметра by: исходное значение '{}'", by);

        if (by == null) {
            log.debug("Параметр by null, используем значение по умолчанию");
            return "title,director";
        }

        String trimmed = by.trim();
        if (trimmed.isEmpty()) {
            log.debug("Параметр by пустой, используем значение по умолчанию");
            return "title,director";
        }

        // Удаляем все пробелы и приводим к нижнему регистру
        String normalized = trimmed.replaceAll("\\s+", "").toLowerCase();
        log.debug("После удаления пробелов: '{}'", normalized);

        // Проверяем, что строка не пустая после обработки
        if (normalized.isEmpty()) {
            log.warn("Параметр by стал пустым после обработки, используем значение по умолчанию");
            return "title,director";
        }

        log.debug("Нормализованный результат: '{}'", normalized);
        return normalized;
    }

    private List<String> parseSearchCriteria(String by) {
        log.debug("Парсинг критериев из строки: '{}'", by);

        List<String> criteria = new ArrayList<>();

        if (by == null || by.trim().isEmpty()) {
            log.debug("Строка критериев пустая, используем оба по умолчанию");
            criteria.add("title");
            criteria.add("director");
            return criteria;
        }

        String[] parts = by.split(",");
        log.debug("Разделено на {} частей: {}", parts.length, Arrays.toString(parts));

        for (String part : parts) {
            String trimmed = part.trim().toLowerCase();
            log.debug("Обработка части: '{}'", trimmed);

            if (trimmed.isEmpty()) {
                log.debug("Пропускаем пустую часть");
                continue;
            }

            if ("title".equals(trimmed)) {
                if (!criteria.contains("title")) {
                    criteria.add("title");
                    log.debug("Добавлен критерий 'title'");
                }
            } else if ("director".equals(trimmed)) {
                if (!criteria.contains("director")) {
                    criteria.add("director");
                    log.debug("Добавлен критерий 'director'");
                }
            } else {
                log.warn("Пропускаем недопустимое значение при парсинге критериев: '{}'", trimmed);
            }
        }

        // Если после парсинга критерии пустые, используем оба
        if (criteria.isEmpty()) {
            log.warn("Не удалось распарсить критерии, используем оба по умолчанию");
            criteria.add("title");
            criteria.add("director");
        }

        log.debug("Результат парсинга критериев: {}", criteria);
        return criteria;
    }
}