package dk.easv.movie.be;

/**
 * Business Entity representing a movie category/genre.
 * Categories can be assigned to multiple movies (many-to-many relationship).
 */
public class Category {

    private int id;
    private String name;

    /**
     * Creates a new Category with default values
     */
    public Category() {
        this.id = 0;
        this.name = "";
    }

    /**
     * Creates a new Category with name only (for new categories)
     * @param name The category name
     */
    public Category(String name) {
        this.id = 0;
        this.name = name;
    }

    /**
     * Creates a new Category with all fields
     * @param id The category ID
     * @param name The category name
     */
    public Category(int id, String name) {
        this.id = id;
        this.name = name;
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

    // Override toString for ComboBox display
    @Override
    public String toString() {
        return name;
    }

    // Override equals and hashCode for proper comparison
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Category category = (Category) obj;
        return id == category.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}

