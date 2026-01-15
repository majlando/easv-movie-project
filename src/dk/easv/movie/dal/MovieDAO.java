package dk.easv.movie.dal;

import dk.easv.movie.be.Category;
import dk.easv.movie.be.Movie;

import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Data Access Object for Movie operations.
 * Handles all CRUD operations for movies in the database.
 */
public class MovieDAO {

    private DatabaseConnector dbConnector;
    private CatMovieDAO catMovieDAO;

    /**
     * Creates a new MovieDAO
     * @throws IOException If database connection fails
     */
    public MovieDAO() throws IOException {
        this.dbConnector = new DatabaseConnector();
        this.catMovieDAO = new CatMovieDAO();
    }

    /**
     * Gets all movies from the database with their categories.
     * Uses batch loading to avoid N+1 query problem.
     * @return List of all movies
     * @throws SQLException If query fails
     */
    public List<Movie> getAllMovies() throws SQLException {
        List<Movie> movies = new ArrayList<>();
        String sql = "SELECT id, name, imdbRating, personalRating, filelink, lastview FROM Movie ORDER BY name";

        try (Connection conn = dbConnector.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                movies.add(extractMovieFromResultSet(rs));
            }
        }

        // Batch load all categories in a single query
        if (!movies.isEmpty()) {
            loadCategoriesForMovies(movies);
        }

