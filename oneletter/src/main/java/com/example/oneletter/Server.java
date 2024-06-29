package com.example.oneletter;

import com.example.oneletter.controllers.CreateGameController;
import com.example.oneletter.database.DatabaseHandler;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class Server {
    private final int port;
    private final String hostName;
    private final String gameName;
    private final String password;

    private CreateGameController createGameController;

    private final int rate = 50; //if rate% of players react positive to the one answer, then it will be accepted as true

    private int numFields;
    private final ArrayList<String> fields;
    private final String gameMode;
    private final int rounds;
    private final int time;

    private int thisRound = 1;
    private String serverPlan;
    private final ArrayList<Character> usedAlphabets = new ArrayList<>();

    public boolean isAcceptingClientEnough = false;

    private int numPlayers = 0;

    private ArrayList<Socket> sockets = new ArrayList<>();
    private ArrayList<Scanner> scanners = new ArrayList<>();
    private ArrayList<PrintWriter> printWriters = new ArrayList<>();

    private ArrayList<Integer> clientsThisRoundPoints = new ArrayList<>();
    private ArrayList<Integer> clientsSumPoints = new ArrayList<>();
    private ArrayList<String> clientsName = new ArrayList<>();

    private ServerSocket serverSocket = null;
    private boolean hostLeftGame = false;

    public Server(int port, String password, ArrayList<String> fields, String hostName, String gameName, int rounds, String gameMode, int time) {
        this.fields = fields;
        if (fields != null) //we need to create some deficient
            this.numFields = fields.size();
        this.hostName = hostName;
        this.gameName = gameName;
        this.password = password;
        this.rounds = rounds;
        this.gameMode = gameMode;
        this.port = port;
        this.time = time;
    }

    public void startAcceptingClient() {
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            e.printStackTrace();
        }

        while (!isAcceptingClientEnough) {
            Socket socket = null;
            Scanner scanner = null;
            PrintWriter printwriter = null;

            try {
                try{
                    socket = serverSocket.accept();
                }catch (SocketException e){ //if the host closes the window, serverSocket.accept() throws exception
                    return;
                }
                scanner = new Scanner(
                        new BufferedReader(
                                new InputStreamReader(
                                        socket.getInputStream())));
                printwriter = new PrintWriter(
                        new BufferedWriter(
                                new OutputStreamWriter(
                                        socket.getOutputStream())), true);
            } catch (IOException e) {
                e.printStackTrace();
            }

            sockets.add(socket);
            scanners.add(scanner);
            printWriters.add(printwriter);
            numPlayers++;
            String playerName = exchangeInfo(scanner, printwriter, numPlayers - 1);
            createGameController.addPlayerToBoard(playerName);
        }

        //remove game from database after starting game
        new DatabaseHandler().removeServer(port);
        closeServerSocket();
    }

    private String exchangeInfo(Scanner scanner, PrintWriter printWriter, int indexBetweenAllPlayers) {
        String name = scanner.nextLine();
        clientsName.add(name);
        printWriter.println(numFields);
        for (String s : fields)
            printWriter.println(s);
        printWriter.println(gameMode);
        printWriter.println(rounds);
        printWriter.println(time);

        return name;
    }

    public void closeServerSocket(){
        if(!serverSocket.isClosed())
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
    }

    public void startGame() {
        //fill points arrayList with 0
        for (int i = 0; i < numPlayers; i++) {
            clientsSumPoints.add(0);
            clientsThisRoundPoints.add(0);
        }
        bringHostFromLastToFirstIndexOfArrayLists();
        setAndSendAlphabetPlan();
        sendGoToGameToClients();
        determineAlphabet();
        waiteToFinishRoundAndCheckAnswers();
    }

    private void bringHostFromLastToFirstIndexOfArrayLists() {
        sockets.add(0, sockets.get(sockets.size() - 1));
        sockets.remove(sockets.size() - 1);

        scanners.add(0, scanners.get(scanners.size() - 1));
        scanners.remove(scanners.size() - 1);

        printWriters.add(0, printWriters.get(printWriters.size() - 1));
        printWriters.remove(printWriters.size() - 1);

        clientsName.add(0, clientsName.get(clientsName.size() - 1));
        clientsName.remove(clientsName.size() - 1);
    }

    private void setAndSendAlphabetPlan() {
        //set plan for server to get alphabet from clients
        //for instance,plan: 0123012 means client index 0 (in scanners list) should
        //determine the game alphabet in the first round,
        //client index 1 should determine in the second round and so on.
        int numPlayers = sockets.size();
        serverPlan = "";
        for (int i = 0; i < rounds; i++) {
            serverPlan += i % numPlayers;
        }

        //set and send plan for determining alphabets via clients
        //for instance,plan: 0100010 means this client should determine the
        //game alphabet in the second and sixth round.
        for (int i = 0; i < numPlayers; i++) {
            String clientPlan = "";
            for (int j = 0; j < rounds; j++) {
                if (j % numPlayers == i)
                    clientPlan += "1";
                else
                    clientPlan += "0";
            }
            printWriters.get(i).println(clientPlan); //send plan
            printWriters.get(i).println(numPlayers + ""); //send numPlayers
            printWriters.get(i).println(i + ""); //send index between all players
        }
    }

    int index;
    private void waiteToFinishRoundAndCheckAnswers() {
        //one player sends "I Finish This Round" and then server sends to all player "Send Your Answers"
        //and then all players send "I Will Send The Answer Now"

        //set a thread for each player to listen to "I Finish This Round" from them
        for (index = 0; index < scanners.size(); index++) {
            new Thread(() -> {
                int relatedIndex = index;
                String message;
                try {
                    message = scanners.get(relatedIndex).nextLine();
                }catch (NoSuchElementException e){ //player left game
                    closeSockets();
                    return;
                }
                if (message.equals("I Finish This Round")) {
                    new Thread(() -> {
                        //listen to "I Will Send The Answer Now" for finisher player
                        scanners.get(relatedIndex).nextLine();
                    }).start();

                    //sleep to start above thread
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    collectAndCheckAnswers();
                } else if (message.equals("I Will Send The Answer Now")) {
                    //nothing
                }
            }).start();
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private synchronized void collectAndCheckAnswers() {
        for (int j = 0; j < printWriters.size(); j++)
            printWriters.get(j).println("Send Your Answers");
            for (int i = 0; i < numFields; i++) {
                String[] answers = new String[numPlayers];
                ArrayList<String> points;

                //set a thread for each player to collect answer from them
                for (index = 0; index < scanners.size(); index++) {
                    new Thread(() -> {
                        int relatedIndex = index;
                        String answer;
                        try {
                            answer = scanners.get(relatedIndex).nextLine();
                        }catch (NoSuchElementException e){ //player left game
                            hostLeftGame = true;
                            closeSockets();
                            return;
                        }
                        answers[relatedIndex] = answer;
                    }).start();

                    try {
                        Thread.sleep(100); // 100 fixed
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                //check if all reactions collected
                while(true) {
                    int j;
                    for (j = 0; j < numPlayers; j++)
                        if(answers[j]==null) //is not the last reaction of this player collected?
                            break;
                    if(j==numPlayers)
                        break;
                    try {
                        Thread.sleep(200); //check for reaction collection each 200 millis
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if(hostLeftGame){
                        return;
                    }
                }
                points = getReactionsAndCalculatePoints(answers, fields.get(i));
                if(points==null) //host left game
                    return;
                //send clients' point
                for (int j = 0; j < printWriters.size(); j++) {
                    printWriters.get(j).println(points.get(j));
                }
                //add points
                for (int j = 0; j < points.size(); j++) {
                    clientsThisRoundPoints.set(j, clientsThisRoundPoints.get(j) + Integer.parseInt(points.get(j)));
                    clientsSumPoints.set(j, clientsSumPoints.get(j) + Integer.parseInt(points.get(j)));
                }
            }

            //(10s) for clients to see their points in gameScreen +
            // (1s) server wants to send s.t after sleep
            // so clients should be ready for it
            try {
                Thread.sleep(11000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            goNextRoundOrFinishGame();
    }

    private void goNextRoundOrFinishGame() {
        if (++thisRound <= rounds) {
            sendRoundScoreToClients(); //and set the array "clientsThisRoundPoints" to 0
            nextRound();
        } else {
            sendRoundScoreToClients();

            //sort clients name by final score
            outer:
            for (int i = 0; i < clientsSumPoints.size(); i++) {
                boolean shouldBreak = true;
                for (int j = 0; j < clientsSumPoints.size() - 1; j++) {
                    if (clientsSumPoints.get(j) < clientsSumPoints.get(j + 1)) {
                        int tempScore = clientsSumPoints.get(j);
                        clientsSumPoints.set(j, clientsSumPoints.get(j + 1));
                        clientsSumPoints.set(j + 1, tempScore);

                        String tempName = clientsName.get(j);
                        clientsName.set(j, clientsName.get(j + 1));
                        clientsName.set(j + 1, tempName);

                        shouldBreak = false;
                    } else if (j == clientsSumPoints.size() - 2 && shouldBreak) {
                        break outer;
                    }

                }
            }
            sendScoreBoardToClients();
            closeSockets();
        }
    }


    public void closeSockets() {
        //disconnect sockets
        for (int i = 0; i < sockets.size(); i++) {
            try {
                sockets.get(i).close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void sendScoreBoardToClients() {
        for (int i = 0; i < numPlayers; i++) {
            for (int j = 0; j < numPlayers; j++) {
                printWriters.get(i).println(clientsName.get(j));
                printWriters.get(i).println(clientsSumPoints.get(j));
            }
        }
    }

    private void sendRoundScoreToClients() {
        for (int i = 0; i < printWriters.size(); i++) {
            printWriters.get(i).println(clientsThisRoundPoints.get(i) + "");
        }
        for (int j = 0; j < clientsThisRoundPoints.size(); j++) {
            clientsThisRoundPoints.set(j, 0);
        }
    }

    private void nextRound() {
        determineAlphabet();
        waiteToFinishRoundAndCheckAnswers();
    }

    private ArrayList<String> getReactionsAndCalculatePoints(String[] answers, String category) {
        //for each player, simultaneously, "others answer" will
        //be sent and reaction to all those will be collected
        String[][] allReactions = new String[numPlayers][];
        for (index = 0; index < numPlayers; index++) {
            allReactions[index] = new String[numPlayers];
            //send "others answer" of players to them
            for (int i = 0; i < numPlayers; i++) {
                if (index == i) //no need to send one's answer to oneself
                    continue;
                printWriters.get(index).println(answers[i]);
            }

            new Thread(() -> {
                int relatedIndex = index;
                for (int i = 0; i < numPlayers; i++) {
                    if (i == relatedIndex) {
                        allReactions[relatedIndex][i] = "Positive"; //oneself reacts "Positive" to oneself answer
                    }
                    else{
                        String reaction;
                        try {
                            reaction = scanners.get(relatedIndex).nextLine();
                        }catch (NoSuchElementException e){
                            hostLeftGame = true;
                            closeSockets();
                            return;
                        }
                        allReactions[relatedIndex][i] = reaction;
                    }
                }
            }).start();
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        //check if all reactions collected
        while(true) {
            int i;
            for (i = 0; i < numPlayers; i++) {
                if(allReactions[i][numPlayers-1]==null) //is not the last reaction of this player collected?
                     break;
            }
            if(i==numPlayers)
                break;
            try {
                Thread.sleep(200); //check for reaction collection each 200 millis
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if(hostLeftGame){
                return null;
            }
        }
        //count pos and neg reactions
        ArrayList<String> filteredAnswers = new ArrayList<>();
        for (int i = 0; i < numPlayers; i++) {
            int positiveReactions = 0;
            for (int j = 0; j < numPlayers; j++) {
                if (i != j && allReactions[j][i].equals("Positive")) {
                    positiveReactions++;
                }
            }
            if (((double) positiveReactions / (numPlayers - 1)) >= ((double) rate / 100)) {
                filteredAnswers.add(answers[i]);
            } else {
                filteredAnswers.add("");
            }
        }
        return checkSimilarities(filteredAnswers);
    }

    private ArrayList<String> checkSimilarities(ArrayList<String> answers) {
        ArrayList<String> points = new ArrayList<>();
        for (int i = 0; i < numPlayers; i++) {
            if (answers.get(i).equals(""))
                points.add(0 + "");
            else
                for (int j = 0; j < numPlayers; j++) {
                    if (i != j && answers.get(i).equals(answers.get(j))) {
                        points.add(5 + "");
                        break;
                    } else if (j == numPlayers - 1)
                        points.add(10 + "");
                }
        }
        return points;
    }

    private void determineAlphabet() {
    //index of player in "scanners" arraylist who has to determine the game alphabet
    int playerIndex = Integer.parseInt(serverPlan.charAt(thisRound - 1) + ""); //because thisRound start from 1
    String alphabetString;
    try {
        alphabetString = scanners.get(playerIndex).nextLine();
    }catch (NoSuchElementException e){ //player left game
        closeSockets();
        return;
    }
    char alphabetChar = alphabetString.charAt(0);

    //store all alphabets in CAPITAL in "usedAlphabets" arr
    if(alphabetChar>=97 && alphabetChar<=122)
        alphabetChar = (char)(alphabetChar - 32);
    if (!usedAlphabets.contains(alphabetChar)) {
        usedAlphabets.add(alphabetChar);
        printWriters.get(playerIndex).println(0 + ""); //code for no problem
        sendAlphabetToClients(alphabetChar);
    } else { //received alphabet was repeated
        printWriters.get(playerIndex).println(-1 + ""); //code for problem
        determineAlphabet();
    }
}
    private void sendAlphabetToClients(char alphabetChar) {
        for (int i = 0; i < printWriters.size(); i++) {
            printWriters.get(i).println(alphabetChar + "");
        }
    }

    private void sendGoToGameToClients() {
        for (PrintWriter p : printWriters) {
            p.println("go to game");
        }
    }

    public String getHostName() {
        return hostName;
    }

    public String getGameName() {
        return gameName;
    }

    public int getRounds() {
        return rounds;
    }

    public String getPassword() {
        return password;
    }

    public ArrayList<String> getFields() {
        return fields;
    }

    public int getPort() {
        return port;
    }

    public void setCreateGameController(CreateGameController createGameController) {
        this.createGameController = createGameController;
    }

    public int getNumPlayers() {
        return numPlayers;
    }

    public void setAcceptingClientEnoughTrue() {
        isAcceptingClientEnough = true;
    }
}

