module com.budget {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires org.xerial.sqlitejdbc;

    opens com.budget to javafx.fxml;
    opens com.budget.ui to javafx.fxml;
    opens com.budget.model to javafx.base;

    exports com.budget;
    exports com.budget.ui;
    exports com.budget.model;
    exports com.budget.dao;
}
