package com.budget.model;

public class MonthSummary {
    private int year;
    private int month;
    private double totalPlanned;
    private double totalSpent;

    public MonthSummary(int year, int month, double totalPlanned, double totalSpent) {
        this.year = year;
        this.month = month;
        this.totalPlanned = totalPlanned;
        this.totalSpent = totalSpent;
    }

    public int getYear() { return year; }
    public int getMonth() { return month; }
    public double getTotalPlanned() { return totalPlanned; }
    public double getTotalSpent() { return totalSpent; }
    public double getDifference() { return totalPlanned - totalSpent; }
}
