package si.f5.sakura_tk.sacra.economyv2.database;

import com.mysql.cj.jdbc.MysqlDataSource;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.*;

public final class DatabaseManager {
    private final JavaPlugin plugin;
    private MysqlDataSource dataSource;

    public DatabaseManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        dataSource = new MysqlDataSource();
        dataSource.setServerName(plugin.getConfig().getString("database.host"));
        dataSource.setPort(plugin.getConfig().getInt("database.port"));
        dataSource.setDatabaseName(plugin.getConfig().getString("database.database"));
        dataSource.setUser(plugin.getConfig().getString("database.username"));
        dataSource.setPassword(plugin.getConfig().getString("database.password"));
        dataSource.setUseSSL(plugin.getConfig().getBoolean("database.use-ssl", true));

        try (Connection c = connection(); Statement s = c.createStatement()) {
            s.executeUpdate("""
                CREATE TABLE IF NOT EXISTS ec_protected_lands (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    owner_uuid VARCHAR(36) NOT NULL,
                    world_name VARCHAR(32) NOT NULL,
                    min_x INT NOT NULL, max_x INT NOT NULL,
                    min_z INT NOT NULL, max_z INT NOT NULL,
                    is_nation TINYINT(1) DEFAULT 0,
                    INDEX idx_coords (world_name, min_x, max_x, min_z, max_z)
                )
                """);

            s.executeUpdate("""
                CREATE TABLE IF NOT EXISTS ec_land_permissions (
                    land_id INT NOT NULL,
                    player_uuid VARCHAR(36) NOT NULL,
                    can_build TINYINT(1) DEFAULT 0,
                    can_break TINYINT(1) DEFAULT 0,
                    can_interact TINYINT(1) DEFAULT 0,
                    can_container TINYINT(1) DEFAULT 0,
                    PRIMARY KEY (land_id, player_uuid),
                    FOREIGN KEY (land_id) REFERENCES ec_protected_lands(id) ON DELETE CASCADE
                )
                """);

            s.executeUpdate("""
                CREATE TABLE IF NOT EXISTS ec_shop_auctions (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    seller_uuid VARCHAR(36) NOT NULL,
                    material_name VARCHAR(64) NOT NULL,
                    amount INT NOT NULL,
                    price DECIMAL(19,4) NOT NULL,
                    created_at BIGINT NOT NULL,
                    is_active TINYINT(1) DEFAULT 1,
                    INDEX idx_active_material_price (material_name, is_active, price)
                )
                """);

            s.executeUpdate("""
                CREATE TABLE IF NOT EXISTS ec_shop_transactions (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    auction_id INT NOT NULL,
                    seller_uuid VARCHAR(36) NOT NULL,
                    buyer_uuid VARCHAR(36) NOT NULL,
                    material_name VARCHAR(64) NOT NULL,
                    amount INT NOT NULL,
                    price DECIMAL(19,4) NOT NULL,
                    created_at BIGINT NOT NULL,
                    INDEX idx_seller (seller_uuid),
                    INDEX idx_buyer (buyer_uuid),
                    FOREIGN KEY (auction_id) REFERENCES ec_shop_auctions(id)
                )
                """);
        } catch (SQLException e) {
            throw new IllegalStateException("Database initialization failed", e);
        }
    }

    public Connection connection() throws SQLException {
        return dataSource.getConnection();
    }

    public void close() {
        // MysqlDataSource is connection-per-operation; no pool shutdown is required.
    }
}
