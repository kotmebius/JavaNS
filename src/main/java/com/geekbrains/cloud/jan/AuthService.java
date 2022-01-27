package com.geekbrains.cloud.jan;

import java.sql.*;
import java.text.SimpleDateFormat;

public class AuthService {
    private static Connection connection;
    private static Statement stmt;

    public static void connect() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:main.db");
            stmt = connection.createStatement();
        } catch (Exception e) {
            e.printStackTrace();
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
            e.printStackTrace();
        }
    }

    public static boolean isAuth(String login, int hash) {
        try {
            boolean isSuccess=false;
            ResultSet rs = stmt.executeQuery("SELECT password FROM users WHERE login = '" + login + "'");
            PreparedStatement ps = connection.prepareStatement("INSERT into authlog (date,login,result) values (?, ?, ?)");
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Timestamp timestamp = new Timestamp(System.currentTimeMillis());
            String ts = sdf.format(timestamp);
            ps.setString(1, ts);
            ps.setString(2, login);
            if (rs.next()) {
                int dbHash = rs.getInt(1);
                isSuccess=(hash == dbHash);
            }
            ps.setBoolean(3, isSuccess);
            ps.executeUpdate();
            return isSuccess;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void disconnect() {
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}