package chat.server;

import chat.DAO.MessagesDAO;
import chat.DAO.UsersDAO;
import chat.entities.Message;
import chat.entities.User;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Thread handling connection with one client
 * Listens to his input and responds with appropriate actions
 * Sending messages to client from other clients handled in separate thread (ListenToOthers)
 */
public class ClientConnection extends Thread {
    private static ArrayList<ClientConnection> allConnections = new ArrayList<>();
    private final Socket socket;
    private final ListenToOthers listenToOthers;
    private final int numOfConnection;
    public static UsersDAO usersDAO;
    public static MessagesDAO messagesDAO;
    private User currentUser;
    ClientConnection adresatClient = null;


    public ClientConnection(Socket socket, int numOfConnection) {
        this.socket = socket;
        this.numOfConnection = numOfConnection;
        listenToOthers = new ListenToOthers(socket);

    }

    /**
     * Listens to users input and invokes appropriate actions (methods)
     */
    public void run() {

        System.out.println("Client " + numOfConnection + " connected!");

        try(DataInputStream input = new DataInputStream(socket.getInputStream());
            DataOutputStream output = new DataOutputStream(socket.getOutputStream()))
        {
            try {

                output.writeUTF("Server: authorize or register");

                //---------------HANDLE USERS INPUT---------------------------
                while (!socket.isClosed()) {
                    String msgFromClient = input.readUTF(); // read a message from client
                    if(msgFromClient.length() == 0) continue;
                    if(msgFromClient.charAt(0) == '/'){
                        String[] inputText = msgFromClient.split(" ");
                        if(currentUser != null) {
                            switch (inputText[0]) {
                                case "/list":
                                    listOnlineUsers(output);
                                    break;
                                case "/exit":
                                    exit();
                                    break;
                                case "/chat":
                                    selectUserToChat(output, inputText);
                                    break;
                                case "/kick":
                                    kickOutUser(output, inputText);
                                    break;
                                case "/grant":
                                    grantUser(output, inputText);
                                    break;
                                case "/revoke":
                                    revokeUser(output, inputText);
                                    break;
                                case "/unread":
                                    showUnreadMessagesAuthors(output);
                                    break;
                                case "/history":
                                    showHistory(output, inputText);
                                    break;
                                case "/stats":
                                    showStats(output);
                                    break;
                                default:
                                    output.writeUTF("Server: incorrect command!");
                            }
                        } else {
                            switch (inputText[0]) {
                                case "/registration":
                                    register(output, inputText);
                                    break;
                                case "/auth":
                                    authorize(output, inputText);
                                    break;
                                default:
                                    output.writeUTF("Server: incorrect command!");
                            }
                        }
                    }else {
                        if (currentUser == null) {
                            output.writeUTF("Server: you are not in the chat!");
                        }
                        else if(adresatClient == null){
                            output.writeUTF("Server: use /list command to choose a user to text!");
                        }else{
                            output.writeUTF(currentUser.getLogin() + ": " + msgFromClient);
                            addMessage(msgFromClient, adresatClient);
                        }
                    }
                }
                //------------------------------------------------------------

            }catch(IOException e){
                System.out.println(e.getMessage());
                socket.close();
                listenToOthers.interrupt();
                deleteConnection(this);

            }
        }catch(IOException e){
            System.out.println(e.getMessage());
            e.printStackTrace();
            listenToOthers.interrupt();
            deleteConnection(this);
        }

    }

    /**
     * Handles registration -
     * - if login is correct, it is not already taken, and the password has at least 8 characters,
     *   registers successfully
     * - otherwise sends back appropriate message
     * @param output - stream to send messages to client
     * @param inputText - input from client
     * @throws IOException when sending output to client fails
     */
    private void register(DataOutputStream output, String[] inputText) throws IOException{
        if(inputText.length < 3){
            output.writeUTF("Server: incorrect command!");
        } else {
            if (inputText[2].length() < 8) {
                output.writeUTF("Server: the password is too short!");
            } else if (usersDAO.doesLoginExist(inputText[1])) {
                output.writeUTF("Server: this login is already taken! Choose another one.");
            } else {
                int password = inputText[2].hashCode();
                currentUser = usersDAO.addUser(inputText[1], Integer.toString(password));
                output.writeUTF("Server: you are registered successfully!");
                listenToOthers.start();
            }
        }
    }

    /**
     * Handles authorization -
     * - if given login and password are correct and exist in the database, authorizes (logs in) successfully
     * - otherwise prints appropriate message
     * @param output - stream to send messages to client
     * @param inputText - input from client
     * @throws IOException when sending output to client fails
     */
    private void authorize(DataOutputStream output, String[] inputText) throws IOException{
        if(inputText.length < 3){
            output.writeUTF("Server: incorrect command!");
        }else {
            if (!usersDAO.doesLoginExist(inputText[1])) {
                output.writeUTF("Server: incorrect login!");
            }else {
                String password = Integer.toString(inputText[2].hashCode());
                currentUser = usersDAO.findByLoginAndPassword(inputText[1], password);
                if (currentUser == null) {
                    output.writeUTF("Server: incorrect password!");
                }
                else if (currentUser.isBanned()) {
                    output.writeUTF("Server: you are banned!");
                    currentUser = null;
                }
                else {
                    output.writeUTF("Server: you are authorized successfully!");
                    listenToOthers.start();
                }
            }
        }
    }


