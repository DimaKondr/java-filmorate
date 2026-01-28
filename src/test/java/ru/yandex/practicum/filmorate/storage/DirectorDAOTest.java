package ru.yandex.practicum.filmorate.storage;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.dao.director.DirectorRowMapper;
import ru.yandex.practicum.filmorate.dao.director.FilmDirectorDAO;
import ru.yandex.practicum.filmorate.dao.friendship.FriendshipRowMapper;
import ru.yandex.practicum.filmorate.dao.friendship.UserFriendshipDAO;
import ru.yandex.practicum.filmorate.dao.genre.FilmGenreDAO;
import ru.yandex.practicum.filmorate.dao.genre.GenreRowMapper;
import ru.yandex.practicum.filmorate.dao.like.FilmLikeDAO;
import ru.yandex.practicum.filmorate.dao.rating.FilmAgeRatingDAO;
import ru.yandex.practicum.filmorate.dao.rating.RatingRowMapper;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.film.FilmDbStorage;
import ru.yandex.practicum.filmorate.storage.film.FilmRowMapper;
import ru.yandex.practicum.filmorate.storage.user.UserDbStorage;
import ru.yandex.practicum.filmorate.storage.user.UserRowMapper;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@AutoConfigureTestDatabase
@Import({FilmDirectorDAO.class, DirectorRowMapper.class, FilmDbStorage.class, FilmRowMapper.class, FilmGenreDAO.class,
        GenreRowMapper.class, FilmAgeRatingDAO.class, RatingRowMapper.class, FilmLikeDAO.class,
        UserDbStorage.class, UserRowMapper.class, UserFriendshipDAO.class, FriendshipRowMapper.class})
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class DirectorDAOTest {

    private final FilmDirectorDAO filmDirectorDAO;
    private final FilmDbStorage filmDbStorage;
    private final FilmLikeDAO filmLikeDAO;
    private final UserDbStorage userDbStorage;

    @Test
    void shouldAddDirector() {
        Director director = new Director();
        director.setName("Director");
        Director saved = filmDirectorDAO.addDirector(director);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("Director");
    }

    @Test
    void shouldGetDirectorById() {
        Director director = filmDirectorDAO.addDirector(new Director(null, "Tarantino"));

        Optional<Director> director1 = filmDirectorDAO.getDirectorById(director.getId());

        assertThat(director1).isPresent();
        assertThat(director1.get().getName()).isEqualTo("Tarantino");
    }

    @Test
    void shouldGetAllDirectors() {
        Director director1 = filmDirectorDAO.addDirector(new Director(null, "Nolan"));
        Director director2 = filmDirectorDAO.addDirector(new Director(null, "Tarantino"));

        List<Director> directors = filmDirectorDAO.getAllDirectors();

        assertThat(directors.size()).isEqualTo(2);
        assertThat(directors).extracting(Director::getName).contains("Nolan", "Tarantino");
    }

    @Test
    void shouldUpdateDirector() {
        Director director = filmDirectorDAO.addDirector(new Director(null, "Tarantino"));
        director.setName("Kubrick");

        Director updated = filmDirectorDAO.addDirector(director);

        assertThat(updated.getName()).isEqualTo("Kubrick");
    }

    @Test
    void shouldRemoveDirector() {
        Director director = filmDirectorDAO.addDirector(new Director(null, "Tarantino"));

        boolean deleted = filmDirectorDAO.removeDirector(director.getId());

        assertThat(deleted).isTrue();
        assertThat(filmDirectorDAO.getDirectorById(director.getId())).isEmpty();
    }

    @Test
    void shouldFilmsSortedByYear() {
        Director director = filmDirectorDAO.addDirector(new Director(null, "Tarantino"));

        Film film1 = new Film(null, "Name", "Description",
                LocalDate.of(2000, 12, 12), 120L);
        film1.getDirectors().add(director);

        Film film2 = new Film(null, "Name2", "Description2",
                LocalDate.of(2012, 12, 12), 130L);
        film2.getDirectors().add(director);

        filmDbStorage.addFilm(film1);
        filmDbStorage.addFilm(film2);

        List<Film> films = filmDbStorage.getFilmsByDirectorSortedByYear(director.getId());

        assertThat(films).hasSize(2);
        assertThat(films.get(0).getReleaseDate()).isBefore(films.get(1).getReleaseDate());
    }

    @Test
    void shouldReturnFilmsSortedByLikes() {
        Director director = filmDirectorDAO.addDirector(new Director(null, "Tarantino"));

        Film film1 = new Film(null, "Name", "Description",
                LocalDate.of(2000, 12, 12), 120L);
        film1.getDirectors().add(director);

        Film film2 = new Film(null, "Name2", "Description2",
                LocalDate.of(2012, 12, 12), 130L);
        film2.getDirectors().add(director);

        filmDbStorage.addFilm(film1);
        filmDbStorage.addFilm(film2);

        User user1 = userDbStorage.addUser(new User(null, "@email", "login", "name", LocalDate.of(2000, 12, 12)));
        User user2 = userDbStorage.addUser(new User(null, "@email1", "login1", "name1", LocalDate.of(2000, 12, 12)));
        User user3 = userDbStorage.addUser(new User(null, "@email3", "login3", "name3", LocalDate.of(2000, 12, 12)));

        filmLikeDAO.addLikeToFilm(film1, user1.getId());
        filmLikeDAO.addLikeToFilm(film1, user2.getId());
        filmLikeDAO.addLikeToFilm(film2, user3.getId());

        List<Film> films = filmDbStorage.getFilmsByDirectorSortedByLikes(director.getId());

        assertThat(films).hasSize(2);
        assertThat(films.getFirst().getId()).isEqualTo(film1.getId());
    }
}
