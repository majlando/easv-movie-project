package dk.easv.movie.gui;

import dk.easv.movie.be.Category;
import dk.easv.movie.be.Movie;
import dk.easv.movie.bll.MovieManager;

import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller for the Add/Edit Movie dialog.
 * Handles creating new movies and editing existing ones.
 */
public class AddMovieController implements Initializable {

    @FXML private Label lblTitle;
    @FXML private TextField txtName;
    @FXML private TextField txtFileLink;
    @FXML private Spinner<Double> spnImdbRating;
    @FXML private Spinner<Double> spnPersonalRating;
    @FXML private CheckBox chkNoPersonalRating;
    @FXML private ListView<Category> lstCategories;
    @FXML private Button btnSave;

    private MovieManager movieManager;
    private ObservableList<Category> categories;
    private Movie movie; // null for new movie, populated for edit
    private boolean saved = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupSpinners();
        setupCategoryList();
    }

    /**
     * Sets up the rating spinners
     */
    private void setupSpinners() {
        // IMDB Rating spinner (0-10 with 0.1 steps)
        SpinnerValueFactory<Double> imdbFactory =
            new SpinnerValueFactory.DoubleSpinnerValueFactory(0.0, 10.0, 7.0, 0.1);
        spnImdbRating.setValueFactory(imdbFactory);

        // Personal Rating spinner (0-10 with 0.5 steps)
        SpinnerValueFactory<Double> personalFactory =
            new SpinnerValueFactory.DoubleSpinnerValueFactory(0.0, 10.0, 5.0, 0.5);
        spnPersonalRating.setValueFactory(personalFactory);
        spnPersonalRating.setDisable(true); // Disabled by default (no rating yet)
    }

    /**
     * Sets up the category list for multiple selection
     */
    private void setupCategoryList() {
        lstCategories.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    }

    /**
     * Sets the movie manager
     * @param movieManager The movie manager to use
     */
    public void setMovieManager(MovieManager movieManager) {
        this.movieManager = movieManager;
    }

    /**
     * Sets the available categories
     * @param categories List of all categories
     */
    public void setCategories(ObservableList<Category> categories) {
        this.categories = categories;
        lstCategories.setItems(categories);
    }

    /**
     * Sets the movie to edit (null for new movie)
     * @param movie The movie to edit
     */
    public void setMovie(Movie movie) {
        this.movie = movie;

        if (movie != null) {
            // Edit mode
            lblTitle.setText("Edit Movie");
            btnSave.setText("Update");

            // Populate fields
            txtName.setText(movie.getName());
            txtFileLink.setText(movie.getFileLink());
            spnImdbRating.getValueFactory().setValue(movie.getImdbRating());

            if (movie.getPersonalRating() != null) {
                chkNoPersonalRating.setSelected(false);
                spnPersonalRating.setDisable(false);
                spnPersonalRating.getValueFactory().setValue(movie.getPersonalRating());
            } else {
                chkNoPersonalRating.setSelected(true);
                spnPersonalRating.setDisable(true);
            }

            // Select movie's categories
            lstCategories.getSelectionModel().clearSelection();
            for (Category category : movie.getCategories()) {
                for (int i = 0; i < categories.size(); i++) {
                    if (categories.get(i).getId() == category.getId()) {
                        lstCategories.getSelectionModel().select(i);
                        break;
                    }
                }
            }
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
    private void onNoPersonalRatingChanged(ActionEvent event) {
        spnPersonalRating.setDisable(chkNoPersonalRating.isSelected());
    }

    @FXML
    private void onBrowseFile(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Movie File");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Movie Files", "*.mp4", "*.mpeg4"),
            new FileChooser.ExtensionFilter("All Files", "*.*")
        );

        // Set initial directory to movies folder if it exists
        File moviesDir = new File("src/resources/movies");
        if (moviesDir.exists() && moviesDir.isDirectory()) {
            fileChooser.setInitialDirectory(moviesDir);
        }

        Stage stage = (Stage) txtFileLink.getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile != null) {
            txtFileLink.setText(selectedFile.getAbsolutePath());
        }
    }

    @FXML
    private void onSave(ActionEvent event) {
        // Validate input
        String name = txtName.getText().trim();
        String fileLink = txtFileLink.getText().trim();

        if (name.isEmpty()) {
            showError("Validation Error", "Please enter a movie title.");
            return;
        }

        if (fileLink.isEmpty()) {
            showError("Validation Error", "Please select a movie file.");
            return;
        }

        if (!movieManager.isValidMovieFile(fileLink)) {
            showError("Validation Error", "Only .mp4 and .mpeg4 files are allowed.");
            return;
        }

        try {
            // Create or update movie
            if (movie == null) {
                movie = new Movie();
            }

            movie.setName(name);
            movie.setFileLink(fileLink);
            movie.setImdbRating(spnImdbRating.getValue());

            if (chkNoPersonalRating.isSelected()) {
                movie.setPersonalRating(null);
            } else {
                movie.setPersonalRating(spnPersonalRating.getValue());
            }

            // Get selected categories
            List<Category> selectedCategories = new ArrayList<>(
                lstCategories.getSelectionModel().getSelectedItems());
            movie.setCategories(selectedCategories);

            // Save to database
            if (movie.getId() == 0) {
                movieManager.createMovie(movie);
            } else {
                movieManager.updateMovie(movie);
            }

            saved = true;
            closeDialog();

        } catch (SQLException | IllegalArgumentException e) {
            showError("Save Error", "Failed to save movie: " + e.getMessage());
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

