package si.f5.sakura_tk.sacra.economyv2.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.SQLException;

public final class DatabaseManager implements AutoCloseable {

    private final JavaPlugin plugin;
    private final HikariDataSource dataSource;

    public DatabaseManager(JavaPlugin plugin) {
        this.plugin = plugin;

        // 設定の読み込み
        String host = plugin.getConfig().getString("database.host", "127.0.0.1");
        int port = plugin.getConfig().getInt("database.port", 3306);
        String database = plugin.getConfig().getString("database.name", "minecraft");
        String username = plugin.getConfig().getString("database.username", "root");
        String password = plugin.getConfig().getString("database.password", "");

        // HikariCPの設定（接続プール）
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database);
        config.setUsername(username);
        config.setPassword(password);

        // 各種最適化パラメーターの追加
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("useSSL", "false");
        config.addDataSourceProperty("characterEncoding", "utf8");
        config.addDataSourceProperty("serverTimezone", "Asia/Tokyo");

        // プールサイズの設定
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);

        this.dataSource = new HikariDataSource(config);
    }

    /**
     * 新しいDB接続（プールから）を取得する。
     * 呼び出し側で try-with-resources を使用すること。
     */
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    /**
     * 旧コードとの互換用。
     */
    public Connection connection() throws SQLException {
        return getConnection();
    }

    /**
     * DB初期化（テーブル名を sc_ に変更・カンマバグ修正済み）
     */
    public void initialize() throws SQLException {
        try (Connection connection = getConnection();
             var statement = connection.createStatement()) {

            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS sc_protected_lands (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    owner_uuid CHAR(36) NOT NULL,
                    world_name VARCHAR(128) NOT NULL,
                    min_x INT NOT NULL,
                    max_x INT NOT NULL,
                    min_z INT NOT NULL,
                    max_z INT NOT NULL,
                    is_nation BOOLEAN NOT NULL DEFAULT FALSE,
                    PRIMARY KEY (id),
                    INDEX idx_land_world (world_name, min_x, max_x, min_z, max_z)
                ) ENGINE=InnoDB
                """);

            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS sc_land_permissions (
                    land_id BIGINT NOT NULL,
                    player_uuid CHAR(36) NOT NULL,
                    can_build BOOLEAN NOT NULL DEFAULT FALSE,
                    can_break BOOLEAN NOT NULL DEFAULT FALSE,
                    can_interact BOOLEAN NOT NULL DEFAULT FALSE,
                    can_container BOOLEAN NOT NULL DEFAULT FALSE,
                    PRIMARY KEY (land_id, player_uuid),
                    CONSTRAINT fk_land_permissions_land
                        FOREIGN KEY (land_id)
                        REFERENCES sc_protected_lands(id)
                        ON DELETE CASCADE
                ) ENGINE=InnoDB
                """);

            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS sc_shop_auctions (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    seller_uuid CHAR(36) NOT NULL,
                    material_name VARCHAR(128) NOT NULL,
                    amount INT NOT NULL,
                    price DECIMAL(19,2) NOT NULL,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    is_active BOOLEAN NOT NULL DEFAULT TRUE,
                    PRIMARY KEY (id),
                    INDEX idx_shop_active_material (material_name, is_active, price)
                ) ENGINE=InnoDB
                """);

            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS sc_shop_transactions (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    listing_id BIGINT NOT NULL,
                    buyer_uuid CHAR(36) NOT NULL,
                    seller_uuid CHAR(36) NOT NULL,
                    material_name VARCHAR(128) NOT NULL,
                    amount INT NOT NULL,
                    price DECIMAL(19,2) NOT NULL,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (id),
                    INDEX idx_shop_transactions_buyer (buyer_uuid),
                    INDEX idx_shop_transactions_seller (seller_uuid)
                ) ENGINE=InnoDB
                """);
        }
    }

    /**
     * 接続プールを安全に閉じる。
     */
    @Override
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}
