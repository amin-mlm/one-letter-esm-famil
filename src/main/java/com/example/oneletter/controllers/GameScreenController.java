package com.example.oneletter.controllers;

import com.example.oneletter.Client;
import com.example.oneletter.animaition.Shaker;
import com.example.oneletter.controllers.ClientCellController;
import io.github.palexdev.materialfx.controls.MFXButton;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

import io.github.palexdev.materialfx.controls.MFXRadioButton;
import io.github.palexdev.materialfx.controls.MFXTextField;
import io.github.palexdev.materialfx.controls.legacy.MFXLegacyListView;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.*;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;


public class GameScreenController {

    @FXML
    private AnchorPane rootPane;

    @FXML
    private FlowPane fieldPane;

    @FXML
    private FlowPane reactionPane;

    @FXML
    private MFXButton finishButton;

    @FXML
    private MFXTextField alphabetField;

    @FXML
    private MFXButton alphabetButton;

    @FXML
    private Label alphabetLabel;

    @FXML
    private Label timeLabel;

    @FXML
    private Label stateLabel;

    @FXML
    private Label roundLabel;

    @FXML
    private MFXLegacyListView<Client> scoreBoardListView;

    @FXML
    private MFXButton newGameButton;

    @FXML
    private MFXButton exitButton;

    static Client client;

    private ArrayList<String> fieldsString = new ArrayList<>();

    private ArrayList<MFXTextField> textFields = new ArrayList<>();

    private ArrayList<VBox> reactionRadioButtons = new ArrayList<>();

    private int rounds;

    private String gameMode;

    private int time;

    private int indexBetweenAllPlayers;

    private String plan;

    private int thisRound = 1;

    private char alphabet;

    private long secondTime;

    private int index;

    private boolean myTurnToDetermineAlphabet;

    private int sumScore = 0;

    private boolean hostLeftGame = false;


