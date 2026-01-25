DELETE FROM reviews_likes;
DELETE FROM reviews;
DELETE FROM user_feeds;
DELETE FROM film_directors;
DELETE FROM film_likes;
DELETE FROM age_ratings;
DELETE FROM film_genres;
DELETE FROM friendship;

DELETE FROM directors;
DELETE FROM films;
DELETE FROM users;

-- Сброс автоинкремента для всех таблиц с пользовательскими данными
ALTER TABLE users ALTER COLUMN id RESTART WITH 1;
ALTER TABLE films ALTER COLUMN id RESTART WITH 1;
ALTER TABLE film_likes ALTER COLUMN id RESTART WITH 1;
ALTER TABLE age_ratings ALTER COLUMN id RESTART WITH 1;
ALTER TABLE film_genres ALTER COLUMN id RESTART WITH 1;
ALTER TABLE friendship ALTER COLUMN id RESTART WITH 1;
ALTER TABLE user_feeds ALTER COLUMN event_id RESTART WITH 1;
ALTER TABLE reviews ALTER COLUMN reviewId RESTART WITH 1;
ALTER TABLE reviews_likes ALTER COLUMN id RESTART WITH 1;
ALTER TABLE directors ALTER COLUMN id RESTART WITH 1;

-- Вставка справочных данных (ratings)
INSERT INTO ratings (name) SELECT 'G' WHERE NOT EXISTS (SELECT 1 FROM ratings WHERE name = 'G');
INSERT INTO ratings (name) SELECT 'PG' WHERE NOT EXISTS (SELECT 1 FROM ratings WHERE name = 'PG');
INSERT INTO ratings (name) SELECT 'PG-13' WHERE NOT EXISTS (SELECT 1 FROM ratings WHERE name = 'PG-13');
INSERT INTO ratings (name) SELECT 'R' WHERE NOT EXISTS (SELECT 1 FROM ratings WHERE name = 'R');
INSERT INTO ratings (name) SELECT 'NC-17' WHERE NOT EXISTS (SELECT 1 FROM ratings WHERE name = 'NC-17');

-- Вставка справочных данных (genres)
INSERT INTO genres (name) SELECT 'Комедия' WHERE NOT EXISTS (SELECT 1 FROM genres WHERE name = 'Комедия');
INSERT INTO genres (name) SELECT 'Драма' WHERE NOT EXISTS (SELECT 1 FROM genres WHERE name = 'Драма');
INSERT INTO genres (name) SELECT 'Мультфильм' WHERE NOT EXISTS (SELECT 1 FROM genres WHERE name = 'Мультфильм');
INSERT INTO genres (name) SELECT 'Триллер' WHERE NOT EXISTS (SELECT 1 FROM genres WHERE name = 'Триллер');
INSERT INTO genres (name) SELECT 'Документальный' WHERE NOT EXISTS (SELECT 1 FROM genres WHERE name = 'Документальный');
INSERT INTO genres (name) SELECT 'Боевик' WHERE NOT EXISTS (SELECT 1 FROM genres WHERE name = 'Боевик');