package dk.easv.movie.gui;

import dk.easv.movie.be.Category;
import dk.easv.movie.be.Movie;
import dk.easv.movie.bll.CategoryManager;
import dk.easv.movie.bll.MovieManager;
import dk.easv.movie.bll.MovieWarningService;
import dk.easv.movie.dal.DatabaseConnector;
import dk.easv.movie.dal.DatabaseInitializer;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Controller for the main application window.
 * Handles movie list display, filtering, sorting, and user interactions.
 */
public class MainController implements Initializable {

    // FXML injected controls
    @FXML private TextField txtSearch;
    @FXML private ComboBox<Category> cmbGenreFilter;
    @FXML private Spinner<Double> spnMinRating;
    @FXML private ComboBox<String> cmbSortBy;
    @FXML private CheckBox chkAscending;

    @FXML private TableView<Movie> tblMovies;
    @FXML private TableColumn<Movie, String> colTitle;
    @FXML private TableColumn<Movie, String> colCategories;
    @FXML private TableColumn<Movie, Double> colImdb;
    @FXML private TableColumn<Movie, String> colPersonal;
    @FXML private TableColumn<Movie, String> colLastView;

    @FXML private ListView<Category> lstCategories;
    @FXML private Spinner<Double> spnPersonalRating;

    @FXML private Label lblStatus;
    @FXML private Label lblMovieCount;

    // Buttons (for UX enable/disable)
    @FXML private Button btnAddMovie;
    @FXML private Button btnEditMovie;
    @FXML private Button btnDeleteMovie;
    @FXML private Button btnPlayMovie;
    @FXML private Button btnSaveRating;
    @FXML private Button btnAddCategory;
    @FXML private Button btnEditCategory;
    @FXML private Button btnDeleteCategory;
    @FXML private Button btnClearFilters;

    // Business layer managers
    private MovieManager movieManager;
    private CategoryManager categoryManager;
    private MovieWarningService warningService;

    // Observable lists
    private ObservableList<Movie> allMovies;
    private ObservableList<Movie> filteredMovies;
    private ObservableList<Category> categories;

    private PauseTransition statusClearTimer;
    private PauseTransition searchDebounce;

    // Color constants matching CSS palette
    private static final String COLOR_SUCCESS = "#16a34a";
    private static final String COLOR_WARNING = "#f59e0b";
    private static final String COLOR_DANGER = "#dc2626";
    private static final String COLOR_MUTED = "#64748b";

    // Rating thresholds
    private static final double RATING_GOOD = 8.0;
    private static final double RATING_FAIR = 6.0;

    /**
     * Initializes the controller
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            // Initialize database
            initializeDatabase();

            // Initialize managers
            movieManager = new MovieManager();
            categoryManager = new CategoryManager();
            warningService = new MovieWarningService();

            // Setup UI components
            setupTableColumns();
            setupFilters();
            setupSpinners();
            setupUx();

            // Load data
            loadCategories();
            loadMovies();

            // Check for warnings
            checkForWarnings();

            setStatus("Application ready", true);

        } catch (Exception e) {
            showError("Initialization Error", "Failed to initialize application: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Initializes the database by creating the database/tables and seeding data if needed.
     */
    private void initializeDatabase() throws IOException, SQLException {
        DatabaseConnector connector = new DatabaseConnector();
        DatabaseInitializer initializer = new DatabaseInitializer(connector);
        String dbStatus = initializer.initializeWithStatus();
        setStatus(dbStatus, true);
    }

