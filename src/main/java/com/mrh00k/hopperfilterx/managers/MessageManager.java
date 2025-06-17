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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

public class MessageManager {

  private final Plugin plugin;

  private FileConfiguration messagesConfig;

  private final Map<String, String> messages = new HashMap<>();

  private static final String DEFAULT_LOCALE = "en_us";

  private String selectedLocale;

  private MessageManager(Plugin plugin) {
    this.plugin = plugin;

    loadMessages();
  }

  private static class MessageManagerHolder {
    private static MessageManager instance;

    private static void initialize(Plugin plugin) {
      if (instance == null) {
        instance = new MessageManager(plugin);
      }
    }
  }

  public static MessageManager getInstance(Plugin plugin) {
    MessageManagerHolder.initialize(plugin);

    return MessageManagerHolder.instance;
  }

  public static MessageManager getInstance() {
    if (MessageManagerHolder.instance == null) {
      throw new IllegalStateException(
          "MessageManager has not been initialized. Call getInstance(Plugin) first.");
    }

    return MessageManagerHolder.instance;
  }

  private void loadMessages() {
    Logger.getInstance().info("Loading messages from lang.yml");

    try {
      File langFile = new File(plugin.getDataFolder(), "lang.yml");

      // Always overwrite lang.yml with the latest from the JAR
      plugin.saveResource("lang.yml", true);

      messagesConfig = YamlConfiguration.loadConfiguration(langFile);

      InputStream defaultStream = plugin.getResource("lang.yml");

      if (defaultStream != null) {
        try (InputStreamReader reader =
            new InputStreamReader(defaultStream, java.nio.charset.StandardCharsets.UTF_8)) {
          YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(reader);

          messagesConfig.setDefaults(defaultConfig);

          // Merge and save any new default messages without overriding user customizations
          messagesConfig.options().copyDefaults(true);
          try {
            messagesConfig.save(langFile);
            Logger.getInstance()
                .info("Language file lang.yml checked and updated with new defaults");
          } catch (IOException ioe) {
            Logger.getInstance().error("Failed to save updated lang.yml: " + ioe.getMessage());
          }
        }
      }

      String systemLocale = Locale.getDefault().toString().toLowerCase();

      String localeToLoad = systemLocale;

      if (!messagesConfig.isConfigurationSection(localeToLoad)) {
        String lang = localeToLoad.split("_")[0];

        for (String key : messagesConfig.getKeys(false)) {
          if (key.startsWith(lang + "_")) {
            localeToLoad = key;

            Logger.getInstance()
                .info(
                    "Locale '"
                        + systemLocale
                        + "' not found, using '"
                        + localeToLoad
                        + "' fallback");

            break;
          }
        }
      }

      if (!messagesConfig.isConfigurationSection(localeToLoad)) {
        localeToLoad = DEFAULT_LOCALE;

        Logger.getInstance().info("Locale fallback to default '" + DEFAULT_LOCALE + "'");
      }

      this.selectedLocale = localeToLoad;

      Logger.getInstance().info("Selected locale: " + selectedLocale);

      loadMessagesFromConfig();

      Logger.getInstance().success("Messages loaded successfully.");

    } catch (IOException e) {
      Logger.getInstance().error("Failed to load messages configuration: " + e.getMessage());

      loadDefaultMessages();
    }
  }