        return movies;
    }

    /**
     * Batch loads categories for a list of movies in a single query.
     * This avoids the N+1 query problem.
     */
    private void loadCategoriesForMovies(List<Movie> movies) throws SQLException {
        if (movies.isEmpty()) return;

        // Create a map for quick lookup
        Map<Integer, Movie> movieMap = new HashMap<>();
        for (Movie movie : movies) {
            movie.setCategories(new ArrayList<>());
            movieMap.put(movie.getId(), movie);
        }

        // Build the IN clause
        String ids = movies.stream()
            .map(m -> String.valueOf(m.getId()))
            .collect(Collectors.joining(","));

        String sql = "SELECT cm.MovieId, c.id, c.name " +
                     "FROM CatMovie cm " +
                     "INNER JOIN Category c ON cm.CategoryId = c.id " +
                     "WHERE cm.MovieId IN (" + ids + ") " +
                     "ORDER BY cm.MovieId, c.name";

        try (Connection conn = dbConnector.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                int movieId = rs.getInt("MovieId");
                Movie movie = movieMap.get(movieId);
                if (movie != null) {
                    Category category = new Category(rs.getInt("id"), rs.getString("name"));
                    movie.addCategory(category);
                }
            }
        }
    }

    /**
     * Gets a movie by its ID
     * @param id The movie ID
     * @return The movie or null if not found
     * @throws SQLException If query fails
     */
    public Movie getMovieById(int id) throws SQLException {
        String sql = "SELECT id, name, imdbRating, personalRating, filelink, lastview FROM Movie WHERE id = ?";

        try (Connection conn = dbConnector.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Movie movie = extractMovieFromResultSet(rs);
                    movie.setCategories(catMovieDAO.getCategoriesForMovie(movie.getId()));
                    return movie;
                }
            }
        }

        return null;
    }

    /**
     * Creates a new movie in the database
     * @param movie The movie to create
     * @return The created movie with generated ID
     * @throws SQLException If insert fails
     */
    public Movie createMovie(Movie movie) throws SQLException {
        String sql = "INSERT INTO Movie (name, imdbRating, personalRating, filelink, lastview) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = dbConnector.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, movie.getName());
            pstmt.setDouble(2, movie.getImdbRating());

            if (movie.getPersonalRating() != null) {
                pstmt.setDouble(3, movie.getPersonalRating());
            } else {
                pstmt.setNull(3, Types.DECIMAL);
            }

            pstmt.setString(4, movie.getFileLink());

            if (movie.getLastView() != null) {
                pstmt.setTimestamp(5, Timestamp.valueOf(movie.getLastView()));
            } else {
                pstmt.setNull(5, Types.TIMESTAMP);
            }

            pstmt.executeUpdate();

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    movie.setId(rs.getInt(1));
                }
            }
        }

        // Save categories
        if (movie.getCategories() != null && !movie.getCategories().isEmpty()) {
            for (Category category : movie.getCategories()) {
                catMovieDAO.addCategoryToMovie(movie.getId(), category.getId());
            }
        }

        return movie;
    }

    /**
     * Updates an existing movie
     * @param movie The movie to update
     * @throws SQLException If update fails
     */
    public void updateMovie(Movie movie) throws SQLException {
        String sql = "UPDATE Movie SET name = ?, imdbRating = ?, personalRating = ?, filelink = ?, lastview = ? WHERE id = ?";

        try (Connection conn = dbConnector.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, movie.getName());
            pstmt.setDouble(2, movie.getImdbRating());

            if (movie.getPersonalRating() != null) {
                pstmt.setDouble(3, movie.getPersonalRating());
            } else {
                pstmt.setNull(3, Types.DECIMAL);
            }

            pstmt.setString(4, movie.getFileLink());

            if (movie.getLastView() != null) {
                pstmt.setTimestamp(5, Timestamp.valueOf(movie.getLastView()));
            } else {
                pstmt.setNull(5, Types.TIMESTAMP);
            }

            pstmt.setInt(6, movie.getId());
            pstmt.executeUpdate();
        }

        // Update categories - remove all and re-add
        catMovieDAO.removeCategoriesFromMovie(movie.getId());
        if (movie.getCategories() != null && !movie.getCategories().isEmpty()) {
            for (Category category : movie.getCategories()) {
                catMovieDAO.addCategoryToMovie(movie.getId(), category.getId());
            }
        }
    }

    /**
     * Updates the last view time of a movie to now
     * @param movieId The movie ID
     * @throws SQLException If update fails
     */
    public void updateLastView(int movieId) throws SQLException {
        String sql = "UPDATE Movie SET lastview = ? WHERE id = ?";

        try (Connection conn = dbConnector.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            pstmt.setInt(2, movieId);
            pstmt.executeUpdate();
        }
    }

    /**
     * Updates just the personal rating of a movie
     * @param movieId The movie ID
     * @param rating The new personal rating
     * @throws SQLException If update fails
     */
    public void updatePersonalRating(int movieId, Double rating) throws SQLException {
        String sql = "UPDATE Movie SET personalRating = ? WHERE id = ?";

        try (Connection conn = dbConnector.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            if (rating != null) {
                pstmt.setDouble(1, rating);
            } else {
                pstmt.setNull(1, Types.DECIMAL);
            }

            pstmt.setInt(2, movieId);
            pstmt.executeUpdate();
        }
    }

    /**
     * Deletes a movie by its ID
     * @param id The movie ID to delete
     * @throws SQLException If delete fails
     */
    public void deleteMovie(int id) throws SQLException {
        // CatMovie entries will be deleted automatically due to CASCADE
        String sql = "DELETE FROM Movie WHERE id = ?";

        try (Connection conn = dbConnector.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        }
    }

    /**
     * Gets movies that have personal rating below threshold and haven't been viewed in years.
     * Uses batch loading to avoid N+1 query problem.
     * @param ratingThreshold The minimum rating threshold
     * @param yearsNotViewed Number of years since last view
     * @return List of movies matching criteria
     * @throws SQLException If query fails
     */
    public List<Movie> getMoviesForWarning(double ratingThreshold, int yearsNotViewed) throws SQLException {
        List<Movie> movies = new ArrayList<>();
        String sql = "SELECT id, name, imdbRating, personalRating, filelink, lastview FROM Movie " +
                     "WHERE personalRating IS NOT NULL AND personalRating < ? " +
                     "AND lastview IS NOT NULL AND lastview < DATEADD(year, ?, GETDATE())";

        try (Connection conn = dbConnector.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setDouble(1, ratingThreshold);
            pstmt.setInt(2, -yearsNotViewed);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    movies.add(extractMovieFromResultSet(rs));
                }
            }
        }

        // Batch load categories
        if (!movies.isEmpty()) {
            loadCategoriesForMovies(movies);
        }

        return movies;
    }

    /**
     * Searches movies by name (partial match).
     * Uses batch loading to avoid N+1 query problem.
     * @param searchTerm The search term
     * @return List of matching movies
     * @throws SQLException If query fails
     */
    public List<Movie> searchMoviesByName(String searchTerm) throws SQLException {
        List<Movie> movies = new ArrayList<>();
        String sql = "SELECT id, name, imdbRating, personalRating, filelink, lastview FROM Movie " +
                     "WHERE name LIKE ? ORDER BY name";

        try (Connection conn = dbConnector.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, "%" + searchTerm + "%");

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    movies.add(extractMovieFromResultSet(rs));
                }
            }
        }

        // Batch load categories
        if (!movies.isEmpty()) {
            loadCategoriesForMovies(movies);
        }

        return movies;
    }

    /**
     * Helper method to extract a Movie from a ResultSet
     * @param rs The ResultSet
     * @return The Movie object
     * @throws SQLException If extraction fails
     */
    private Movie extractMovieFromResultSet(ResultSet rs) throws SQLException {
        Movie movie = new Movie();
        movie.setId(rs.getInt("id"));
        movie.setName(rs.getString("name"));
        movie.setImdbRating(rs.getDouble("imdbRating"));

        double personalRating = rs.getDouble("personalRating");
        movie.setPersonalRating(rs.wasNull() ? null : personalRating);

        movie.setFileLink(rs.getString("filelink"));

        Timestamp lastView = rs.getTimestamp("lastview");
        movie.setLastView(lastView != null ? lastView.toLocalDateTime() : null);

        return movie;
    }
}

