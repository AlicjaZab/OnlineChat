package chat.DAO;

import chat.entities.User;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalTime;
import java.util.ArrayList;

/**
 * Class  for communication with the database
 */
public class UsersDAO {
    private Connection connection;
    private Statement statement;

    /**
     * Creates 'users' table and admin user (if don't exist)
     * @param connection - connection with the database
     */
    public void init(Connection connection) {
        this.connection = connection;
        try {
            statement = connection.createStatement();
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS users(" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "login TEXT," +
                    "password TEXT," +
                    "type INTEGER DEFAULT 0," +
                    "ban_exp TEXT DEFAULT '00:00:00')");

            ResultSet admin = statement.executeQuery("SELECT id FROM users WHERE login ='admin';");
            if(!admin.next()){
                String password = Integer.toString("12345678".hashCode());
                statement.executeUpdate("INSERT INTO users (login, password, type) " +
                        "VALUES ('admin', '" +password +"', 2);");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Adds new user to "users" table
     * @param login
     * @param password hash of the password
     */
    public User addUser(String login, String password){
        try {
            statement.executeUpdate("INSERT INTO users (login, password) VALUES " +
                    "('" + login + "' ,'" + password + "');");
            ResultSet user = statement.executeQuery("SELECT id, type FROM users WHERE login ='" + login + "';");
            int type = user.getInt("type");
            return new User(login, password, type, this);
        }catch(SQLException e) {
            System.out.println("Failed to write to database: operation ADD NEW USER");
        }
        return null;
    }

    /**
     *
     * @param login
     * @return whether the login has already been used
     */
    public boolean doesLoginExist(String login) {
        try (ResultSet users = statement.executeQuery("SELECT login FROM users WHERE login='" + login + "';")) {
            if (users.next()) {
                return true;
            }
        }catch(SQLException e) {
            System.out.println("Failed to read from database: operation FIND LOGIN");
        }
        return false;
    }

    /**
     * Finds user with given login and password (hashed)
     * @param login
     * @param password
     * @return user if found, null otherwise
     */
    public User findByLoginAndPassword(String login, String password) {
        try (ResultSet user = statement.executeQuery("SELECT id, login, password, type, ban_exp FROM users WHERE login='" + login + "' AND password='" + password + "';")) {
            if (user.next()) {
                int type = user.getInt("type");
                LocalTime banExpiration = LocalTime.parse(user.getString("ban_exp"));
                return new User(login, password, type, banExpiration, this);
            }
        }catch(SQLException e) {
            System.out.println("Failed to read from database: operation FIND USER");
        }
        return null;
    }

    /**
     * Changes the type of user to 'moderator'
     * @param login - login of user to update
     */
    public void addModeratorPermissionsByLogin(String login) {
        try {
            statement.executeUpdate("UPDATE users SET type=1 WHERE login='" + login + "';");
        }catch(SQLException e) {
            System.out.println("Failed to write to database: operation ADD MODERATOR PERMISSIONS");
        }
    }

    /**
     * Changes the type of user to 'normal'
     * @param login - login of user to update
     */
    public void cancelModeratorPermissionsByLogin(String login) {
        try {
            statement.executeUpdate("UPDATE users SET type=0 WHERE login='" + login + "';");
        }catch(SQLException e) {
            System.out.println("Failed to write to database: operation CANCEL MODERATOR PERMISSIONS");
        }
    }

    /**
     * Sets ban expiration time for user
     * Invoked when user is being banned
     * @param login - login of user to ban
     * @param banExpiration -  five minutes in the future from now
     */
    public void banByLogin(String login, LocalTime banExpiration) {
        try {
            statement.executeUpdate("UPDATE users SET ban_exp='" + banExpiration.toString() + "' WHERE login='" + login + "';");
        }catch(SQLException e) {
            System.out.println("Failed to write to database: operation BAN USER");
        }
    }

}