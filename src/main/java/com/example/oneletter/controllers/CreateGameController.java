package com.example.oneletter.controllers;

import com.example.oneletter.*;
import com.example.oneletter.animaition.Fade;
import com.example.oneletter.animaition.Shaker;
import com.example.oneletter.database.DatabaseHandler;
import io.github.palexdev.materialfx.controls.*;

import java.io.*;
import java.util.ArrayList;

import io.github.palexdev.materialfx.controls.legacy.MFXLegacyTableView;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TableColumn;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;


public class CreateGameController {
    private final int MINIMUM_FIELD_NEEDED = 1;

    @FXML
    private AnchorPane rootPane;

    @FXML
    private MFXCheckbox animalCheck;

    @FXML
    private MFXCheckbox carCheck;

    @FXML
    private MFXCheckbox cityCheck;

    @FXML
    private MFXCheckbox clothsCheck;

    @FXML
    private MFXCheckbox countryCheck;

    @FXML
    private MFXCheckbox firstnameCheck;

    @FXML
    private MFXCheckbox flowerCheck;

    @FXML
    private MFXCheckbox foodCheck;

    @FXML
    private MFXCheckbox fruitCheck;

    @FXML
    private MFXCheckbox inanimateCheck;

    @FXML
    private MFXCheckbox lastnameCheck;

    @FXML
    private MFXTextField hostNameField;

    @FXML
    private MFXTextField gameNameField;

    @FXML
    private MFXTextField passwordField;

    @FXML
    private MFXTextField roundsField;

    @FXML
    private MFXRadioButton stopy;

    @FXML
    private MFXRadioButton timey;

    @FXML
    private MFXSlider timeSlider;

    @FXML
    private MFXButton createGameButton;

    @FXML
    private MFXButton startGameButton;

    @FXML
    private Label errorLabel;

    @FXML
    private MFXLegacyTableView<Client> playerNameTableView = new MFXLegacyTableView<>();

    ArrayList<String> fields = new ArrayList<>();

    ObservableList<Client> playerNameList ;

    public Server server;

    public DatabaseHandler databaseHandler = new DatabaseHandler();

    private boolean isGameCreated = false;

    String hostName;
    String gameName;
    String password;
    String mode;
    int rounds;
    int time;

