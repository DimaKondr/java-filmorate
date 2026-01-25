package ru.yandex.practicum.filmorate.service;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.dao.friendship.FriendshipDAO;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Getter
@Slf4j
public class UserService {
    private final UserStorage userStorage;
    private final FriendshipDAO friendshipDAO;
    private final FilmStorage filmStorage;

    @Autowired
    public UserService(@Qualifier("userDbStorage") UserStorage userStorage, FriendshipDAO friendshipDAO, @Qualifier("filmDbStorage") FilmStorage filmStorage) {
        this.userStorage = userStorage;
        this.friendshipDAO = friendshipDAO;
        this.filmStorage = filmStorage;
    }

    public User addFriend(Long userId, Long addedFriendsId) {
        log.info("Начат процесс добавления нового друга.");
        if (userId.equals(addedFriendsId)) {
            log.error("ID {} пользователя и ID {} друга для добавления совпадают", userId, addedFriendsId);
            throw new ValidationException("ID=" + userId + " пользователя и ID= "
                    + addedFriendsId + " друга для добавления совпадают");
        }
        User user = userStorage.getUserById(userId);
        User addedFriend = userStorage.getUserById(addedFriendsId);
        log.info("Начат процесс добавления друга с ID: {} к пользователю с ID: {}.", addedFriendsId, userId);
        return friendshipDAO.addFriendToUser(user, addedFriend);
    }

    public User removeFriend(Long userId, Long removedFriendsId) {
        log.info("Начат процесс удаления из списка друзей.");
        if (userId.equals(removedFriendsId)) {
            log.error("ID {} пользователя и ID {} друга для удаления совпадают", userId, removedFriendsId);
            throw new ValidationException("ID=" + userId + " пользователя и ID= "
                    + removedFriendsId + " друга для добавления совпадают");
        }
        User user = userStorage.getUserById(userId);
        User removedFriend = userStorage.getUserById(removedFriendsId);
        log.info("Начат процесс удаления друга с ID: {} у пользователя с ID: {}.", removedFriendsId, userId);
        return friendshipDAO.removeFriendFromUser(user, removedFriend);
    }

    public List<User> getFriendsListOfUser(Long userId) {
        log.info("Начат процесс получения списка друзей пользователя с ID {}.", userId);
        User user = userStorage.getUserById(userId);
        return friendshipDAO.getFriendsList(user);
    }

    public List<User> getMutualFriendsList(Long firstUserId, Long secondUserId) {
        log.info("Начат процесс получения списка общих друзей пользователей с ID = {} и ID = {}",
                firstUserId, secondUserId);
        if (firstUserId.equals(secondUserId)) {
            log.error("ID {} и ID {} обоих пользователей совпадают", firstUserId, secondUserId);
            throw new ValidationException("ID обоих пользователей совпадают");
        }
        User firstUser = userStorage.getUserById(firstUserId);
        User secondUser = userStorage.getUserById(secondUserId);
        return friendshipDAO.getMutualFriendsList(firstUser, secondUser);
    }

    private int compareUsersLikes(User user1, User user2) {
        Set<Long> user1likes = userStorage.getUserLikes(user1.getId());
        Set<Long> user2likes = userStorage.getUserLikes(user2.getId());

        log.debug("Сравнение пользователей {} и {}: лайки1={}, лайки2={}",
                user1.getId(), user2.getId(), user1likes, user2likes);

        user1likes.retainAll(user2likes); // останутся только общие
        int commonLikes = user1likes.size();

        log.debug("Общих лайков между {} и {}: {}", user1.getId(), user2.getId(), commonLikes);
        return commonLikes; // 0 если общих нет
    }

    private List<User> findSimilarUsers(User targetUser) {
        List<User> candidates = userStorage.getAllUsers().stream()
                .filter(user -> !user.getId().equals(targetUser.getId())) // убрать самого юзера из кандидатов в рекомендации
                .toList();

        Map<User, Integer> similarities = new HashMap<>();

        for (User user : candidates) {
            int commonLikes = compareUsersLikes(targetUser, user);
            if (commonLikes > 0) { // если есть хоть одно совпадение
                similarities.put(user, commonLikes); // кладем такого юзера + совпадения
            }
        }

        log.info("Найдено {} похожих пользователей (с общими лайками)", similarities.size());

        return similarities.entrySet().stream()
                .sorted(Map.Entry.<User, Integer>comparingByValue().reversed()) // сравниваем по значению + наоборот
                .map(Map.Entry::getKey) // остается только ключ(Юзер)
                .toList();
    }

