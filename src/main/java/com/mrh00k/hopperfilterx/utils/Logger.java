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

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

public class Logger {

  private FileConfiguration config;

  private boolean debugEnabled = false;

  private static class LoggerHolder {
    private static final Logger INSTANCE = new Logger();
  }

  public static Logger getInstance() {
    return LoggerHolder.INSTANCE;
  }

  public void initialize(Plugin plugin) {
    this.config = plugin.getConfig();

    loadDebugConfiguration();
  }

  private void loadDebugConfiguration() {
    if (config != null) {
      debugEnabled = config.getBoolean("debug.enabled", false);

      info(
          "Debug mode "
              + (debugEnabled ? "enabled" : "disabled")
              + " - configuration loaded successfully");
    } else {
      warning("Debug configuration could not be loaded - config is null, using default settings");
    }
  }

  public void reloadDebugConfiguration(Plugin plugin) {
    info("Reloading debug configuration from config.yml");

    if (plugin != null) {
      plugin.reloadConfig();

      this.config = plugin.getConfig();

      loadDebugConfiguration();
    }
  }

  public boolean isDebugEnabled() {
    return debugEnabled;
  }

  public void info(String message) {
    sendMessage(ChatColor.GRAY + message);
  }

  public void success(String message) {
    sendMessage(ChatColor.GREEN + message);
  }

  public void warning(String message) {
    sendMessage(ChatColor.YELLOW + message);
  }

  public void error(String message) {
    sendMessage(ChatColor.RED + message);
  }

  public void debug(String message) {
    if (debugEnabled) {
      sendMessage(ChatColor.AQUA + "[DEBUG] " + message);
    }
  }

  private void sendMessage(String message) {
    Bukkit.getConsoleSender().sendMessage("[HopperFilterX] " + message);
  }
}
