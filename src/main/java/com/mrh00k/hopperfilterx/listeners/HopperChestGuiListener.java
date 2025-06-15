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

import com.mrh00k.hopperfilterx.managers.MessageManager;
import com.mrh00k.hopperfilterx.managers.SoundManager;
import com.mrh00k.hopperfilterx.utils.HopperUtils;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class HopperChestGuiListener implements Listener {
  private final NamespacedKey filteredHopperKey;

  public HopperChestGuiListener(JavaPlugin plugin) {
    this.filteredHopperKey = new NamespacedKey(plugin, "filtered_hopper");
  }

  @EventHandler
  public void onInventoryClick(InventoryClickEvent event) {
    if (event.getView().getTitle() == null
        || !event.getView().getTitle().contains("Filtered Hopper Chest")) {
      return;
    }

    ItemStack moving = null;

    if (event.getClickedInventory() == event.getView().getTopInventory()) {
      if (event.getClick() == org.bukkit.event.inventory.ClickType.NUMBER_KEY) {
        moving = event.getView().getBottomInventory().getItem(event.getHotbarButton());
      } else {
        moving = event.getCursor();
      }
    } else if (event.isShiftClick() && event.getWhoClicked() instanceof Player) {
      moving = event.getCurrentItem();
    }

    if (HopperUtils.isFilteredHopper(moving, filteredHopperKey)
        && HopperUtils.getUuidFromFilteredHopper(moving, filteredHopperKey) != null) {
      event.setCancelled(true);
      if (event.getWhoClicked() instanceof Player) {
        Player player = (Player) event.getWhoClicked();
        SoundManager.playErrorSound(player);
        player.sendMessage(MessageManager.getInstance().getMessage("hopper.filter-denied"));
      }
    }
  }
}
