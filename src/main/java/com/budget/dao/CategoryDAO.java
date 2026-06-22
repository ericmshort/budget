package com.budget.dao;

import com.budget.model.Category;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CategoryDAO {
    private final Connection conn;

    public CategoryDAO() throws SQLException {
        conn = DatabaseManager.getInstance().getConnection();
    }

    public List<Category> findAll() throws SQLException {
        List<Category> list = new ArrayList<>();
        try (ResultSet rs = conn.createStatement().executeQuery("SELECT id, name FROM categories ORDER BY name")) {
            while (rs.next()) {
                list.add(new Category(rs.getInt("id"), rs.getString("name")));
            }
        }
        return list;
    }

    public Category insert(String name) throws SQLException {
        String sql = "INSERT INTO categories (name) VALUES (?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name.trim());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return new Category(keys.getInt(1), name.trim());
                }
            }
        }
        throw new SQLException("Insert failed, no key returned");
    }

    public void delete(int id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM categories WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public void rename(int id, String newName) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE categories SET name = ? WHERE id = ?")) {
            ps.setString(1, newName.trim());
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }
}
