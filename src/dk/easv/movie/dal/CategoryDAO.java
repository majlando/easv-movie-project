package dk.easv.movie.dal;

import dk.easv.movie.be.Category;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Category operations.
 * Handles all CRUD operations for categories in the database.
 */
public class CategoryDAO {

    private DatabaseConnector dbConnector;

    /**
     * Creates a new CategoryDAO
     * @throws IOException If database connection fails
     */
    public CategoryDAO() throws IOException {
        this.dbConnector = new DatabaseConnector();
    }

    /**
     * Gets all categories from the database
     * @return List of all categories
     * @throws SQLException If query fails
     */
    public List<Category> getAllCategories() throws SQLException {
        List<Category> categories = new ArrayList<>();
        String sql = "SELECT id, name FROM Category ORDER BY name";

        try (Connection conn = dbConnector.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Category category = new Category(
                    rs.getInt("id"),
                    rs.getString("name")
                );
                categories.add(category);
            }
        }

        return categories;
    }

    /**
     * Gets a category by its ID
     * @param id The category ID
     * @return The category or null if not found
     * @throws SQLException If query fails
     */
    public Category getCategoryById(int id) throws SQLException {
        String sql = "SELECT id, name FROM Category WHERE id = ?";

        try (Connection conn = dbConnector.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new Category(
                        rs.getInt("id"),
                        rs.getString("name")
                    );
                }
            }
        }

        return null;
    }

    /**
     * Creates a new category in the database
     * @param category The category to create
     * @return The created category with generated ID
     * @throws SQLException If insert fails
     */
    public Category createCategory(Category category) throws SQLException {
        String sql = "INSERT INTO Category (name) VALUES (?)";

        try (Connection conn = dbConnector.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, category.getName());
            pstmt.executeUpdate();

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    category.setId(rs.getInt(1));
                }
            }
        }

        return category;
    }

    /**
     * Updates an existing category
     * @param category The category to update
     * @throws SQLException If update fails
     */
    public void updateCategory(Category category) throws SQLException {
        String sql = "UPDATE Category SET name = ? WHERE id = ?";

        try (Connection conn = dbConnector.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, category.getName());
            pstmt.setInt(2, category.getId());
            pstmt.executeUpdate();
        }
    }

    /**
     * Deletes a category by its ID
     * @param id The category ID to delete
     * @throws SQLException If delete fails
     */
    public void deleteCategory(int id) throws SQLException {
        String sql = "DELETE FROM Category WHERE id = ?";

        try (Connection conn = dbConnector.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        }
    }

    /**
     * Checks if a category name already exists
     * @param name The name to check
     * @param excludeId ID to exclude from check (for updates)
     * @return true if name exists
     * @throws SQLException If query fails
     */
    public boolean categoryNameExists(String name, int excludeId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Category WHERE name = ? AND id != ?";

        try (Connection conn = dbConnector.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, name);
            pstmt.setInt(2, excludeId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }

        return false;
    }
}

