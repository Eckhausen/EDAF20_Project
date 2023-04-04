package krusty;

import java.sql.SQLException;

public class DatabaseMain {
    public static void main(String[] args) throws SQLException {
        Database db = new Database();
        db.connect();
        db.reset(null, null);
    }
}
