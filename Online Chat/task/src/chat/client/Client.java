package chat.client;
import java.io.*;
import java.net.*;
import java.util.Scanner;

/**
 * Class creates socket for client, listens to messages from client (standard input) and sends them to server
 */
public class Client {
    private static final String SERVER_ADDRESS = "127.0.0.1";
    private static final int SERVER_PORT = 23456;

    public static void main(String[] args) throws InterruptedException{
        try (
                Socket socket = new Socket(InetAddress.getByName(SERVER_ADDRESS), SERVER_PORT);
                DataOutputStream output  = new DataOutputStream(socket.getOutputStream())
        ) {
            System.out.println("Client started!");

            ListenToServer listenToServer = new ListenToServer(socket);
            listenToServer.start();
            try {
                while (!socket.isClosed()) {
                    Scanner scanner = new Scanner(System.in);
                    String m = scanner.nextLine();  //read message form standard input
                    output.writeUTF(m);             //send it to server
                    if(m.equals("/exit")){          //if exit, close connection and shut down
                        socket.close();
                    }
                }
            }catch(IOException e){
                listenToServer.interrupt();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

/**
 * Thread listening to messages form server, and printing them to standard output
 */
class ListenToServer extends Thread {
    private final Socket socket;

    public ListenToServer(Socket socket) {
        this.socket = socket;
    }

    public void run() {
        try (
                DataInputStream input = new DataInputStream(socket.getInputStream())
        ) {
            while(!isInterrupted()){
                String msg = input.readUTF();   // read a message from server
                System.out.println(msg);        //print it
            }
        } catch(IOException e) {
            //e.printStackTrace();  //closed socket
        }
    }
}