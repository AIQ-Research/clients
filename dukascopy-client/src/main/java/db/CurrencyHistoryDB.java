package db;

import java.io.IOException;
import java.sql.*;
import java.util.List;
import java.util.Map;

/**
 * Created by vicident on 15/10/15.
 */
public class CurrencyHistoryDB {

    private final String DRIVER = "org.sqlite.JDBC";
    private final String JDBC = "jdbc:sqlite";
    private final int TIMEOUT = 30;

    private final String sDbUrl;
    private Connection conn;
    private String sTableColumns;
    private List<String> pairs;


    public CurrencyHistoryDB(String dbPath, List<String> pairs) throws ClassNotFoundException, SQLException, IOException {

        this.pairs = pairs;

        // register the driver
        String sDriverName = DRIVER;
        Class.forName(sDriverName);

        sDbUrl = JDBC + ":" + dbPath;

        sTableColumns = "(time BIGINT PRIMARY KEY";
        for (String pair: pairs) {
            sTableColumns += ", " + pair.replace("/", "_") + " FLOAT";
        }
        sTableColumns += ");";

        conn = null;
    }

    public void connectDB() throws SQLException {
        // create a database connection
        conn = DriverManager.getConnection(sDbUrl);
    }

    public void createTable(String tableName) throws SQLException {
        String sMakeTable = "CREATE TABLE " + tableName + sTableColumns;
        try {
            Statement stmt = conn.createStatement();
            try {
                stmt.setQueryTimeout(TIMEOUT);
                stmt.executeUpdate(sMakeTable);
                stmt.execute("pragma synchronous = off;");
            } finally {
                try { stmt.close(); } catch (Exception ignore) {}
            }
        } finally {
        }
    }

    public void writeRow(String tableName, long time, Map<String, Double> values) {

        String sInsertRow = "INSERT INTO " + tableName + " VALUES (" + String.valueOf(time);
        for (String pair: pairs) {
            sInsertRow += ", " + values.get(pair);
        }
        sInsertRow += ");";

        try {
            Statement stmt = conn.createStatement();
            try {
                stmt.setQueryTimeout(TIMEOUT);
                stmt.executeUpdate( sInsertRow );
            } finally {
                try { stmt.close(); } catch (Exception ignore) {}
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
        }
    }

    public void closeDB() throws SQLException {
        conn.close();
    }
}
