-- Seed Data Script
-- Inserts initial data if tables are empty

-- Seed Categories
IF NOT EXISTS (SELECT * FROM Category)
BEGIN
    INSERT INTO Category (name) VALUES
        ('Action'),
        ('Adventure'),
        ('Animation'),
        ('Biography'),
        ('Comedy'),
        ('Crime'),
        ('Documentary'),
        ('Drama'),
        ('Family'),
        ('Fantasy'),
        ('Film-Noir'),
        ('History'),
        ('Horror'),
        ('Music'),
        ('Musical'),
        ('Mystery'),
        ('Romance'),
        ('Sci-Fi'),
        ('Sport'),
        ('Thriller'),
        ('War'),
        ('Western');
    PRINT 'Categories seeded successfully.';
END

-- Seed sample Movies (matching files in movies folder)
IF NOT EXISTS (SELECT * FROM Movie)
BEGIN
    INSERT INTO Movie (name, imdbRating, personalRating, filelink, lastview) VALUES
        ('The Dark Knight', 9.0, NULL, 'src/resources/movies/The Dark Knight.mp4', NULL),
        ('The Shawshank Redemption', 9.3, NULL, 'src/resources/movies/The Shawshank Redemption.mp4', NULL),
        ('Pulp Fiction', 8.9, NULL, 'src/resources/movies/Pulp Fiction.mp4', NULL),
        ('The Godfather', 9.2, NULL, 'src/resources/movies/The Godfather.mp4', NULL),
        ('The Godfather Part II', 9.0, NULL, 'src/resources/movies/The Godfather Part II.mp4', NULL),
        ('Forrest Gump', 8.8, NULL, 'src/resources/movies/Forrest Gump.mp4', NULL),
        ('Schindler''s List', 9.0, NULL, 'src/resources/movies/Schindler''s List.mp4', NULL),
        ('12 Angry Men', 9.0, NULL, 'src/resources/movies/12 Angry Men.mp4', NULL);
    PRINT 'Movies seeded successfully.';

    -- Assign categories to movies
    -- The Dark Knight: Action, Crime, Drama
    INSERT INTO CatMovie (CategoryId, MovieId)
    SELECT c.id, m.id FROM Category c, Movie m
    WHERE c.name = 'Action' AND m.name = 'The Dark Knight';
    INSERT INTO CatMovie (CategoryId, MovieId)
    SELECT c.id, m.id FROM Category c, Movie m
    WHERE c.name = 'Crime' AND m.name = 'The Dark Knight';
    INSERT INTO CatMovie (CategoryId, MovieId)
    SELECT c.id, m.id FROM Category c, Movie m
    WHERE c.name = 'Drama' AND m.name = 'The Dark Knight';

    -- The Shawshank Redemption: Drama
    INSERT INTO CatMovie (CategoryId, MovieId)
    SELECT c.id, m.id FROM Category c, Movie m
    WHERE c.name = 'Drama' AND m.name = 'The Shawshank Redemption';

    -- Pulp Fiction: Crime, Drama
    INSERT INTO CatMovie (CategoryId, MovieId)
    SELECT c.id, m.id FROM Category c, Movie m
    WHERE c.name = 'Crime' AND m.name = 'Pulp Fiction';
    INSERT INTO CatMovie (CategoryId, MovieId)
    SELECT c.id, m.id FROM Category c, Movie m
    WHERE c.name = 'Drama' AND m.name = 'Pulp Fiction';

    -- The Godfather: Crime, Drama
    INSERT INTO CatMovie (CategoryId, MovieId)
    SELECT c.id, m.id FROM Category c, Movie m
    WHERE c.name = 'Crime' AND m.name = 'The Godfather';
    INSERT INTO CatMovie (CategoryId, MovieId)
    SELECT c.id, m.id FROM Category c, Movie m
    WHERE c.name = 'Drama' AND m.name = 'The Godfather';

    -- The Godfather Part II: Crime, Drama
    INSERT INTO CatMovie (CategoryId, MovieId)
    SELECT c.id, m.id FROM Category c, Movie m
    WHERE c.name = 'Crime' AND m.name = 'The Godfather Part II';
    INSERT INTO CatMovie (CategoryId, MovieId)
    SELECT c.id, m.id FROM Category c, Movie m
    WHERE c.name = 'Drama' AND m.name = 'The Godfather Part II';

    -- Forrest Gump: Drama, Romance
    INSERT INTO CatMovie (CategoryId, MovieId)
    SELECT c.id, m.id FROM Category c, Movie m
    WHERE c.name = 'Drama' AND m.name = 'Forrest Gump';
    INSERT INTO CatMovie (CategoryId, MovieId)
    SELECT c.id, m.id FROM Category c, Movie m
    WHERE c.name = 'Romance' AND m.name = 'Forrest Gump';

    -- Schindler's List: Biography, Drama, History
    INSERT INTO CatMovie (CategoryId, MovieId)
    SELECT c.id, m.id FROM Category c, Movie m
    WHERE c.name = 'Biography' AND m.name = 'Schindler''s List';
    INSERT INTO CatMovie (CategoryId, MovieId)
    SELECT c.id, m.id FROM Category c, Movie m
    WHERE c.name = 'Drama' AND m.name = 'Schindler''s List';
    INSERT INTO CatMovie (CategoryId, MovieId)
    SELECT c.id, m.id FROM Category c, Movie m
    WHERE c.name = 'History' AND m.name = 'Schindler''s List';

    -- 12 Angry Men: Crime, Drama
    INSERT INTO CatMovie (CategoryId, MovieId)
    SELECT c.id, m.id FROM Category c, Movie m
    WHERE c.name = 'Crime' AND m.name = '12 Angry Men';
    INSERT INTO CatMovie (CategoryId, MovieId)
    SELECT c.id, m.id FROM Category c, Movie m
    WHERE c.name = 'Drama' AND m.name = '12 Angry Men';

    PRINT 'Movie categories assigned successfully.';
END

