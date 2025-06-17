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
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class VersionManager {
  private static final String MODRINTH_API_URL =
      "https://api.modrinth.com/v2/project/N0S7uV70/version";
  private static final String TARGET_GAME_VERSION = "1.21";
  private static final List<String> COMPATIBLE_LOADERS = java.util.Arrays.asList("spigot");
  private final Plugin plugin;
  private final Logger logger;
  private String currentVersion;
  private String latestVersion;
  private boolean updateAvailable = false;
  private LocalDateTime lastCheck;

  // Helper class to store version information
  private static class VersionInfo {
    final String versionNumber;
    final String datePublished;
    final List<String> gameVersions;
    final List<String> loaders;

    VersionInfo(
        String versionNumber,
        String datePublished,
        List<String> gameVersions,
        List<String> loaders) {
      this.versionNumber = versionNumber;
      this.datePublished = datePublished;
      this.gameVersions = gameVersions;
      this.loaders = loaders;
    }
  }

  private VersionManager(Plugin plugin) {
    this.plugin = plugin;
    this.logger = Logger.getInstance();
    this.currentVersion = plugin.getDescription().getVersion();
  }

  private static class VersionManagerHolder {
    private static VersionManager instance;

    private static void initialize(Plugin plugin) {
      if (instance == null) {
        instance = new VersionManager(plugin);
        instance.checkForUpdatesAsync();
      }
    }
  }

  public static void initialize(Plugin plugin) {
    VersionManagerHolder.initialize(plugin);
  }

  public static VersionManager getInstance() {
    if (VersionManagerHolder.instance == null) {
      throw new IllegalStateException(
          "VersionManager has not been initialized. Call initialize(Plugin) first.");
    }
    return VersionManagerHolder.instance;
  }

  public void checkForUpdatesAsync() {
    CompletableFuture.runAsync(
        () -> {
          try {
            checkForUpdates();
          } catch (Exception e) {
            logger.error("Failed to check for updates: " + e.getMessage());
            logger.debug("Update check error details: " + e.getClass().getSimpleName());
          }
        });
  }

  private void checkForUpdates() throws IOException {
    logger.debug("Checking for plugin updates from Modrinth API");

    URL url = new URL(MODRINTH_API_URL);
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();

    try {
      connection.setRequestMethod("GET");
      connection.setRequestProperty(
          "User-Agent", "HopperFilterX/" + currentVersion + " (Minecraft Plugin)");
      connection.setConnectTimeout(10000);
      connection.setReadTimeout(10000);

      int responseCode = connection.getResponseCode();
      if (responseCode != 200) {
        logger.warning(
            "Failed to fetch version data from Modrinth API. Response code: " + responseCode);
        return;
      }

      StringBuilder response = new StringBuilder();
      try (BufferedReader reader =
          new BufferedReader(
              new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
        String line;
        while ((line = reader.readLine()) != null) {
          response.append(line);
        }
      }

      parseVersionResponse(response.toString());
      lastCheck = LocalDateTime.now();

    } finally {
      connection.disconnect();
    }
  }

  private void parseVersionResponse(String jsonResponse) {
    try {
      logger.debug(
          "Parsing version response to find latest compatible version for "
              + TARGET_GAME_VERSION
              + " with loaders: "
              + COMPATIBLE_LOADERS);

      List<VersionInfo> compatibleVersions = new ArrayList<>();

      // Parse JSON array manually to extract version information
      // Split by version entries (each starts with "game_versions")
      String[] versionEntries = jsonResponse.split("\\{\\s*\"game_versions\"");

      for (String entry : versionEntries) {
        if (entry.trim().isEmpty()) continue;

        // Add back the opening part
        String fullEntry = "{\"game_versions\"" + entry;

        // Extract game_versions array
        Pattern gameVersionsPattern = Pattern.compile("\"game_versions\"\\s*:\\s*\\[(.*?)\\]");
        Matcher gameVersionsMatcher = gameVersionsPattern.matcher(fullEntry);

        // Extract loaders array
        Pattern loadersPattern = Pattern.compile("\"loaders\"\\s*:\\s*\\[(.*?)\\]");
        Matcher loadersMatcher = loadersPattern.matcher(fullEntry);

        if (gameVersionsMatcher.find() && loadersMatcher.find()) {
          String gameVersionsStr = gameVersionsMatcher.group(1);
          String loadersStr = loadersMatcher.group(1);

          // Check if this version supports our target game version
          boolean hasTargetGameVersion =
              gameVersionsStr.contains("\"" + TARGET_GAME_VERSION + "\"");

          // Check if this version has compatible loaders
          boolean hasCompatibleLoader = false;
          for (String compatibleLoader : COMPATIBLE_LOADERS) {
            if (loadersStr.contains("\"" + compatibleLoader + "\"")) {
              hasCompatibleLoader = true;
              break;
            }
          }

          if (hasTargetGameVersion && hasCompatibleLoader) {

            // Extract version number
            Pattern versionPattern = Pattern.compile("\"version_number\"\\s*:\\s*\"([^\"]+)\"");
            Matcher versionMatcher = versionPattern.matcher(fullEntry);

            // Extract date published
            Pattern datePattern = Pattern.compile("\"date_published\"\\s*:\\s*\"([^\"]+)\"");
            Matcher dateMatcher = datePattern.matcher(fullEntry);

            if (versionMatcher.find() && dateMatcher.find()) {
              String versionNumber = versionMatcher.group(1);
              String datePublished = dateMatcher.group(1);

              // Parse game versions into list
              List<String> gameVersionsList = new ArrayList<>();
              Pattern individualVersionPattern = Pattern.compile("\"([^\"]+)\"");
              Matcher individualMatcher = individualVersionPattern.matcher(gameVersionsStr);
              while (individualMatcher.find()) {
                gameVersionsList.add(individualMatcher.group(1));
              }

              // Parse loaders into list
              List<String> loadersList = new ArrayList<>();
              Matcher loadersIndividualMatcher = individualVersionPattern.matcher(loadersStr);
              while (loadersIndividualMatcher.find()) {
                loadersList.add(loadersIndividualMatcher.group(1));
              }

              VersionInfo versionInfo =
                  new VersionInfo(versionNumber, datePublished, gameVersionsList, loadersList);
              compatibleVersions.add(versionInfo);
              logger.debug(
                  "Found compatible version: "
                      + versionNumber
                      + " for game versions: "
                      + versionInfo.gameVersions
                      + " and loaders: "
                      + versionInfo.loaders);
            }
          } else {
            logger.debug(
                "Skipping version entry - game version compatible: "
                    + hasTargetGameVersion
                    + ", loader compatible: "
                    + hasCompatibleLoader);
          }
        }
      }

      // Find the latest compatible version (most recent date)
      if (!compatibleVersions.isEmpty()) {
        VersionInfo latestCompatible = compatibleVersions.get(0);
        for (VersionInfo version : compatibleVersions) {
          // Compare dates (ISO format allows string comparison)
          if (version.datePublished.compareTo(latestCompatible.datePublished) > 0) {
            latestCompatible = version;
          }
        }
        latestVersion = latestCompatible.versionNumber;
        logger.debug(
            "Latest compatible version found: "
                + latestVersion
                + " (supports game versions: "
                + latestCompatible.gameVersions
                + " and loaders: "
                + latestCompatible.loaders
                + ")");
        logger.debug("Current version: " + currentVersion);

        if (isNewerVersion(latestVersion, currentVersion)) {
          updateAvailable = true;
          logger.info(
              "New version available: "
                  + latestVersion
                  + " (current: "
                  + currentVersion
                  + ") - Compatible with "
                  + TARGET_GAME_VERSION
                  + " and supports loaders: "
                  + latestCompatible.loaders);
        } else {
          updateAvailable = false;
          logger.debug("Plugin is up to date");
        }
      } else {
        logger.warning(
            "No compatible versions found for game version "
                + TARGET_GAME_VERSION
                + " with compatible loaders: "
                + COMPATIBLE_LOADERS);
      }

    } catch (Exception e) {
      logger.error("Failed to parse version response: " + e.getMessage());
      logger.debug("JSON Response: " + jsonResponse);
    }
  }

  private boolean isNewerVersion(String latest, String current) {
    try {
      // Remove common prefixes and suffixes for comparison
      String cleanLatest = cleanVersionString(latest);
      String cleanCurrent = cleanVersionString(current);

      logger.debug(
          "Comparing versions - Latest: '" + cleanLatest + "' vs Current: '" + cleanCurrent + "'");

      // Simple string comparison - if they're different, assume latest is newer
      return !cleanLatest.equals(cleanCurrent);

    } catch (Exception e) {
      logger.error("Error comparing versions: " + e.getMessage());
      return false;
    }
  }

  private String cleanVersionString(String version) {
    if (version == null) return "";

    // Remove common prefixes and normalize
    String cleaned =
        version.toLowerCase().replaceFirst("^v", "").replaceFirst("-snapshot$", "").trim();

    return cleaned;
  }

  private void notifyAboutUpdate() {
    // Schedule notification on main thread
    new BukkitRunnable() {
      @Override
      public void run() {
        // Notify online operators
        for (Player player : Bukkit.getOnlinePlayers()) {
          if (player.isOp()) {
            notifyPlayerAboutUpdate(player);
          }
        }
      }
    }.runTask(plugin);
  }

  public void notifyPlayerAboutUpdate(Player player) {
    if (updateAvailable && player.isOp()) {
      MessageManager messageManager = MessageManager.getInstance();
      player.sendMessage(
          messageManager.getMessage(
              "version.update-available", "current", currentVersion, "latest", latestVersion));
    }
  }

  public boolean isUpdateAvailable() {
    return updateAvailable;
  }

  public String getCurrentVersion() {
    return currentVersion;
  }

  public String getLatestVersion() {
    return latestVersion;
  }

  public LocalDateTime getLastCheck() {
    return lastCheck == null ? null : LocalDateTime.from(lastCheck);
  }

  public String getFormattedLastCheck() {
    if (lastCheck == null) {
      return "Never";
    }
    return lastCheck.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
  }

  public void forceCheck() {
    forceCheck(null);
  }

  // Nueva función: permite forzar la consulta y ejecutar un callback cuando termine
  public void forceCheck(Runnable onComplete) {
    CompletableFuture.runAsync(
            () -> {
              try {
                checkForUpdates();
              } catch (Exception e) {
                logger.error("Failed to check for updates: " + e.getMessage());
                logger.debug("Update check error details: " + e.getClass().getSimpleName());
              }
            })
        .whenComplete(
            (result, throwable) -> {
              // Ejecutar el callback en el hilo principal de Bukkit
              if (onComplete != null) {
                new org.bukkit.scheduler.BukkitRunnable() {
                  @Override
                  public void run() {
                    onComplete.run();
                  }
                }.runTask(plugin);
              }
            });
  }
}
