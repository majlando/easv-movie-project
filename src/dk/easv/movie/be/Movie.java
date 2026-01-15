package dk.easv.movie.be;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Business Entity representing a movie in the collection.
 * Contains movie information including ratings, file location, and categories.
 */
public class Movie {

    private int id;
    private String name;
    private double imdbRating;
    private Double personalRating; // Nullable - might not have personal rating
    private String fileLink;
    private LocalDateTime lastView;
    private List<Category> categories;

    /**
     * Creates a new Movie with default values
     */
    public Movie() {
        this.id = 0;
        this.name = "";
        this.imdbRating = 0.0;
        this.personalRating = null;
        this.fileLink = "";
        this.lastView = null;
        this.categories = new ArrayList<>();
    }

    /**
     * Creates a new Movie with basic information
     * @param name Movie name
     * @param imdbRating IMDB rating (0-10)
     * @param fileLink Path to the movie file
     */
    public Movie(String name, double imdbRating, String fileLink) {
        this.id = 0;
        this.name = name;
        this.imdbRating = imdbRating;
        this.personalRating = null;
        this.fileLink = fileLink;
        this.lastView = null;
        this.categories = new ArrayList<>();
    }

    /**
     * Creates a new Movie with all fields
     */
    public Movie(int id, String name, double imdbRating, Double personalRating,
                 String fileLink, LocalDateTime lastView) {
        this.id = id;
        this.name = name;
        this.imdbRating = imdbRating;
        this.personalRating = personalRating;
        this.fileLink = fileLink;
        this.lastView = lastView;
        this.categories = new ArrayList<>();
    }

    // Getters and Setters

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getImdbRating() {
        return imdbRating;
    }

    public void setImdbRating(double imdbRating) {
        this.imdbRating = imdbRating;
    }

    public Double getPersonalRating() {
        return personalRating;
    }

    public void setPersonalRating(Double personalRating) {
        this.personalRating = personalRating;
    }

    public String getFileLink() {
        return fileLink;
    }

    public void setFileLink(String fileLink) {
        this.fileLink = fileLink;
    }

    public LocalDateTime getLastView() {
        return lastView;
    }

    public void setLastView(LocalDateTime lastView) {
        this.lastView = lastView;
    }

    public List<Category> getCategories() {
        return categories;
    }

    public void setCategories(List<Category> categories) {
        this.categories = categories;
    }

    // Helper methods

    /**
     * Gets the personal rating formatted for display
     * @return Formatted personal rating or "Not rated"
     */
    public String getPersonalRatingDisplay() {
        if (personalRating == null) {
            return "Not rated";
        }
        return String.format("%.1f", personalRating);
    }

    /**
     * Gets all category names as a comma-separated string
     * @return Categories string
     */
    public String getCategoriesString() {
        if (categories == null || categories.isEmpty()) {
            return "No categories";
        }
        return categories.stream()
                .map(Category::getName)
                .collect(java.util.stream.Collectors.joining(", "));
    }

    /**
     * Checks if the movie has a specific category by name
     * @param categoryName The category name to check
     * @return true if movie has this category
     */
    public boolean hasCategory(String categoryName) {
        if (categories == null) return false;
        for (Category category : categories) {
            if (category.getName().equalsIgnoreCase(categoryName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Adds a category to this movie
     * @param category The category to add
     */
    public void addCategory(Category category) {
        if (categories == null) {
            categories = new ArrayList<>();
        }
        if (!categories.contains(category)) {
            categories.add(category);
        }
    }

    /**
     * Removes a category from this movie
     * @param category The category to remove
     */
    public void removeCategory(Category category) {
        if (categories != null) {
            categories.remove(category);
        }
    }

    @Override
    public String toString() {
        return name + " (" + String.format("%.1f", imdbRating) + ")";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Movie movie = (Movie) obj;
        return id == movie.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}

