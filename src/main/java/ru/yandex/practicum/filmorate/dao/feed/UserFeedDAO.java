package ru.yandex.practicum.filmorate.dao.feed;

import ru.yandex.practicum.filmorate.model.Operation;
import ru.yandex.practicum.filmorate.model.UserFeed;

import java.util.List;

public interface UserFeedDAO {

    void addEvent(UserFeed event);

    List<UserFeed> getFeedByUserId(Long userId);

    void addLikeEvent(Long userId, Long entityId, Operation operation);

    void addFriendEvent(Long userId, Long entityId, Operation operation);

}