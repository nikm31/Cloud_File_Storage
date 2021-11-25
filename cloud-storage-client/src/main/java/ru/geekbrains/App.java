package ru.geekbrains;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent parent = FXMLLoader.load(getClass().getResource("layout.fxml"));
        primaryStage.setTitle("Cloud Storage");
        primaryStage.setResizable(false);
        primaryStage.setMinWidth(780);
        primaryStage.setMinHeight(400);
        primaryStage.setScene(new Scene(parent));
        primaryStage.show();
    }
}
