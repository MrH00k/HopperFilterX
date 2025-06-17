/*
 * This file is part of HopperFilterX.
 *
 * Copyright (C) 2025 MrH00k <https://github.com/MrH00k>
 *
 * HopperFilterX is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 only,
 * as published by the Free Software Foundation.
 *
 * HopperFilterX is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License version 3
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package com.mrh00k.hopperfilterx.managers;

import com.mrh00k.hopperfilterx.utils.Logger;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public class DatabaseManager {
  private static Connection connection;
  private static final int CURRENT_DB_VERSION = 1;

  public static void initialize(Plugin plugin) throws SQLException {
    File dataFolder = plugin.getDataFolder();

    if (!dataFolder.exists()) {
      boolean created = dataFolder.mkdirs();

      if (!created) {
        Logger.getInstance()
            .warning("Failed to create data folder: " + dataFolder.getAbsolutePath());
      }
    }

    File dbFile = new File(dataFolder, "data.db");

    connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());

    // Disable auto-commit for transactional migrations
    connection.setAutoCommit(false);

    // Apply schema migrations if needed
    applyMigrations();

    // Restore auto-commit mode
    connection.setAutoCommit(true);

    // Set PRAGMA synchronous only after auto-commit is restored (outside transaction)
    Statement stmt = connection.createStatement();
    try {
      stmt.execute("PRAGMA synchronous = FULL");

      stmt.executeUpdate(
          "CREATE TABLE IF NOT EXISTS filtered_hoppers ("
              + "id TEXT PRIMARY KEY, "
              + "world TEXT NOT NULL, "
              + "chunk_x INTEGER NOT NULL, "
              + "chunk_z INTEGER NOT NULL, "
              + "x INTEGER NOT NULL, "
              + "y INTEGER NOT NULL, "
              + "z INTEGER NOT NULL, "
              + "owner TEXT NOT NULL, "
              + "is_placed INTEGER DEFAULT 1 NOT NULL, "
              + "items TEXT"
              + ")");

      stmt.executeUpdate(
          "CREATE TABLE IF NOT EXISTS creative_hoppers ("
              + "player_uuid TEXT PRIMARY KEY, "
              + "items TEXT NOT NULL"
              + ")");

      stmt.executeUpdate(
          "CREATE TABLE IF NOT EXISTS hopper_permissions ("
              + "owner TEXT NOT NULL, "
              + "permitted TEXT NOT NULL, "
              + "hopper_uuid TEXT, "
              + "PRIMARY KEY (owner, permitted, hopper_uuid)"
              + ")");
    } finally {
      if (stmt != null) {
        stmt.close();
      }
    }
  }

  private static void applyMigrations() throws SQLException {
    int version = getUserVersion();
    if (version < CURRENT_DB_VERSION) {
      // Temporarily enable auto-commit to allow PRAGMA changes
      boolean prevAutoCommit = connection.getAutoCommit();
      connection.setAutoCommit(true);
      Statement pragmaStmt = connection.createStatement();
      try {
        pragmaStmt.execute("PRAGMA synchronous = FULL");
      } finally {
        if (pragmaStmt != null) {
          pragmaStmt.close();
        }
      }
      connection.setAutoCommit(prevAutoCommit);
      // Now run migrations inside the transaction
      Statement migrationStmt = connection.createStatement();
      try {
        migrationStmt.executeUpdate(
            "CREATE TABLE IF NOT EXISTS filtered_hoppers ("
                + "id TEXT PRIMARY KEY, "
                + "world TEXT NOT NULL, "
                + "chunk_x INTEGER NOT NULL, "
                + "chunk_z INTEGER NOT NULL, "
                + "x INTEGER NOT NULL, "
                + "y INTEGER NOT NULL, "
                + "z INTEGER NOT NULL, "
                + "owner TEXT NOT NULL, "
                + "is_placed INTEGER DEFAULT 1 NOT NULL, "
                + "items TEXT"
                + ")");
        migrationStmt.executeUpdate(
            "CREATE TABLE IF NOT EXISTS creative_hoppers ("
                + "player_uuid TEXT PRIMARY KEY, "
                + "items TEXT NOT NULL"
                + ")");
        migrationStmt.executeUpdate(
            "CREATE TABLE IF NOT EXISTS hopper_permissions ("
                + "owner TEXT NOT NULL, "
                + "permitted TEXT NOT NULL, "
                + "hopper_uuid TEXT, "
                + "PRIMARY KEY (owner, permitted, hopper_uuid)"
                + ")");
        migrationStmt.execute("PRAGMA user_version = " + CURRENT_DB_VERSION);
      } finally {
        if (migrationStmt != null) {
          migrationStmt.close();
        }
      }
      connection.commit();
    }
  }

  private static int getUserVersion() throws SQLException {
    Statement stmt = null;
    ResultSet rs = null;
    try {
      stmt = connection.createStatement();
      rs = stmt.executeQuery("PRAGMA user_version");
      if (rs.next()) {
        return rs.getInt(1);
      }
      return 0;
    } finally {
      if (rs != null) {
        try {
          rs.close();
        } catch (SQLException e) {
          // Log but don't throw to avoid suppressing the original exception
        }
      }
      if (stmt != null) {
        try {
          stmt.close();
        } catch (SQLException e) {
          // Log but don't throw to avoid suppressing the original exception
        }
      }
    }
  }

  public static void close() throws SQLException {
    if (connection != null && !connection.isClosed()) {
      connection.close();
    }
  }

  public static class HopperData {
    private final String id;
    private final Location location;
    private final String owner;
    private boolean isPlaced;

    public HopperData(String id, Location location, String owner) {
      this(id, location, owner, true);
    }

    public HopperData(String id, Location location, String owner, boolean isPlaced) {
      this.id = id;

      this.location = location.clone();

      this.owner = owner;

      this.isPlaced = isPlaced;
    }

    public String getId() {
      return id;
    }

    public Location getLocation() {
      return location.clone();
    }

    public String getOwner() {
      return owner;
    }

    public boolean isPlaced() {
      return isPlaced;
    }

    public void setPlaced(boolean placed) {
      this.isPlaced = placed;
    }
  }

  public static List<HopperData> loadAllHopperData() throws SQLException {
    List<HopperData> entries = new ArrayList<>();

    String query =
        "SELECT id, world, x, y, z, owner, COALESCE(is_placed, 1) as is_placed FROM filtered_hoppers";

    Statement stmt = null;
    ResultSet rs = null;
    try {
      stmt = connection.createStatement();
      rs = stmt.executeQuery(query);
      while (rs.next()) {
        String id = rs.getString("id");

        String world = rs.getString("world");

        int x = rs.getInt("x");

        int y = rs.getInt("y");

        int z = rs.getInt("z");

        String owner = rs.getString("owner");

        boolean isPlaced = rs.getInt("is_placed") == 1;

        if (Bukkit.getWorld(world) != null) {
          Location loc = new Location(Bukkit.getWorld(world), x, y, z);

          entries.add(new HopperData(id, loc, owner, isPlaced));
        }
      }
      return entries;
    } finally {
      if (rs != null) {
        try {
          rs.close();
        } catch (SQLException e) {
          // Log but don't throw to avoid suppressing the original exception
        }
      }
      if (stmt != null) {
        try {
          stmt.close();
        } catch (SQLException e) {
          // Log but don't throw to avoid suppressing the original exception
        }
      }
    }
  }

  public static String insertFilteredHopper(Location location, String owner) {
    String uuid = java.util.UUID.randomUUID().toString();

    String sql =
        "INSERT INTO filtered_hoppers(id, world, chunk_x, chunk_z, x, y, z, owner) VALUES(?, ?, ?, ?, ?, ?, ?, ?)";
    try (PreparedStatement ps = connection.prepareStatement(sql)) {
      org.bukkit.World world = location.getWorld();
      if (world == null) {
        Logger.getInstance().error("insertFilteredHopper called with null world; operation failed");
        return null;
      }

      String worldName = world.getName();

      int chunkX = location.getBlockX() >> 4;

      int chunkZ = location.getBlockZ() >> 4;
      ps.setString(1, uuid);

      ps.setString(2, worldName);

      ps.setInt(3, chunkX);

      ps.setInt(4, chunkZ);

      ps.setInt(5, location.getBlockX());

      ps.setInt(6, location.getBlockY());

      ps.setInt(7, location.getBlockZ());

      ps.setString(8, owner);

      ps.executeUpdate();

      return uuid;
    } catch (SQLException e) {
      Logger.getInstance()
          .error("Failed to insert filtered hopper into database: " + e.getMessage());
    }

    return null;
  }

  public static void deleteFilteredHopper(String id) {
    // Remove permissions for this hopper before deleting
    removeAllPermissionsForHopper(id);
    String owner = null;
    try (PreparedStatement ps =
        connection.prepareStatement("SELECT owner FROM filtered_hoppers WHERE id = ?")) {
      ps.setString(1, id);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          owner = rs.getString("owner");
        }
      }
    } catch (SQLException e) {
      Logger.getInstance().error("Failed to fetch owner for hopper " + id + ": " + e.getMessage());
    }
    String sql = "DELETE FROM filtered_hoppers WHERE id = ?";
    try (PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setString(1, id);
      ps.executeUpdate();
    } catch (SQLException e) {
      Logger.getInstance()
          .error("Failed to delete filtered hopper from database: " + e.getMessage());
    }
    // After deletion, check if owner has any more hoppers. If not, remove all global permissions
    // for owner
    if (owner != null) {
      boolean hasMore = false;
      try (PreparedStatement ps =
          connection.prepareStatement("SELECT 1 FROM filtered_hoppers WHERE owner = ? LIMIT 1")) {
        ps.setString(1, owner);
        try (ResultSet rs = ps.executeQuery()) {
          if (rs.next()) {
            hasMore = true;
          }
        }
      } catch (SQLException e) {
        Logger.getInstance()
            .error("Failed to check remaining hoppers for owner " + owner + ": " + e.getMessage());
      }
      if (!hasMore) {
        removeAllGlobalPermissionsForOwner(owner);
      }
    }
  }

  public static void markHopperAsNotPlaced(String id) {
    String sql = "UPDATE filtered_hoppers SET is_placed = 0 WHERE id = ?";

    try (PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setString(1, id);

      int rowsUpdated = ps.executeUpdate();

      if (rowsUpdated > 0) {
        Logger.getInstance().debug("Marked hopper " + id + " as not placed in database");
      } else {
        Logger.getInstance().warning("No hopper found with id " + id + " to mark as not placed");
      }
    } catch (SQLException e) {
      Logger.getInstance().error("Failed to mark filtered hopper as not placed: " + e.getMessage());
    }
  }

  public static void markHopperAsPlaced(String id, Location location) {
    String sql =
        "UPDATE filtered_hoppers SET is_placed = 1, world = ?, chunk_x = ?, chunk_z = ?, x = ?, y = ?, z = ? WHERE id = ?";

    try (PreparedStatement ps = connection.prepareStatement(sql)) {
      org.bukkit.World world = location.getWorld();
      if (world == null) {
        Logger.getInstance().error("markHopperAsPlaced called with null world; operation failed");
        return;
      }

      ps.setString(1, world.getName());

      ps.setInt(2, location.getBlockX() >> 4);

      ps.setInt(3, location.getBlockZ() >> 4);

      ps.setInt(4, location.getBlockX());

      ps.setInt(5, location.getBlockY());

      ps.setInt(6, location.getBlockZ());

      ps.setString(7, id);

      int rowsUpdated = ps.executeUpdate();

      if (rowsUpdated > 0) {
        Logger.getInstance()
            .debug(
                "Marked hopper "
                    + id
                    + " as placed and updated position in database to "
                    + location);
      } else {
        Logger.getInstance()
            .warning("No hopper found with id " + id + " to mark as placed and update position");
      }
    } catch (SQLException e) {
      Logger.getInstance()
          .error("Failed to mark filtered hopper as placed and update position: " + e.getMessage());
    }
  }

  public static void saveCreativeHoppers(UUID playerId, List<ItemStack> items) throws SQLException {
    YamlConfiguration tmp = new YamlConfiguration();

    tmp.set("items", items);

    String serialized = tmp.saveToString();

    String sql = "INSERT OR REPLACE INTO creative_hoppers(player_uuid, items) VALUES(?, ?)";

    try (PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setString(1, playerId.toString());

      ps.setString(2, serialized);

      ps.executeUpdate();
    }
  }

  public static List<ItemStack> loadCreativeHoppers(UUID playerId) throws SQLException {
    String sql = "SELECT items FROM creative_hoppers WHERE player_uuid = ?";

    try (PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setString(1, playerId.toString());

      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          String serialized = rs.getString("items");

          YamlConfiguration tmp = new YamlConfiguration();

          try {
            tmp.loadFromString(serialized);
          } catch (InvalidConfigurationException e) {
            Logger.getInstance()
                .error("Failed to parse creative hoppers for " + playerId + ": " + e.getMessage());
            return Collections.emptyList();
          }

          List<?> raw = tmp.getList("items");

          List<ItemStack> list = new ArrayList<>();

          if (raw != null) {
            for (Object obj : raw) {
              if (obj instanceof ItemStack) {
                list.add((ItemStack) obj);
              }
            }
          }

          return list;
        }
      }
    }

    return Collections.emptyList();
  }

  public static void deleteCreativeHoppers(UUID playerId) throws SQLException {
    String sql = "DELETE FROM creative_hoppers WHERE player_uuid = ?";

    try (PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setString(1, playerId.toString());

      ps.executeUpdate();
    }
  }

  public static void flushAndSync() {
    if (connection != null) {
      Statement stmt = null;
      try {
        stmt = connection.createStatement();
        stmt.execute("PRAGMA synchronous = FULL");

        stmt.execute("PRAGMA wal_checkpoint(FULL)");

        if (!connection.getAutoCommit()) {
          connection.commit();
        }

        Logger.getInstance()
            .debug(
                "Database flush and sync completed (PRAGMA synchronous=FULL, checkpoint, commit)");
      } catch (Exception e) {
        Logger.getInstance().warning("Database flush/sync failed: " + e.getMessage());
      } finally {
        if (stmt != null) {
          try {
            stmt.close();
          } catch (SQLException e) {
            Logger.getInstance().warning("Failed to close statement: " + e.getMessage());
          }
        }
      }
    }
  }

  public static boolean filteredHopperExists(String uuid) {
    String sql = "SELECT 1 FROM filtered_hoppers WHERE id = ? LIMIT 1";
    try (PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setString(1, uuid);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next();
      }
    } catch (SQLException e) {
      Logger.getInstance().error("Failed to check existence of filtered hopper: " + e.getMessage());
      return false;
    }
  }

  public static void saveFilteredHopperItems(String hopperId, List<ItemStack> items)
      throws SQLException {
    YamlConfiguration tmp = new YamlConfiguration();
    tmp.set("items", items);
    String serialized = tmp.saveToString();
    String sql = "UPDATE filtered_hoppers SET items = ? WHERE id = ?";
    try (PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setString(1, serialized);
      ps.setString(2, hopperId);
      ps.executeUpdate();
    }
  }

  public static List<ItemStack> loadFilteredHopperItems(String hopperId) throws SQLException {
    String sql = "SELECT items FROM filtered_hoppers WHERE id = ?";
    try (PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setString(1, hopperId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          String serialized = rs.getString("items");
          if (serialized == null || serialized.isEmpty()) {
            return Collections.emptyList();
          }
          YamlConfiguration tmp = new YamlConfiguration();
          try {
            tmp.loadFromString(serialized);
          } catch (InvalidConfigurationException e) {
            Logger.getInstance()
                .error(
                    "Failed to parse filtered hopper items for "
                        + hopperId
                        + ": "
                        + e.getMessage());
            return Collections.emptyList();
          }
          List<?> raw = tmp.getList("items");
          List<ItemStack> list = new ArrayList<>();
          if (raw != null) {
            for (Object obj : raw) {
              if (obj instanceof ItemStack) {
                list.add((ItemStack) obj);
              }
            }
          }
          return list;
        }
      }
    }
    return Collections.emptyList();
  }

  // Permisos de acceso a filtros
  public static boolean addHopperPermission(String owner, String permitted, String hopperUuid)
      throws SQLException {
    String sql =
        "INSERT OR IGNORE INTO hopper_permissions(owner, permitted, hopper_uuid) VALUES (?, ?, ?)";
    try (PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setString(1, owner);
      ps.setString(2, permitted);
      ps.setString(3, hopperUuid);
      return ps.executeUpdate() > 0;
    }
  }

  public static boolean hasHopperPermission(String owner, String permitted, String hopperUuid)
      throws SQLException {
    String sql =
        "SELECT 1 FROM hopper_permissions WHERE owner = ? AND permitted = ? AND (hopper_uuid = ? OR hopper_uuid IS NULL) LIMIT 1";
    try (PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setString(1, owner);
      ps.setString(2, permitted);
      ps.setString(3, hopperUuid);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next();
      }
    } catch (SQLException e) {
      Logger.getInstance().error("Failed to check hopper permission: " + e.getMessage());
      return false;
    }
  }

  public static boolean hasAnyHopperPermission(String owner, String permitted) throws SQLException {
    String sql =
        "SELECT 1 FROM hopper_permissions WHERE owner = ? AND permitted = ? AND hopper_uuid IS NULL LIMIT 1";
    try (PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setString(1, owner);
      ps.setString(2, permitted);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next();
      }
    } catch (SQLException e) {
      Logger.getInstance().error("Failed to check any hopper permission: " + e.getMessage());
      return false;
    }
  }

  public static List<String> getPermittedPlayers(String owner, String hopperUuid)
      throws SQLException {
    List<String> permitted = new ArrayList<>();
    String sql =
        "SELECT permitted FROM hopper_permissions WHERE owner = ? AND (hopper_uuid = ? OR hopper_uuid IS NULL)";
    try (PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setString(1, owner);
      ps.setString(2, hopperUuid);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          permitted.add(rs.getString("permitted"));
        }
      }
    } catch (SQLException e) {
      Logger.getInstance().error("Failed to get permitted players: " + e.getMessage());
    }
    return permitted;
  }

  // Remover permisos de acceso a filtros
  public static boolean removeHopperPermission(String owner, String permitted, String hopperUuid)
      throws SQLException {
    String sql;
    if (hopperUuid == null) {
      sql =
          "DELETE FROM hopper_permissions WHERE owner = ? AND permitted = ? AND hopper_uuid IS NULL";
    } else {
      sql = "DELETE FROM hopper_permissions WHERE owner = ? AND permitted = ? AND hopper_uuid = ?";
    }
    try (PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setString(1, owner);
      ps.setString(2, permitted);
      if (hopperUuid != null) {
        ps.setString(3, hopperUuid);
      }
      return ps.executeUpdate() > 0;
    }
  }

  public static void removeAllPermissionsForHopper(String hopperUuid) {
    String sql = "DELETE FROM hopper_permissions WHERE hopper_uuid = ?";
    try (PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setString(1, hopperUuid);
      int removed = ps.executeUpdate();
      Logger.getInstance()
          .debug("Removed " + removed + " permissions for hopper UUID " + hopperUuid);
    } catch (SQLException e) {
      Logger.getInstance()
          .error(
              "Failed to remove permissions for hopper UUID " + hopperUuid + ": " + e.getMessage());
    }
  }

  // Remove all global permissions for an owner (hopper_uuid IS NULL)
  public static void removeAllGlobalPermissionsForOwner(String owner) {
    String sql = "DELETE FROM hopper_permissions WHERE owner = ? AND hopper_uuid IS NULL";
    try (PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setString(1, owner);
      int removed = ps.executeUpdate();
      Logger.getInstance().debug("Removed " + removed + " global permissions for owner " + owner);
    } catch (SQLException e) {
      Logger.getInstance()
          .error("Failed to remove global permissions for owner " + owner + ": " + e.getMessage());
    }
  }

  // UUID validation methods for command validation

  /**
   * Checks if a UUID exists in the filtered_hoppers table
   *
   * @param uuid The UUID to check
   * @return true if the UUID exists, false otherwise
   */
  public static boolean uuidExists(String uuid) {
    String sql = "SELECT 1 FROM filtered_hoppers WHERE id = ? LIMIT 1";

    try (PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setString(1, uuid);

      try (ResultSet rs = ps.executeQuery()) {
        boolean exists = rs.next();
        Logger.getInstance().debug("UUID " + uuid + " exists in database: " + exists);
        return exists;
      }
    } catch (SQLException e) {
      Logger.getInstance()
          .error("Failed to check UUID existence for " + uuid + ": " + e.getMessage());
      return false;
    }
  }

  /**
   * Checks if a UUID belongs to a specific player
   *
   * @param uuid The UUID to check
   * @param playerName The player name to verify ownership
   * @return true if the UUID belongs to the player, false otherwise
   */
  public static boolean uuidBelongsToPlayer(String uuid, String playerName) {
    String sql = "SELECT owner FROM filtered_hoppers WHERE id = ? LIMIT 1";

    try (PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setString(1, uuid);

      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          String owner = rs.getString("owner");
          boolean belongsToPlayer = playerName.equals(owner);
          Logger.getInstance()
              .debug(
                  "UUID "
                      + uuid
                      + " belongs to player "
                      + playerName
                      + ": "
                      + belongsToPlayer
                      + " (actual owner: "
                      + owner
                      + ")");
          return belongsToPlayer;
        } else {
          Logger.getInstance().debug("UUID " + uuid + " not found in database");
          return false;
        }
      }
    } catch (SQLException e) {
      Logger.getInstance()
          .error("Failed to check UUID ownership for " + uuid + ": " + e.getMessage());
      return false;
    }
  }
}
