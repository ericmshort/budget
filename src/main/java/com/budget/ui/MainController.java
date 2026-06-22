package com.budget.ui;

import com.budget.dao.BudgetEntryDAO;
import com.budget.dao.CategoryDAO;
import com.budget.dao.ExpenseDAO;
import com.budget.model.Category;
import com.budget.model.CategorySummary;
import com.budget.model.Expense;
import com.budget.model.MonthSummary;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.converter.DoubleStringConverter;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;

public class MainController implements Initializable {

    // --- Month navigation ---
    @FXML private Label lblCurrentMonth;
    @FXML private Button btnPrevMonth;
    @FXML private Button btnNextMonth;

    // --- Budget tab ---
    @FXML private TableView<CategorySummary> budgetTable;
    @FXML private TableColumn<CategorySummary, String> colCategory;
    @FXML private TableColumn<CategorySummary, Double> colPlanned;
    @FXML private TableColumn<CategorySummary, Double> colSpent;
    @FXML private TableColumn<CategorySummary, Double> colRemaining;
    @FXML private Label lblTotalPlanned;
    @FXML private Label lblTotalSpent;
    @FXML private Label lblTotalRemaining;
    @FXML private Button btnCopyPrevMonth;

    // --- Expense tab ---
    @FXML private ComboBox<Category> cbExpenseCategory;
    @FXML private TextField tfExpenseAmount;
    @FXML private TextField tfExpenseDesc;
    @FXML private DatePicker dpExpenseDate;
    @FXML private TableView<Expense> expenseTable;
    @FXML private TableColumn<Expense, String> colExpCat;
    @FXML private TableColumn<Expense, Double> colExpAmount;
    @FXML private TableColumn<Expense, String> colExpDesc;
    @FXML private TableColumn<Expense, String> colExpDate;

    // --- Categories tab ---
    @FXML private ListView<Category> categoryList;
    @FXML private TextField tfNewCategory;

    // --- Chart tab ---
    @FXML private LineChart<Number, Number> budgetChart;
    @FXML private NumberAxis chartXAxis;
    @FXML private NumberAxis chartYAxis;

    private int currentYear;
    private int currentMonth;

    private CategoryDAO categoryDAO;
    private BudgetEntryDAO budgetEntryDAO;
    private ExpenseDAO expenseDAO;

    private final ObservableList<CategorySummary> summaryList = FXCollections.observableArrayList();
    private final ObservableList<Expense> expenseList = FXCollections.observableArrayList();
    private final ObservableList<Category> categoryObsList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        try {
            categoryDAO = new CategoryDAO();
            budgetEntryDAO = new BudgetEntryDAO();
            expenseDAO = new ExpenseDAO();
        } catch (SQLException e) {
            showError("Database Error", e.getMessage());
            return;
        }

        LocalDate now = LocalDate.now();
        currentYear = now.getYear();
        currentMonth = now.getMonthValue();

        setupBudgetTable();
        setupExpenseTable();
        setupCategoryList();
        setupChart();

