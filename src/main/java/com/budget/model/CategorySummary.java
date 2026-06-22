package com.budget.model;

import javafx.beans.property.*;

public class CategorySummary {
    private final StringProperty categoryName = new SimpleStringProperty();
    private final DoubleProperty planned = new SimpleDoubleProperty();
    private final DoubleProperty spent = new SimpleDoubleProperty();
    private final DoubleProperty remaining = new SimpleDoubleProperty();
    private int categoryId;

    public CategorySummary(int categoryId, String categoryName, double planned, double spent) {
        this.categoryId = categoryId;
        this.categoryName.set(categoryName);
        this.planned.set(planned);
        this.spent.set(spent);
        this.remaining.set(planned - spent);
    }

    public int getCategoryId() { return categoryId; }
    public String getCategoryName() { return categoryName.get(); }
    public StringProperty categoryNameProperty() { return categoryName; }
    public double getPlanned() { return planned.get(); }
    public DoubleProperty plannedProperty() { return planned; }
    public double getSpent() { return spent.get(); }
    public DoubleProperty spentProperty() { return spent; }
    public double getRemaining() { return remaining.get(); }
    public DoubleProperty remainingProperty() { return remaining; }
}
