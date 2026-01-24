package ru.yandex.practicum.filmorate.service;

import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.dao.director.FilmDirectorDAO;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Director;

import java.util.List;

@Service
public class DirectorService {

    private final FilmDirectorDAO filmDirectorDAO;

    public DirectorService(FilmDirectorDAO filmDirectorDAO) {
        this.filmDirectorDAO = filmDirectorDAO;
    }

    public Director addDirector(Director director) {
        return filmDirectorDAO.addDirector(director);
    }

    public Director updateDirector(Director director) {
        filmDirectorDAO.getDirectorById(director.getId())
                .orElseThrow(() -> new NotFoundException("Режиссер с id =" + director.getId() + " не найден"));
        return filmDirectorDAO.updateDirector(director);
    }

    public List<Director> getAllDirectors() {
        return filmDirectorDAO.getAllDirectors();
    }

    public Director getDirectorById(Long directorId) {
        return filmDirectorDAO.getDirectorById(directorId)
                .orElseThrow(() -> new NotFoundException("Режиссер с id =" + directorId + " не найден"));
    }

    public void removeDirector(Long id) {
        if (!filmDirectorDAO.removeDirector(id)) {
            throw new NotFoundException("Режиссер с id =" + id + " не найден");
        }
    }
}