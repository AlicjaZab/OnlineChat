package chat.server;

import chat.entities.Message;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

/**
 * Thread for client connection listening to other clients (or messages from server)
 * Prints new messages to standard output
 */
public class ListenToOthers extends Thread {
    private ArrayList<Message> messagesWithPerson;
    private final Socket socket;
    private ClientConnection person;
    private String serverMessage = null;

    public ListenToOthers(Socket socket) { this.socket = socket; messagesWithPerson = new ArrayList<>();}

    public void run() {
        try(DataOutputStream output = new DataOutputStream(socket.getOutputStream())) {
            while (true) {
                if(serverMessage != null){
                    output.writeUTF(serverMessage);
                    serverMessage = null;
                }
                if (messagesWithPerson.size() == 0 || person == null) {
                    Thread.currentThread().sleep(25);
                }else{
                    for (Message message : messagesWithPerson) {
                        if(!message.isRead() && message.getFromLogin().equals(person.getCurrentUser().getLogin())) {
                            output.writeUTF("(new) " + message.toString());
                            message.setRead();
                        }
                        else {
                            output.writeUTF(message.toString());
                        }
                    }
                    messagesWithPerson.clear();
                }
            }
        }catch (IOException | InterruptedException e) {

        }
    }

    /**
     * Adds given message to list of messages to be printed (from person we are having a conversation)
     * @param message - given message
     */
    public void sendMeAMessage(Message message) {
        if(person != null && person.getCurrentUser().getLogin().equals(message.getFromLogin())){
            message.setRead();
            messagesWithPerson.add(message);
        }
    }

    /**
     * Changes the person with whom we are currently having a conversation (listening to)
     * @param person - new person
     */
    public void changePerson(ClientConnection person) {
        this.person = person;
        /*TODO coś jeszcze??*/
    }

    /**
     * Adds given message from server
     * @param message - message to print
     */
    public void sendMeAServerMessage( String message) {
        this.serverMessage = message;
    }

    /**
     * Adds given messages to list of messages to be printed (for conversation - to and from current user)
     * @param messages - given messages
     */
    public void loadMessagesWithPerson(ArrayList<Message> messages) {
        if (messages.size() > 25) {
            int counter = messages.size();
            for (Message m : messages) {
                if (counter > 25) {
                    m.setRead();
                    counter--;
                }else {
                    messagesWithPerson.add(m);
                }
            }
        }else {
            messagesWithPerson.addAll(messages);
        }


    }
}