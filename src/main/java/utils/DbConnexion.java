package utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Connexion JDBC simple (singleton), meme idee que {@code MyDatabase} du workshop :
 * {@link DriverManager}, constructeur prive, {@link #getInstance()}, {@link #getConnection()}.
 */
public final class DbConnexion {

    private static final String URL = "jdbc:mysql://127.0.0.1:3306/smart_voyage"
            + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=utf8";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    private Connection connection;
    private static volatile DbConnexion instance;

    private DbConnexion() {
        try {
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("Connection established");
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }

    public static DbConnexion getInstance() {
        if (instance == null) {
            synchronized (DbConnexion.class) {
                if (instance == null) {
                    instance = new DbConnexion();
                }
            }
        }
        return instance;
    }

    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
        }
        return connection;
    }

    public static void shutdown() {
        synchronized (DbConnexion.class) {
            if (instance != null && instance.connection != null) {
                try {
                    instance.connection.close();
                } catch (SQLException ignored) {
                }
                instance.connection = null;
                instance = null;
            }
        }
    }
}
