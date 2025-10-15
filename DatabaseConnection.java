package NerdTech.DR_Fashion.DatabaseConnection;

import java.sql.*;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class DatabaseConnection {

    private static final String LOCAL_URL = "Local_Database_URL";
    private static final String ONLINE_URL
            = "Online_Database_URL";

    private static final String LOCAL_USER = "Local_Database_Username";
    private static final String LOCAL_PASSWORD = "Local_Database_Password";
    private static final String ONLINE_USER = "Online_Database_Username";
    private static final String ONLINE_PASSWORD = "Online_Database_Password";

    // ✅ CORRECT ORDER: Parent tables first, then child tables
    private static final List<String> ALL_TABLES = Arrays.asList(
            "role", // No dependencies
            "type", // No dependencies
            "user", // No dependencies (or depends on role if FK exists)
            "employee", // Depends on role
            "stock", // Depends on type (if FK exists)
            "accesories", // Depends on type (FK: type_id)
            "attendence", // Depends on employee
            "monthly_payment", // Depends on role
            "per_day_salary" // No dependencies (or minimal)
    );

    private static Consumer<String> statusCallback;

    public static void setStatusCallback(Consumer<String> callback) {
        statusCallback = callback;
    }

    private static void updateStatus(String message) {
        if (statusCallback != null) {
            statusCallback.accept(message);
        }
        System.out.println(message);
    }

    public static Connection getLocalConnection() throws SQLException {
        updateStatus("Connecting to Local Database...");
        return DriverManager.getConnection(LOCAL_URL, LOCAL_USER, LOCAL_PASSWORD);
    }

    public static Connection getOnlineConnection() throws SQLException {
        updateStatus("Connecting to Online Database...");
        return DriverManager.getConnection(ONLINE_URL, ONLINE_USER, ONLINE_PASSWORD);
    }

    public static Connection getConnection() throws Exception {
        try {
            updateStatus("Trying Online Database...");
            return getOnlineConnection();
        } catch (Exception ex) {
            updateStatus("⚠️ Online DB failed, switching to Local DB...");
            return getLocalConnection();
        }
    }

    // =======================
    // Local → Online Sync (all tables)
    // =======================
    public static void syncLocalToOnline() throws Exception {
        updateStatus("🔄 Starting Local → Online sync for all tables...");

        try (Connection localConn = getLocalConnection(); Connection onlineConn = getOnlineConnection()) {

            // Disable foreign key checks temporarily
            onlineConn.createStatement().execute("SET FOREIGN_KEY_CHECKS=0");

            for (String table : ALL_TABLES) {
                syncTableLocalToOnline(localConn, onlineConn, table);
            }

            // Re-enable foreign key checks
            onlineConn.createStatement().execute("SET FOREIGN_KEY_CHECKS=1");

            updateStatus("✅ Local → Online sync completed for all tables!");
        } catch (Exception e) {
            updateStatus("❌ Local → Online sync failed: " + e.getMessage());
            throw e;
        }
    }

    private static void syncTableLocalToOnline(Connection localConn, Connection onlineConn, String tableName) throws SQLException {
        updateStatus("  ➡️ Syncing " + tableName + " (Local → Online)...");

        Statement stmt = localConn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT * FROM " + tableName);

        ResultSetMetaData meta = rs.getMetaData();
        int colCount = meta.getColumnCount();

        // INSERT statement හදන්න
        StringBuilder insertSQL = new StringBuilder("INSERT INTO " + tableName + " (");
        StringBuilder values = new StringBuilder("VALUES (");
        StringBuilder update = new StringBuilder(" ON DUPLICATE KEY UPDATE ");

        for (int i = 1; i <= colCount; i++) {
            String colName = meta.getColumnName(i);
            insertSQL.append(colName);
            values.append("?");

            if (i > 1) { // skip primary key update 
                if (i > 2) {
                    update.append(", ");
                }
                update.append(colName).append("=VALUES(").append(colName).append(")");
            }

            if (i < colCount) {
                insertSQL.append(", ");
                values.append(", ");
            }
        }

        insertSQL.append(") ").append(values).append(")").append(update);

        PreparedStatement ps = onlineConn.prepareStatement(insertSQL.toString());
        int batchCount = 0;

        while (rs.next()) {
            for (int i = 1; i <= colCount; i++) {
                ps.setObject(i, rs.getObject(i));
            }
            ps.addBatch();
            batchCount++;

            if (batchCount % 100 == 0) { // batch processing
                ps.executeBatch();
            }
        }

        ps.executeBatch(); // remaining records
        updateStatus("    ✓ " + tableName + " synced (" + batchCount + " rows)");

        ps.close();
        rs.close();
        stmt.close();
    }

    // =======================
    // Online → Local Sync (all tables)
    // =======================
    public static void syncOnlineToLocal() throws Exception {
        updateStatus("🔁 Starting Online → Local sync for all tables...");

        try (Connection localConn = getLocalConnection(); Connection onlineConn = getOnlineConnection()) {

            // Disable foreign key checks temporarily
            localConn.createStatement().execute("SET FOREIGN_KEY_CHECKS=0");

            for (String table : ALL_TABLES) {
                syncTableOnlineToLocal(onlineConn, localConn, table);
            }

            // Re-enable foreign key checks
            localConn.createStatement().execute("SET FOREIGN_KEY_CHECKS=1");

            updateStatus("✅ Online → Local sync completed for all tables!");
        } catch (Exception e) {
            updateStatus("❌ Online → Local sync failed: " + e.getMessage());
            throw e;
        }
    }

    private static void syncTableOnlineToLocal(Connection onlineConn, Connection localConn, String tableName) throws SQLException {
        updateStatus("  ⬅️ Syncing " + tableName + " (Online → Local)...");

        Statement stmt = onlineConn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT * FROM " + tableName);

        ResultSetMetaData meta = rs.getMetaData();
        int colCount = meta.getColumnCount();

        // INSERT statement creating
        StringBuilder insertSQL = new StringBuilder("INSERT INTO " + tableName + " (");
        StringBuilder values = new StringBuilder("VALUES (");
        StringBuilder update = new StringBuilder(" ON DUPLICATE KEY UPDATE ");

        for (int i = 1; i <= colCount; i++) {
            String colName = meta.getColumnName(i);
            insertSQL.append(colName);
            values.append("?");

            if (i > 1) { // skip primary key update
                if (i > 2) {
                    update.append(", ");
                }
                update.append(colName).append("=VALUES(").append(colName).append(")");
            }

            if (i < colCount) {
                insertSQL.append(", ");
                values.append(", ");
            }
        }

        insertSQL.append(") ").append(values).append(")").append(update);

        PreparedStatement ps = localConn.prepareStatement(insertSQL.toString());
        int batchCount = 0;

        while (rs.next()) {
            for (int i = 1; i <= colCount; i++) {
                ps.setObject(i, rs.getObject(i));
            }
            ps.addBatch();
            batchCount++;

            if (batchCount % 100 == 0) { // batch processing
                ps.executeBatch();
            }
        }

        ps.executeBatch(); // remaining records
        updateStatus("    ✓ " + tableName + " synced (" + batchCount + " rows)");

        ps.close();
        rs.close();
        stmt.close();
    }
}
