package chat.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Listens to new sockets, creates connections
 */
public class ConnectionsManager extends Thread{
    private final ServerSocket server;
    private Socket socket;
    private static int countOfClients = 0;

    public ConnectionsManager(ServerSocket server) {this.server = server; }

    public void run() {
        try {
            while(true) {
                socket = server.accept(); // accepting a new client
                ClientConnection newConnection = new ClientConnection(socket, ++countOfClients);
                ClientConnection.addConnection(newConnection);
            }
        }catch (IOException e ) {
            e.printStackTrace();
        }
    }

    public void closeSocket() throws IOException{
        socket.close();     //to interrupt server.accept()
    }

}