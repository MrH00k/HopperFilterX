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
package com.mrh00k.hopperfilterx.listeners;

import com.mrh00k.hopperfilterx.Main;
import com.mrh00k.hopperfilterx.managers.DatabaseManager;
import com.mrh00k.hopperfilterx.utils.HopperUtils;
import com.mrh00k.hopperfilterx.utils.Logger;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class InventoryListener implements Listener {
  private final Logger logger = Logger.getInstance();

  private final NamespacedKey filteredHopperKey;

  public InventoryListener(Main plugin) {
    this.filteredHopperKey = new NamespacedKey(plugin, "filtered_hopper");
  }

  @EventHandler
  public void onPlayerDropItem(PlayerDropItemEvent event) {
    try {
      ItemStack droppedItem = event.getItemDrop().getItemStack();

      if (HopperUtils.isFilteredHopper(droppedItem, filteredHopperKey)) {
        String uuid = HopperUtils.getUuidFromFilteredHopper(droppedItem, filteredHopperKey);

        if (uuid != null) {
          logger.debug(
              "Player "
                  + event.getPlayer().getName()
                  + " dropped filtered hopper with UUID "
                  + uuid);
        }
      }
    } catch (Exception e) {
      logger.error("Error handling player drop item event: " + e.getMessage());
    }
  }

  @EventHandler
  public void onInventoryClick(InventoryClickEvent event) {
    try {
      ItemStack currentItem = event.getCurrentItem();

      if (currentItem != null && HopperUtils.isFilteredHopper(currentItem, filteredHopperKey)) {
        String uuid = HopperUtils.getUuidFromFilteredHopper(currentItem, filteredHopperKey);

        if (uuid != null) {
          HumanEntity human = event.getWhoClicked();

          if (human instanceof Player) {
            Player player = (Player) human;

            ItemMeta meta = currentItem.getItemMeta();
            String owner = null;
            if (meta != null && meta.getLore() != null) {
              List<String> lore = meta.getLore();

              if (lore != null) {
                for (String line : lore) {
                  if (line != null) {
                    String plain = ChatColor.stripColor(line);
                    if (plain != null && plain.startsWith("Owner: ")) {
                      owner = plain.substring("Owner: ".length()).trim();
                      break;
                    }
                  }
                }
              }
            }

            if (owner != null && !owner.equals(player.getName())) {
              boolean hasPerm = false;
              try {
                hasPerm =
                    com.mrh00k.hopperfilterx.managers.DatabaseManager.hasHopperPermission(
                        owner, player.getName(), uuid);
              } catch (Exception e) {
                logger.error("Failed to check hopper permission: " + e.getMessage());
              }
              if (!hasPerm) {
                event.setCancelled(true);
                com.mrh00k.hopperfilterx.managers.SoundManager.playErrorSound(player);
                player.sendMessage(
                    com.mrh00k.hopperfilterx.managers.MessageManager.getInstance()
                        .getMessage("hopper.not-owner"));
                logger.debug(
                    "Player "
                        + player.getName()
                        + " tried to take filtered hopper UUID "
                        + uuid
                        + " owned by "
                        + owner
                        + " - denied");
                return;
              }
            }

            if (owner != null
                && owner.equals(player.getName())
                && HopperUtils.isPlayerInCreative(player)) {
              event.setCancelled(true);
              try {
                UUID playerId = player.getUniqueId();
                List<ItemStack> creativeHoppers = DatabaseManager.loadCreativeHoppers(playerId);
                if (creativeHoppers == null) creativeHoppers = new ArrayList<>();
                creativeHoppers = new ArrayList<>(creativeHoppers);
                creativeHoppers.add(currentItem.clone());
                DatabaseManager.saveCreativeHoppers(playerId, creativeHoppers);
                logger.debug(
                    "Saved creative hopper UUID " + uuid + " for player " + player.getName());
              } catch (SQLException e) {
                logger.error(
                    "Failed to save creative hopper for player "
                        + player.getName()
                        + ": "
                        + e.getMessage());
              }
              ItemStack defaultHopper = HopperUtils.createFilteredHopper(filteredHopperKey);
              player.getInventory().addItem(defaultHopper);

              org.bukkit.inventory.Inventory clickedInv = event.getClickedInventory();
              if (clickedInv != null) {
                clickedInv.setItem(event.getSlot(), null);
              }
              return;
            }
          }

          logger.debug(
              "Inventory interaction with filtered hopper UUID "
                  + uuid
                  + " by player "
                  + event.getWhoClicked().getName());
        }
      }
    } catch (Exception e) {
      logger.error("Error handling inventory click event: " + e.getMessage());
    }
  }

  @EventHandler
  public void onEntityPickupItem(EntityPickupItemEvent event) {
    if (!(event.getEntity() instanceof Player)) return;

    Player player = (Player) event.getEntity();

    ItemStack stack = event.getItem().getItemStack();

    if (!HopperUtils.isFilteredHopper(stack, filteredHopperKey)) return;

    String uuid = HopperUtils.getUuidFromFilteredHopper(stack, filteredHopperKey);

    if (uuid == null) return;

    String owner = null;

    ItemMeta meta = stack.getItemMeta();

    if (meta != null && meta.getLore() != null) {
      for (String line : meta.getLore()) {
        if (line != null) {
          String plain = ChatColor.stripColor(line);

          if (plain != null && plain.startsWith("Owner: ")) {
            owner = plain.substring("Owner: ".length()).trim();

            break;
          }
        }
      }
    }

    if (owner != null && !owner.equals(player.getName())) {
      boolean hasPerm = false;
      try {
        hasPerm =
            com.mrh00k.hopperfilterx.managers.DatabaseManager.hasHopperPermission(
                owner, player.getName(), uuid);
      } catch (Exception e) {
        logger.error("Failed to check hopper permission: " + e.getMessage());
      }
      if (!hasPerm) {
        event.setCancelled(true);

        logger.debug(
            "Player "
                + player.getName()
                + " tried to pick up filtered hopper UUID "
                + uuid
                + " owned by "
                + owner
                + " - denied");

        return;
      }
    }

    if (owner != null && owner.equals(player.getName()) && HopperUtils.isPlayerInCreative(player)) {
      event.setCancelled(true);

      try {
        UUID playerId = player.getUniqueId();

        List<ItemStack> creativeHoppers =
            new ArrayList<>(DatabaseManager.loadCreativeHoppers(playerId));

        creativeHoppers.add(stack.clone());

        DatabaseManager.saveCreativeHoppers(playerId, creativeHoppers);

        logger.debug("Saved creative hopper UUID " + uuid + " for player " + player.getName());
      } catch (SQLException e) {
        logger.error(
            "Failed to save creative hopper for player "
                + player.getName()
                + ": "
                + e.getMessage());
      }

      ItemStack defaultHopper = HopperUtils.createFilteredHopper(filteredHopperKey);

      player.getInventory().addItem(defaultHopper);

      event.getItem().remove();

      return;
    }
  }

  @EventHandler
  public void onInventoryClose(org.bukkit.event.inventory.InventoryCloseEvent event) {
    if (event.getPlayer() instanceof Player) {
      Player player = (Player) event.getPlayer();
      String title = event.getView().getTitle();
      if (title.contains("Filtered Hopper Chest")) {
        logger.debug("Filtered hopper GUI closed by player '" + player.getName() + "'");

        if (player.hasMetadata("filtered_hopper_uuid")) {
          String hopperId = null;
          try {
            hopperId = player.getMetadata("filtered_hopper_uuid").get(0).asString();
          } catch (Exception e) {
            logger.error("Failed to extract filtered hopper UUID from metadata: " + e.getMessage());
          }
          if (hopperId != null && !hopperId.isEmpty()) {
            org.bukkit.inventory.Inventory inv = event.getInventory();
            java.util.List<org.bukkit.inventory.ItemStack> contents =
                com.mrh00k.hopperfilterx.gui.HopperChestGUI.getContents(inv);
            try {
              com.mrh00k.hopperfilterx.managers.DatabaseManager.saveFilteredHopperItems(
                  hopperId, contents);
              logger.debug(
                  "Saved filtered hopper items for UUID "
                      + hopperId
                      + " ("
                      + player.getName()
                      + ")");
            } catch (java.sql.SQLException e) {
              logger.error(
                  "Failed to save filtered hopper items for UUID "
                      + hopperId
                      + ": "
                      + e.getMessage());
            }
          }

          com.mrh00k.hopperfilterx.managers.SoundManager.playChestCloseSound(
              player, player.getLocation());

          player.removeMetadata(
              "filtered_hopper_uuid",
              com.mrh00k.hopperfilterx.Main.getPlugin(com.mrh00k.hopperfilterx.Main.class));
        }
      }
    }
  }
}
