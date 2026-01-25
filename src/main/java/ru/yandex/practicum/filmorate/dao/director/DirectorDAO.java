package ru.yandex.practicum.filmorate.dao.director;

import ru.yandex.practicum.filmorate.model.Director;

import java.util.List;
import java.util.Optional;

public interface DirectorDAO {

    //Список всех режиссёров
    List<Director> getAllDirectors();

    //Получение режиссёра по id
    Optional<Director> getDirectorById(Long id);

    //Создание режиссёра
    Director addDirector(Director director);

    //Изменение режиссёра
    Director updateDirector(Director director);

    //Удаление режиссёра
    boolean removeDirector(Long id);
}