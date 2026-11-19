package in.advocateportal;

import java.sql.*;

public class DB {

    private static final String URL = Config.get("DB_URL", "db.url",
            "jdbc:mysql://localhost:3306/advocate_portal?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true");
    private static final String USER = Config.get("DB_USER", "db.user", "root");
    private static final String PASS = Config.get("DB_PASSWORD", "db.password", "");

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Connection get() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }
}
