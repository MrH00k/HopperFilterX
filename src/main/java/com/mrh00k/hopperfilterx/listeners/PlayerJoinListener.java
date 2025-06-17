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
import com.mrh00k.hopperfilterx.managers.VersionManager;
import com.mrh00k.hopperfilterx.utils.Logger;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class PlayerJoinListener implements Listener {

  private final Logger logger = Logger.getInstance();

  public PlayerJoinListener(Main plugin) {
    // Constructor que recibe el plugin para consistencia con otros listeners
  }

  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();

    // Solo notificar a operadores sobre actualizaciones disponibles
    if (player.isOp()) {
      // Delay para asegurar que el jugador esté completamente cargado
      new BukkitRunnable() {
        @Override
        public void run() {
          try {
            VersionManager versionManager = VersionManager.getInstance();
            // Usar el nuevo método con callback para notificar justo cuando termine la consulta
            versionManager.forceCheck(
                () -> {
                  try {
                    versionManager.notifyPlayerAboutUpdate(player);
                    logger.debug(
                        "Version update check completed for operator: " + player.getName());
                  } catch (Exception e) {
                    logger.error(
                        "Failed to check version update for player "
                            + player.getName()
                            + ": "
                            + e.getMessage());
                  }
                });
          } catch (Exception e) {
            logger.error(
                "Failed to trigger version check for player "
                    + player.getName()
                    + ": "
                    + e.getMessage());
          }
        }
      }.runTaskLater(org.bukkit.plugin.java.JavaPlugin.getPlugin(Main.class), 20L); // Wait 1 second
    }
  }
}
