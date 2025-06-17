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
import com.mrh00k.hopperfilterx.utils.InventoryUtils;
import com.mrh00k.hopperfilterx.utils.Logger;
import com.mrh00k.hopperfilterx.utils.ServerUtils;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.bukkit.GameMode;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.inventory.ItemStack;

public class GameModeChangeListener implements Listener {
  private final NamespacedKey filteredHopperKey;

  public GameModeChangeListener(Main plugin) {
    this.filteredHopperKey = new NamespacedKey(plugin, "filtered_hopper");
  }

  @EventHandler
  public void onGameModeChange(PlayerGameModeChangeEvent event) {
    Player player = event.getPlayer();
    GameMode oldMode = player.getGameMode();
    GameMode newMode = event.getNewGameMode();

    boolean forceGamemode = ServerUtils.isForceGamemodeEnabled();
    if ((forceGamemode && newMode == GameMode.CREATIVE)
        || (!forceGamemode && oldMode != GameMode.CREATIVE && newMode == GameMode.CREATIVE)) {
      UUID playerId = player.getUniqueId();
      List<ItemStack> originalHoppers = new ArrayList<>();
      for (int i = 0; i < player.getInventory().getSize(); i++) {
        ItemStack item = player.getInventory().getItem(i);
        if (HopperUtils.isFilteredHopper(item, filteredHopperKey)
            && HopperUtils.getUuidFromFilteredHopper(item, filteredHopperKey) != null) {
          originalHoppers.add(item.clone());

          player.getInventory().setItem(i, null);
        }
      }
      if (!originalHoppers.isEmpty()) {
        try {
          DatabaseManager.saveCreativeHoppers(playerId, originalHoppers);
        } catch (SQLException e) {
          Logger.getInstance()
              .error("Failed to save creative hoppers for " + playerId + ": " + e.getMessage());
        }

        ItemStack genericHopper = HopperUtils.createFilteredHopper(filteredHopperKey);

        genericHopper.setAmount(1);

        InventoryUtils.giveItemToPlayer(player, genericHopper);
      }
    } else if (oldMode == GameMode.CREATIVE && newMode == GameMode.SURVIVAL) {
      UUID playerId = player.getUniqueId();

      List<ItemStack> saved;

      try {
        saved = DatabaseManager.loadCreativeHoppers(playerId);
      } catch (SQLException e) {
        Logger.getInstance()
            .error("Failed to load creative hoppers for " + playerId + ": " + e.getMessage());
        saved = Collections.emptyList();
      }

      if (saved != null && !saved.isEmpty()) {
        List<ItemStack> toRestore = new ArrayList<>();
        List<ItemStack> toDelete = new ArrayList<>();
        for (ItemStack orig : saved) {
          String uuid = HopperUtils.getUuidFromFilteredHopper(orig, filteredHopperKey);
          if (uuid != null && DatabaseManager.filteredHopperExists(uuid)) {
            toRestore.add(orig);
          } else {
            toDelete.add(orig);
          }
        }

        for (int i = 0; i < player.getInventory().getSize(); i++) {
          ItemStack item = player.getInventory().getItem(i);
          if (HopperUtils.isFilteredHopper(item, filteredHopperKey)
              && HopperUtils.getUuidFromFilteredHopper(item, filteredHopperKey) == null) {
            player.getInventory().setItem(i, null);
          }
        }

        for (ItemStack orig : toRestore) {
          InventoryUtils.giveItemToPlayer(player, orig);
        }

        if (!toDelete.isEmpty()) {
          try {
            List<ItemStack> newList = new ArrayList<>(toRestore);
            DatabaseManager.saveCreativeHoppers(playerId, newList);
          } catch (SQLException e) {
            Logger.getInstance()
                .error("Failed to update creative hoppers for " + playerId + ": " + e.getMessage());
          }
        } else {
          try {
            DatabaseManager.deleteCreativeHoppers(playerId);
          } catch (SQLException e) {
            Logger.getInstance()
                .error("Failed to delete creative hoppers for " + playerId + ": " + e.getMessage());
          }
        }
      }
    }
  }
}