        refresh();
    }

    // -------------------------------------------------------------------------
    // Month navigation
    // -------------------------------------------------------------------------

    @FXML
    private void onPrevMonth() {
        if (--currentMonth < 1) { currentMonth = 12; currentYear--; }
        refresh();
    }

    @FXML
    private void onNextMonth() {
        if (++currentMonth > 12) { currentMonth = 1; currentYear++; }
        refresh();
    }

    private void updateMonthLabel() {
        String name = Month.of(currentMonth).getDisplayName(TextStyle.FULL, Locale.getDefault());
        lblCurrentMonth.setText(name + " " + currentYear);
    }

    // -------------------------------------------------------------------------
    // Budget table setup
    // -------------------------------------------------------------------------

    private void setupBudgetTable() {
        colCategory.setCellValueFactory(new PropertyValueFactory<>("categoryName"));
        colPlanned.setCellValueFactory(new PropertyValueFactory<>("planned"));
        colSpent.setCellValueFactory(new PropertyValueFactory<>("spent"));
        colRemaining.setCellValueFactory(new PropertyValueFactory<>("remaining"));

        colPlanned.setCellFactory(tc -> new TextFieldTableCell<>(new DoubleStringConverter()) {
            @Override public void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                if (!empty && v != null) setText(String.format("$%.2f", v));
            }
        });
        colSpent.setCellFactory(tc -> new TextFieldTableCell<>(new DoubleStringConverter()) {
            @Override public void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                if (!empty && v != null) setText(String.format("$%.2f", v));
            }
        });
        colRemaining.setCellFactory(tc -> new TextFieldTableCell<>(new DoubleStringConverter()) {
            @Override public void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                if (!empty && v != null) {
                    setText(String.format("$%.2f", v));
                    setStyle(v < 0 ? "-fx-text-fill: #d32f2f;" : "-fx-text-fill: #388e3c;");
                } else {
                    setStyle("");
                }
            }
        });

        colPlanned.setEditable(true);
        budgetTable.setEditable(true);
        colPlanned.setOnEditCommit(event -> {
            CategorySummary row = event.getRowValue();
            double newVal = event.getNewValue();
            try {
                budgetEntryDAO.upsert(row.getCategoryId(), currentYear, currentMonth, newVal);
                refresh();
            } catch (SQLException e) {
                showError("Save Error", e.getMessage());
            }
        });

        budgetTable.setItems(summaryList);
    }

    // -------------------------------------------------------------------------
    // Expense table setup
    // -------------------------------------------------------------------------

    private void setupExpenseTable() {
        colExpCat.setCellValueFactory(new PropertyValueFactory<>("categoryName"));
        colExpAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));
        colExpDesc.setCellValueFactory(new PropertyValueFactory<>("description"));
        colExpDate.setCellValueFactory(new PropertyValueFactory<>("date"));

        colExpAmount.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : String.format("$%.2f", v));
            }
        });

        expenseTable.setItems(expenseList);
        dpExpenseDate.setValue(LocalDate.now());
        cbExpenseCategory.setItems(categoryObsList);
    }

    // -------------------------------------------------------------------------
    // Category list setup
    // -------------------------------------------------------------------------

    private void setupCategoryList() {
        categoryList.setItems(categoryObsList);
    }

    // -------------------------------------------------------------------------
    // Chart setup
    // -------------------------------------------------------------------------

    private void setupChart() {
        chartXAxis.setLabel("Month");
        chartYAxis.setLabel("Amount ($)");
        // X values are encoded as year*100+month (e.g. 202601 = Jan 2026)
        chartXAxis.setTickLabelFormatter(new NumberAxis.DefaultFormatter(chartXAxis) {
            @Override public String toString(Number object) {
                int val = object.intValue();
                int year = val / 100;
                int month = val % 100;
                if (month < 1 || month > 12 || year < 2000) return "";
                String mon = Month.of(month).getDisplayName(TextStyle.SHORT, Locale.getDefault());
                return mon + " " + year;
            }
        });
        chartXAxis.setTickLabelRotation(-45);
        budgetChart.setTitle("Budget vs Actual by Month");
        budgetChart.setAnimated(false);
    }

    // -------------------------------------------------------------------------
    // Refresh all views
    // -------------------------------------------------------------------------

    private void refresh() {
        updateMonthLabel();
        refreshBudgetTable();
        refreshExpenses();
        refreshCategories();
        refreshChart();
    }

    private void refreshBudgetTable() {
        try {
            List<CategorySummary> data = budgetEntryDAO.getMonthlySummary(currentYear, currentMonth);
            summaryList.setAll(data);

            double totalPlanned = data.stream().mapToDouble(CategorySummary::getPlanned).sum();
            double totalSpent = data.stream().mapToDouble(CategorySummary::getSpent).sum();
            double totalRemaining = totalPlanned - totalSpent;

            lblTotalPlanned.setText(String.format("$%.2f", totalPlanned));
            lblTotalSpent.setText(String.format("$%.2f", totalSpent));
            lblTotalRemaining.setText(String.format("$%.2f", totalRemaining));
            lblTotalRemaining.setStyle(totalRemaining < 0
                ? "-fx-text-fill: #d32f2f; -fx-font-weight: bold;"
                : "-fx-text-fill: #388e3c; -fx-font-weight: bold;");

            boolean hasPrev = budgetEntryDAO.findPreviousMonth(currentYear, currentMonth) != null;
            btnCopyPrevMonth.setDisable(!hasPrev);
        } catch (SQLException e) {
            showError("Load Error", e.getMessage());
        }
    }

    private void refreshExpenses() {
        try {
            List<Expense> data = expenseDAO.findByMonth(currentYear, currentMonth);
            expenseList.setAll(data);
        } catch (SQLException e) {
            showError("Load Error", e.getMessage());
        }
    }

    private void refreshCategories() {
        try {
            List<Category> cats = categoryDAO.findAll();
            categoryObsList.setAll(cats);
        } catch (SQLException e) {
            showError("Load Error", e.getMessage());
        }
    }

    private void refreshChart() {
        try {
            List<MonthSummary> totals = budgetEntryDAO.getMonthlyTotals();
            XYChart.Series<Number, Number> plannedSeries = new XYChart.Series<>();
            plannedSeries.setName("Planned");
            XYChart.Series<Number, Number> actualSeries = new XYChart.Series<>();
            actualSeries.setName("Actual");

            for (MonthSummary ms : totals) {
                double xVal = ms.getYear() * 100 + ms.getMonth();
                plannedSeries.getData().add(new XYChart.Data<>(xVal, ms.getTotalPlanned()));
                actualSeries.getData().add(new XYChart.Data<>(xVal, ms.getTotalSpent()));
            }

            budgetChart.getData().clear();
            budgetChart.getData().addAll(plannedSeries, actualSeries);
        } catch (SQLException e) {
            showError("Chart Error", e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Action handlers — Budget tab
    // -------------------------------------------------------------------------

    @FXML
    private void onCopyPrevMonth() {
        try {
            int[] prev = budgetEntryDAO.findPreviousMonth(currentYear, currentMonth);
            if (prev == null) return;
            String monthName = Month.of(prev[1]).getDisplayName(TextStyle.FULL, Locale.getDefault());
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Copy budget from " + monthName + " " + prev[0] + "?",
                ButtonType.YES, ButtonType.NO);
            confirm.setTitle("Copy Previous Month");
            confirm.setHeaderText(null);
            if (confirm.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
                budgetEntryDAO.copyFromPreviousMonth(prev[0], prev[1], currentYear, currentMonth);
                refresh();
            }
        } catch (SQLException e) {
            showError("Copy Error", e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Action handlers — Expense tab
    // -------------------------------------------------------------------------

    @FXML
    private void onAddExpense() {
        Category cat = cbExpenseCategory.getValue();
        if (cat == null) { showWarning("Select a category."); return; }
        String amtText = tfExpenseAmount.getText().trim();
        if (amtText.isEmpty()) { showWarning("Enter an amount."); return; }
        double amount;
        try {
            amount = Double.parseDouble(amtText.replace(",", "").replace("$", ""));
            if (amount <= 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            showWarning("Enter a valid positive amount.");
            return;
        }
        LocalDate date = dpExpenseDate.getValue();
        if (date == null) date = LocalDate.now();
        String desc = tfExpenseDesc.getText().trim();

        try {
            expenseDAO.insert(cat.getId(), amount, desc, date);
            tfExpenseAmount.clear();
            tfExpenseDesc.clear();
            dpExpenseDate.setValue(LocalDate.now());
            refresh();
        } catch (SQLException e) {
            showError("Save Error", e.getMessage());
        }
    }

    @FXML
    private void onDeleteExpense() {
        Expense selected = expenseTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showWarning("Select an expense to delete."); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Delete this expense?", ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        if (confirm.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            try {
                expenseDAO.delete(selected.getId());
                refresh();
            } catch (SQLException e) {
                showError("Delete Error", e.getMessage());
            }
        }
    }

    // -------------------------------------------------------------------------
    // Action handlers — Categories tab
    // -------------------------------------------------------------------------

    @FXML
    private void onAddCategory() {
        String name = tfNewCategory.getText().trim();
        if (name.isEmpty()) return;
        try {
            categoryDAO.insert(name);
            tfNewCategory.clear();
            refreshCategories();
        } catch (SQLException e) {
            showError("Save Error", e.getMessage());
        }
    }

    @FXML
    private void onDeleteCategory() {
        Category selected = categoryList.getSelectionModel().getSelectedItem();
        if (selected == null) { showWarning("Select a category to delete."); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Delete '" + selected.getName() + "'? All related budget entries and expenses will also be removed.",
            ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        if (confirm.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            try {
                categoryDAO.delete(selected.getId());
                refresh();
            } catch (SQLException e) {
                showError("Delete Error", e.getMessage());
            }
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void showError(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.setTitle(title);
        a.setHeaderText(null);
        a.showAndWait();
    }

    private void showWarning(String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.showAndWait();
    }
}
