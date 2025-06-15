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
package com.mrh00k.hopperfilterx.managers;

import com.mrh00k.hopperfilterx.utils.Logger;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.Location;

public class ChunkHopperManager {

  private final Map<Long, Set<DatabaseManager.HopperData>> chunkHoppers = new ConcurrentHashMap<>();

  private final Map<String, Integer> worldHashCache = new ConcurrentHashMap<>();

  private final Map<Location, DatabaseManager.HopperData> hopperLookup = new ConcurrentHashMap<>();

  private final Map<Location, Long> chunkKeyCache = new ConcurrentHashMap<>();

  private final AtomicInteger worldHashCounter = new AtomicInteger(0);

  private final Logger logger = Logger.getInstance();

  public void addFilteredHopper(DatabaseManager.HopperData data) {
    Location location = data.getLocation();

    if (location == null || location.getWorld() == null) {
      logger.warning("Attempted to add filtered hopper with null location or world");

      return;
    }

    long chunkKey = getOptimizedChunkKey(location);

    chunkHoppers.computeIfAbsent(chunkKey, k -> new HashSet<>()).add(data);

    hopperLookup.put(location.clone(), data);

    logger.debug(
        "Added filtered hopper id="
            + data.getId()
            + " owner="
            + data.getOwner()
            + " at "
            + location
            + " to optimized chunk "
            + formatChunkKey(chunkKey));
  }

  public boolean removeFilteredHopper(Location location) {
    if (location == null || location.getWorld() == null) {
      logger.warning("removeFilteredHopper called with null location or world; operation skipped");

      return false;
    }

    DatabaseManager.HopperData data = hopperLookup.remove(location);

    long chunkKey = getOptimizedChunkKey(location);

    Set<DatabaseManager.HopperData> chunkSet = chunkHoppers.get(chunkKey);

    if (chunkSet != null && data != null) {
      chunkSet.remove(data);

      if (chunkSet.isEmpty()) {
        chunkHoppers.remove(chunkKey);

        logger.info("Removed empty chunk entry for optimized chunk " + formatChunkKey(chunkKey));
      }
    }

    chunkKeyCache.remove(location);

    if (data != null) {
      logger.success(
          "Filtered hopper removed id="
              + data.getId()
              + " owner="
              + data.getOwner()
              + " at "
              + location
              + " from optimized chunk "
              + formatChunkKey(chunkKey));

      return true;
    }

    return false;
  }

  public boolean hasFilteredHopper(Location location) {
    return location != null && hopperLookup.containsKey(location);
  }

  public DatabaseManager.HopperData getHopperData(Location location) {
    return hopperLookup.get(location);
  }

  public void optimize() {
    chunkKeyCache.entrySet().removeIf(entry -> !hopperLookup.containsKey(entry.getKey()));

    if (chunkKeyCache.size() > 500) {
      chunkKeyCache.clear();
      logger.debug("Cleared oversized chunk key cache");
    }

    logger.debug("Optimized ChunkHopperManager - Cache size: " + chunkKeyCache.size());
  }

  private long getOptimizedChunkKey(Location location) {
    Long cachedKey = chunkKeyCache.get(location);

    if (cachedKey != null) {
      return cachedKey;
    }
    org.bukkit.World world = location.getWorld();
    if (world == null) {
      logger.warning("getOptimizedChunkKey called with null world; returning default key");
      return 0L; // Return default value if world is null
    }

    long key =
        getOptimizedChunkKey(world.getName(), location.getBlockX() >> 4, location.getBlockZ() >> 4);

    chunkKeyCache.put(location.clone(), key);

    return key;
  }

  private long getOptimizedChunkKey(String worldName, int chunkX, int chunkZ) {
    int worldHash =
        worldHashCache.computeIfAbsent(worldName, name -> worldHashCounter.getAndIncrement());

    return ((long) worldHash << 48)
        | (((long) chunkX & 0xFFFFFF) << 24)
        | ((long) chunkZ & 0xFFFFFF);
  }

  private String formatChunkKey(long chunkKey) {
    int worldHash = (int) (chunkKey >>> 48);

    int chunkX = (int) ((chunkKey >>> 24) & 0xFFFFFF);

    int chunkZ = (int) (chunkKey & 0xFFFFFF);

    if (chunkX > 0x7FFFFF) chunkX -= 0x1000000;

    if (chunkZ > 0x7FFFFF) chunkZ -= 0x1000000;

    return String.format("world:%d,x:%d,z:%d", worldHash, chunkX, chunkZ);
  }
}
