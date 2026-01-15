package dk.easv.movie.bll;

import dk.easv.movie.be.Category;
import dk.easv.movie.dal.CategoryDAO;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/**
 * Business Logic Layer manager for Category operations.
 * Handles all category-related business logic including validation.
 */
public class CategoryManager {

    private CategoryDAO categoryDAO;

    /**
     * Creates a new CategoryManager
     * @throws IOException If database connection fails
     */
    public CategoryManager() throws IOException {
        this.categoryDAO = new CategoryDAO();
    }

    /**
     * Gets all categories from the database
     * @return List of all categories
     * @throws SQLException If query fails
     */
    public List<Category> getAllCategories() throws SQLException {
        return categoryDAO.getAllCategories();
    }

    /**
     * Gets a category by its ID
     * @param id The category ID
     * @return The category or null if not found
     * @throws SQLException If query fails
     */
    public Category getCategoryById(int id) throws SQLException {
        return categoryDAO.getCategoryById(id);
    }

    /**
     * Creates a new category
     * @param category The category to create
     * @return The created category with generated ID
     * @throws SQLException If insert fails
     * @throws IllegalArgumentException If validation fails
     */
    public Category createCategory(Category category) throws SQLException, IllegalArgumentException {
        validateCategory(category, 0);
        return categoryDAO.createCategory(category);
    }

    /**
     * Creates a new category by name
     * @param name The category name
     * @return The created category with generated ID
     * @throws SQLException If insert fails
     * @throws IllegalArgumentException If validation fails
     */
    public Category createCategory(String name) throws SQLException, IllegalArgumentException {
        Category category = new Category(name);
        return createCategory(category);
    }

    /**
     * Updates an existing category
     * @param category The category to update
     * @throws SQLException If update fails
     * @throws IllegalArgumentException If validation fails
     */
    public void updateCategory(Category category) throws SQLException, IllegalArgumentException {
        validateCategory(category, category.getId());
        categoryDAO.updateCategory(category);
    }

    /**
     * Deletes a category by its ID
     * @param id The category ID to delete
     * @throws SQLException If delete fails
     */
    public void deleteCategory(int id) throws SQLException {
        categoryDAO.deleteCategory(id);
    }

    /**
     * Validates a category object
     * @param category The category to validate
     * @param excludeId ID to exclude when checking for duplicates
     * @throws IllegalArgumentException If validation fails
     */
    private void validateCategory(Category category, int excludeId) throws IllegalArgumentException {
        if (category.getName() == null || category.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Category name cannot be empty");
        }

        try {
            if (categoryDAO.categoryNameExists(category.getName().trim(), excludeId)) {
                throw new IllegalArgumentException("A category with this name already exists");
            }
        } catch (SQLException e) {
            throw new IllegalArgumentException("Unable to validate category: " + e.getMessage());
        }
    }

    /**
     * Checks if a category name already exists
     * @param name The name to check
     * @return true if the name exists
     * @throws SQLException If query fails
     */
    public boolean categoryExists(String name) throws SQLException {
        return categoryDAO.categoryNameExists(name, 0);
    }
}

