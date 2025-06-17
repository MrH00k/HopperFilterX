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
package com.mrh00k.hopperfilterx.utils;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class HopperUtils {

  public static ItemStack createFilteredHopper(NamespacedKey filteredHopperKey) {
    if (filteredHopperKey == null) {
      Logger.getInstance().error("Cannot create filtered hopper with null NamespacedKey");

      return new ItemStack(Material.HOPPER);
    }

    ItemStack hopper = new ItemStack(Material.HOPPER);

    ItemMeta meta = hopper.getItemMeta();

    if (meta != null) {
      meta.setDisplayName(ChatColor.GREEN + "Filtered Hopper");

      meta.setLore(
          java.util.Collections.singletonList(
              ChatColor.GRAY + "A hopper with filtering capabilities."));

      meta.getPersistentDataContainer().set(filteredHopperKey, PersistentDataType.BYTE, (byte) 1);

      if (!hopper.setItemMeta(meta)) {
        Logger.getInstance().error("Failed to set item meta for filtered hopper.");
      } else {
        Logger.getInstance().debug("Successfully created filtered hopper with metadata");
      }
    } else {
      Logger.getInstance().error("Failed to get item meta for filtered hopper - meta is null");
    }

    return hopper;
  }

  public static boolean isFilteredHopper(ItemStack item, NamespacedKey filteredHopperKey) {
    if (item == null || item.getType() != Material.HOPPER) {
      return false;
    }

    if (filteredHopperKey == null) {
      Logger.getInstance().warning("Cannot check filtered hopper status with null NamespacedKey");

      return false;
    }

    ItemMeta meta = item.getItemMeta();

    if (meta == null) {
      return false;
    }

    boolean isFiltered =
        meta.getPersistentDataContainer().has(filteredHopperKey, PersistentDataType.BYTE);

    Logger.getInstance()
        .debug("Checked hopper filter status: " + isFiltered + " for item " + item.getType());

    return isFiltered;
  }

  public static boolean isPlayerInSurvival(Player player) {
    if (player == null) {
      Logger.getInstance().warning("Cannot check game mode for null player");

      return false;
    }

    boolean isSurvival = player.getGameMode() == GameMode.SURVIVAL;

    Logger.getInstance()
        .debug(
            "Player "
                + player.getName()
                + " game mode check: "
                + player.getGameMode()
                + " (survival: "
                + isSurvival
                + ")");

    return isSurvival;
  }

  public static boolean isPlayerInCreative(Player player) {
    if (player == null) {
      Logger.getInstance().warning("Cannot check game mode for null player");

      return false;
    }

    boolean isCreative = player.getGameMode() == GameMode.CREATIVE;

    Logger.getInstance()
        .debug(
            "Player "
                + player.getName()
                + " game mode check: "
                + player.getGameMode()
                + " (creative: "
                + isCreative
                + ")");

    return isCreative;
  }

  public static ItemStack addUuidToFilteredHopper(
      ItemStack item, String uuid, String player, NamespacedKey filteredHopperKey) {
    if (item == null || uuid == null || filteredHopperKey == null) {
      Logger.getInstance().warning("Cannot add UUID to filtered hopper: null parameters");

      return item;
    }

    if (!isFilteredHopper(item, filteredHopperKey)) {
      Logger.getInstance().warning("Attempted to add UUID to non-filtered hopper");

      return item;
    }

    ItemMeta meta = item.getItemMeta();

    if (meta != null) {
      NamespacedKey uuidKey = new NamespacedKey(filteredHopperKey.getNamespace(), "uuid");

      meta.getPersistentDataContainer().set(uuidKey, PersistentDataType.STRING, uuid);

      List<String> lore = new ArrayList<>();

      lore.add(ChatColor.GRAY + "UUID: " + uuid);

      lore.add(ChatColor.GRAY + "Owner: " + player);

      meta.setLore(lore);

      if (!item.setItemMeta(meta)) {
        Logger.getInstance().error("Failed to set UUID metadata on filtered hopper");
      } else {
        Logger.getInstance().debug("Successfully added UUID " + uuid + " to filtered hopper item");
      }
    } else {
      Logger.getInstance().error("Failed to get item meta when adding UUID to filtered hopper");
    }

    return item;
  }

  public static String getUuidFromFilteredHopper(ItemStack item, NamespacedKey filteredHopperKey) {
    if (item == null || filteredHopperKey == null) {
      return null;
    }

    if (!isFilteredHopper(item, filteredHopperKey)) {
      return null;
    }

    ItemMeta meta = item.getItemMeta();

    if (meta != null) {
      NamespacedKey uuidKey = new NamespacedKey(filteredHopperKey.getNamespace(), "uuid");

      String uuid = meta.getPersistentDataContainer().get(uuidKey, PersistentDataType.STRING);

      Logger.getInstance()
          .debug("Extracted UUID from filtered hopper: " + (uuid != null ? uuid : "none"));

      return uuid;
    }

    return null;
  }

  // Returns true if the material is a block or entity with an inventory (chest, barrel, shulker,
  // etc.)
  // Updated to include all common inventory blocks/entities in Minecraft 1.14+
  public static boolean isInventoryBlock(Material material) {
    if (material == null) return false;
    // Chests
    if (material == Material.CHEST
        || material == Material.TRAPPED_CHEST
        || material == Material.ENDER_CHEST) return true;
    // Barrel
    if (material == Material.BARREL) return true;
    // Shulker boxes (normal y de color)
    if (material == Material.SHULKER_BOX || material.name().endsWith("_SHULKER_BOX")) return true;
    // Dispenser, Dropper
    if (material == Material.DISPENSER || material == Material.DROPPER) return true;
    // Furnace types
    if (material == Material.FURNACE
        || material == Material.BLAST_FURNACE
        || material == Material.SMOKER) return true;
    // Hopper
    if (material == Material.HOPPER) return true;
    // Brewing stand
    if (material == Material.BREWING_STAND) return true;
    // Beacon
    if (material == Material.BEACON) return true;
    // Cartography table, grindstone, smithing table, loom
    if (material == Material.CARTOGRAPHY_TABLE
        || material == Material.GRINDSTONE
        || material == Material.SMITHING_TABLE
        || material == Material.LOOM) return true;
    // Enchanting table
    if (material == Material.ENCHANTING_TABLE) return true;
    // Jukebox, Lectern
    if (material == Material.JUKEBOX || material == Material.LECTERN) return true;
    // Minecart containers (entities, but can be placed as items)
    if (material == Material.CHEST_MINECART
        || material == Material.HOPPER_MINECART
        || material == Material.FURNACE_MINECART) return true;
    // Add more as needed for future versions
    return false;
  }
}