    /**
     * Добавить фильмы от одного похожего пользователя в счетчик
     */
    private void addFilmsFromUser(User similarUser, Set<Long> targetUserWatched, Map<Long, Integer> filmScores) {

        Set<Long> similarUserLikes = userStorage.getUserLikes(similarUser.getId());

        int addedCount = 0;
        // Добавляем каждый фильм в счетчик
        for (Long filmId : similarUserLikes) {
            // Пропускаем, если целевой пользователь уже смотрел
            if (!targetUserWatched.contains(filmId)) {
                // Увеличиваем счетчик на 1
                filmScores.merge(filmId, 1, Integer::sum);
                addedCount++;
                log.debug("Добавлен фильм {} в рекомендации (счет: {})",
                        filmId, filmScores.get(filmId));
            } else {
                log.debug("Фильм {} пропущен - уже просмотрен", filmId);
            }
        }

        log.info("От пользователя {} добавлено {} новых фильмов в рекомендации",
                similarUser.getId(), addedCount);
    }

    /**
     * Собрать фильмы от похожих пользователей и подсчитать их популярность
     */
    private Map<Long, Integer> collectFilmScores(Long targetUserId, List<User> similarUsers) {
        // Фильмы, которые целевой пользователь уже смотрел
        Set<Long> alreadyWatched = userStorage.getUserLikes(targetUserId);

        // Счетчик популярности фильмов
        Map<Long, Integer> filmScores = new HashMap<>();

        // Проходим по каждому похожему пользователю
        for (User similarUser : similarUsers) {
            log.info("Обрабатываем похожего пользователя {}", similarUser.getId());
            addFilmsFromUser(similarUser, alreadyWatched, filmScores);
        }

        log.info("Итого собрано фильмов-кандидатов: {}. Популярность: {}",
                filmScores.size(), filmScores);

        return filmScores;
    }

    private List<Film> convertToFilmList(Map<Long, Integer> filmScores, int count) {
        log.info("Конвертация {} фильмов в список. Запрошено: {}", filmScores.size(), count);

        List<Film> films = filmScores.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .limit(count)
                .map(entry -> {
                    return filmStorage.getFilmById(entry.getKey());
                })
                .collect(Collectors.toList());

        log.info("Сформирован список из {} фильмов", films.size());
        return films;
    }

    public List<Film> getRecommendations(Long userId, int count) {
        log.info("=== НАЧАЛО: Получение {} рекомендаций для пользователя {} ===", count, userId);

        // 1. Получаем целевого пользователя
        User targetUser = userStorage.getUserById(userId);
        if (targetUser == null) {
            log.error("Пользователь {} не найден!", userId);
            return Collections.emptyList();
        }
        log.info("Целевой пользователь найден: {}", targetUser);

        // 2. Находим похожих пользователей
        List<User> similarUsers = findSimilarUsers(targetUser);

        if (similarUsers.isEmpty()) {
            log.warn("Не найдено похожих пользователей для {}", userId);
            return Collections.emptyList();
        }

        log.info("Найдено {} похожих пользователей", similarUsers.size());

        // 3. Собираем счетчики популярности
        Map<Long, Integer> filmScores = collectFilmScores(userId, similarUsers);

        if (filmScores.isEmpty()) {
            log.warn("Не найдено фильмов для рекомендаций (все фильмы уже просмотрены)");
            return Collections.emptyList();
        }

        // 4. Превращаем в список фильмов
        List<Film> recommendations = convertToFilmList(filmScores, count);

        log.info("=== РЕЗУЛЬТАТ: Найдено {} рекомендаций для пользователя {} ===",
                recommendations.size(), userId);
        return recommendations;
    }

}