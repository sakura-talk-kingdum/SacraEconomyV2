package si.f5.sakura_tk.sacra.economyv2.database;

import com.mysql.cj.jdbc.MysqlDataSource;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.SQLException;

public final class DatabaseManager {

    private final JavaPlugin plugin;
    private final MysqlDataSource dataSource;

    public DatabaseManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dataSource = new MysqlDataSource();

        String host = plugin.getConfig().getString(
            "database.host",
            "127.0.0.1"
        );

        int port = plugin.getConfig().getInt(
            "database.port",
            3306
        );

        String database = plugin.getConfig().getString(
            "database.name",
            "minecraft"
        );

        String username = plugin.getConfig().getString(
            "database.username",
            "root"
        );

        String password = plugin.getConfig().getString(
            "database.password",
            ""
        );

        dataSource.setURL(
            "jdbc:mysql://"
                + host
                + ":"
                + port
                + "/"
                + database
                + "?useSSL=false"
                + "&characterEncoding=utf8"
                + "&serverTimezone=UTC"
        );

        dataSource.setUser(username);
        dataSource.setPassword(password);
    }

    /**
     * 新しいDB接続を取得する。
     *
     * 呼び出し側で try-with-resources を使用すること。
     */
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    /**
     * 旧コードとの互換用。
     *
     * ShopRepository / ShopService / LandRepository が
     * database.connection() を使用しているため残す。
     */
    public Connection connection() throws SQLException {
        return getConnection();
    }

    /**
     * DB初期化。
     */
    public void initialize() throws SQLException {

        try (Connection connection = getConnection()) {

            try (var statement = connection.createStatement()) {
                statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS ec_protected_lands (
                        id BIGINT NOT NULL AUTO_INCREMENT,
                        owner_uuid CHAR(36) NOT NULL,
                        world_name VARCHAR(128) NOT NULL,
                        min_x INT NOT NULL,
                        max_x INT NOT NULL,
                        min_z INT NOT NULL,
                        max_z INT NOT NULL,
                        is_nation BOOLEAN NOT NULL DEFAULT FALSE,

                        PRIMARY KEY (id),

                        INDEX idx_land_world (
                            world_name,
                            min_x,
                            max_x,
                            min_z,
                            max_z
                        )
                    ) ENGINE=InnoDB
                    """);

                statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS ec_land_permissions (
                        land_id BIGINT NOT NULL,
                        player_uuid VARCHAR(36) NOT NULL,

                        can_build BOOLEAN NOT NULL DEFAULT FALSE,
                        can_break BOOLEAN NOT NULL DEFAULT FALSE,
                        can_interact BOOLEAN NOT NULL DEFAULT FALSE,
                        can_container BOOLEAN NOT NULL DEFAULT FALSE,

                        PRIMARY KEY (land_id, player_uuid),

                        CONSTRAINT fk_land_permissions_land
                            FOREIGN KEY (land_id)
                            REFERENCES ec_protected_lands(id)
                            ON DELETE CASCADE
                    ) ENGINE=InnoDB
                    """);

                statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS ec_shop_auctions (
                        id BIGINT NOT NULL AUTO_INCREMENT,

                        seller_uuid CHAR(36) NOT NULL,
                        material_name VARCHAR(128) NOT NULL,
                        amount INT NOT NULL,
                        price DECIMAL(19,2) NOT NULL,

                        created_at TIMESTAMP NOT NULL
                            DEFAULT CURRENT_TIMESTAMP,

                        is_active BOOLEAN NOT NULL
                            DEFAULT TRUE,

                        PRIMARY KEY (id),

                        INDEX idx_shop_active_material (
                            material_name,
                            is_active,
                            price
                        )
                    ) ENGINE=InnoDB
                    """);

                statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS ec_shop_transactions (
                        id BIGINT NOT NULL AUTO_INCREMENT,

                        listing_id BIGINT NOT NULL,
                        buyer_uuid CHAR(36) NOT NULL,
                        seller_uuid CHAR(36) NOT NULL,
                        material_name VARCHAR(128) NOT NULL,
                        amount INT NOT NULL,
                        price DECIMAL(19,2) NOT NULL,

                        created_at TIMESTAMP NOT NULL
                            DEFAULT CURRENT_TIMESTAMP,

                        PRIMARY KEY (id),

                        INDEX idx_shop_transactions_buyer (
                            buyer_uuid
                        ),

                        INDEX idx_shop_transactions_seller (
                            seller_uuid
                        )
                    ) ENGINE=InnoDB
                    """);
            }
        }
    }

    /**
     * 現在はDataSource方式なので、
     * 個別のConnectionを保持していない。
     *
     * そのためclose()は何もしない。
     *
     * plugin disable時に呼ばれても安全なように
     * 互換メソッドとして用意している。
     */
    public void close() {
        // MysqlDataSource自体を閉じる必要はない。
        // 各Connectionは try-with-resources で閉じる。
    }
}
