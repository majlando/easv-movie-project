# Private Movie Collection

A JavaFX desktop application for managing a personal movie collection with MS SQL Server backend.

## Features

- **Movie Management** - Add, edit, delete movies with IMDB and personal ratings
- **Category Management** - Create and assign multiple categories per movie
- **Advanced Filtering** - Filter by title, genre, and minimum IMDB rating
- **Sorting** - Sort by title, IMDB rating, personal rating, or category
- **Playback** - Launch movies in system default media player
- **Smart Warnings** - Alerts for low-rated unwatched movies

## Architecture

```
GUI Layer (gui)          → JavaFX Controllers & FXML Views
Business Logic (bll)     → Managers & Services
Data Access Layer (dal)  → DAOs & Database Connection
Business Entities (be)   → Movie, Category POJOs
```

## Prerequisites

- Java 17+ with JavaFX (Liberica Full JDK recommended)
- MS SQL Server access
- IntelliJ IDEA (recommended)

## Setup

1. Clone the repository
2. Open in IntelliJ IDEA
3. Configure database in `src/resources/config.properties`:

```properties
db.server=your-server
db.port=1433
db.database=movie
db.username=your-username
db.password=your-password
db.autoCreate=true
```

4. Run `dk.easv.movie.gui.Main`

The application automatically creates tables and seeds sample data on first run.

## Project Structure

```
src/
├── dk/easv/movie/
│   ├── be/                 # Business Entities
│   │   ├── Category.java
│   │   └── Movie.java
│   ├── bll/                # Business Logic
│   │   ├── CategoryManager.java
│   │   ├── MovieManager.java
│   │   └── MovieWarningService.java
│   ├── dal/                # Data Access
│   │   ├── CategoryDAO.java
│   │   ├── CatMovieDAO.java
│   │   ├── DatabaseConnector.java
│   │   ├── DatabaseInitializer.java
│   │   └── MovieDAO.java
│   └── gui/                # GUI
│       ├── AddCategoryController.java
│       ├── AddMovieController.java
│       ├── Main.java
│       └── MainController.java
└── resources/
    ├── config.properties
    ├── sql/
    │   ├── setup_database.sql
    │   └── seed_data.sql
    └── views/
        ├── AddCategoryView.fxml
        ├── AddMovieView.fxml
        ├── MainView.fxml
        └── styles.css
```

## Database Schema

```sql
Category (id, name)
Movie (id, name, imdbRating, personalRating, filelink, lastview)
CatMovie (id, CategoryId, MovieId)  -- Junction table
```

## Usage

- **Add Movie**: Click "+ Add Movie", fill form, select categories
- **Play Movie**: Double-click or select and click "Play"
- **Rate Movie**: Select movie, adjust rating spinner, click "Save"
- **Filter**: Use search box, genre dropdown, or min rating spinner
- **Sort**: Select criteria from dropdown, toggle ascending/descending