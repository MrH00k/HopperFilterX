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

import com.mrh00k.hopperfilterx.managers.SoundManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class InventoryUtils {

  private static final Logger logger = Logger.getInstance();

  public static ItemStack getItemInHand(Player player) {
    if (player == null) {
      return null;
    }

    ItemStack handItem = player.getInventory().getItemInMainHand();

    return handItem.getType() == Material.AIR ? null : handItem.clone();
  }

  public static void setItemInHand(Player player, ItemStack item) {
    if (player == null) {
      return;
    }

    player.getInventory().setItemInMainHand(item);

    logger.debug(
        "Set item in hand for player "
            + player.getName()
            + ": "
            + (item != null ? item.getType().name() : "null"));
  }

  public static boolean dropItemAtPlayer(Player player, ItemStack item) {
    if (player == null || item == null || item.getType() == Material.AIR) {
      return false;
    }

    try {
      if (player.getWorld() != null) {
        player.getWorld().dropItemNaturally(player.getLocation(), item);

        SoundManager.playInventoryManagementSound(player, false);

        logger.debug(
            "Dropped item "
                + item.getType().name()
                + " at player "
                + player.getName()
                + "'s location with sound");

        return true;
      }
    } catch (Exception e) {
      logger.error("Failed to drop item at player location: " + e.getMessage());
    }

    return false;
  }

  public static boolean giveItemToPlayer(Player player, ItemStack item) {
    if (player == null) {
      logger.warning("Cannot give item to null player");

      return false;
    }

    if (item == null || item.getType() == Material.AIR) {
      logger.warning("Cannot give null or air item to player " + player.getName());

      return false;
    }

    try {
      logger.info(
          "Attempting to give "
              + item.getType().name()
              + " x"
              + item.getAmount()
              + " to player '"
              + player.getName()
              + "'");

      logger.debug(
          "Inventory state for player '"
              + player.getName()
              + "': "
              + getInventoryDebugInfo(player));

      PlayerInventory inventory = player.getInventory();

      java.util.HashMap<Integer, ItemStack> leftOver = inventory.addItem(item);

      if (leftOver.isEmpty()) {
        logger.success(
            "Added "
                + item.getType().name()
                + " x"
                + item.getAmount()
                + " to '"
                + player.getName()
                + "' inventory");

        SoundManager.playInventoryManagementSound(player, false);

        return true;
      }

      logger.warning(
          "Inventory full for player '" + player.getName() + "', handling leftover items");

      ItemStack leftoverItem = leftOver.values().iterator().next();

      ItemStack handItem = getItemInHand(player);

      setItemInHand(player, leftoverItem);

      if (handItem != null) {
        dropItemAtPlayer(player, handItem);

        SoundManager.playInventoryManagementSound(player, true);

        logger.info(
            "Replaced hand item with leftover and dropped old item for player '"
                + player.getName()
                + "'");
      } else {
        SoundManager.playInventoryManagementSound(player, false);

        logger.info("Placed leftover item in empty hand for player '" + player.getName() + "'");
      }

      return true;

    } catch (Exception e) {
      logger.error("Error giving item to player " + player.getName() + ": " + e.getMessage());
      return false;
    }
  }

  public static int getEmptySlotCount(Player player) {
    if (player == null) {
      return 0;
    }

    PlayerInventory inventory = player.getInventory();

    int emptySlots = 0;

    for (int i = 0; i < inventory.getSize(); i++) {
      ItemStack slot = inventory.getItem(i);

      if (slot == null || slot.getType() == Material.AIR) {
        emptySlots++;
      }
    }

    return emptySlots;
  }

  public static String getInventoryDebugInfo(Player player) {
    if (player == null) {
      return "Player is null";
    }

    int emptySlots = getEmptySlotCount(player);

    boolean isFull = isInventoryFull(player);

    ItemStack handItem = getItemInHand(player);

    return String.format(
        "Player %s: %d empty slots, full=%s, hand=%s",
        player.getName(),
        emptySlots,
        isFull,
        handItem != null ? handItem.getType().name() : "empty");
  }

  public static boolean isInventoryFull(Player player) {
    if (player == null) {
      return false;
    }

    PlayerInventory inventory = player.getInventory();

    if (inventory.firstEmpty() != -1) {
      return false;
    }

    for (int i = 0; i < inventory.getSize(); i++) {
      ItemStack slot = inventory.getItem(i);
      if (slot != null && slot.getAmount() < slot.getMaxStackSize()) {
        return false;
      }
    }

    return true;
  }
}