    @FXML
    void initialize() {
        ToggleGroup gameModeToggle = new ToggleGroup();
        stopy.setToggleGroup(gameModeToggle);
        timey.setToggleGroup(gameModeToggle);


        timey.setOnAction(actionEvent -> {
            timeSlider.setDisable(false);
        });
        stopy.setOnAction(actionEvent -> {
            timeSlider.setDisable(true);
        });

        createGameButton.addEventFilter(ActionEvent.ACTION, actionEvent -> {
            fields.clear();

            errorLabel.setText("");

            //back to default state
            hostNameField.setStyle("-fx-border-color: ");
            timey.setStyle("-fx-border-color: ");
            stopy.setStyle("-fx-border-color: ");
            roundsField.setStyle("-fx-border-color: ");
            gameNameField.setStyle("-fx-border-color: ");


            if(isGameCreated){
                errorLabel.setText("Game Is Already Created");
                new Fade(errorLabel).fadeIn();
                new Shaker(createGameButton).shake();

                actionEvent.consume();
            }

            time = 0;
            if(timey.isSelected()){
                time = (int)timeSlider.getValue() * 60; //time is set in seconds
                if(time==0){
                    errorLabel.setText("Determine Game Time");
                    new Fade(errorLabel).fadeIn();
                    new Shaker(timeSlider).shake();

                    actionEvent.consume();
                }
            }

            if((gameModeToggle.getSelectedToggle())==null){
                errorLabel.setText("Choose Game Mode");
                new Fade(errorLabel).fadeIn();
                timey.setStyle("-fx-border-color: crimson;");
                stopy.setStyle("-fx-border-color: crimson;");

                new Shaker(timey).shake();
                new Shaker(stopy).shake();

                actionEvent.consume();
            }else
                mode = ((RadioButton)gameModeToggle.getSelectedToggle()).getText();
            try{
                rounds = Integer.parseInt(roundsField.getText());
                if(rounds==0){
                    errorLabel.setText("Rounds Cannot Be 0");
                    new Fade(errorLabel).fadeIn();

                    //fxmodify
                    roundsField.setStyle("-fx-border-color: crimson;");
                    new Shaker(roundsField).shake();

                    actionEvent.consume();
                }
            }catch(NumberFormatException e){
                errorLabel.setText("Insert Only A Number(Integer) For Rounds");
                new Fade(errorLabel).fadeIn();
                roundsField.setStyle("-fx-border-color: crimson;");
                new Shaker(roundsField).shake();

                actionEvent.consume();
            }

            password = passwordField.getText();

            gameName = gameNameField.getText();
            if(gameName.equals("") || gameName.charAt(0)==' '){
                errorLabel.setText("Invalid Game Name");
                if(!gameName.equals(""))
                    errorLabel.setText("Invalid Game Name(Shouldn't Start With Space)");
                new Fade(errorLabel).fadeIn();
                gameNameField.setStyle("-fx-border-color: crimson;");
                new Shaker(gameNameField).shake();

                actionEvent.consume();
            }

            hostName = hostNameField.getText();
            if(hostName.equals("") || hostName.charAt(0)==' '){
                errorLabel.setText("Invalid Name");
                if(!hostName.equals(""))
                    errorLabel.setText("Invalid Name(Shouldn't Start With Space)");
                new Fade(errorLabel).fadeIn();
                hostNameField.setStyle("-fx-border-color: crimson;");
                new Shaker(hostNameField).shake();

                actionEvent.consume();
            }

            if (firstnameCheck.isSelected()) fields.add(firstnameCheck.getText());
            if (lastnameCheck.isSelected()) fields.add(lastnameCheck.getText());
            if (cityCheck.isSelected()) fields.add(cityCheck.getText());
            if (countryCheck.isSelected()) fields.add(countryCheck.getText());
            if (clothsCheck.isSelected()) fields.add(clothsCheck.getText());
            if (animalCheck.isSelected()) fields.add(animalCheck.getText());
            if (foodCheck.isSelected()) fields.add(foodCheck.getText());
            if (flowerCheck.isSelected()) fields.add(flowerCheck.getText());
            if (carCheck.isSelected()) fields.add(carCheck.getText());
            if (fruitCheck.isSelected()) fields.add(fruitCheck.getText());
            if (inanimateCheck.isSelected()) fields.add(inanimateCheck.getText());

            if(fields.size()<MINIMUM_FIELD_NEEDED){
                errorLabel.setText("Choose At Least " + MINIMUM_FIELD_NEEDED + " Field(s) To Create The Game");
                new Fade(errorLabel).fadeIn();

                actionEvent.consume();
            }
        });


        createGameButton.setOnAction(actionEvent -> {
            isGameCreated = true;

            firstnameCheck.setDisable(true);
            lastnameCheck.setDisable(true);
            cityCheck.setDisable(true);
            countryCheck.setDisable(true);
            clothsCheck.setDisable(true);
            animalCheck.setDisable(true);
            foodCheck.setDisable(true);
            flowerCheck.setDisable(true);
            carCheck.setDisable(true);
            fruitCheck.setDisable(true);
            inanimateCheck.setDisable(true);

            hostNameField.setDisable(true);
            gameNameField.setDisable(true);
            passwordField.setDisable(true);
            roundsField.setDisable(true);
            stopy.setDisable(true);
            timey.setDisable(true);
            timeSlider.setDisable(true);

            createTableView();

            server = ServerFactory.createServer(fields, hostName, gameName, password, rounds, mode, time);
            server.setCreateGameController(this);
            new Thread( ()-> {
                server.startAcceptingClient();
            }).start();
        });

        startGameButton.addEventFilter(ActionEvent.ACTION, actionEvent -> {
            if(!isGameCreated){
                errorLabel.setText("Create The Game First");
                new Fade(errorLabel).fadeIn();
                new Shaker(startGameButton).shake();

                actionEvent.consume();
                return;
            }
            if(server.getNumPlayers()<1){
                errorLabel.setText("No One Has Been Joined The Game Yet!");
                new Fade(errorLabel).fadeIn();

                new Shaker(startGameButton).shake();

                actionEvent.consume();
            }
        });
        startGameButton.setOnAction(actionEvent -> {
            server.setAcceptingClientEnoughTrue();

            //make a Client object for host to be considered a player
            Client client = new Client(hostNameField.getText());
            new Thread( ()->{
                client.joinToServer(server.getPort());
                client.waitForStart();
            }).start();

            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            new Thread( ()->{
                server.startGame();
            }).start();
        });

        //remove created game from database if the window is closed
        Main.mainStage.setOnCloseRequest(windowEvent -> {
            if(server!=null){
                databaseHandler.removeServer(server.getPort());
                server.closeServerSocket();
                server.closeSockets();
            }
        });

    }

    private void createTableView() {
        playerNameTableView.setVisible(true);
        playerNameTableView.setDisable(false);

        playerNameList = FXCollections.observableArrayList();

        TableColumn<Client, String> nameCol = new TableColumn<>("Player Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setMinWidth(200);
        playerNameTableView.getColumns().add(nameCol);

        playerNameTableView.setItems(playerNameList);
    }


    public void addPlayerToBoard(String playerName) {
        playerNameList.add(new Client(playerName));
    }

    public void gotoGameScreen(Client client){
        Platform.runLater(()->{
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/example/oneletter/gameScreen.fxml"));
            GameScreenController gameScreenController = new GameScreenController();
            try {
                fxmlLoader.setController(gameScreenController);
                gameScreenController.setClient(client);
                Parent root = fxmlLoader.load();
                rootPane.getChildren().setAll(root);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
