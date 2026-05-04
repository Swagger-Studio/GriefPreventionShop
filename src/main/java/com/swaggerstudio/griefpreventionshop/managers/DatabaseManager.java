package com.swaggerstudio.griefpreventionshop.managers;

import com.swaggerstudio.griefpreventionshop.GriefPreventionShop;
import org.bukkit.configuration.ConfigurationSection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;

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
            Properties props = new Properties();
            if (type.equals("MYSQL")) {
                ConfigurationSection mysql = dbSec.getConfigurationSection("mysql");
                String url = "jdbc:mysql://" + mysql.getString("host") + ":" + mysql.getInt("port") + "/" + mysql.getString("database");
                props.setProperty("user", mysql.getString("username"));
                props.setProperty("password", mysql.getString("password"));
                
                // Direct instantiation
                connection = new com.mysql.cj.jdbc.Driver().connect(url, props);
                plugin.getLogger().info("Connected to MySQL database using direct driver.");
            } else {
                // Default to H2
                String path = plugin.getDataFolder().getAbsolutePath() + "/history";
                String url = "jdbc:h2:" + path + ";MODE=MySQL";
                
                // Direct instantiation
                connection = new org.h2.Driver().connect(url, props);
                plugin.getLogger().info("Connected to H2 database using direct driver.");
            }

            if (connection != null) {
                createTable();
            }
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
                "world VARCHAR(64) NOT NULL, " +
                "currency VARCHAR(64)" +
                ")";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.execute();
        }

        // Add currency column if it doesn't exist (for existing tables)
        try {
            String alterSql = "ALTER TABLE gpshop_history ADD COLUMN IF NOT EXISTS currency VARCHAR(64)";
            try (PreparedStatement ps = connection.prepareStatement(alterSql)) {
                ps.execute();
            }
        } catch (SQLException ignored) {
            // IF NOT EXISTS might not be supported in all MySQL versions, but H2 supports it.
            // For MySQL, we could check metadata, but this is a simple fallback.
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
