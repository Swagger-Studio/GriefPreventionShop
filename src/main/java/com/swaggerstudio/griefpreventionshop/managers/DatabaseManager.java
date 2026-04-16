package com.swaggerstudio.griefpreventionshop.managers;

import com.swaggerstudio.griefpreventionshop.GriefPreventionShop;
import org.bukkit.configuration.ConfigurationSection;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DatabaseManager {

    private final GriefPreventionShop plugin;
    private Connection connection;

    public DatabaseManager(GriefPreventionShop plugin) {
        this.plugin = plugin;
        initialize();
    }

    private void initialize() {
        ConfigurationSection dbSec = plugin.getConfig().getConfigurationSection("database");
        String type = dbSec.getString("type", "H2").toUpperCase();

        try {
            if (type.equals("MYSQL")) {
                ConfigurationSection mysql = dbSec.getConfigurationSection("mysql");
                String url = "jdbc:mysql://" + mysql.getString("host") + ":" + mysql.getInt("port") + "/" + mysql.getString("database");
                connection = DriverManager.getConnection(url, mysql.getString("username"), mysql.getString("password"));
                plugin.getLogger().info("Connected to MySQL database.");
            } else {
                // Default to H2
                String path = plugin.getDataFolder().getAbsolutePath() + "/history";
                connection = DriverManager.getConnection("jdbc:h2:" + path + ";MODE=MySQL");
                plugin.getLogger().info("Connected to H2 database.");
            }

            createTable();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to connect to database: " + e.getMessage());
        }
    }

    private void createTable() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS gpshop_history (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "player_uuid VARCHAR(36) NOT NULL, " +
                "timestamp BIGINT NOT NULL, " +
                "amount INT NOT NULL, " +
                "price DOUBLE NOT NULL, " +
                "world VARCHAR(64) NOT NULL" +
                ")";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.execute();
        }
    }

    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                initialize();
            }
        } catch (SQLException e) {
            initialize();
        }
        return connection;
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not close database connection!");
        }
    }
}
