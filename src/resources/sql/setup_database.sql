-- Setup Database Script
-- Creates the database tables if they do not exist

-- Create Category table
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'Category')
BEGIN
    CREATE TABLE Category (
        id INT IDENTITY(1,1) PRIMARY KEY,
        name NVARCHAR(100) NOT NULL UNIQUE
    );
    PRINT 'Category table created successfully.';
END

-- Create Movie table
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'Movie')
BEGIN
    CREATE TABLE Movie (
        id INT IDENTITY(1,1) PRIMARY KEY,
        name NVARCHAR(255) NOT NULL,
        imdbRating DECIMAL(3,1) DEFAULT 0.0,
        personalRating DECIMAL(3,1) DEFAULT NULL,
        filelink NVARCHAR(500) NOT NULL,
        lastview DATETIME DEFAULT NULL
    );
    PRINT 'Movie table created successfully.';
END

-- Create CatMovie junction table for many-to-many relationship
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'CatMovie')
BEGIN
    CREATE TABLE CatMovie (
        id INT IDENTITY(1,1) PRIMARY KEY,
        CategoryId INT NOT NULL,
        MovieId INT NOT NULL,
        CONSTRAINT FK_CatMovie_Category FOREIGN KEY (CategoryId) REFERENCES Category(id) ON DELETE CASCADE,
        CONSTRAINT FK_CatMovie_Movie FOREIGN KEY (MovieId) REFERENCES Movie(id) ON DELETE CASCADE,
        CONSTRAINT UQ_CatMovie UNIQUE (CategoryId, MovieId)
    );
    PRINT 'CatMovie table created successfully.';
END

