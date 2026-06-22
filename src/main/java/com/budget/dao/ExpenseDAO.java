package com.budget.dao;

import com.budget.model.Expense;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ExpenseDAO {
    private final Connection conn;

    public ExpenseDAO() throws SQLException {
        conn = DatabaseManager.getInstance().getConnection();
    }

    public Expense insert(int categoryId, double amount, String description, LocalDate date) throws SQLException {
        String sql = "INSERT INTO expenses (category_id, amount, description, expense_date) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, categoryId);
            ps.setDouble(2, amount);
            ps.setString(3, description);
            ps.setString(4, date.toString());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    Expense e = new Expense(categoryId, amount, description, date);
                    e.setId(keys.getInt(1));
                    return e;
                }
            }
        }
        throw new SQLException("Insert failed");
    }

    public void delete(int id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM expenses WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public List<Expense> findByMonth(int year, int month) throws SQLException {
        String sql = """
            SELECT e.id, e.category_id, c.name, e.amount, e.description, e.expense_date
            FROM expenses e
            JOIN categories c ON c.id = e.category_id
            WHERE strftime('%Y', e.expense_date) = printf('%04d', ?)
              AND strftime('%m', e.expense_date) = printf('%02d', ?)
            ORDER BY e.expense_date DESC
        """;
        List<Expense> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, year);
            ps.setInt(2, month);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Expense e = new Expense(
                        rs.getInt("category_id"),
                        rs.getDouble("amount"),
                        rs.getString("description"),
                        LocalDate.parse(rs.getString("expense_date"))
                    );
                    e.setId(rs.getInt("id"));
                    e.setCategoryName(rs.getString("name"));
                    list.add(e);
                }
            }
        }
        return list;
    }
}
