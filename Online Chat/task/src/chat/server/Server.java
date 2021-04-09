package chat.server;
import chat.DAO.MessagesDAO;
import chat.DAO.UsersDAO;
import org.sqlite.SQLiteDataSource;

import java.io.*;
import java.net.*;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Creates connection with the database, starts thread listening to new connections (clients)
 */
public class Server {
    static String address = "127.0.0.1";
    private static final int PORT = 23456;
    private static final String url = "jdbc:sqlite:C:/SQLite/db/chat.db";
    private static Connection connection;

    public static void main(String[] args){
        try (ServerSocket server = new ServerSocket(PORT, 50, InetAddress.getByName(address))) {

            System.out.println("Server started!");

            //----------Connect to database----------
            SQLiteDataSource dataSource = new SQLiteDataSource();
            dataSource.setUrl(url);


            try {
                connection = dataSource.getConnection();
                if (connection.isValid(5)) {
                    System.out.println("Connection is valid.");
                }
            }catch (SQLException e) {
                throw e;
            }
            UsersDAO  usersDAO = new UsersDAO();
            usersDAO.init(connection);
            ClientConnection.usersDAO = usersDAO;
            MessagesDAO messagesDAO = new MessagesDAO();
            messagesDAO.init(connection);
            ClientConnection.messagesDAO = messagesDAO;
            //----------------------------------------

            ConnectionsManager acceptConnections = new ConnectionsManager(server);
            acceptConnections.start();

            try {
                while (true) {
                    Thread.sleep(1000);
                }
            }catch(InterruptedException e){
                acceptConnections.closeSocket();
            }

        } catch (IOException | SQLException e ) {
            e.printStackTrace();
        } finally {
            try {
                connection.close();
            }catch (SQLException e){
                e.printStackTrace();
            }
        }
    }
}