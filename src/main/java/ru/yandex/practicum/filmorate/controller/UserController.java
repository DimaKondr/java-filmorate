package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/users")
@Slf4j
public class UserController {
    private final Map<Long, User> users = new HashMap<>();

    @PostMapping
    public User addUser(@Valid @RequestBody User user) {
        if (user == null) {
            log.error("Запрос на добавление нового пользователя поступил с пустым телом");
            throw new ValidationException("Запрос на добавление пользователя поступил с пустым телом");
        }
        log.info("Начат процесс добавления нового пользователя. Проверяем уникальность E-mail");
        for (User u : users.values()) {
            if (user.getEmail().equals(u.getEmail())) {
                log.error("E-mail: {} уже используется", user.getEmail());
                throw new ValidationException("Указанный E-mail уже используется");
            }
        }
        if (user.getName() == null || user.getName().isBlank()) {
            log.debug("Имя не указано, устанавливаем имя как логин: {}", user.getLogin());
            user.setName(user.getLogin());
        }
        user.setId(getNextId());
        log.debug("Новому пользователю назначен ID: {}", user.getId());
        users.put(user.getId(), user);
        log.info("Успешно добавлен новый пользователь с ID: {}", user.getId());
        return user;
    }

    @PutMapping
    public User updateUser(@Valid @RequestBody User updatedUser) {
        if (updatedUser == null) {
            log.error("Запрос на обновление данных пользователя поступил с пустым телом");
            throw new ValidationException("Запрос на обновление данных пользователя поступил с пустым телом");
        }
        log.info("Начат процесс обновления данных пользователя. Проверяем ID пользователя");
        if (updatedUser.getId() == null) {
            log.error("Пользователь имеет ID со значением null");
            throw new ValidationException("ID пользователя должен быть указан");
        }
        log.info("Начата проверка наличия пользователя с ID: {}", updatedUser.getId());
        if (users.containsKey(updatedUser.getId())) {
            User oldUser = users.get(updatedUser.getId());
            if (!oldUser.getEmail().equals(updatedUser.getEmail()) && updatedUser.getEmail() != null
                    && !updatedUser.getEmail().isBlank()) {
                log.info("Начата проверка уникальности обновленного E-mail");
                for (User u : users.values()) {
                    if (updatedUser.getEmail().equals(u.getEmail())) {
                        log.error("Обновляемый E-mail: {} уже используется", updatedUser.getEmail());
                        throw new ValidationException("Обновляемый E-mail уже используется");
                    }
                }
                log.debug("Установлен новый E-mail: {}", updatedUser.getEmail());
                oldUser.setEmail(updatedUser.getEmail());
            }
            if (!oldUser.getLogin().equals(updatedUser.getLogin())) {
                log.debug("Устанавливаем обновленный логин: {}", updatedUser.getLogin());
                oldUser.setLogin(updatedUser.getLogin());
            }
            if (!oldUser.getName().equals(updatedUser.getName()) && updatedUser.getName() != null
                    && !updatedUser.getName().isBlank()) {
                log.debug("Устанавливаем обновленное имя пользователя: {}", updatedUser.getName());
                oldUser.setName(updatedUser.getName());
            }
            if (!oldUser.getBirthday().isEqual(updatedUser.getBirthday())) {
                log.debug("Устанавливаем обновленную дату рождения: {}", updatedUser.getBirthday());
                oldUser.setBirthday(updatedUser.getBirthday());
            }
            log.info("Данные пользователя с ID: {} успешно обновлены", oldUser.getId());
            return oldUser;
        }
        log.error("Пользователь с ID: {} не найден", updatedUser.getId());
        throw new ValidationException("Пользователь с ID: " + updatedUser.getId() + " не найден");
    }

    @GetMapping
    public List<User> getAllUsers() {
        return users.values().stream().toList();
    }

    //Генерируем ID нового пользователя
    private long getNextId() {
        long currentMaxId = users.keySet()
                .stream()
                .mapToLong(id -> id)
                .max()
                .orElse(0);
        return ++currentMaxId;
    }

}