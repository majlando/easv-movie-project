package dk.easv.movie.gui;

import dk.easv.movie.be.Category;
import dk.easv.movie.bll.CategoryManager;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

/**
 * Controller for the Add/Edit Category dialog.
 * Handles creating new categories and editing existing ones.
 */
public class AddCategoryController implements Initializable {

    @FXML private Label lblTitle;
    @FXML private TextField txtName;
    @FXML private Button btnSave;

    private CategoryManager categoryManager;
    private Category category; // null for new category, populated for edit
    private boolean saved = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // No special initialization needed
    }

    /**
     * Sets the category manager
     * @param categoryManager The category manager to use
     */
    public void setCategoryManager(CategoryManager categoryManager) {
        this.categoryManager = categoryManager;
    }

    /**
     * Sets the category to edit (null for new category)
     * @param category The category to edit
     */
    public void setCategory(Category category) {
        this.category = category;

        if (category != null) {
            // Edit mode
            lblTitle.setText("Edit Category");
            btnSave.setText("Update");
            txtName.setText(category.getName());
        }
    }

    /**
     * Returns whether the dialog was saved
     * @return true if saved
     */
    public boolean isSaved() {
        return saved;
    }

    @FXML
    private void onSave(ActionEvent event) {
        // Validate input
        String name = txtName.getText().trim();

        if (name.isEmpty()) {
            showError("Validation Error", "Please enter a category name.");
            return;
        }

        try {
            // Create or update category
            if (category == null) {
                category = new Category(name);
                categoryManager.createCategory(category);
            } else {
                category.setName(name);
                categoryManager.updateCategory(category);
            }

            saved = true;
            closeDialog();

        } catch (SQLException | IllegalArgumentException e) {
            showError("Save Error", "Failed to save category: " + e.getMessage());
        }
    }

    @FXML
    private void onCancel(ActionEvent event) {
        closeDialog();
    }

    /**
     * Closes the dialog window
     */
    private void closeDialog() {
        Stage stage = (Stage) btnSave.getScene().getWindow();
        stage.close();
    }

    /**
     * Shows an error dialog
     */
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

