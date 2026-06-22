package com.budget.dao;

import com.budget.model.BudgetEntry;
import com.budget.model.CategorySummary;
import com.budget.model.MonthSummary;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BudgetEntryDAO {
    private final Connection conn;

    public BudgetEntryDAO() throws SQLException {
        conn = DatabaseManager.getInstance().getConnection();
    }

    public void upsert(int categoryId, int year, int month, double amount) throws SQLException {
        String sql = """
            INSERT INTO budget_entries (category_id, year, month, planned_amount)
            VALUES (?, ?, ?, ?)
            ON CONFLICT(category_id, year, month) DO UPDATE SET planned_amount = excluded.planned_amount
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, categoryId);
            ps.setInt(2, year);
            ps.setInt(3, month);
            ps.setDouble(4, amount);
            ps.executeUpdate();
        }
    }

    public List<BudgetEntry> findByMonth(int year, int month) throws SQLException {
        String sql = """
            SELECT be.id, be.category_id, c.name, be.year, be.month, be.planned_amount
            FROM budget_entries be
            JOIN categories c ON c.id = be.category_id
            WHERE be.year = ? AND be.month = ?
            ORDER BY c.name
        """;
        List<BudgetEntry> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, year);
            ps.setInt(2, month);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    BudgetEntry e = new BudgetEntry();
                    e.setId(rs.getInt("id"));
                    e.setCategoryId(rs.getInt("category_id"));
                    e.setCategoryName(rs.getString("name"));
                    e.setYear(rs.getInt("year"));
                    e.setMonth(rs.getInt("month"));
                    e.setPlannedAmount(rs.getDouble("planned_amount"));
                    list.add(e);
                }
            }
        }
        return list;
    }

    /** Returns per-category summary (planned vs spent) for a given month. */
    public List<CategorySummary> getMonthlySummary(int year, int month) throws SQLException {
        String sql = """
            SELECT c.id, c.name,
                   COALESCE(be.planned_amount, 0) AS planned,
                   COALESCE(SUM(e.amount), 0) AS spent
            FROM categories c
            LEFT JOIN budget_entries be ON be.category_id = c.id AND be.year = ? AND be.month = ?
            LEFT JOIN expenses e ON e.category_id = c.id
                AND strftime('%Y', e.expense_date) = printf('%04d', ?)
                AND strftime('%m', e.expense_date) = printf('%02d', ?)
            GROUP BY c.id, c.name, be.planned_amount
            ORDER BY c.name
        """;
        List<CategorySummary> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, year);
            ps.setInt(2, month);
            ps.setInt(3, year);
            ps.setInt(4, month);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new CategorySummary(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getDouble("planned"),
                        rs.getDouble("spent")
                    ));
                }
            }
        }
        return list;
    }

    /** Returns month-level totals for the chart (all months that have data). */
    public List<MonthSummary> getMonthlyTotals() throws SQLException {
        String sql = """
            SELECT y, m,
                   SUM(planned) AS total_planned,
                   SUM(spent)   AS total_spent
            FROM (
                SELECT be.year AS y, be.month AS m,
                       COALESCE(be.planned_amount, 0) AS planned,
                       0 AS spent
                FROM budget_entries be
                UNION ALL
                SELECT CAST(strftime('%Y', e.expense_date) AS INTEGER) AS y,
                       CAST(strftime('%m', e.expense_date) AS INTEGER) AS m,
                       0 AS planned,
                       e.amount AS spent
                FROM expenses e
            )
            GROUP BY y, m
            ORDER BY y, m
        """;
        List<MonthSummary> list = new ArrayList<>();
        try (ResultSet rs = conn.createStatement().executeQuery(sql)) {
            while (rs.next()) {
                list.add(new MonthSummary(
                    rs.getInt("y"),
                    rs.getInt("m"),
                    rs.getDouble("total_planned"),
                    rs.getDouble("total_spent")
                ));
            }
        }
        return list;
    }

    /** Copy all budget entries from one month into another (for auto-fill). */
    public void copyFromPreviousMonth(int fromYear, int fromMonth, int toYear, int toMonth) throws SQLException {
        String sql = """
            INSERT OR IGNORE INTO budget_entries (category_id, year, month, planned_amount)
            SELECT category_id, ?, ?, planned_amount
            FROM budget_entries
            WHERE year = ? AND month = ?
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, toYear);
            ps.setInt(2, toMonth);
            ps.setInt(3, fromYear);
            ps.setInt(4, fromMonth);
            ps.executeUpdate();
        }
    }

    /** Find the most recent month that has budget entries, before the given month. */
    public int[] findPreviousMonth(int year, int month) throws SQLException {
        String sql = """
            SELECT year, month FROM budget_entries
            WHERE (year < ?) OR (year = ? AND month < ?)
            ORDER BY year DESC, month DESC
            LIMIT 1
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, year);
            ps.setInt(2, year);
            ps.setInt(3, month);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new int[]{rs.getInt("year"), rs.getInt("month")};
                }
            }
        }
        return null;
    }
}
