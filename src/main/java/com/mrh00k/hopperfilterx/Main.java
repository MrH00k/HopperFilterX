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
package com.mrh00k.hopperfilterx;

import com.mrh00k.hopperfilterx.commands.HopperCommand;
import com.mrh00k.hopperfilterx.listeners.GameModeChangeListener;
import com.mrh00k.hopperfilterx.listeners.HopperListener;
import com.mrh00k.hopperfilterx.managers.DatabaseManager;
import com.mrh00k.hopperfilterx.managers.MessageManager;
import com.mrh00k.hopperfilterx.utils.Logger;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

  private Logger logger;
  private String PluginVersion;
  private String PluginName;

  @Override
  public void onEnable() {
    try {
      logger = Logger.getInstance();

      PluginVersion = getPluginVersion();

      PluginName = getPluginName();

      logger.info(
          PluginName + " v" + PluginVersion + " is initializing - beginning startup sequence");

      // Always overwrite config.yml with the latest from the JAR
      saveResource("config.yml", true);
      // Always overwrite plugin.yml with the latest from the JAR
      saveResource("plugin.yml", true);
      reloadConfig();

      logger.info("Configuration file lang.yml checked and updated with new defaults");

      logger.info("Default configuration file saved");

      logger.initialize(this);

      logger.info(
          "Logger initialized (debug mode: "
              + (logger.isDebugEnabled() ? "enabled" : "disabled")
              + ")");

      MessageManager.getInstance(this);

      logger.info("MessageManager initialized and ready for localized messaging");
      try {
        DatabaseManager.initialize(this);

        logger.info("SQLite database initialized and ready");
      } catch (Exception e) {
        logger.error("Failed to initialize database: " + e.getMessage());
      }

      // Initialize VersionManager after database
      try {
        com.mrh00k.hopperfilterx.managers.VersionManager.initialize(this);
        logger.info("VersionManager initialized");
      } catch (Exception e) {
        logger.error("Failed to initialize VersionManager: " + e.getMessage());
      }

      registerCommands();

      registerListeners();

      logger.success(
          "Plugin "
              + PluginName
              + " v"
              + PluginVersion
              + " enabled successfully - all systems operational");

      logger.info("Startup sequence completed and plugin is operational");
    } catch (Exception e) {
      logger.error("Failed to enable plugin: " + e.getMessage());

      logger.error("Stack trace: " + java.util.Arrays.toString(e.getStackTrace()));

      logger.debug("Plugin enable failed - disabling plugin to prevent issues");

      getServer().getPluginManager().disablePlugin(this);
    }
  }

  @Override
  public void onDisable() {
    if (logger != null) {
      logger.info(
          "Shutting down " + PluginName + " v" + PluginVersion + " - beginning cleanup sequence");

      try {
        DatabaseManager.flushAndSync();
      } catch (Exception e) {
        logger.warning("Database flush/sync failed before shutdown: " + e.getMessage());
      }

      try {
        DatabaseManager.close();

        logger.info("SQLite database connection closed");
      } catch (Exception e) {
        logger.error("Failed to close database: " + e.getMessage());
      }

      logger.success("Plugin " + PluginName + " v" + PluginVersion + " disabled successfully");
    }
  }

  private void registerCommands() {
    try {
      logger.debug("Registering plugin commands - initializing HopperCommand");

      HopperCommand hopperCommand = new HopperCommand(this);

      PluginCommand hopperCmd = getCommand("hopper");
      if (hopperCmd != null) {
        hopperCmd.setExecutor(hopperCommand);
        hopperCmd.setTabCompleter(hopperCommand);
        logger.info(
            "Command registration completed - /hopper command is now available with tab completion");
      } else {
        logger.error("Command registration failed - 'hopper' command not found in plugin.yml");
      }
    } catch (Exception e) {
      logger.error(
          "Command registration failed - unable to initialize /hopper command: " + e.getMessage());

      logger.debug("Command registration error details: " + e.getClass().getSimpleName());

      throw new RuntimeException("Command registration failed", e);
    }
  }

  private void registerListeners() {
    try {
      logger.debug(
          "Registering event listeners - initializing HopperListener for block interactions");

      getServer().getPluginManager().registerEvents(new HopperListener(this), this);

      logger.info("HopperListener registered - hopper block events will now be handled");

      logger.debug(
          "Registering event listeners - initializing InventoryListener for item tracking");

      getServer()
          .getPluginManager()
          .registerEvents(new com.mrh00k.hopperfilterx.listeners.InventoryListener(this), this);

      logger.info("InventoryListener registered - hopper inventory events will now be handled");

      getServer()
          .getPluginManager()
          .registerEvents(new com.mrh00k.hopperfilterx.listeners.ClearCommandListener(this), this);

      logger.info(
          "ClearCommandListener registered - /clear command will not remove filtered hoppers");

      getServer().getPluginManager().registerEvents(new GameModeChangeListener(this), this);

      logger.info(
          "GameModeChangeListener registered - creative/survival hopper persistence enabled");
      getServer()
          .getPluginManager()
          .registerEvents(
              new com.mrh00k.hopperfilterx.listeners.HopperChestGuiListener(this), this);

      logger.info(
          "HopperChestGuiListener registered - hopper chest GUI interactions will now be handled");

      getServer()
          .getPluginManager()
          .registerEvents(new com.mrh00k.hopperfilterx.listeners.PlayerJoinListener(this), this);

      logger.info(
          "PlayerJoinListener registered - version update notifications will be sent to operators");

      logger.info(
          "Event listener registration completed - all hopper events will now be processed");
    } catch (Exception e) {
      logger.error(
          "Event listener registration failed - some hopper events may not be processed: "
              + e.getMessage());

      logger.debug("Listener registration error details: " + e.getClass().getSimpleName());

      throw new RuntimeException("Listener registration failed", e);
    }
  }

  public String getPluginVersion() {
    return getDescription().getVersion();
  }

  public String getPluginName() {
    return getDescription().getName();
  }
}
