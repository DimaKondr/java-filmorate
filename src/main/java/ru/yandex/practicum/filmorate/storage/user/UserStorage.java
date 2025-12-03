package ru.yandex.practicum.filmorate.storage.user;

import ru.yandex.practicum.filmorate.model.User;

import java.util.List;

public interface UserStorage {

    // Добавляем нового пользователя.
    User addUser(User user);

    // Удаляем имеющегося пользователя.
    User removeUser(Long userId);

    // Обновляем имеющегося пользователя.
    User updateUser(User updatedUser);

    // Получаем список всех имеющихся пользователей.
    List<User> getAllUsers();

    // Получаем пользователя по ID.
    User getUserById(Long userId);

}