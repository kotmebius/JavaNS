package Server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;

public class AuthService {
    private static Connection connection;
    private static Statement stmt;
    private static final Logger LOGGER = LogManager.getLogger(AuthService.class);

    public static void connect() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:main.db");
            stmt = connection.createStatement();
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
    }

    public static void addUser(String login, String pass, String nick) {
        try {
            String query = "INSERT INTO users (login, password, nickname) VALUES (?, ?, ?);";
            PreparedStatement ps = connection.prepareStatement(query);
            ps.setString(1, login);
            ps.setInt(2, pass.hashCode());
            ps.setString(3, nick);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        }
    }

    public static boolean isAuth(String login, int hash) {
        try {
            ResultSet rs = stmt.executeQuery("SELECT password FROM users WHERE login = '" + login + "'");
            if (rs.next()) {
                int dbHash = rs.getInt(1);
                if (hash == dbHash) {
                    LOGGER.info("Пользователь " + login+ " успешно подключился" );
                    return true;
                } else {
                    LOGGER.info("Неудачная попытка подключения пользователя " + login+ " успешно подключился" );
                }
                ;
            }
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        }
        return false;
    }

    public static void disconnect() {
        try {
            connection.close();
        } catch (SQLException throwables) {
            LOGGER.error(throwables.getMessage());
        }
    }

}
