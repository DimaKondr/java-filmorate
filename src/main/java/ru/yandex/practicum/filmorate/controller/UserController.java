package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.UserService;

import java.util.List;

@RestController
@RequestMapping("/users")
@Slf4j
@Validated
public class UserController {
    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public User addUser(@Valid @RequestBody User user) {
        return userService.getUserStorage().addUser(user);
    }

    @GetMapping("/{id}")
    public User getUser(@PathVariable("id")
                            @NotNull(message = "id не может быть null")
                            @Min(value = 1, message = "id должен быть положительным целым числом")
                            @Valid Long userId) {
        return userService.getUserStorage().getUserById(userId);
    }

    @DeleteMapping("/{id}")
    public User removeUser(@PathVariable("id")
                               @NotNull(message = "id не может быть null")
                               @Min(value = 1, message = "id должен быть положительным целым числом")
                               @Valid Long removedUserId) {
        return userService.getUserStorage().removeUser(removedUserId);
    }

    @PutMapping
    public User updateUser(@Valid @RequestBody User updatedUser) {
        return userService.getUserStorage().updateUser(updatedUser);
    }

    @GetMapping
    public List<User> getAllUsers() {
        return userService.getUserStorage().getAllUsers();
    }

    @PutMapping("/{id}/friends/{friendId}")
    public User addFriend(@PathVariable("id")
                              @NotNull(message = "id не может быть null")
                              @Min(value = 1, message = "id должен быть положительным целым числом")
                              @Valid Long userId,
                          @PathVariable("friendId")
                              @NotNull(message = "id не может быть null")
                              @Min(value = 1, message = "id должен быть положительным целым числом")
                              @Valid Long addedFriendsId) {
        return userService.addFriend(userId, addedFriendsId);
    }

    @DeleteMapping("/{id}/friends/{friendId}")
    public User removeFriend(@PathVariable("id")
                                 @NotNull(message = "id не может быть null")
                                 @Min(value = 1, message = "id должен быть положительным целым числом")
                                 @Valid Long userId,
                             @PathVariable("friendId")
                                 @NotNull(message = "id не может быть null")
                                 @Min(value = 1, message = "id должен быть положительным целым числом")
                                 @Valid Long removedFriendsId) {
        return userService.removeFriend(userId, removedFriendsId);
    }

    @GetMapping("/{id}/friends")
    public List<User> getFriendsListOfUser(@PathVariable("id")
                                               @NotNull(message = "id не может быть null")
                                               @Min(value = 1, message = "id должен быть положительным целым числом")
                                               @Valid Long userId) {
        return userService.getFriendsListOfUser(userId);
    }

    @GetMapping("/{id}/friends/common/{otherId}")
    public List<User> getMutualFriendsList(@PathVariable("id")
                                               @NotNull(message = "id не может быть null")
                                               @Min(value = 1, message = "id должен быть положительным целым числом")
                                               @Valid Long userId,
                                           @PathVariable("otherId")
                                               @NotNull(message = "id не может быть null")
                                               @Min(value = 1, message = "id должен быть положительным целым числом")
                                               @Valid Long anotherUserId) {
        return userService.getMutualFriendsList(userId, anotherUserId);
    }

}