package si.f5.sakura_tk.sacra.economyv2.land;

import si.f5.sakura_tk.sacra.economyv2.database.DatabaseManager;

import java.sql.*;
import java.util.UUID;

public final class LandRepository {
    private final DatabaseManager database;

    public LandRepository(DatabaseManager database) {
        this.database = database;
    }

    public boolean create(UUID owner, String world,
                          int minX, int maxX, int minZ, int maxZ,
                          boolean nation) throws SQLException {
        try (Connection c = database.connection()) {
            c.setAutoCommit(false);

            try {
                try (PreparedStatement ps = c.prepareStatement("""
                    SELECT id
                    FROM ec_protected_lands
                    WHERE world_name = ?
                      AND min_x <= ? AND max_x >= ?
                      AND min_z <= ? AND max_z >= ?
                    LIMIT 1
                    FOR UPDATE
                    """)) {
                    ps.setString(1, world);
                    ps.setInt(2, maxX);
                    ps.setInt(3, minX);
                    ps.setInt(4, maxZ);
                    ps.setInt(5, minZ);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            c.rollback();
                            return false;
                        }
                    }
                }

                try (PreparedStatement ps = c.prepareStatement("""
                    INSERT INTO ec_protected_lands
                    (owner_uuid, world_name, min_x, max_x, min_z, max_z, is_nation)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """)) {
                    ps.setString(1, owner.toString());
                    ps.setString(2, world);
                    ps.setInt(3, minX);
                    ps.setInt(4, maxX);
                    ps.setInt(5, minZ);
                    ps.setInt(6, maxZ);
                    ps.setBoolean(7, nation);
                    ps.executeUpdate();
                }

                c.commit();
                return true;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }
}
