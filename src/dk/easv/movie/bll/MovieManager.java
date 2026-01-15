package dk.easv.movie.bll;

import dk.easv.movie.be.Category;
import dk.easv.movie.be.Movie;
import dk.easv.movie.dal.MovieDAO;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Business Logic Layer manager for Movie operations.
 * Handles all movie-related business logic including filtering, sorting, and validation.
 */
public class MovieManager {

    private MovieDAO movieDAO;

    /**
     * Creates a new MovieManager
     * @throws IOException If database connection fails
     */
    public MovieManager() throws IOException {
        this.movieDAO = new MovieDAO();
    }

    /**
     * Gets all movies from the database
     * @return List of all movies
     * @throws SQLException If query fails
     */
    public List<Movie> getAllMovies() throws SQLException {
        return movieDAO.getAllMovies();
    }

    /**
     * Gets a movie by its ID
     * @param id The movie ID
     * @return The movie or null if not found
     * @throws SQLException If query fails
     */
    public Movie getMovieById(int id) throws SQLException {
        return movieDAO.getMovieById(id);
    }

    /**
     * Creates a new movie
     * @param movie The movie to create
     * @return The created movie with generated ID
     * @throws SQLException If insert fails
     * @throws IllegalArgumentException If validation fails
     */
    public Movie createMovie(Movie movie) throws SQLException, IllegalArgumentException {
        validateMovie(movie);
        return movieDAO.createMovie(movie);
    }

    /**
     * Updates an existing movie
     * @param movie The movie to update
     * @throws SQLException If update fails
     * @throws IllegalArgumentException If validation fails
     */
    public void updateMovie(Movie movie) throws SQLException, IllegalArgumentException {
        validateMovie(movie);
        movieDAO.updateMovie(movie);
    }

    /**
     * Updates the personal rating of a movie
     * @param movieId The movie ID
     * @param rating The new personal rating (0-10 or null)
     * @throws SQLException If update fails
     * @throws IllegalArgumentException If rating is invalid
     */
    public void updatePersonalRating(int movieId, Double rating) throws SQLException, IllegalArgumentException {
        if (rating != null && (rating < 0 || rating > 10)) {
            throw new IllegalArgumentException("Personal rating must be between 0 and 10");
        }
        movieDAO.updatePersonalRating(movieId, rating);
    }

    /**
     * Deletes a movie by its ID
     * @param id The movie ID to delete
     * @throws SQLException If delete fails
     */
    public void deleteMovie(int id) throws SQLException {
        movieDAO.deleteMovie(id);
    }

    /**
     * Plays a movie file using the system's default media player
     * Also updates the last view time
     * @param movie The movie to play
     * @throws IOException If the file cannot be opened
     * @throws SQLException If updating last view fails
     */
    public void playMovie(Movie movie) throws IOException, SQLException {
        File file = new File(movie.getFileLink());

        if (!file.exists()) {
            throw new IOException("Movie file not found: " + movie.getFileLink());
        }

        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().open(file);
            // Update last view time
            movieDAO.updateLastView(movie.getId());
            movie.setLastView(LocalDateTime.now());
        } else {
            throw new IOException("Desktop operations not supported on this system");
        }
    }

    /**
     * Filters movies based on multiple criteria
     * @param movies The list of movies to filter
     * @param titleFilter Title or partial title to filter by (can be null/empty)
     * @param categories Categories to filter by (can be null/empty - movie must have at least one)
     * @param minImdbRating Minimum IMDB rating (can be null)
     * @return Filtered list of movies
     */
    public List<Movie> filterMovies(List<Movie> movies, String titleFilter,
                                     List<Category> categories, Double minImdbRating) {
        return movies.stream()
            .filter(movie -> {
                // Title filter
                if (titleFilter != null && !titleFilter.trim().isEmpty()) {
                    if (!movie.getName().toLowerCase().contains(titleFilter.toLowerCase().trim())) {
                        return false;
                    }
                }

                // Category filter - movie must have at least one of the selected categories
                if (categories != null && !categories.isEmpty()) {
                    boolean hasCategory = false;
                    for (Category category : categories) {
                        if (movie.hasCategory(category.getName())) {
                            hasCategory = true;
                            break;
                        }
                    }
                    if (!hasCategory) {
                        return false;
                    }
                }

                // IMDB rating filter
                if (minImdbRating != null && movie.getImdbRating() < minImdbRating) {
                    return false;
                }

                return true;
            })
            .collect(Collectors.toList());
    }

    /**
     * Sorts movies by the specified criteria
     * @param movies The list of movies to sort
     * @param sortBy The sort criteria: "title", "imdb", "personal", "category"
     * @param ascending True for ascending, false for descending
     * @return Sorted list of movies
     */
    public List<Movie> sortMovies(List<Movie> movies, String sortBy, boolean ascending) {
        List<Movie> sortedMovies = new ArrayList<>(movies);

        Comparator<Movie> comparator;

        switch (sortBy.toLowerCase()) {
            case "title":
                comparator = Comparator.comparing(Movie::getName, String.CASE_INSENSITIVE_ORDER);
                break;
            case "imdb":
                comparator = Comparator.comparingDouble(Movie::getImdbRating);
                break;
            case "personal":
                comparator = Comparator.comparing(Movie::getPersonalRating,
                    Comparator.nullsLast(Comparator.naturalOrder()));
                break;
            case "category":
                comparator = Comparator.comparing(Movie::getCategoriesString, String.CASE_INSENSITIVE_ORDER);
                break;
            default:
                comparator = Comparator.comparing(Movie::getName, String.CASE_INSENSITIVE_ORDER);
        }

        if (!ascending) {
            comparator = comparator.reversed();
        }

        sortedMovies.sort(comparator);
        return sortedMovies;
    }

    /**
     * Validates a movie file link
     * @param fileLink The file link to validate
     * @return true if the file is a valid movie file
     */
    public boolean isValidMovieFile(String fileLink) {
        if (fileLink == null || fileLink.trim().isEmpty()) {
            return false;
        }
        String lowerLink = fileLink.toLowerCase();
        return lowerLink.endsWith(".mp4") || lowerLink.endsWith(".mpeg4");
    }

    /**
     * Validates a movie object
     * @param movie The movie to validate
     * @throws IllegalArgumentException If validation fails
     */
    private void validateMovie(Movie movie) throws IllegalArgumentException {
        if (movie.getName() == null || movie.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Movie name cannot be empty");
        }

        if (movie.getFileLink() == null || movie.getFileLink().trim().isEmpty()) {
            throw new IllegalArgumentException("Movie file link cannot be empty");
        }

        if (!isValidMovieFile(movie.getFileLink())) {
            throw new IllegalArgumentException("Only .mp4 and .mpeg4 files are allowed");
        }

        if (movie.getImdbRating() < 0 || movie.getImdbRating() > 10) {
            throw new IllegalArgumentException("IMDB rating must be between 0 and 10");
        }

        if (movie.getPersonalRating() != null &&
            (movie.getPersonalRating() < 0 || movie.getPersonalRating() > 10)) {
            throw new IllegalArgumentException("Personal rating must be between 0 and 10");
        }
    }

    /**
     * Gets movies that need to be reviewed for deletion
     * (personal rating < 6 and not viewed in 2+ years)
     * @return List of movies to consider for deletion
     * @throws SQLException If query fails
     */
    public List<Movie> getMoviesForDeletionWarning() throws SQLException {
        return movieDAO.getMoviesForWarning(6.0, 2);
    }
}

