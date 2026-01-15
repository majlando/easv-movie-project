package dk.easv.movie.bll;

import dk.easv.movie.be.Movie;
import dk.easv.movie.dal.MovieDAO;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/**
 * Service for generating movie warnings on application startup.
 * Warns the user about movies with low personal ratings that haven't been viewed recently.
 */
public class MovieWarningService {

    private MovieDAO movieDAO;

    // Threshold values for warnings
    private static final double RATING_THRESHOLD = 6.0;
    private static final int YEARS_NOT_VIEWED = 2;

    /**
     * Creates a new MovieWarningService
     * @throws IOException If database connection fails
     */
    public MovieWarningService() throws IOException {
        this.movieDAO = new MovieDAO();
    }

    /**
     * Gets movies that should be considered for deletion
     * Movies with personal rating below 6 and not viewed in more than 2 years
     * @return List of movies to warn about
     * @throws SQLException If query fails
     */
    public List<Movie> getMoviesForWarning() throws SQLException {
        return movieDAO.getMoviesForWarning(RATING_THRESHOLD, YEARS_NOT_VIEWED);
    }

    /**
     * Checks if there are any movies that warrant a warning
     * @return true if there are movies to warn about
     * @throws SQLException If query fails
     */
    public boolean hasWarnings() throws SQLException {
        List<Movie> moviesForWarning = getMoviesForWarning();
        return moviesForWarning != null && !moviesForWarning.isEmpty();
    }

    /**
     * Generates a warning message for the movies that need attention
     * @return The warning message or empty string if no warnings
     * @throws SQLException If query fails
     */
    public String generateWarningMessage() throws SQLException {
        List<Movie> moviesForWarning = getMoviesForWarning();

        if (moviesForWarning == null || moviesForWarning.isEmpty()) {
            return "";
        }

        StringBuilder message = new StringBuilder();
        message.append("The following movies have a personal rating below ")
               .append(String.format("%.1f", RATING_THRESHOLD))
               .append(" and have not been watched in over ")
               .append(YEARS_NOT_VIEWED)
               .append(" years.\n\n");
        message.append("Consider deleting them to free up space:\n\n");

        for (Movie movie : moviesForWarning) {
            message.append("• ").append(movie.getName());
            if (movie.getPersonalRating() != null) {
                message.append(" (Rating: ").append(String.format("%.1f", movie.getPersonalRating())).append(")");
            }
            message.append("\n");
        }

        return message.toString();
    }

    /**
     * Gets the rating threshold used for warnings
     * @return The rating threshold
     */
    public double getRatingThreshold() {
        return RATING_THRESHOLD;
    }

    /**
     * Gets the years not viewed threshold used for warnings
     * @return The years threshold
     */
    public int getYearsNotViewedThreshold() {
        return YEARS_NOT_VIEWED;
    }
}

