package dk.easv.movie.dal;

import dk.easv.movie.be.Category;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Category-Movie relationships.
 * Handles the many-to-many relationship between categories and movies.
 */
public class CatMovieDAO {

    private DatabaseConnector dbConnector;

    /**
     * Creates a new CatMovieDAO
     * @throws IOException If database connection fails
     */
    public CatMovieDAO() throws IOException {
        this.dbConnector = new DatabaseConnector();
    }

    /**
     * Gets all categories for a specific movie
     * @param movieId The movie ID
     * @return List of categories for the movie
     * @throws SQLException If query fails
     */
    public List<Category> getCategoriesForMovie(int movieId) throws SQLException {
        List<Category> categories = new ArrayList<>();
        String sql = "SELECT c.id, c.name FROM Category c " +
                     "INNER JOIN CatMovie cm ON c.id = cm.CategoryId " +
                     "WHERE cm.MovieId = ? ORDER BY c.name";

        try (Connection conn = dbConnector.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, movieId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Category category = new Category(
                        rs.getInt("id"),
                        rs.getString("name")
                    );
                    categories.add(category);
                }
            }
        }

        return categories;
    }

    /**
     * Gets all movie IDs for a specific category
     * @param categoryId The category ID
     * @return List of movie IDs in the category
     * @throws SQLException If query fails
     */
    public List<Integer> getMovieIdsForCategory(int categoryId) throws SQLException {
        List<Integer> movieIds = new ArrayList<>();
        String sql = "SELECT MovieId FROM CatMovie WHERE CategoryId = ?";

        try (Connection conn = dbConnector.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, categoryId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    movieIds.add(rs.getInt("MovieId"));
                }
            }
        }

        return movieIds;
    }

    /**
     * Adds a category to a movie.
     * Silently ignores duplicate entries (idempotent operation).
     * @param movieId The movie ID
     * @param categoryId The category ID
     * @throws SQLException If insert fails (except for duplicate key errors)
     */
    public void addCategoryToMovie(int movieId, int categoryId) throws SQLException {
        String sql = "INSERT INTO CatMovie (MovieId, CategoryId) VALUES (?, ?)";

        try (Connection conn = dbConnector.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, movieId);
            pstmt.setInt(2, categoryId);

            try {
                pstmt.executeUpdate();
            } catch (SQLException e) {
                // SQL Server error codes for duplicate key violations:
                // 2627 = Violation of PRIMARY KEY/UNIQUE constraint
                // 2601 = Cannot insert duplicate key row
                int errorCode = e.getErrorCode();
                if (errorCode != 2627 && errorCode != 2601) {
                    throw e;
                }
                // Silently ignore duplicate - relationship already exists
            }
        }
    }

    /**
     * Removes a category from a movie
     * @param movieId The movie ID
     * @param categoryId The category ID
     * @throws SQLException If delete fails
     */
    public void removeCategoryFromMovie(int movieId, int categoryId) throws SQLException {
        String sql = "DELETE FROM CatMovie WHERE MovieId = ? AND CategoryId = ?";

        try (Connection conn = dbConnector.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, movieId);
            pstmt.setInt(2, categoryId);
            pstmt.executeUpdate();
        }
    }

    /**
     * Removes all categories from a movie
     * @param movieId The movie ID
     * @throws SQLException If delete fails
     */
    public void removeCategoriesFromMovie(int movieId) throws SQLException {
        String sql = "DELETE FROM CatMovie WHERE MovieId = ?";

        try (Connection conn = dbConnector.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, movieId);
            pstmt.executeUpdate();
        }
    }

    /**
     * Checks if a movie has a specific category
     * @param movieId The movie ID
     * @param categoryId The category ID
     * @return true if the movie has the category
     * @throws SQLException If query fails
     */
    public boolean movieHasCategory(int movieId, int categoryId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM CatMovie WHERE MovieId = ? AND CategoryId = ?";

        try (Connection conn = dbConnector.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, movieId);
            pstmt.setInt(2, categoryId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }

        return false;
    }
}

