package chat.entities;

import chat.DAO.MessagesDAO;

/**
 * class representing a single message between two clients
 */
public class Message {

    private int id;
    private String fromLogin;
    private String toLogin;
    private String content;
    private boolean read; //whether message was read by the receiver
    MessagesDAO messagesDAO;

    public Message(int id, String fromLogin, String toLogin, String content, boolean read, MessagesDAO messagesDAO) {
        this.id = id;
        this.fromLogin = fromLogin;
        this.toLogin = toLogin;
        this.content = content;
        this.read = read;
        this.messagesDAO = messagesDAO;
    }

    public Message(String fromLogin, String toLogin, String content, boolean read, MessagesDAO messagesDAO) {
        this.fromLogin = fromLogin;
        this.toLogin = toLogin;
        this.content = content;
        this.read = read;
        this.messagesDAO = messagesDAO;
    }

    public String getToLogin() { return toLogin; }
    public String getFromLogin() { return fromLogin; }

    public void save() {
        id = messagesDAO.saveMessage(this);
    }

    @Override
    public String toString() {
        return fromLogin + ": " + content;
    }

    public boolean isRead() { return read; }

    public void setRead() {
        this.read = true;
        messagesDAO.setMessageToRead(id); }

    public String getContent() { return content; }
}