    private void setupUx() {
        // Status auto-clear (keeps UI calm)
        statusClearTimer = new PauseTransition(Duration.seconds(6));
        statusClearTimer.setOnFinished(e -> lblStatus.setText(""));

        // Search debounce (prevents excessive filtering on fast typing)
        searchDebounce = new PauseTransition(Duration.millis(250));
        searchDebounce.setOnFinished(e -> applyFilters());

        // Disable actions until selections exist
        updateMovieActionState(null);
        updateCategoryActionState(null);

        tblMovies.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            updateMovieActionState(newVal);
            // Sync rating spinner with selected movie for convenience
            if (newVal != null) {
                spnPersonalRating.getValueFactory().setValue(newVal.getPersonalRating() != null ? newVal.getPersonalRating() : 5.0);
            }
        });

        lstCategories.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) ->
            updateCategoryActionState(newVal));

        // Keyboard shortcuts
        Platform.runLater(() -> {
            Scene scene = txtSearch.getScene();
            if (scene == null) return;

            scene.getAccelerators().put(new KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN), () -> {
                txtSearch.requestFocus();
                txtSearch.selectAll();
                setStatus("Search focused (Ctrl+F)", true);
            });

            scene.getAccelerators().put(new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN), () -> onAddMovie(null));

            scene.getAccelerators().put(new KeyCodeCombination(KeyCode.ESCAPE), () -> {
                txtSearch.clear();
                txtSearch.getParent().requestFocus();
                applyFilters();
                setStatus("Cleared search", true);
            });
        });

        // Table key handling: Enter to play, Delete to delete
        tblMovies.setOnKeyPressed(ev -> {
            if (ev.getCode() == KeyCode.ENTER) {
                onPlayMovie(null);
                ev.consume();
            } else if (ev.getCode() == KeyCode.DELETE) {
                onDeleteMovie(null);
                ev.consume();
            }
        });

        // Context menus
        tblMovies.setRowFactory(tv -> {
            TableRow<Movie> row = new TableRow<>();
            ContextMenu menu = new ContextMenu();
            MenuItem play = new MenuItem("Play");
            play.setOnAction(e -> { tv.getSelectionModel().select(row.getItem()); onPlayMovie(null); });
            MenuItem edit = new MenuItem("Edit");
            edit.setOnAction(e -> { tv.getSelectionModel().select(row.getItem()); onEditMovie(null); });
            MenuItem del = new MenuItem("Delete");
            del.setOnAction(e -> { tv.getSelectionModel().select(row.getItem()); onDeleteMovie(null); });
            menu.getItems().addAll(play, edit, del);

            row.contextMenuProperty().bind(javafx.beans.binding.Bindings.when(row.emptyProperty())
                .then((ContextMenu) null)
                .otherwise(menu));

            return row;
        });

        ContextMenu catMenu = new ContextMenu();
        MenuItem addCat = new MenuItem("Add");
        addCat.setOnAction(e -> onAddCategory(null));
        MenuItem editCat = new MenuItem("Edit");
        editCat.setOnAction(e -> onEditCategory(null));
        MenuItem delCat = new MenuItem("Delete");
        delCat.setOnAction(e -> onDeleteCategory(null));
        catMenu.getItems().addAll(addCat, editCat, delCat);
        lstCategories.setContextMenu(catMenu);
    }

    private void updateMovieActionState(Movie selected) {
        boolean has = selected != null;
        if (btnPlayMovie != null) btnPlayMovie.setDisable(!has);
        if (btnEditMovie != null) btnEditMovie.setDisable(!has);
        if (btnDeleteMovie != null) btnDeleteMovie.setDisable(!has);
        if (btnSaveRating != null) btnSaveRating.setDisable(!has);
        spnPersonalRating.setDisable(!has);
    }

    private void updateCategoryActionState(Category selected) {
        boolean has = selected != null && selected.getId() != 0; // ignore "All Genres" synthetic
        if (btnEditCategory != null) btnEditCategory.setDisable(!has);
        if (btnDeleteCategory != null) btnDeleteCategory.setDisable(!has);
    }

    /**
     * Sets up the table columns with cell value factories
     */
    private void setupTableColumns() {
        colTitle.setCellValueFactory(new PropertyValueFactory<>("name"));

        colCategories.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getCategoriesString()));

        colImdb.setCellValueFactory(new PropertyValueFactory<>("imdbRating"));
        colImdb.setCellFactory(column -> new TableCell<Movie, Double>() {
            @Override
            protected void updateItem(Double rating, boolean empty) {
                super.updateItem(rating, empty);
                if (empty || rating == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(String.format("%.1f", rating));
                    setStyle("-fx-text-fill: " + getRatingColor(rating) + "; -fx-font-weight: bold;");
                }
            }
        });

        colPersonal.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getPersonalRatingDisplay()));
        colPersonal.setCellFactory(column -> new TableCell<Movie, String>() {
            @Override
            protected void updateItem(String rating, boolean empty) {
                super.updateItem(rating, empty);
                if (empty || rating == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(rating);
                    if (!"Not rated".equals(rating)) {
                        try {
                            double value = Double.parseDouble(rating);
                            setStyle("-fx-text-fill: " + getRatingColor(value) + "; -fx-font-weight: bold;");
                        } catch (NumberFormatException e) {
                            // Fallback for malformed data
                            setStyle("-fx-text-fill: " + COLOR_MUTED + ";");
                        }
                    } else {
                        setStyle("-fx-text-fill: " + COLOR_MUTED + "; -fx-font-style: italic;");
                    }
                }
            }
        });

        colLastView.setCellValueFactory(cellData -> {
            LocalDateTime lastView = cellData.getValue().getLastView();
            String formatted = lastView != null ?
                lastView.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) : "Never";
            return new javafx.beans.property.SimpleStringProperty(formatted);
        });

        // Enable row selection
        tblMovies.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
    }

    /**
     * Sets up filter controls
     */
    private void setupFilters() {
        // Setup sort by combo box
        cmbSortBy.setItems(FXCollections.observableArrayList(
            "Title", "IMDB Rating", "Personal Rating", "Category"
        ));
        cmbSortBy.getSelectionModel().selectFirst();
    }

    /**
     * Sets up spinner controls
     */
    private void setupSpinners() {
        // Min rating spinner (0-10 with 0.5 steps)
        SpinnerValueFactory<Double> ratingFactory =
            new SpinnerValueFactory.DoubleSpinnerValueFactory(0.0, 10.0, 0.0, 0.5);
        spnMinRating.setValueFactory(ratingFactory);
        spnMinRating.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());

        // Personal rating spinner (0-10 with 0.5 steps)
        SpinnerValueFactory<Double> personalFactory =
            new SpinnerValueFactory.DoubleSpinnerValueFactory(0.0, 10.0, 5.0, 0.5);
        spnPersonalRating.setValueFactory(personalFactory);
    }

    /**
     * Loads all categories from the database
     */
    private void loadCategories() {
        try {
            List<Category> categoryList = categoryManager.getAllCategories();
            categories = FXCollections.observableArrayList(categoryList);
            lstCategories.setItems(categories);

            // Setup genre filter with "All Genres" option
            ObservableList<Category> filterCategories = FXCollections.observableArrayList();
            filterCategories.add(new Category(0, "All Genres"));
            filterCategories.addAll(categoryList);
            cmbGenreFilter.setItems(filterCategories);
            cmbGenreFilter.getSelectionModel().selectFirst();

        } catch (SQLException e) {
            showError("Load Error", "Failed to load categories: " + e.getMessage());
        }
    }

    /**
     * Loads all movies from the database
     */
    private void loadMovies() {
        try {
            List<Movie> movieList = movieManager.getAllMovies();
            allMovies = FXCollections.observableArrayList(movieList);
            filteredMovies = FXCollections.observableArrayList(movieList);
            tblMovies.setItems(filteredMovies);
            updateMovieCount();

        } catch (SQLException e) {
            showError("Load Error", "Failed to load movies: " + e.getMessage());
        }
    }

    /**
     * Checks for movie warnings and displays them if any exist
     */
    private void checkForWarnings() {
        try {
            if (warningService.hasWarnings()) {
                String warningMessage = warningService.generateWarningMessage();

                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Movie Cleanup Reminder");
                alert.setHeaderText("Some movies may need to be removed");
                alert.setContentText(warningMessage);
                alert.getDialogPane().setMinHeight(300);
                alert.showAndWait();
            }
        } catch (SQLException e) {
            System.err.println("Warning check failed: " + e.getMessage());
        }
    }

    /**
     * Applies all active filters to the movie list
     */
    private void applyFilters() {
        String titleFilter = txtSearch.getText();
        Category selectedCategory = cmbGenreFilter.getValue();
        Double minRating = spnMinRating.getValue();

        // Convert selected category to list (null for "All Genres")
        List<Category> categoryFilter = null;
        if (selectedCategory != null && selectedCategory.getId() != 0) {
            categoryFilter = List.of(selectedCategory);
        }

        // Apply filters
        List<Movie> filtered = movieManager.filterMovies(
            allMovies, titleFilter, categoryFilter, minRating > 0 ? minRating : null);

        // Apply sorting
        String sortBy = getSortByValue();
        boolean ascending = chkAscending.isSelected();
        filtered = movieManager.sortMovies(filtered, sortBy, ascending);

        // Update display
        filteredMovies.setAll(filtered);
        updateMovieCount();
    }

    /**
     * Gets the sort by value from the combo box
     */
    private String getSortByValue() {
        String selected = cmbSortBy.getValue();
        if (selected == null) return "title";

        switch (selected) {
            case "IMDB Rating": return "imdb";
            case "Personal Rating": return "personal";
            case "Category": return "category";
            default: return "title";
        }
    }

    /**
     * Updates the movie count display
     */
    private void updateMovieCount() {
        lblMovieCount.setText(String.format("Movies: %d / %d", filteredMovies.size(), allMovies.size()));
    }

    /**
     * Returns the appropriate color for a rating value
     */
    private String getRatingColor(double rating) {
        if (rating >= RATING_GOOD) {
            return COLOR_SUCCESS;
        } else if (rating >= RATING_FAIR) {
            return COLOR_WARNING;
        } else {
            return COLOR_DANGER;
        }
    }

    /**
     * Sets the status bar message
     */
    private void setStatus(String message) {
        setStatus(message, false);
    }

    private void setStatus(String message, boolean autoClear) {
        lblStatus.setText(message);
        if (statusClearTimer != null) {
            statusClearTimer.stop();
            if (autoClear) {
                statusClearTimer.playFromStart();
            }
        }
    }

    // Event Handlers

    @FXML
    private void onSearchKeyReleased() {
        // Use debounce to prevent excessive filtering
        searchDebounce.playFromStart();
    }

    @FXML
    private void onFilterChanged(ActionEvent event) {
        applyFilters();
    }

    @FXML
    private void onSortChanged(ActionEvent event) {
        applyFilters();
    }

    @FXML
    private void onClearFilters(ActionEvent event) {
        txtSearch.clear();
        cmbGenreFilter.getSelectionModel().selectFirst();
        spnMinRating.getValueFactory().setValue(0.0);
        applyFilters();
        setStatus("Filters cleared");
    }

    @FXML
    private void onMovieClicked(MouseEvent event) {
        if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
            // Double-click to play movie
            onPlayMovie(null);
        }
    }

    @FXML
    private void onAddMovie(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/AddMovieView.fxml"));
            Parent root = loader.load();

            AddMovieController controller = loader.getController();
            controller.setCategories(categories);
            controller.setMovieManager(movieManager);

            Stage stage = new Stage();
            stage.setTitle("Add New Movie");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();

            if (controller.isSaved()) {
                loadMovies();
                applyFilters();
                setStatus("Movie added successfully");
            }

        } catch (IOException e) {
            showError("Error", "Failed to open add movie dialog: " + e.getMessage());
        }
    }

    @FXML
    private void onEditMovie(ActionEvent event) {
        Movie selectedMovie = tblMovies.getSelectionModel().getSelectedItem();
        if (selectedMovie == null) {
            showWarning("No Selection", "Please select a movie to edit.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/AddMovieView.fxml"));
            Parent root = loader.load();

            AddMovieController controller = loader.getController();
            controller.setCategories(categories);
            controller.setMovieManager(movieManager);
            controller.setMovie(selectedMovie);

            Stage stage = new Stage();
            stage.setTitle("Edit Movie");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();

            if (controller.isSaved()) {
                loadMovies();
                applyFilters();
                setStatus("Movie updated successfully");
            }

        } catch (IOException e) {
            showError("Error", "Failed to open edit movie dialog: " + e.getMessage());
        }
    }

    @FXML
    private void onDeleteMovie(ActionEvent event) {
        Movie selectedMovie = tblMovies.getSelectionModel().getSelectedItem();
        if (selectedMovie == null) {
            showWarning("No Selection", "Please select a movie to delete.");
            return;
        }

        Optional<ButtonType> result = showConfirmation("Delete Movie",
            "Are you sure you want to delete '" + selectedMovie.getName() + "'?");

        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                movieManager.deleteMovie(selectedMovie.getId());
                loadMovies();
                applyFilters();
                setStatus("Movie deleted successfully");
            } catch (SQLException e) {
                showError("Delete Error", "Failed to delete movie: " + e.getMessage());
            }
        }
    }

    @FXML
    private void onPlayMovie(ActionEvent event) {
        Movie selectedMovie = tblMovies.getSelectionModel().getSelectedItem();
        if (selectedMovie == null) {
            showWarning("No Selection", "Please select a movie to play.");
            return;
        }

        try {
            movieManager.playMovie(selectedMovie);
            loadMovies(); // Refresh to show updated last view time
            applyFilters();
            setStatus("Playing: " + selectedMovie.getName());
        } catch (IOException | SQLException e) {
            showError("Play Error", "Failed to play movie: " + e.getMessage());
        }
    }

    @FXML
    private void onSetPersonalRating(ActionEvent event) {
        Movie selectedMovie = tblMovies.getSelectionModel().getSelectedItem();
        if (selectedMovie == null) {
            showWarning("No Selection", "Please select a movie to rate.");
            return;
        }

        try {
            Double rating = spnPersonalRating.getValue();
            movieManager.updatePersonalRating(selectedMovie.getId(), rating);
            loadMovies();
            applyFilters();
            setStatus("Rating updated for: " + selectedMovie.getName());
        } catch (SQLException | IllegalArgumentException e) {
            showError("Rating Error", "Failed to update rating: " + e.getMessage());
        }
    }

    @FXML
    private void onAddCategory(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/AddCategoryView.fxml"));
            Parent root = loader.load();

            AddCategoryController controller = loader.getController();
            controller.setCategoryManager(categoryManager);

            Stage stage = new Stage();
            stage.setTitle("Add New Category");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();

            if (controller.isSaved()) {
                loadCategories();
                setStatus("Category added successfully");
            }

        } catch (IOException e) {
            showError("Error", "Failed to open add category dialog: " + e.getMessage());
        }
    }

    @FXML
    private void onEditCategory(ActionEvent event) {
        Category selectedCategory = lstCategories.getSelectionModel().getSelectedItem();
        if (selectedCategory == null) {
            showWarning("No Selection", "Please select a category to edit.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/AddCategoryView.fxml"));
            Parent root = loader.load();

            AddCategoryController controller = loader.getController();
            controller.setCategoryManager(categoryManager);
            controller.setCategory(selectedCategory);

            Stage stage = new Stage();
            stage.setTitle("Edit Category");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();

            if (controller.isSaved()) {
                loadCategories();
                loadMovies(); // Categories might have changed
                applyFilters();
                setStatus("Category updated successfully");
            }

        } catch (IOException e) {
            showError("Error", "Failed to open edit category dialog: " + e.getMessage());
        }
    }

    @FXML
    private void onDeleteCategory(ActionEvent event) {
        Category selectedCategory = lstCategories.getSelectionModel().getSelectedItem();
        if (selectedCategory == null) {
            showWarning("No Selection", "Please select a category to delete.");
            return;
        }

        Optional<ButtonType> result = showConfirmation("Delete Category",
            "Are you sure you want to delete '" + selectedCategory.getName() + "'?\n" +
            "This will remove the category from all movies.");

        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                categoryManager.deleteCategory(selectedCategory.getId());
                loadCategories();
                loadMovies(); // Refresh movies as categories changed
                applyFilters();
                setStatus("Category deleted successfully");
            } catch (SQLException e) {
                showError("Delete Error", "Failed to delete category: " + e.getMessage());
            }
        }
    }

    // Helper methods for dialogs

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private Optional<ButtonType> showConfirmation(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        return alert.showAndWait();
    }
}

