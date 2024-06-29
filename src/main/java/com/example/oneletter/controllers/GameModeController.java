package com.example.oneletter.controllers;

import com.example.oneletter.animaition.Shaker;
import io.github.palexdev.materialfx.controls.MFXButton;

import java.io.IOException;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.layout.*;

public class GameModeController {

    @FXML
    private Label labelLetter;

    @FXML
    private Label labelOne;

    @FXML
    private MFXButton createButton;

    @FXML
    private MFXButton joinButton;

    @FXML
    private MFXButton helpButton;

    @FXML
    private MFXButton exitButton;

    @FXML
    private AnchorPane rootPane;

    public static FXMLLoader fxmlLoader;

    @FXML
    void initialize() {
        createButton.setOnAction(actionEvent -> {
            fxmlLoader = new FXMLLoader(getClass().getResource("/com/example/oneletter/createGame.fxml"));
            try {
                rootPane.getChildren().setAll((Pane)(fxmlLoader.load()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });



        joinButton.setOnAction(actionEvent -> {
            fxmlLoader = new FXMLLoader(getClass().getResource("/com/example/oneletter/joinGame.fxml"));
            try {
                rootPane.getChildren().setAll((Pane)(fxmlLoader.load()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        exitButton.setOnAction(actionEvent -> {
            Platform.exit();
        });

        new Thread(()->{
            try {
                Thread.sleep(1000); //animation
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            new Shaker(labelOne).transit();

            try {
                Thread.sleep(1000); //animation
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            new Shaker(labelLetter).transit();
        }).start();
    }
}
