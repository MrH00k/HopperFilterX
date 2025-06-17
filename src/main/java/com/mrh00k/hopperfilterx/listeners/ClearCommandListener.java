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
import com.mrh00k.hopperfilterx.utils.HopperUtils;
import com.mrh00k.hopperfilterx.utils.Logger;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitRunnable;

public class ClearCommandListener implements Listener {
  private final Logger logger = Logger.getInstance();

  private final NamespacedKey filteredHopperKey;

  public ClearCommandListener(Main plugin) {
    this.filteredHopperKey = new NamespacedKey(plugin, "filtered_hopper");
  }

  @EventHandler
  public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
    String message = event.getMessage().toLowerCase();

    if (!message.startsWith("/clear ")) return;

    String[] args = message.split(" ");

    if (args.length < 2) return;

    Player target = Bukkit.getPlayerExact(args[1]);

    if (target == null) {
      return;
    }

    boolean isHopperClear = false;

    for (int i = 2; i < args.length; i++) {
      if (args[i].equals("hopper") || args[i].equals("minecraft:hopper") || args[i].equals("*")) {
        isHopperClear = true;

        break;
      }
    }

    if (!isHopperClear) return;

    PlayerInventory inv = target.getInventory();

    List<ItemStack> filteredHoppers = new ArrayList<>();

    for (int i = 0; i < inv.getSize(); i++) {
      ItemStack item = inv.getItem(i);

      if (item != null
          && item.getType() == Material.HOPPER
          && HopperUtils.isFilteredHopper(item, filteredHopperKey)) {
        filteredHoppers.add(item.clone());

        inv.clear(i);
      }
    }

    target.updateInventory();

    new BukkitRunnable() {
      @Override
      public void run() {
        for (ItemStack filtered : filteredHoppers) {
          inv.addItem(filtered);
        }
        target.updateInventory();
      }
    }.runTaskLater(
        org.bukkit.plugin.java.JavaPlugin.getPlugin(com.mrh00k.hopperfilterx.Main.class), 1L);

    logger.debug("/clear: temporarily hid filtered hoppers for player: " + target.getName());
  }
}