    /**
     * Lists logins of users which are online (connected, logged-in clients)
     * @param output - stream to send messages to client
     * @throws IOException when sending output to client fails
     */
    public void listOnlineUsers(DataOutputStream output) throws IOException {
        ArrayList<String> names = new ArrayList<>();
        for (ClientConnection connection : allConnections) {
            if (connection.currentUser!= null && !connection.currentUser.getLogin().equals(currentUser.getLogin())) {
                names.add(connection.currentUser.getLogin());
            }
        }
        Collections.sort(names);
        StringWriter msgForClient = new StringWriter();
        if(names.size() == 0){
            output.writeUTF("Server: no one online");
            return;
        }
        msgForClient.append("Server: online:");
        for (String name : names) {
            msgForClient.append(" " + name);
        }
        output.writeUTF(msgForClient.toString());
        //}
    }

    /**
     * Closes connection with client
     * @throws IOException when closing socket fails
     */
    public void exit() throws IOException {
        System.out.println("Client " + numOfConnection + " disconnected!");
        socket.close();
        listenToOthers.interrupt();
        deleteConnection(this);
    }

    /**
     * Handles selecting user to chat with:
     * - if user with given login is currently connected, just piccks him (saves to the adresatClient variable)
     * - otherwise prints appropriate message
     * @param output - stream to send messages to client
     * @param inputText - input from client (should contain login)
     * @throws IOException when sending output to client fails
     */
    public void selectUserToChat(DataOutputStream output, String[] inputText) throws IOException{
        if(inputText.length < 2){
            output.writeUTF("Server: incorrect command!");
        }else {
            for (ClientConnection connection : allConnections) {
                if (connection.currentUser.getLogin().equals(inputText[1])) {
                    adresatClient = connection;
                    listenToOthers.changePerson(adresatClient);
                    ArrayList<Message> m = messagesDAO.getLastMassagesForOneConversation(currentUser.getLogin(), adresatClient.currentUser.getLogin());
                    listenToOthers.loadMessagesWithPerson(m);
                    return;
                }
            }
            if (adresatClient == null) {
                output.writeUTF("Server: the user is not online!");
            }
        }
    }

    /**
     * Handles kicking out a user (banning):
     * - if user to kick is a normal user, is online and current user is a moderator or admin -> bans succesfully
     * - if user to ban is a moderator, is online and current user is an admin -> bans successfully
     * - otherwise (and when user is trying to kick himself) -> prints appropriate message
     * @param output - stream to send messages to client
     * @param inputText - input from client
     * @throws IOException when sending output to client fails
     */
    private void kickOutUser(DataOutputStream output, String[] inputText) throws IOException{

        if(currentUser.getType() == 0){
            output.writeUTF("Server: you are not a moderator or an admin!");
            return;
        }
        if(inputText.length < 2){
            output.writeUTF("Server: incorrect command!");
            return;
        }
        String name = inputText[1];
        ClientConnection clientToKick = findConnectionByLogin(inputText[1]);
        if(clientToKick == null){
            output.writeUTF("Server: user not online.");
            return;
        }
        if(clientToKick.currentUser.getLogin().equals(currentUser.getLogin())){
            output.writeUTF("Server: you can't kick yourself!");
            return;
        }
        if(clientToKick.currentUser.getType() == 2) {
            output.writeUTF("Server: you can't kick an admin!");
            return;
        }
        if (clientToKick.currentUser.getType() == 1 && currentUser.getType() == 1){
            output.writeUTF("Server: you can't kick a moderator!");
            return;
        }
        clientToKick.ban();
        output.writeUTF("Server: " + inputText[1] + " was kicked!");
    }

    /**
     * Handles making user a moderator
     * - if user to grant is a normal user and current user is an admin -> grants successfully
     * - otherwise prints appropriate message
     * @param output - stream to send messages to client
     * @param input - input from client
     * @throws IOException when sending output to client fails
     */
    private void grantUser(DataOutputStream output, String[] input) throws IOException{
        if(currentUser.getType() != 2){
            output.writeUTF("Server: you are not an admin!");
            return;
        }
        ClientConnection userToGrant = findConnectionByLogin(input[1]);
        if(userToGrant.currentUser.getType() != 0){
            output.writeUTF("Server: this user is already a moderator!");
            return;
        }
        userToGrant.grant();
        output.writeUTF("Server: " + input[1] +" is the new moderator!");
    }

