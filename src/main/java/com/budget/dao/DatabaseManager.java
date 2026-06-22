package com.budget.dao;

import java.io.File;
import java.sql.*;

public class DatabaseManager {
    private static DatabaseManager instance;
    private Connection connection;

    private static final String DB_DIR = System.getProperty("user.home") + File.separator + ".budgetapp";
    private static final String DB_PATH = DB_DIR + File.separator + "budget.db";

    private DatabaseManager() throws SQLException {
        new File(DB_DIR).mkdirs();
        connection = DriverManager.getConnection("jdbc:sqlite:" + DB_PATH);
        connection.createStatement().execute("PRAGMA foreign_keys = ON");
        initSchema();
    }

    public static DatabaseManager getInstance() throws SQLException {
        if (instance == null || instance.connection.isClosed()) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    public Connection getConnection() { return connection; }

    private void initSchema() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS categories (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL UNIQUE
                )
            """);
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS budget_entries (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    category_id INTEGER NOT NULL,
                    year INTEGER NOT NULL,
                    month INTEGER NOT NULL,
                    planned_amount REAL NOT NULL DEFAULT 0,
                    FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE CASCADE,
                    UNIQUE(category_id, year, month)
                )
            """);
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS expenses (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    category_id INTEGER NOT NULL,
                    amount REAL NOT NULL,
                    description TEXT,
                    expense_date TEXT NOT NULL,
                    FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE CASCADE
                )
            """);
        }
    }

    public void close() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }
}
