package si.f5.sakura_tk.sacra.economyv2.shop;

import si.f5.sakura_tk.sacra.economyv2.database.DatabaseManager;

import java.math.BigDecimal;
import java.sql.*;
import java.util.UUID;

public final class ShopRepository {
    public record Auction(int id, UUID seller, String material, int amount, BigDecimal price) {}

    private final DatabaseManager database;

    public ShopRepository(DatabaseManager database) {
        this.database = database;
    }

    public DatabaseManager database() {
        return database;
    }

    public void createAuction(UUID seller, String material, int amount, BigDecimal price)
            throws SQLException {
        try (Connection c = database.connection();
             PreparedStatement ps = c.prepareStatement("""
                 INSERT INTO ec_shop_auctions
                 (seller_uuid, material_name, amount, price, created_at, is_active)
                 VALUES (?, ?, ?, ?, ?, 1)
                 """)) {
            ps.setString(1, seller.toString());
            ps.setString(2, material);
            ps.setInt(3, amount);
            ps.setBigDecimal(4, price);
            ps.setLong(5, System.currentTimeMillis());
            ps.executeUpdate();
        }
    }

    public Auction findCheapestLocked(Connection c, String material) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
            SELECT id, seller_uuid, material_name, amount, price
            FROM ec_shop_auctions
            WHERE material_name = ? AND is_active = 1
            ORDER BY price ASC, id ASC
            LIMIT 1
            FOR UPDATE
            """)) {
            ps.setString(1, material);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return new Auction(
                    rs.getInt("id"),
                    UUID.fromString(rs.getString("seller_uuid")),
                    rs.getString("material_name"),
                    rs.getInt("amount"),
                    rs.getBigDecimal("price")
                );
            }
        }
    }

    public boolean deactivateAndRecord(Connection c, Auction a, UUID buyer)
            throws SQLException {
        try (PreparedStatement update = c.prepareStatement("""
                UPDATE ec_shop_auctions
                SET is_active = 0
                WHERE id = ? AND is_active = 1
                """);
             PreparedStatement insert = c.prepareStatement("""
                INSERT INTO ec_shop_transactions
                (auction_id, seller_uuid, buyer_uuid, material_name, amount, price, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """)) {

            update.setInt(1, a.id());
            if (update.executeUpdate() != 1) return false;

            insert.setInt(1, a.id());
            insert.setString(2, a.seller().toString());
            insert.setString(3, buyer.toString());
            insert.setString(4, a.material());
            insert.setInt(5, a.amount());
            insert.setBigDecimal(6, a.price());
            insert.setLong(7, System.currentTimeMillis());
            insert.executeUpdate();
            return true;
        }
    }
}