    @FXML
    void initialize() {
        prepareFieldPane();
        Platform.runLater(()->{
            roundLabel.setText("Round " + thisRound + "/" + rounds);
        });
        myTurnToDetermineAlphabet = Integer.parseInt(plan.charAt(thisRound - 1) + "") == 1; //because thisRound starts from 1
        if (myTurnToDetermineAlphabet) {   //it can be either 0 or 1
            prepareToDetermineAlphabet();
        } else {
            new Thread(() -> {
                if(listenForAlphabet()==-1) //host left the game
                    return;
                startGame();
            }).start();
        }

        alphabetButton.setOnAction(actionEvent -> {
            new Thread(() -> {
                if(sendAlphabet()==0){ //input alphabet is valid
                    if(listenForAlphabet()==-1) //host left the game
                        return;
                    startGame();
                }
            }).start();
        });

        finishButton.setOnAction(actionEvent -> {
            for (int i = 0; i < textFields.size(); i++) {
                if(textFields.get(i).getText().equals("")){
                    new Shaker(finishButton).shake();

                    new Thread(()->{
                        Platform.runLater(()->{
                            finishButton.setText("F I N I S H !\n(you have some empty fields yet)");
                        });
                        try {
                            Thread.sleep(3000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        Platform.runLater(()->{
                            finishButton.setText("F I N I S H !");
                        });
                    }).start();

                    break;
                }
                if(i==fieldsString.size()-1)
                    client.sendFinishState();
            }
        });

        newGameButton.setOnAction(actionEvent -> {
            closeSocket();
            newGame();
        });

        exitButton.setOnAction(actionEvent -> {
            closeSocket();
            Platform.exit();
        });
    }

    private void prepareToDetermineAlphabet() {
        alphabetField.setDisable(false);
        alphabetField.setVisible(true);
        alphabetButton.setDisable(false);
        alphabetButton.setVisible(true);

        Platform.runLater(()->{
            alphabetLabel.setText("Determine Game Alphabet Below");
        });
    }


    private int sendAlphabet() {
        String alphabetString = alphabetField.getText();
        char alphabetChar;

        if (alphabetString.length() != 1) {
            alphabetField.setStyle("-fx-border-color: crimson");

            Platform.runLater(()->{
                alphabetField.setText("");
                alphabetField.setPromptText("Only 1 letter");
            });
            new Shaker(alphabetField).shake();
            new Shaker(alphabetButton).shake();

            return -1; //code for invalid alphabet error
        }
        else{
            alphabetChar = alphabetString.charAt(0);
        }
        if (!((alphabetChar >= 65 && alphabetChar <= 90) || (alphabetChar >= 97 && alphabetChar <= 122))) {
            alphabetField.setStyle("-fx-border-color: crimson");
            Platform.runLater(()->{
                alphabetField.setText("");
                alphabetField.setPromptText("Between A to Z");
            });
            new Shaker(alphabetField).shake();
            new Shaker(alphabetButton).shake();

            return -1; //code for invalid alphabet error
        } else {
            int result = client.sendAlphabet(alphabetString);
            if (result == 0) {
                alphabetField.setDisable(true);
                alphabetField.setVisible(false);
                alphabetButton.setDisable(true);
                alphabetButton.setVisible(false);
                return 0; //code for everything is ok
            } else if (result == -1) {
                alphabetField.setStyle("-fx-border-color: crimson");
                Platform.runLater(()->{
                    alphabetField.setText("");
                    alphabetField.setPromptText("repeated alphabet");
                });
                new Shaker(alphabetField).shake();
                new Shaker(alphabetButton).shake();

                return -1; //code for invalid alphabet error
            }else if(result == -2){ //host left game
                return -1;
            }
        }
        return 0;
    }

    private void startGame() {
        new Thread(() -> {

            String message = client.listenToSendAnswerMessage();

            if(message==null) //host left the game
                return;
            if (message.equals("Send Your Answers")) {
                Platform.runLater(() -> {
                    finishButton.setVisible(false);
                    finishButton.setDisable(true);
                    for (MFXTextField textField : textFields) {
                        textField.setDisable(true);
                    }
                });

                //sleep here, instead of in server
                //let server start to listen to all clients(222 server)

                //was active
//                try {
//                    Thread.sleep(200); //fewer? how many? // can + 1000 for seeing disability of TextFs?
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }

                for (int i = 0; i < textFields.size(); i++) {
                    //sleep to avoid concurrent modification with other players
                    try {
                        Thread.sleep((long)100 * (indexBetweenAllPlayers + 1));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    String answer = removeSpaceFromAnswer(textFields.get(i).getText());
                    ArrayList<String> othersAnswers = client.sendAnswerAndGetOthersAnswers(answer);
                    if(othersAnswers==null){ //host left the game
                        return;
                    }
                    int point = sendReactionAndGetPoint(othersAnswers, fieldsString.get(i));
                    if(point==-1){ //host left game
                        return;
                    }

                    sumScore += point;

                    String tempAnswer = textFields.get(i).getText();
                    textFields.get(i).setText(tempAnswer + ", " + point);

                    Platform.runLater(()->{
                        alphabetLabel.setText("Here Are Your Points :");
                    });
                    showTime(10); //seeing the points. if time is changed, "sleep in server" before "sending roundScore" in collectAndCheckAnswers() should be updated to (time + 1)
                }

                if (++thisRound <= rounds) {
                    int result = prepareNextRound();
                    if(result==-1) //host left game
                        return;
                    nextRound();
                }else{
                    int result = prepareNextRound();
                    if(result==-1) //host left game
                        return;
                    alphabetLabel.setVisible(false);
                    alphabetLabel.setDisable(true);

                    roundLabel.setVisible(false);
                    roundLabel.setDisable(true);

                    scoreBoardListView.setVisible(true);
                    scoreBoardListView.setDisable(false);

                    client.setFinalScore(sumScore);

                    int lastRank=1;
                    ObservableList<Client> clientsObservableList = FXCollections.observableArrayList();
                    for (int i = 0; i < client.getNumOfAllPlayers(); i++) {
                        String name = client.listenToClientNameForScoreBoard();
                        int finalScore = client.listenToClientScoreForScoreBoard();
                        //calculate rank
                        int rank;
                        if(i==0)
                            rank = 1;
                        else{
                            if(finalScore==clientsObservableList.get(i-1).getFinalScore())
                                rank = lastRank;
                            else
                                rank = ++lastRank;
                        }
                        Client client = new Client(name, finalScore, rank);

                        clientsObservableList.add(client);
                    }
                    scoreBoardListView.setItems(clientsObservableList);
                    scoreBoardListView.setCellFactory(param -> new ClientCellController());

                    newGameButton.setVisible(true);
                    newGameButton.setDisable(false);

                    exitButton.setVisible(true);
                    exitButton.setDisable(false);
                }
            }
        }).start();

        makeTextFieldsEnable();
        if (gameMode.equals("Game Is Finished When The Time Is Over")) {
            showTime(time/*10*/);

            if(myTurnToDetermineAlphabet)
                client.sendFinishState();

        } else if (gameMode.equals("Game Is Finished When A Player Finishes")) {
            finishButton.setVisible(true);
            finishButton.setDisable(false);
        }
    }

    private void newGame() {
        scoreBoardListView.setVisible(false);
        scoreBoardListView.setDisable(true);

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/example/oneletter/gameMode.fxml"));
        try {
            rootPane.getChildren().setAll((Parent)fxmlLoader.load());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void closeSocket() {
        client.closeSocket();
    }

    private String removeSpaceFromAnswer(String primaryAnswer) {
        if(primaryAnswer.equals("")) return primaryAnswer;

        var scanner = new Scanner(primaryAnswer);
        String answer = "";
        while (scanner.hasNext()) {
            answer += scanner.next() + " ";
        }
        return answer.substring(0, answer.length()-1); //remove last space
    }

    private int prepareNextRound() {
        int thisRoundScore = client.listenToRoundScore();
        if(thisRoundScore == -1) //host left game
            return -1;

        stateLabel.setVisible(true);
        stateLabel.setDisable(false);

        Platform.runLater(()->{
            alphabetLabel.setText("");
            stateLabel.setText("You Got  " + thisRoundScore + "  In This Round !" +
                    "\nWait For The Next Round");
        });
        fieldPane.setVisible(false);
        fieldPane.setDisable(true);


        for (int i = 0; i < textFields.size(); i++) {
            textFields.get(i).setText("");
        }
        showTime(10);
        stateLabel.setVisible(false);
        stateLabel.setDisable(true);
        return 0;
    }
    private void nextRound() {
        initialize();
    }

    private void makeTextFieldsEnable() {
        for (int i = 0; i < textFields.size(); i++) {
            textFields.get(i).setDisable(false);
        }
    }
    private int sendReactionAndGetPoint(ArrayList<String> othersAnswers, String category)  {
        fieldPane.setVisible(false);
        fieldPane.setDisable(true);

        reactionPane.setVisible(true);
        reactionPane.setDisable(false);

        Platform.runLater(()->{
            alphabetLabel.setText("Are These Words \"" + category + "\" ?" +
                                  "\nIf You Don't React , \"Yes\" Will Be Automatically Ticked");
        });
        ArrayList<ToggleGroup> toggleGroups = new ArrayList<>();
        ArrayList<RadioButton> radioButtonsYes = new ArrayList<>();
        ArrayList<RadioButton> radioButtonsNo = new ArrayList<>();

        int[] similarAnswers = new int[othersAnswers.size()]; //e.g. othersAnswer[i] is similar to similarAnswers[i] so we won't create radioButton for othersAnswers.get(i)
        Arrays.fill(similarAnswers, -1);

        for (int i = 0; i < othersAnswers.size(); i++) {
            int state = isAnswerSimilarToOthers(othersAnswers.get(i), i, othersAnswers);
            if(othersAnswers.get(i).equals("") || !(othersAnswers.get(i).charAt(0)+"").equalsIgnoreCase(alphabet+"")){
                continue;
            }
            else if(state!=-1){
                similarAnswers[i] = state;
                continue;
            }

            VBox vBox = new VBox();

            Label answer = new Label(othersAnswers.get(i));
            answer.setFont(Font.font("Arial", FontWeight.BOLD, FontPosture.ITALIC, 15));

            ToggleGroup toggle = new ToggleGroup();
            MFXRadioButton radioButtonYes = new MFXRadioButton("Yes");
            radioButtonYes.setFont(Font.font("Arial", FontWeight.BOLD, FontPosture.ITALIC, 15));
            radioButtonsYes.add(radioButtonYes);
            MFXRadioButton radioButtonNo = new MFXRadioButton("No");
            radioButtonNo.setFont(Font.font("Arial", FontWeight.BOLD, FontPosture.ITALIC, 15));
            radioButtonsNo.add(radioButtonNo);

            radioButtonYes.setToggleGroup(toggle);
            radioButtonNo.setToggleGroup(toggle);
            toggleGroups.add(toggle);

            vBox.getChildren().addAll(answer, radioButtonYes, radioButtonNo);

            reactionRadioButtons.add(vBox);

            Platform.runLater(()->{
                reactionPane.getChildren().add(vBox);
            });
        }


        if(reactionRadioButtons.size()==0){
            stateLabel.setVisible(true);
            stateLabel.setDisable(false);

            Platform.runLater(()->{
                stateLabel.setText("No fields to check!");
            });
        }

        showTime(5 * othersAnswers.size());

        stateLabel.setVisible(false);
        stateLabel.setDisable(true);

        ArrayList<String> reactions = new ArrayList<>();
        index = 0;
        for (int i=0; i < othersAnswers.size(); i++) {
            if(othersAnswers.get(i).equals("") || !(othersAnswers.get(i).charAt(0)+"").equalsIgnoreCase(alphabet+"")) {
                reactions.add("Negative");
                continue;
            }else if(similarAnswers[i]!=-1){
                reactions.add(reactions.get(similarAnswers[i]));
                continue;
            }

            radioButtonsNo.get(index).setDisable(true);
            radioButtonsYes.get(index).setDisable(true);

            if(toggleGroups.get(index).getSelectedToggle()==null){
                Platform.runLater(()->{
                    toggleGroups.get(index).selectToggle(radioButtonsYes.get(index));
                });
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            try{
                if(toggleGroups.get(index).getSelectedToggle().equals(radioButtonsYes.get(index))){
                    reactions.add("Positive");
                }else{
                    reactions.add("Negative");
                }
                index++;
            }catch (NullPointerException e){ //host left game
//                return -1;
            }
        }

        try {
            Thread.sleep(1000); //"Yes" automatically ticked is visible on screen
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        reactionPane.setVisible(false);
        reactionPane.setDisable(true);

        fieldPane.setVisible(true);
        fieldPane.setDisable(false);

        //remove previous round reaction radioButtons
        while (reactionRadioButtons.size()!=0) {
            Platform.runLater(()->{
                reactionPane.getChildren().remove(0);
            });
            reactionRadioButtons.remove(0);
            try {
                Thread.sleep(100); //can be ommited?
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        try {
            Thread.sleep((long) 100 * (indexBetweenAllPlayers+1)); //was 10
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return client.sendReactionsAndGetPoint(reactions);
    }

    private int isAnswerSimilarToOthers(String answer, int index, ArrayList<String> othersAnswers) {
        for (int i = 0; i < othersAnswers.size(); i++) {
            if(i==index)
                break;
            if(othersAnswers.get(i).equals(answer))
                return i;
        }
        return -1;
    }

    private int listenForAlphabet() {
        Platform.runLater(()->{
            alphabetLabel.setText("Let Game Alphabet Be Determined"); //add needs
        });

        alphabet = Character.toUpperCase(client.listenForAlphabet());
        if(alphabet==' ') //host left the game
            return -1;


        Platform.runLater(() -> {
            alphabetLabel.setText("Go With ' " + alphabet + " '");
        });

        return 0;
    }


    private void showTime(int time) {
        timeLabel.setVisible(true);
        timeLabel.setDisable(false);

        long firstTime = (long) (System.nanoTime() / Math.pow(10, 9));

        secondTime = firstTime;
        do {
            Platform.runLater(() -> {
                timeLabel.setText((time + firstTime - secondTime) / 60 + ":" + (time + firstTime - secondTime) % 60);
            });

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            secondTime = (long) (System.nanoTime() / Math.pow(10, 9));
        } while (secondTime - firstTime <= time && timeLabel.getScene().getWindow().isShowing());

        timeLabel.setVisible(false);
        timeLabel.setDisable(true);
    }

    public void setClient(Client client) {
        this.client = client;
        setGameInfo();
        client.setGameScreenController(this);
    }

    private void setGameInfo() {
        fieldsString = client.getFields();
        rounds = client.getRounds();
        gameMode = client.getGameMode();
        time = client.getTime();
        indexBetweenAllPlayers = client.getIndexBetweenAllPlayers();
        plan = client.getPlan();
    }

    private void prepareFieldPane() {
        fieldPane.setVisible(true);
        fieldPane.setDisable(false);

        if(thisRound==1){
            for (String s : fieldsString) {
                MFXTextField textField = new MFXTextField();
                textField.setBackground(new Background(new BackgroundFill(Paint.valueOf("#a1cdd1"), CornerRadii.EMPTY, Insets.EMPTY)));
                textField.setFont(Font.font("Arial", FontWeight.BOLD, FontPosture.ITALIC, 15));
                textField.setDisable(true);
                textField.setFloatingText(s);
                textField.setPrefHeight(60);
                textField.setPrefWidth(120);

                textFields.add(textField);
                fieldPane.getChildren().add(textField);
            }
        }

    }
    public void notifHostLeftGame() {
        hostLeftGame = true;

        fieldPane.setVisible(false);
        fieldPane.setDisable(true);

        finishButton.setVisible(false);
        finishButton.setDisable(true);

        alphabetLabel.setVisible(false);
        alphabetLabel.setDisable(true);

        roundLabel.setVisible(false);
        roundLabel.setDisable(true);

        timeLabel.setVisible(false);
        timeLabel.setDisable(true);

        alphabetField.setVisible(false);
        alphabetField.setDisable(true);

        alphabetButton.setVisible(false);
        alphabetButton.setDisable(true);

        stateLabel.setVisible(true);
        stateLabel.setDisable(false);

        Platform.runLater(()->{
            stateLabel.setText("\t:(\nHost Left The Game");
        });
    }
}
