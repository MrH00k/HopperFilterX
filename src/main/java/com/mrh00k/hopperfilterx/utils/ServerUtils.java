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

public class ServerUtils {
  public static boolean isForceGamemodeEnabled() {
    java.io.File file = new java.io.File("server.properties");
    if (file.exists()) {
      java.util.Properties props = new java.util.Properties();
      try (java.io.FileInputStream fis = new java.io.FileInputStream(file)) {
        props.load(fis);
      } catch (java.io.IOException e) {
        com.mrh00k.hopperfilterx.utils.Logger.getInstance()
            .warning("Could not read server.properties for force-gamemode: " + e.getMessage());
      }
      String value = props.getProperty("force-gamemode");
      return value != null && value.equalsIgnoreCase("true");
    }
    return false;
  }
}
