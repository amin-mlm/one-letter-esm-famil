package com.example.oneletter.controllers;

import com.example.oneletter.Client;
import io.github.palexdev.materialfx.controls.legacy.MFXLegacyListCell;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

import java.io.IOException;

public class ClientCellController extends MFXLegacyListCell<Client> {
    @FXML
    private Label nameLabel;

    @FXML
    private Label scoreLabel;

    @FXML
    private AnchorPane rootPane;

    @FXML
    private ImageView userImage;

    @FXML
    private ImageView rank1Image;

    @FXML
    private ImageView rank2Image;

    @FXML
    private ImageView rank3Image;

    FXMLLoader fxmlLoader;

    @Override
    protected void updateItem(Client item, boolean empty) {
        super.updateItem(item, empty);
        if(empty || item==null){
            setText(null);
            setGraphic(null);
        }else{
            if (fxmlLoader == null ) {
                fxmlLoader = new FXMLLoader(getClass()
                        .getResource("/com/example/oneletter/clientCell.fxml"));
                fxmlLoader.setController(this);
                try {
                    fxmlLoader.load();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            switch (item.getRank()) {
                case 1 -> {
                    rank1Image.setVisible(true);
                }
                case 2 -> {
                    rank2Image.setVisible(true);
                }
                case 3 -> {
                    rank3Image.setVisible(true);
                }
                default -> {
                }
            }


            nameLabel.setText(item.getName());
            scoreLabel.setText(item.getFinalScore()+"");
            if(GameScreenController.client.getName().equals(item.getName())){
                userImage.setVisible(true);
            }


            setText(null);
            setGraphic(rootPane);
        }
    }
}

