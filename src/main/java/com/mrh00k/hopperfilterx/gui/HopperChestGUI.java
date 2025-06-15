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
package com.mrh00k.hopperfilterx.gui;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class HopperChestGUI {
  public static void open(Player player) {
    Inventory gui = Bukkit.createInventory(null, 27, "Filtered Hopper Chest");
    player.openInventory(gui);
  }

  public static void open(Player player, java.util.List<org.bukkit.inventory.ItemStack> items) {
    Inventory gui = Bukkit.createInventory(null, 27, "Filtered Hopper Chest");
    if (items != null) {
      for (int i = 0; i < Math.min(items.size(), 27); i++) {
        gui.setItem(i, items.get(i));
      }
    }
    player.openInventory(gui);
  }

  public static java.util.List<org.bukkit.inventory.ItemStack> getContents(
      org.bukkit.inventory.Inventory gui) {
    java.util.List<org.bukkit.inventory.ItemStack> contents = new java.util.ArrayList<>(27);
    for (int i = 0; i < 27; i++) {
      contents.add(gui.getItem(i));
    }
    return contents;
  }
}