  private void loadDefaultMessages() {
    Logger.getInstance().warning("Loading default messages as fallback.");
    Logger.getInstance().info("Populating default fallback messages into memory");
    messages.put("prefix", "&8[&6HopperFilterX&8]&r");
    messages.put("no-permission", "&cYou don't have permission to use this command.");
    // Command messages
    messages.put("command.usage", "&eUse: /hopper give <player> [amount]");
    messages.put("command.unknown", "&cUnknown command. Use: /hopper give <player> [amount]");
    messages.put("command.usage-give", "&cUsage: /hopper give <player> [amount]");
    messages.put("command.invalid-amount", "&cAmount must be greater than 0.");
    messages.put("command.amount-too-high", "&cAmount must be less than or equal to 64.");
    messages.put("command.invalid-number", "&cInvalid amount.");
    messages.put("command.player-not-found", "&cPlayer not found.");
    messages.put("command.give-success", "&bGave {amount} filtered hopper(s) to {player}.");
    messages.put(
        "command.give-received", "&aYou received {amount} filtered hopper(s) from {sender}.");
    messages.put("command.reload-success", "&aPlugin reloaded successfully.");
    messages.put("command.usage-remove", "&cUsage: /hopper remove <player> <uuid>");
    messages.put("command.remove-success", "&aRemoved hopper with UUID {uuid} from {player}.");
    messages.put(
        "command.remove-notify",
        "&cYour filtered hopper with UUID {uuid} was removed by an administrator.");
    messages.put(
        "command.remove-not-found", "&cNo filtered hopper with UUID {uuid} found for {player}.");
    messages.put("command.remove-no-hoppers", "&cNo filtered hoppers found for {player}.");
    messages.put("command.usage-list", "&eUse: /hopper list <player>");
    messages.put("command.list-header", "&bFiltered hoppers of {player}:");
    messages.put("command.list-no-hoppers", "&cNo filtered hoppers found for {player}.");
    messages.put(
        "command.list-no-hoppers-user", "&cNo filtered hoppers belonging to you were found.");
    messages.put("command.list-entry-placed", "&6State: Placed in {world} @ {x},{y},{z}");
    messages.put("command.list-entry-inventory", "&bState: In inventory");
    messages.put("command.click-to-copy", "&7Click to copy");
    messages.put("command.remove-success-all", "&aRemoved all filtered hoppers for {player}.");
    messages.put(
        "command.remove-notify-all",
        "&cAll your filtered hoppers were removed by an administrator.");
    messages.put("command.usage-addperm", "&eUse: /hopper addperm <player> [uuid]");
    messages.put(
        "command.addperm-success-uuid", "&aPlayer {player} now has access to your filter {uuid}.");
    messages.put(
        "command.addperm-success-all", "&aPlayer {player} now has access to all your filters.");
    messages.put("command.addperm-already-exists", "&eThat player already has permission.");
    messages.put(
        "command.addperm-no-hoppers", "&cNo filtered hoppers belonging to you were found.");
    messages.put("command.usage-removeperm", "&eUse: /hopper removeperm <player> [uuid]");
    messages.put(
        "command.removeperm-success-uuid",
        "&aPlayer {player} no longer has access to your filter {uuid}.");
    messages.put(
        "command.removeperm-success-all",
        "&aPlayer {player} no longer has access to any of your filters.");
    messages.put("command.removeperm-not-exists", "&eThat player does not have permission.");
    messages.put(
        "command.removeperm-no-hoppers", "&cNo filtered hoppers belonging to you were found.");
    // Hopper interactions
    messages.put(
        "hopper.placed", "&aFiltered hopper placed! Shift + Right-click to configure filters.");
    messages.put("hopper.broken", "&eFiltered hopper broken! Metadata preserved.");
    messages.put(
        "hopper.broken.survival",
        "&eFiltered hopper broken in survival! You can place it back to restore its data.");
    messages.put(
        "hopper.broken.creative",
        "&cFiltered hopper removed in creative mode! Data permanently deleted.");
    messages.put(
        "hopper.replaced", "&aFiltered hopper re-placed! Previous configuration restored.");
    messages.put("hopper.config-opening", "&eOpening filter configuration...");
    messages.put("hopper.not-owner", "&cYou are not the owner of this filtered hopper.");
    messages.put(
        "hopper.filter-denied", "&cThis hopper has a filter, you cannot insert this item.");
    // Error messages
    messages.put("error.give-failed", "&cFailed to give hopper to player {player}.");
    messages.put("error.unexpected", "&cAn unexpected error occurred: {error}");
    messages.put("error.uuid-not-found", "&cUUID {uuid} does not exist in the database.");
    messages.put("error.uuid-not-owned", "&cUUID {uuid} does not belong to player {player}.");
    // Version update messages
    messages.put(
        "version.update-available",
        "&eA new version of HopperFilterX is available! Current: &c{current} &e→ Latest: &a{latest}");
    Logger.getInstance().info("Default fallback messages loaded (" + messages.size() + " entries)");
  }

  private void loadMessagesFromConfig() {
    try {
      ConfigurationSection section = messagesConfig.getConfigurationSection(selectedLocale);

      if (section == null) {
        Logger.getInstance().warning("Language section not found: " + selectedLocale);
        loadDefaultMessages();
        return;
      }

      for (String key : section.getKeys(true)) {
        if (section.isString(key)) {
          messages.put(key, section.getString(key));
        }
      }

      Logger.getInstance()
          .info("Loaded " + messages.size() + " messages for locale " + selectedLocale);
    } catch (Exception e) {
      Logger.getInstance().error("Error loading messages from config: " + e.getMessage());

      loadDefaultMessages();
    }
  }

  public String getMessage(String key, Object... placeholders) {
    try {
      String message = messages.getOrDefault(key, "&cMessage not found: " + key);

      if (placeholders.length > 0 && placeholders.length % 2 == 0) {
        for (int i = 0; i < placeholders.length; i += 2) {
          String placeholder = "{" + placeholders[i] + "}";

          String replacement = String.valueOf(placeholders[i + 1]);

          message = message.replace(placeholder, replacement);
        }
      }

      String prefix = messages.getOrDefault("prefix", "&8[&6HopperFilterX&8]&r");

      if (!message.startsWith(prefix) && !key.equals("prefix")) {
        message = prefix + " " + message;
      }

      return ChatColor.translateAlternateColorCodes('&', message);

    } catch (Exception e) {
      Logger.getInstance().error("Error formatting message '" + key + "': " + e.getMessage());

      return ChatColor.RED + "[HopperFilterX] Error loading message: " + key;
    }
  }

  public String getMessageWithoutPrefix(String key, Object... placeholders) {
    try {
      String message = messages.getOrDefault(key, "&cMessage not found: " + key);

      if (placeholders.length > 0 && placeholders.length % 2 == 0) {
        for (int i = 0; i < placeholders.length; i += 2) {
          String placeholder = "{" + placeholders[i] + "}";

          String replacement = String.valueOf(placeholders[i + 1]);

          message = message.replace(placeholder, replacement);
        }
      }

      return ChatColor.translateAlternateColorCodes('&', message);

    } catch (Exception e) {
      Logger.getInstance().error("Error formatting message '" + key + "': " + e.getMessage());

      return ChatColor.RED + "[HopperFilterX] Error loading message: " + key;
    }
  }

  public void reload() {
    try {
      messages.clear();

      loadMessages();

      Logger.getInstance().success("Messages reloaded successfully.");
    } catch (Exception e) {
      Logger.getInstance().error("Failed to reload messages: " + e.getMessage());
    }
  }
}
