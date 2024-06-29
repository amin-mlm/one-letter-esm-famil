package com.example.oneletter;

import com.example.oneletter.controllers.CreateGameController;
import com.example.oneletter.controllers.GameModeController;
import com.example.oneletter.controllers.GameScreenController;
import com.example.oneletter.controllers.JoinGameController;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class Client {
    String name;

    private ArrayList<String> fields = new ArrayList<>();
    private String gameMode;
    private int rounds;
    private int time;
    private int indexBetweenAllPlayers;
    private String plan;

    private int numOfAllPlayers;

    private Socket socket;
    private Scanner scanner;
    private PrintWriter printWriter;

    private int finalScore;
    private int rank;

    private GameScreenController gameScreenController;
    private JoinGameController joinGameController;


    public Client(String name) {
        this.name = name;
    }

    public Client(String name, int finalScore, int rank) {
        this.name = name;
        this.finalScore = finalScore;
        this.rank = rank;
    }

    public void joinToServer(int gameId) {
        try {
            socket = new Socket(ServerFactory.MAIN_HOST, gameId);
            scanner = new Scanner(
                    new BufferedReader(
                            new InputStreamReader(
                                    socket.getInputStream())));
            printWriter = new PrintWriter(
                    new BufferedWriter(
                            new OutputStreamWriter(
                                    socket.getOutputStream())), true);

            exchangeInfo();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void exchangeInfo() {
        printWriter.println(name);

        int numField = scanner.nextInt();
        scanner.nextLine();
        for(int i=0; i<numField; i++){
            fields.add(scanner.nextLine());
        }
        gameMode = scanner.nextLine();
        rounds = scanner.nextInt();
        time = scanner.nextInt();
        scanner.nextLine();
    }

    public int waitForStart() {
        try {
            plan = scanner.nextLine();
        }catch (NoSuchElementException e){ //host left the game
            return -1;
        }
        numOfAllPlayers = Integer.parseInt(scanner.nextLine());
        indexBetweenAllPlayers = Integer.parseInt(scanner.nextLine());
        String message = scanner.nextLine();
        if(message.equals("go to game")){
            if((GameModeController.fxmlLoader.getController()) instanceof CreateGameController)
                ((CreateGameController)GameModeController.fxmlLoader.getController()).gotoGameScreen(this);
            else
                ((JoinGameController)GameModeController.fxmlLoader.getController()).gotoGameScreen(this);
        }
        return 0;
    }

    public int sendAlphabet(String alphabetChar) {
        printWriter.println(alphabetChar);
        try{
            return Integer.parseInt(scanner.nextLine()); //result of entered alphabet
        }catch (NoSuchElementException e){
            gameScreenController.notifHostLeftGame();
            return -2;
        }
    }

    public char listenForAlphabet() {
        try{
            return scanner.nextLine().charAt(0);
        }catch (NoSuchElementException e){ //host left the game
            gameScreenController.notifHostLeftGame();
            return ' ';
        }
    }

    public String listenToSendAnswerMessage() {
        String message;
        try{
            message = scanner.nextLine();
        }catch (NoSuchElementException e){ //host left the game
            gameScreenController.notifHostLeftGame();
            return null;
        }
        if(message.equals("Send Your Answers")){
            printWriter.println("I Will Send The Answer Now");
        }
        return message;
    }

    public void sendFinishState() {
        printWriter.println("I Finish This Round");
    }

    public ArrayList<String> sendAnswerAndGetOthersAnswers(String answer) {
        printWriter.println(answer);
        ArrayList<String> othersAnswers = new ArrayList<>();
        for (int i = 0; i < numOfAllPlayers - 1; i++) {
            try {
                String otherAnswer = scanner.nextLine();
                othersAnswers.add(otherAnswer);
            }catch (NoSuchElementException e){
                gameScreenController.notifHostLeftGame();
                return null;
            }
        }
        return othersAnswers;
    }


    public int sendReactionsAndGetPoint(ArrayList<String> reactions) {
        for (int i = 0; i < reactions.size(); i++) {
            printWriter.println(reactions.get(i));
        }
        try{
            int point = Integer.parseInt(scanner.nextLine());
            return point;
        }catch(NoSuchElementException e){
            gameScreenController.notifHostLeftGame();
            return -1;
        }
    }


    public Socket getSocket() {
        return socket;
    }

    public String getName() {
        return name;
    }

    public ArrayList<String> getFields() {
        return fields;
    }

    public String getGameMode() {
        return gameMode;
    }

    public int getRounds() {
        return rounds;
    }

    public int getTime() {
        return time;
    }

    public int getIndexBetweenAllPlayers() {
        return indexBetweenAllPlayers;
    }

    public String getPlan() {
        return plan;
    }

    public int getNumOfAllPlayers() {
        return numOfAllPlayers;
    }

    public int getFinalScore() {
        return finalScore;
    }

    public int getRank() {
        return rank;
    }

    public void setFinalScore(int finalScore) {
        this.finalScore = finalScore;
    }

    public void setGameScreenController(GameScreenController gameScreenController) {
        this.gameScreenController = gameScreenController;
    }

    public int listenToRoundScore() {
        try{
            return Integer.parseInt(scanner.nextLine());
        }catch (NoSuchElementException e){
            gameScreenController.notifHostLeftGame();
            return -1;
        }
    }

    public String listenToClientNameForScoreBoard() {
        return scanner.nextLine();
    }

    public int listenToClientScoreForScoreBoard() {
        return Integer.parseInt(scanner.nextLine());
    }

    public void closeSocket()  {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setJoinGameController(JoinGameController joinGameController) {
        this.joinGameController = joinGameController;
    }
}