package com.budget.model;

public class BudgetEntry {
    private int id;
    private int categoryId;
    private String categoryName;
    private int year;
    private int month;
    private double plannedAmount;

    public BudgetEntry() {}

    public BudgetEntry(int categoryId, int year, int month, double plannedAmount) {
        this.categoryId = categoryId;
        this.year = year;
        this.month = month;
        this.plannedAmount = plannedAmount;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getCategoryId() { return categoryId; }
    public void setCategoryId(int categoryId) { this.categoryId = categoryId; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }
    public int getMonth() { return month; }
    public void setMonth(int month) { this.month = month; }
    public double getPlannedAmount() { return plannedAmount; }
    public void setPlannedAmount(double plannedAmount) { this.plannedAmount = plannedAmount; }
}
