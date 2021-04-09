package chat.entities;

import chat.DAO.UsersDAO;

import java.time.LocalTime;

/**
 * class representing user
 */
public class User {
    private String login;
    private String password;
    private int type = 0; //0 - normal, 1 - moderator, 2 - admin
    UsersDAO usersDAO;
    LocalTime banExpiration = null;

    public User(String login, String password, int type, UsersDAO usersDAO){
        this.login = login;
        this.password = password;
        this.type = type;
        this.usersDAO = usersDAO;
    }

    public User(String login, String password, int type, LocalTime banExpiration, UsersDAO usersDAO){
        this.login = login;
        this.password = password;
        this.type = type;
        this.banExpiration = banExpiration;
        this.usersDAO = usersDAO;
    }

    public String getLogin() { return login; }

    public int getType() { return type; }

    public void ban() {
        banExpiration = LocalTime.now().plusMinutes(5);
        usersDAO.banByLogin(login, banExpiration);
    }

    public boolean isBanned() {
        if (banExpiration.isAfter(LocalTime.now())) return true;
        return false;
    }

    public void setType(int i) {
        type = i;
        if(i == 1){
            usersDAO.addModeratorPermissionsByLogin(login);
        }else {
            usersDAO.cancelModeratorPermissionsByLogin(login);
        }

    }
}