    /**
     * Handles making user a normal user (makes not-a-moderator)
     * - if user to revoke is a moderator and current user is an admin -> revokes successfully
     * - otherwise prints appropriate message
     * @param output - stream to send messages to client
     * @param inputText - input from client
     * @throws IOException when sending output to client fails
     */
    private void revokeUser(DataOutputStream output, String[] inputText) throws IOException{
        if(currentUser.getType() != 2){
            output.writeUTF("Server: you are not an admin!");
            return;
        }
        ClientConnection userToRevoke = findConnectionByLogin(inputText[1]);
        if(userToRevoke.currentUser.getType() != 1){
            output.writeUTF("Server: this user is not a moderator!");
            return;
        }
        userToRevoke.revoke();
        output.writeUTF("Server: " + inputText[1] +" is no longer a moderator!");
    }

    /**
     * Sends to client a list of authors of unread messages sent to current user
     * @param output - stream to send messages to client
     * @throws IOException when sending output to client fails
     */
    private void showUnreadMessagesAuthors(DataOutputStream output) throws IOException{
        ArrayList<String> logins = messagesDAO.findAuthorsOfUnreadMessages(currentUser.getLogin());
        if (logins.size() ==0 ) {
            output.writeUTF("Server: no one unread");
            return;
        }
        Collections.sort(logins);
        StringWriter msgForClient = new StringWriter();
        msgForClient.append("Server: unread from:");
        for(String login : logins) {
            msgForClient.append(" " + login);
        }
        output.writeUTF(msgForClient.toString());
    }

    /**
     * Sends to client list of messages from past (see MessagesDAO.getMessagesFromHistory)
     * @param output - stream to send messages to client
     * @param inputText - input from client
     * @throws IOException when sending output to client fails
     */
    private void showHistory(DataOutputStream output, String[] inputText) throws IOException{
        if (inputText.length < 2){
            output.writeUTF("Server: incorrect command!");
            return;
        }
        try {
            int count = Integer.parseInt(inputText[1]);
            ArrayList<Message> messages = messagesDAO.getMessagesFromHistory(currentUser.getLogin(), adresatClient.currentUser.getLogin(), count);
            StringWriter msgForClient = new StringWriter();
            msgForClient.append("Server:\n");
            for(Message m : messages) {
                msgForClient.append(m.toString() + "\n");
            }
            output.writeUTF(msgForClient.toString());
        }catch(NumberFormatException e) {
            output.writeUTF("Server: " + inputText[1] + " is not a number!");
        }
    }

    /**
     * Sends to client stats for conversation with current adresat client
     * (see MessagesDAO.getStatsForConversation)
     * @param output - stream to send messages to client
     * @throws IOException when sending output to client fails
     */
    private void showStats(DataOutputStream output) throws IOException{
        String stats = messagesDAO.getStatsForConversation(currentUser.getLogin(), adresatClient.currentUser.getLogin());
        String msgForClient = "Server:\nStatistics with " + adresatClient.currentUser.getLogin() + ":\n" + stats;
        output.writeUTF(msgForClient);

    }

    /**
     * Saves new message from client to database and sends it to receiver client
     * @param content - content of the message
     * @param receiver - receiver client
     */
    public synchronized void addMessage(String content, ClientConnection receiver) {
        Message newMessage = new Message(currentUser.getLogin(), receiver.currentUser.getLogin(), content, false, messagesDAO);
        newMessage.save();
        receiver.listenToOthers.sendMeAMessage(newMessage);

    }

    /**
     * Bans (kicks out) current user
     * Invoked when a moderator or an admin kicks current user out
     */
    public void ban(){

        listenToOthers.sendMeAServerMessage("Server: you have been kicked out of the server!");
        currentUser.ban();
        currentUser = null;
        listenToOthers.changePerson(null);

    }

    /**
     * Makes current user a modeartor
     * Invoked when an admin grants current user
     */
    public void grant(){
        listenToOthers.sendMeAServerMessage("Server: you are the new moderator now!");

        currentUser.setType(1);
    }

    /**
     * Makes current user a normal user (not-a-moderator)
     * Invoked when an admin revokes current user
     */
    private void revoke(){
        listenToOthers.sendMeAServerMessage("Server: you are no longer a moderator!");
        currentUser.setType(0);
    }

    /**
     * Adds connection to list of all connections
     * @param connection - connection to add
     */
    public static void addConnection(ClientConnection connection){
        allConnections.add(connection);
        connection.start();
    }

    /**
     * Removes connection from list of all connections
     * @param connection - connection to remove
     */
    public static void deleteConnection(ClientConnection connection){
        allConnections.remove(connection);
    }

    /**
     *
     * @return current user
     */
    public User getCurrentUser() {
        return currentUser;
    }

    /**
     * Finds connection by login
     * - if user with given login is currently online, returns his connection
     * @param login
     * @return connection if found, null otherwise
     */
    public ClientConnection findConnectionByLogin(String login) {
        for (ClientConnection connection : allConnections) {
            if (connection.currentUser != null && connection.currentUser.getLogin().equals(login)){
                return connection;
            }
        }
        return null;
    }
}