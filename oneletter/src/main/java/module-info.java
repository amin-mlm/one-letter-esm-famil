module com.example.oneletter {
    requires javafx.controls;
    requires javafx.fxml;
    requires MaterialFX;

    requires org.kordamp.bootstrapfx.core;
    requires java.sql;

    opens com.example.oneletter to javafx.fxml;
    exports com.example.oneletter;
    exports com.example.oneletter.controllers;
    opens com.example.oneletter.controllers to javafx.fxml;
    exports com.example.oneletter.database;
    opens com.example.oneletter.database to javafx.fxml;
    exports com.example.oneletter.properties;
    opens com.example.oneletter.properties to javafx.fxml;
}