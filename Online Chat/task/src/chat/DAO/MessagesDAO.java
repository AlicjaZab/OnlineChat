package chat.DAO;

import chat.entities.Message;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Class for communication with the database
 */
public class MessagesDAO {
    private Connection connection;
    private Statement statement;

    /**
     * Creates tebale messages (if doesn't exist)
     * @param connection - connection with the database
     */
    public void init(Connection connection) {
        this.connection = connection;

        try {
            statement = connection.createStatement();
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS messages(" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "fromID INTEGER," +
                    "toID INTEGER," +
                    "content TEXT," +
                    "read INTEGER," +
                    "FOREIGN KEY(fromID) REFERENCES users(id)," +
                    "FOREIGN KEY(toID) REFERENCES users(id))");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns last messages for one conversation (messages between two specific users)
     *  - if the user (login1) has more than 25 or 25 unread messages, it returns all of those unread messages, but nothing more
     *  - if the user has less than 25 unread messages, it returns all of unread messages,
     *    and additionally at most 10 other read messages, but the total of messages can't be more than 25
     *  - if there are no unread messages, it returns 10 last messages
     * @param login1 - login of current (operated) user
     * @param login2 - login of the other user for the conversation
     * @return messages, as explained above.
     */
    public synchronized ArrayList<Message> getLastMassagesForOneConversation(String login1, String login2) {
        ArrayList<Message> messages= new ArrayList<>();
        try {
            int unread = 0;
            int limit = 25;
            ResultSet unreadMessages = statement.executeQuery(" select COUNT(id1) as unread from" +
                    " (select messages.id as id1, users.login as fromL, content, read from messages join users on fromID=users.id)" +
                    "  join (select messages.id as id2, users.login as toL from messages join users on toID=users.id)" +
                    "  on id1=id2 where (fromL='"+ login2 +"' and toL='"+ login1 +"' and read=0) ORDER BY id1 DESC;");
            if (unreadMessages.next()) {
                unread = unreadMessages.getInt("unread");
            }
            if (unread > 25){  limit = unread; }
            if (unread < 15) { limit = unread + 10; }
            ResultSet resultSet = statement.executeQuery(" select id1, fromL, toL, content, read from" +
                    " (select messages.id as id1, users.login as fromL, content, read from messages join users on fromID=users.id)" +
                    "  join (select messages.id as id2, users.login as toL from messages join users on toID=users.id)" +
                    "  on id1=id2 where (fromL='"+ login1 +"' or toL='"+ login1 +"') and (fromL='"+ login2 +"' or toL='"+ login2 +"')" +
                    " ORDER BY id1 DESC LIMIT " + limit +";");
            while (resultSet.next()) {
                int id = unreadMessages.getInt("id1");
                String fromLogin = unreadMessages.getString("fromL");
                String toLogin = unreadMessages.getString("toL");
                String content = unreadMessages.getString("content");
                boolean read = unreadMessages.getInt("read") == 0 ? false : true;
                messages.add(new Message(id, fromLogin, toLogin, content, read, this));
            }

        }catch(SQLException e) {
            System.out.println("Failed to read from database: operation GET MESSAGES FOR CONVERSATION");
        }
        Collections.reverse(messages);
        return messages;
    }

    /**
     * Sets the "read" field to true (1) which means that the message has been read by the receiver.
     * @param id - id of the message
     */
    public void setMessageToRead(int id) {
        try {
            statement.executeUpdate("UPDATE messages SET read=1 WHERE id=" + id + ";");
        }catch(SQLException e) {
            System.out.println("Failed to write to database: operation READ MESSAGE");
        }
    }

    /**
     * Saves given message to the 'messages' table
     * @param message - message to be saved
     * @return id of the saved message
     */
    public synchronized int saveMessage(Message message) {
        try {
            int idFrom = -1, idTo = -1;
            ResultSet resultSet1 = statement.executeQuery("SELECT id FROM users WHERE login='" + message.getFromLogin() + "';");
            if (resultSet1.next()) {
                idFrom = resultSet1.getInt("id");
            }
            ResultSet resultSet2 = statement.executeQuery("SELECT id FROM users WHERE login='" + message.getToLogin() + "';");
            if (resultSet2.next()) {
                idTo = resultSet2.getInt("id");
            }
            statement.executeUpdate("INSERT INTO messages (fromID, toID, content, read) VALUES (" + idFrom + ", " + idTo + ", '" + message.getContent() + "', 0);");
            ResultSet resultSet3 = statement.executeQuery("SELECT id FROM messages order by id desc limit 1;");
            if (resultSet3.next()) {
                int idM = resultSet3.getInt("id");
                return idM;
            }
        }catch(SQLException e) {
            System.out.println("Failed to write to database: operation SAVE MESSAGE: " + e.getMessage());
        }
        return -1;
    }

    /**
     * Finds the authors of unread messages send to given user
     * @param login - login of the user
     * @return - array of logins of those authors
     */
    public synchronized ArrayList<String> findAuthorsOfUnreadMessages(String login) {
        ArrayList<String> logins = new ArrayList<>();
        try (ResultSet users = statement.executeQuery("SELECT DISTINCT loginF FROM " +
                "(SELECT messages.id AS id1, users.login AS loginF, read FROM  messages JOIN users ON fromID=users.id) " +
                "JOIN (SELECT messages.id AS id2, users.login AS loginT FROM messages JOIN users ON toID=users.ID) " +
                "ON id1=id2 WHERE read = 0 AND loginT='" + login + "';")) {
            while (users.next()) {
                logins.add(users.getString("loginF"));
            }
        }catch(SQLException e) {
            System.out.println("Failed to read from database: operation FIND LOGINS / UNREAD MESSAGES");
        }
        return logins;
    }

    /**
     * Finds messages from "history" for one conversation - for the given number (count) finds messages starting with
     * that number and the next 25 messages or less if there are no more messages
     * e.g. if the conversation has 100 messages, for the given number 30, it finds the last messages from thirtieth
     * to fifth.
     * @param login - login of fisrt user for the conversation
     * @param login1 - login of second user for the conversation
     * @param count - given number
     * @return found messages
     */
    public synchronized ArrayList<Message> getMessagesFromHistory(String login, String login1, int count) {
        ArrayList<Message> messages = new ArrayList<>();
        try(ResultSet resultSet = statement.executeQuery("select " +
                "id1, fromL, toL, content, read from (select messages.id as id1, users.login as fromL, content, read " +
                "from messages join users on fromID=users.id) join (select messages.id as id2, users.login as toL from " +
                "messages join users on toID=users.id) on id1=id2 where (fromL='" + login1 + "' or toL='" + login1 + "') and " +
                "(fromL='" + login + "' or toL='" + login + "') ORDER BY id1 DESC;")){
            ArrayList<Message> allMessages = new ArrayList<>();
            while (resultSet.next()) {
                //int num = resultSet.getInt("row_num");
                int id = resultSet.getInt("id1");
                String fromLogin = resultSet.getString("fromL");
                String toLogin = resultSet.getString("toL");
                String content = resultSet.getString("content");
                boolean read = resultSet.getInt("read") == 0 ? false : true;
                allMessages.add(new Message(id, fromLogin, toLogin, content, read, this));
            }
            int to = count;
            int from = count - 25 >= 0 ? count - 25 : 0;
            if(allMessages.size() < count) {
                to = allMessages.size();
                from = to - 25 >= 0 ? to - 25 : 0;
            }else {
                to = count;
                from = count - 25 >= 0 ? count - 25 : 0;
            }
            for (int  i = from; i < to; i++) {
                messages.add(allMessages.get(i));
            }
        }catch(SQLException e){
            System.out.println("Failed to read from database: operation SHOW HISTORY: " + e.getMessage());
        }
        Collections.reverse(messages);
        return messages;
    }

    /**
     * Finds statistics for conversation : number of all messages, number of sent messages (from login1), and number
     * of received messages (from login2)
     * @param login1 - login of fisrt user for the conversation
     * @param login2 - login of second user for the conversation
     * @return String with those statistics
     */
    public synchronized String getStatsForConversation(String login1, String login2) {
        int all = -1, from1 = -1, from2 = -1;
        String stats = "Error!";
        try{
            ResultSet allMessages = statement.executeQuery("select COUNT(id1) as all_m, fromL, toL  " +
                    "from (select messages.id as id1, users.login as fromL from messages join users on fromID=users.id) " +
                    "join (select messages.id as id2, users.login as toL from messages join users on toID=users.id) " +
                    "on id1=id2 where (fromL='" + login1 + "' or toL='" + login1 + "') and (fromL='" + login2 + "' or toL='" + login2 + "') ORDER BY id1 DESC;");
            if (allMessages.next()) {
                all = allMessages.getInt("all_m");
            }
            ResultSet from1Messages = statement.executeQuery("select COUNT(id1) as from1, fromL, toL  from " +
                    "(select messages.id as id1, users.login as fromL from messages join users on fromID=users.id) " +
                    "join (select messages.id as id2, users.login as toL from messages join users on toID=users.id) " +
                    "on id1=id2 where (fromL='" + login1 + "' and toL='" + login2 + "') ORDER BY id1 DESC;");
            if (from1Messages.next()) {
                from1 = from1Messages.getInt("from1");
            }
            ResultSet from2Messages = statement.executeQuery("select COUNT(id1) as from2, fromL, toL  " +
                    "from (select messages.id as id1, users.login as fromL from messages join users on fromID=users.id) " +
                    "join (select messages.id as id2, users.login as toL from messages join users on toID=users.id) " +
                    "on id1=id2 where (fromL='" + login2 + "' and toL='" + login1 + "') ORDER BY id1 DESC;");
            if (from2Messages.next()) {
                from2 = from2Messages.getInt("from2");
            }
            stats = "Total messages: " + all + "\nMessages from " + login1 + ": " + from1 + "\nMessages from " + login2 + ": "+ from2;
            allMessages.close();
            from1Messages.close();
            from2Messages.close();
        }catch(SQLException e) {
            System.out.println("Failed to read from database: operation GET STATS: " + e.getMessage());
        }
        return stats;
    }
}