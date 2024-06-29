package com.example.oneletter;

import com.example.oneletter.database.DatabaseHandler;

import java.sql.SQLException;
import java.util.ArrayList;


public class ServerFactory {
    public static int FIRST_PORT = 8080;
    static String MAIN_HOST = "localhost";

    static DatabaseHandler databaseHandler = new DatabaseHandler();

    public static Server createServer(ArrayList<String> fields, String hostName, String gameName, String password, int rounds, String mode, int time) {
        int gamePort = 0;
        try {
            gamePort = databaseHandler.createServer(password, gameName, hostName, rounds, mode, time);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        Server server = new Server(gamePort, password, fields, hostName, gameName, rounds, mode, time);
        return server;
    }
}

