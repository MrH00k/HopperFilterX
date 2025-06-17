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
package com.mrh00k.hopperfilterx.commands;

import com.mrh00k.hopperfilterx.Main;
import com.mrh00k.hopperfilterx.managers.DatabaseManager;
import com.mrh00k.hopperfilterx.managers.MessageManager;
import com.mrh00k.hopperfilterx.managers.SoundManager;
import com.mrh00k.hopperfilterx.utils.HopperUtils;
import com.mrh00k.hopperfilterx.utils.InventoryUtils;
import com.mrh00k.hopperfilterx.utils.Logger;
import java.util.ArrayList;
import java.util.List;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class HopperCommand implements CommandExecutor, TabCompleter {
  private final Logger logger = Logger.getInstance();

  private final NamespacedKey filteredHopperKey;

  public HopperCommand(Main plugin) {
    this.filteredHopperKey = new NamespacedKey(plugin, "filtered_hopper");
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    try {
      if (args.length == 0) {
        sender.sendMessage(MessageManager.getInstance().getMessage("command.usage"));
        return true;
      }
      String subCommand = args[0].toLowerCase();
      if (subCommand.equals("addperm")) {
        handleAddPermCommand(sender, args);
      } else if (subCommand.equals("removeperm")) {
        handleRemovePermCommand(sender, args);
      } else if (subCommand.equals("give")) {
        handleGiveCommand(sender, args);
      } else if (subCommand.equals("remove")) {
        handleRemoveCommand(sender, args);
      } else if (subCommand.equals("list")) {
        handleListCommand(sender, args);
      } else if (subCommand.equals("reload")) {
        if (sender instanceof Player && !sender.hasPermission("hopperfilterx.reload")) {
          SoundManager.playErrorSound((Player) sender);
          sender.sendMessage(MessageManager.getInstance().getMessage("no-permission"));
          return true;
        }
        logger.info("Reloading plugin configuration and messages");
        Main pluginInstance = JavaPlugin.getPlugin(Main.class);
        pluginInstance.reloadConfig();
        Logger.getInstance().reloadDebugConfiguration(pluginInstance);
        MessageManager.getInstance().reload();
        sender.sendMessage(MessageManager.getInstance().getMessage("command.reload-success"));
        logger.success("Plugin reloaded successfully");
      } else {
        sender.sendMessage(MessageManager.getInstance().getMessage("command.unknown"));
      }
    } catch (Exception e) {
      logger.error("Error executing hopper command: " + e.getMessage());
      logger.debug(
          "Command execution error details - sender: "
              + sender.getName()
              + ", args: "
              + java.util.Arrays.toString(args));
      sender.sendMessage(
          MessageManager.getInstance().getMessage("error.unexpected", "error", e.getMessage()));
    }
    return true;
  }

  @Override
  public java.util.List<String> onTabComplete(
      CommandSender sender, Command command, String alias, String[] args) {
    List<String> completions = new ArrayList<>();

    // Solo operadores pueden ver los tabs de operador
    boolean isOperator =
        sender.isOp()
            || sender.hasPermission("hopperfilterx.give")
            || sender.hasPermission("hopperfilterx.remove")
            || sender.hasPermission("hopperfilterx.reload");

    if (!isOperator) {
      // Usuarios normales solo ven los comandos permitidos
      if (args.length == 1) {
        completions.add("list");
        completions.add("addperm");
        completions.add("removeperm");
      } else if (args.length == 2 && args[0].equalsIgnoreCase("addperm")) {
        for (Player player : Bukkit.getOnlinePlayers()) {
          if (!player.getName().equalsIgnoreCase(sender.getName())) {
            completions.add(player.getName());
          }
        }
      } else if (args.length == 3 && args[0].equalsIgnoreCase("addperm")) {
        completions.add("<uuid>");
      } else if (args.length == 2 && args[0].equalsIgnoreCase("removeperm")) {
        for (Player player : Bukkit.getOnlinePlayers()) {
          if (!player.getName().equalsIgnoreCase(sender.getName())) {
            completions.add(player.getName());
          }
        }
      } else if (args.length == 3 && args[0].equalsIgnoreCase("removeperm")) {
        completions.add("<uuid>");
      } else if (args.length == 2 && args[0].equalsIgnoreCase("list")) {
        if (sender instanceof Player && sender.getName().equalsIgnoreCase(args[1])) {
          completions.add(sender.getName());
        }
      }
      return completions;
    }

    // Operadores ven todos los comandos y sugerencias
    if (args.length == 1) {
      completions.add("give");
      completions.add("remove");
      completions.add("list");
      completions.add("reload");
      completions.add("addperm");
      completions.add("removeperm");
    } else if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
      for (Player player : Bukkit.getOnlinePlayers()) {
        completions.add(player.getName());
      }
    } else if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
      completions.add("<amount>");
    } else if (args.length == 2 && args[0].equalsIgnoreCase("remove")) {
      for (Player player : Bukkit.getOnlinePlayers()) {
        completions.add(player.getName());
      }
    } else if (args.length == 3 && args[0].equalsIgnoreCase("remove")) {
      completions.add("<uuid>");
    } else if (args.length == 2 && args[0].equalsIgnoreCase("list")) {
      for (Player player : Bukkit.getOnlinePlayers()) {
        completions.add(player.getName());
      }
    } else if (args.length == 2 && args[0].equalsIgnoreCase("addperm")) {
      for (Player player : Bukkit.getOnlinePlayers()) {
        if (!player.getName().equalsIgnoreCase(sender.getName())) {
          completions.add(player.getName());
        }
      }
    } else if (args.length == 3 && args[0].equalsIgnoreCase("addperm")) {
      completions.add("<uuid>");
    } else if (args.length == 2 && args[0].equalsIgnoreCase("removeperm")) {
      for (Player player : Bukkit.getOnlinePlayers()) {
        if (!player.getName().equalsIgnoreCase(sender.getName())) {
          completions.add(player.getName());
        }
      }
    } else if (args.length == 3 && args[0].equalsIgnoreCase("removeperm")) {
      completions.add("<uuid>");
    }

    return completions;
  }

  private void handleAddPermCommand(CommandSender sender, String[] args) {
    try {
      if (!(sender.hasPermission("hopperfilterx.addperm"))) {
        sender.sendMessage(MessageManager.getInstance().getMessage("no-permission"));
        return;
      }
      Player player = sender instanceof Player ? (Player) sender : null;
      String owner = player != null ? player.getName() : "CONSOLE";
      if (args.length < 2) {
        sender.sendMessage(MessageManager.getInstance().getMessage("command.usage-addperm"));
        return;
      }
      String permitted = args[1];
      // Evitar que un usuario se dé permisos a sí mismo
      if (owner.equalsIgnoreCase(permitted)) {
        sender.sendMessage(
            MessageManager.getInstance().getMessage("command.self-permission-denied"));
        return;
      }
      String hopperUuid = null;
      if (args.length >= 3) {
        hopperUuid = args[2];

        // Check if UUID exists in database
        if (hopperUuid != null
            && !hopperUuid.isEmpty()
            && !DatabaseManager.uuidExists(hopperUuid)) {
          sender.sendMessage(
              MessageManager.getInstance().getMessage("error.uuid-not-found", "uuid", hopperUuid));
          return;
        }

        // Check if UUID belongs to the command sender (owner)
        if (hopperUuid != null
            && !hopperUuid.isEmpty()
            && !DatabaseManager.uuidBelongsToPlayer(hopperUuid, owner)) {
          sender.sendMessage(
              MessageManager.getInstance()
                  .getMessage("error.uuid-not-owned", "uuid", hopperUuid, "player", owner));
          return;
        }
      }
      // Verificar si el jugador tiene hoppers
      List<com.mrh00k.hopperfilterx.managers.DatabaseManager.HopperData> hoppers =
          com.mrh00k.hopperfilterx.managers.DatabaseManager.loadAllHopperData();
      boolean hasHopper = false;
      if (hopperUuid != null) {
        for (com.mrh00k.hopperfilterx.managers.DatabaseManager.HopperData h : hoppers) {
          if (h.getOwner().equalsIgnoreCase(owner) && h.getId().equals(hopperUuid)) {
            hasHopper = true;
            break;
          }
        }
      } else {
        for (com.mrh00k.hopperfilterx.managers.DatabaseManager.HopperData h : hoppers) {
          if (h.getOwner().equalsIgnoreCase(owner)) {
            hasHopper = true;
            break;
          }
        }
      }
      if (!hasHopper) {
        sender.sendMessage(MessageManager.getInstance().getMessage("command.addperm-no-hoppers"));
        return;
      }
      // --- VERIFICACIÓN DE PERMISOS EXISTENTES ---
      boolean alreadyExists = false;
      try {
        if (hopperUuid == null) {
          // Permiso global: ¿ya tiene global?
          alreadyExists =
              com.mrh00k.hopperfilterx.managers.DatabaseManager.hasAnyHopperPermission(
                  owner, permitted);
        } else {
          // Permiso específico: ¿ya tiene global o ya tiene este uuid?
          alreadyExists =
              com.mrh00k.hopperfilterx.managers.DatabaseManager.hasAnyHopperPermission(
                      owner, permitted)
                  || com.mrh00k.hopperfilterx.managers.DatabaseManager.hasHopperPermission(
                      owner, permitted, hopperUuid);
        }
      } catch (Exception e) {
        // fallback a la lógica anterior si hay error
      }
      if (alreadyExists) {
        sender.sendMessage(
            MessageManager.getInstance().getMessage("command.addperm-already-exists"));
        return;
      }
      boolean added =
          com.mrh00k.hopperfilterx.managers.DatabaseManager.addHopperPermission(
              owner, permitted, hopperUuid);
      if (added) {
        if (hopperUuid != null) {
          sender.sendMessage(
              MessageManager.getInstance()
                  .getMessage(
                      "command.addperm-success-uuid", "player", permitted, "uuid", hopperUuid));
        } else {
          sender.sendMessage(
              MessageManager.getInstance()
                  .getMessage("command.addperm-success-all", "player", permitted));
        }
      } else {
        sender.sendMessage(
            MessageManager.getInstance().getMessage("command.addperm-already-exists"));
      }
    } catch (Exception e) {
      sender.sendMessage(
          MessageManager.getInstance().getMessage("error.unexpected", "error", e.getMessage()));
    }
  }

  private void handleGiveCommand(CommandSender sender, String[] args) {
    try {

      if (!(sender.hasPermission("hopperfilterx.give") || sender.isOp())) {
        if (sender instanceof Player) {
          SoundManager.playErrorSound((Player) sender);
        }

        sender.sendMessage(MessageManager.getInstance().getMessage("no-permission"));

        return;
      }

      if (args.length < 2) {
        sender.sendMessage(MessageManager.getInstance().getMessage("command.usage-give"));

        logger.warning("Insufficient arguments for give command by '" + sender.getName() + "'");

        return;
      }

      String targetPlayerName = args[1];

      if (targetPlayerName == null || targetPlayerName.trim().isEmpty()) {
        sender.sendMessage(MessageManager.getInstance().getMessage("command.usage-give"));

        logger.warning("Invalid target player name for give command by '" + sender.getName() + "'");

        return;
      }

      int amount = 1;

      if (args.length >= 3) {
        try {
          amount = Integer.parseInt(args[2]);

          if (amount <= 0) {
            sender.sendMessage(MessageManager.getInstance().getMessage("command.invalid-amount"));

            return;
          } else if (amount > 64) {
            sender.sendMessage(MessageManager.getInstance().getMessage("command.amount-too-high"));

            return;
          }
        } catch (NumberFormatException e) {
          logger.warning(
              "Invalid number format for amount: '" + args[2] + "' by '" + sender.getName() + "'");

          sender.sendMessage(MessageManager.getInstance().getMessage("command.invalid-number"));

          return;
        }
      }

      Player targetPlayer = Bukkit.getPlayer(targetPlayerName);

      if (targetPlayer == null) {
        sender.sendMessage(MessageManager.getInstance().getMessage("command.player-not-found"));

        logger.warning(
            "Give command failed: player '" + targetPlayerName + "' not found or offline");

        return;
      }

      logger.info(
          "Creating filtered hopper item (amount: "
              + amount
              + ") for '"
              + targetPlayer.getName()
              + "'");

      ItemStack hopper = HopperUtils.createFilteredHopper(filteredHopperKey);

      hopper.setAmount(amount);

      logger.info(
          "Attempting to transfer filtered hopper item(s) to '" + targetPlayer.getName() + "'");

      if (InventoryUtils.giveItemToPlayer(targetPlayer, hopper)) {
        SoundManager.playHopperGivenSound(
            sender instanceof Player ? (Player) sender : null, targetPlayer, amount);

        logger.success(
            "Successfully gave "
                + amount
                + " filtered hopper(s) to '"
                + targetPlayer.getName()
                + "'");

        sender.sendMessage(
            MessageManager.getInstance()
                .getMessage(
                    "command.give-success", "amount", amount, "player", targetPlayer.getName()));

        targetPlayer.sendMessage(
            MessageManager.getInstance()
                .getMessage("command.give-received", "amount", amount, "sender", sender.getName()));
      } else {
        sender.sendMessage(
            MessageManager.getInstance()
                .getMessage("error.give-failed", "player", targetPlayer.getName()));

        logger.error(
            "Hopper give operation failed for player '"
                + targetPlayer.getName()
                + "' - inventory transfer issue");

        logger.warning(
            "Inventory transfer may have failed: player inventory full or invalid state");
      }
    } catch (Exception e) {
      logger.error("Unexpected error executing '/hopper' command: " + e.getMessage());

      logger.debug(
          "'/hopper' execution error details - sender='"
              + sender.getName()
              + "', args="
              + java.util.Arrays.toString(args)
              + ", exception="
              + e.getClass().getSimpleName());

      sender.sendMessage(
          MessageManager.getInstance().getMessage("error.unexpected", "error", e.getMessage()));
    }
  }

  private void handleRemoveCommand(CommandSender sender, String[] args) {
    try {
      if (!(sender.hasPermission("hopperfilterx.remove") || sender.isOp())) {
        if (sender instanceof Player) {
          SoundManager.playErrorSound((Player) sender);
        }

        sender.sendMessage(MessageManager.getInstance().getMessage("no-permission"));

        return;
      }

      if (args.length < 2) {
        sender.sendMessage(MessageManager.getInstance().getMessage("command.usage-remove"));

        return;
      }

      String targetPlayerName = args[1];

      Player target = Bukkit.getPlayerExact(targetPlayerName);

      if (target == null) {
        sender.sendMessage(MessageManager.getInstance().getMessage("command.player-not-found"));

        return;
      }

      boolean removed = false;

      if (args.length < 3) {
        java.util.List<com.mrh00k.hopperfilterx.managers.DatabaseManager.HopperData> allHoppers =
            com.mrh00k.hopperfilterx.managers.DatabaseManager.loadAllHopperData();
        java.util.List<String> toRemove = new java.util.ArrayList<>();
        for (com.mrh00k.hopperfilterx.managers.DatabaseManager.HopperData data : allHoppers) {
          if (data.getOwner().equalsIgnoreCase(targetPlayerName)) {
            toRemove.add(data.getId());
          }
        }
        for (String uuid : toRemove) {
          handleRemoveCommand(sender, new String[] {"remove", targetPlayerName, uuid});
          removed = true;
        }
        if (removed) {
          sender.sendMessage(
              MessageManager.getInstance()
                  .getMessage("command.remove-success-all", "player", targetPlayerName));

          target.sendMessage(MessageManager.getInstance().getMessage("command.remove-notify-all"));
        } else {
          sender.sendMessage(
              MessageManager.getInstance()
                  .getMessage("command.remove-no-hoppers", "player", targetPlayerName));
        }
        return;
      }

      String uuid = args[2];

      // Check if UUID exists in database
      if (uuid != null && !uuid.isEmpty() && !DatabaseManager.uuidExists(uuid)) {
        sender.sendMessage(
            MessageManager.getInstance().getMessage("error.uuid-not-found", "uuid", uuid));
        return;
      }

      // Check if UUID belongs to the specified player
      if (uuid != null
          && !uuid.isEmpty()
          && !DatabaseManager.uuidBelongsToPlayer(uuid, targetPlayerName)) {
        sender.sendMessage(
            MessageManager.getInstance()
                .getMessage("error.uuid-not-owned", "uuid", uuid, "player", targetPlayerName));
        return;
      }

      for (int i = 0; i < target.getInventory().getSize(); i++) {
        ItemStack item = target.getInventory().getItem(i);

        if (HopperUtils.isFilteredHopper(item, filteredHopperKey)
            && uuid.equals(HopperUtils.getUuidFromFilteredHopper(item, filteredHopperKey))) {
          target.getInventory().setItem(i, null);

          removed = true;
        }
      }

      if (target.getOpenInventory() != null
          && target.getOpenInventory().getTopInventory() != null) {
        org.bukkit.inventory.Inventory inv = target.getOpenInventory().getTopInventory();

        for (int i = 0; i < inv.getSize(); i++) {
          ItemStack item = inv.getItem(i);

          if (HopperUtils.isFilteredHopper(item, filteredHopperKey)
              && uuid.equals(HopperUtils.getUuidFromFilteredHopper(item, filteredHopperKey))) {
            inv.setItem(i, null);

            removed = true;
          }
        }
      }

      org.bukkit.inventory.Inventory enderInv = target.getEnderChest();

      for (int i = 0; i < enderInv.getSize(); i++) {
        ItemStack item = enderInv.getItem(i);

        if (HopperUtils.isFilteredHopper(item, filteredHopperKey)
            && uuid.equals(HopperUtils.getUuidFromFilteredHopper(item, filteredHopperKey))) {
          enderInv.setItem(i, null);

          removed = true;
        }
      }

      for (org.bukkit.World world : org.bukkit.Bukkit.getWorlds()) {
        for (org.bukkit.Chunk chunk : world.getLoadedChunks()) {
          for (org.bukkit.block.BlockState state : chunk.getTileEntities()) {
            if (state instanceof org.bukkit.inventory.InventoryHolder) {
              org.bukkit.inventory.Inventory inv =
                  ((org.bukkit.inventory.InventoryHolder) state).getInventory();

              for (int i = 0; i < inv.getSize(); i++) {
                ItemStack item = inv.getItem(i);

                if (HopperUtils.isFilteredHopper(item, filteredHopperKey)
                    && uuid.equals(
                        HopperUtils.getUuidFromFilteredHopper(item, filteredHopperKey))) {
                  inv.setItem(i, null);

                  removed = true;
                }
              }
            }
          }
        }
      }

      com.mrh00k.hopperfilterx.managers.ChunkHopperManager chunkHopperManager =
          new com.mrh00k.hopperfilterx.managers.ChunkHopperManager();

      java.util.List<com.mrh00k.hopperfilterx.managers.DatabaseManager.HopperData> allHoppers =
          com.mrh00k.hopperfilterx.managers.DatabaseManager.loadAllHopperData();

      for (com.mrh00k.hopperfilterx.managers.DatabaseManager.HopperData data : allHoppers) {
        if (uuid.equals(data.getId()) && data.isPlaced()) {
          org.bukkit.Location loc = data.getLocation();

          org.bukkit.World world = loc.getWorld();

          if (world != null && world.getBlockAt(loc).getType() == org.bukkit.Material.HOPPER) {
            SoundManager.playHopperBrokenSound(target, loc);

            world.getBlockAt(loc).setType(org.bukkit.Material.AIR);

            removed = true;

            chunkHopperManager.removeFilteredHopper(loc);
          }
        }
      }

      com.mrh00k.hopperfilterx.managers.DatabaseManager.deleteFilteredHopper(uuid);

      if (!removed) {
        boolean foundInDb = false;
        try {
          if (com.mrh00k.hopperfilterx.managers.DatabaseManager.filteredHopperExists(uuid)) {
            com.mrh00k.hopperfilterx.managers.DatabaseManager.deleteFilteredHopper(uuid);
            foundInDb = true;
          }

          java.util.UUID playerUuid = target.getUniqueId();
          java.util.List<org.bukkit.inventory.ItemStack> creativeHoppers =
              com.mrh00k.hopperfilterx.managers.DatabaseManager.loadCreativeHoppers(playerUuid);
          java.util.List<org.bukkit.inventory.ItemStack> updated = new java.util.ArrayList<>();
          for (org.bukkit.inventory.ItemStack item : creativeHoppers) {
            if (!uuid.equals(
                com.mrh00k.hopperfilterx.utils.HopperUtils.getUuidFromFilteredHopper(
                    item, filteredHopperKey))) {
              updated.add(item);
            } else {
              foundInDb = true;
            }
          }
          if (updated.size() != creativeHoppers.size()) {
            com.mrh00k.hopperfilterx.managers.DatabaseManager.saveCreativeHoppers(
                playerUuid, updated);
          }
        } catch (Exception e) {
          sender.sendMessage(
              MessageManager.getInstance().getMessage("error.unexpected", "error", e.getMessage()));
          logger.error("Error removing hopper from database: " + e.getMessage());
        }
        if (foundInDb) {
          sender.sendMessage(
              MessageManager.getInstance()
                  .getMessage("command.remove-success", "player", targetPlayerName, "uuid", uuid));
          target.sendMessage(
              MessageManager.getInstance().getMessage("command.remove-notify", "uuid", uuid));
        } else {
          sender.sendMessage(
              MessageManager.getInstance()
                  .getMessage(
                      "command.remove-not-found", "player", targetPlayerName, "uuid", uuid));
        }
        return;
      }

      com.mrh00k.hopperfilterx.managers.DatabaseManager.deleteFilteredHopper(uuid);

      sender.sendMessage(
          MessageManager.getInstance()
              .getMessage("command.remove-success", "player", targetPlayerName, "uuid", uuid));
      target.sendMessage(
          MessageManager.getInstance().getMessage("command.remove-notify", "uuid", uuid));
    } catch (Exception e) {
      sender.sendMessage(
          MessageManager.getInstance().getMessage("error.unexpected", "error", e.getMessage()));

      logger.error("Error executing hopper remove command: " + e.getMessage());
    }
  }

  private void handleRemovePermCommand(CommandSender sender, String[] args) {
    try {
      if (!(sender.hasPermission("hopperfilterx.removeperm"))) {
        sender.sendMessage(MessageManager.getInstance().getMessage("no-permission"));
        return;
      }
      Player player = sender instanceof Player ? (Player) sender : null;
      String owner = player != null ? player.getName() : "CONSOLE";
      if (args.length < 2) {
        sender.sendMessage(MessageManager.getInstance().getMessage("command.usage-removeperm"));
        return;
      }
      String permitted = args[1];
      // Evitar que un usuario se remueva permisos a sí mismo
      if (owner.equalsIgnoreCase(permitted)) {
        sender.sendMessage(MessageManager.getInstance().getMessage("command.self-remove-denied"));
        return;
      }
      String hopperUuid = null;
      if (args.length >= 3) {
        hopperUuid = args[2];

        // Check if UUID exists in database
        if (hopperUuid != null
            && !hopperUuid.isEmpty()
            && !DatabaseManager.uuidExists(hopperUuid)) {
          sender.sendMessage(
              MessageManager.getInstance().getMessage("error.uuid-not-found", "uuid", hopperUuid));
          return;
        }

        // Check if UUID belongs to the command sender (owner)
        if (hopperUuid != null
            && !hopperUuid.isEmpty()
            && !DatabaseManager.uuidBelongsToPlayer(hopperUuid, owner)) {
          sender.sendMessage(
              MessageManager.getInstance()
                  .getMessage("error.uuid-not-owned", "uuid", hopperUuid, "player", owner));
          return;
        }
      }
      List<com.mrh00k.hopperfilterx.managers.DatabaseManager.HopperData> hoppers =
          com.mrh00k.hopperfilterx.managers.DatabaseManager.loadAllHopperData();
      boolean hasHopper = false;
      if (hopperUuid != null) {
        for (com.mrh00k.hopperfilterx.managers.DatabaseManager.HopperData h : hoppers) {
          if (h.getOwner().equalsIgnoreCase(owner) && h.getId().equals(hopperUuid)) {
            hasHopper = true;
            break;
          }
        }
      } else {
        for (com.mrh00k.hopperfilterx.managers.DatabaseManager.HopperData h : hoppers) {
          if (h.getOwner().equalsIgnoreCase(owner)) {
            hasHopper = true;
            break;
          }
        }
      }
      if (!hasHopper) {
        sender.sendMessage(
            MessageManager.getInstance().getMessage("command.removeperm-no-hoppers"));
        return;
      }
      // --- VERIFICACIÓN DE PERMISOS EXISTENTES ---
      boolean hadPermission = false;
      try {
        if (hopperUuid == null) {
          // Remover global: ¿tiene global o algún específico?
          hadPermission =
              com.mrh00k.hopperfilterx.managers.DatabaseManager.hasAnyHopperPermission(
                  owner, permitted);
          if (!hadPermission) {
            // Si no tiene global, verifica si tiene algún específico
            for (com.mrh00k.hopperfilterx.managers.DatabaseManager.HopperData h : hoppers) {
              if (h.getOwner().equalsIgnoreCase(owner)) {
                if (com.mrh00k.hopperfilterx.managers.DatabaseManager.hasHopperPermission(
                    owner, permitted, h.getId())) {
                  hadPermission = true;
                  break;
                }
              }
            }
          }
        } else {
          // Remover específico: ¿tiene global o ese específico?
          hadPermission =
              com.mrh00k.hopperfilterx.managers.DatabaseManager.hasAnyHopperPermission(
                      owner, permitted)
                  || com.mrh00k.hopperfilterx.managers.DatabaseManager.hasHopperPermission(
                      owner, permitted, hopperUuid);
        }
      } catch (Exception e) {
        // fallback a la lógica anterior si hay error
      }
      if (!hadPermission) {
        sender.sendMessage(
            MessageManager.getInstance().getMessage("command.removeperm-not-exists"));
        return;
      }
      boolean removed = false;
      if (hopperUuid != null) {
        removed =
            com.mrh00k.hopperfilterx.managers.DatabaseManager.removeHopperPermission(
                owner, permitted, hopperUuid);
      } else {
        // Remove all permissions for this permitted user for any hopper owned by the sender
        boolean anyRemoved = false;
        // Remove global permission
        if (com.mrh00k.hopperfilterx.managers.DatabaseManager.removeHopperPermission(
            owner, permitted, null)) {
          anyRemoved = true;
        }
        // Remove per-hopper permissions
        for (com.mrh00k.hopperfilterx.managers.DatabaseManager.HopperData h : hoppers) {
          if (h.getOwner().equalsIgnoreCase(owner)) {
            if (com.mrh00k.hopperfilterx.managers.DatabaseManager.removeHopperPermission(
                owner, permitted, h.getId())) {
              anyRemoved = true;
            }
          }
        }
        removed = anyRemoved;
      }
      if (removed) {
        if (hopperUuid != null) {
          sender.sendMessage(
              MessageManager.getInstance()
                  .getMessage(
                      "command.removeperm-success-uuid", "player", permitted, "uuid", hopperUuid));
        } else {
          sender.sendMessage(
              MessageManager.getInstance()
                  .getMessage("command.removeperm-success-all", "player", permitted));
        }
      } else {
        sender.sendMessage(
            MessageManager.getInstance().getMessage("command.removeperm-not-exists"));
      }
    } catch (Exception e) {
      sender.sendMessage(
          MessageManager.getInstance().getMessage("error.unexpected", "error", e.getMessage()));
    }
  }

  private void handleListCommand(CommandSender sender, String[] args) {
    try {
      if (!(sender.hasPermission("hopperfilterx.list"))) {
        if (sender instanceof Player) {
          SoundManager.playErrorSound((Player) sender);
        }
        sender.sendMessage(MessageManager.getInstance().getMessage("no-permission"));
        return;
      }

      String targetPlayerName = null;
      if (args.length < 2) {
        // Si es jugador, mostrar solo sus propios hoppers
        if (sender instanceof Player) {
          targetPlayerName = sender.getName();
        } else {
          sender.sendMessage(MessageManager.getInstance().getMessage("command.usage-list"));
          return;
        }
      } else {
        targetPlayerName = args[1];
        // Solo puede consultar otros si es op
        if (sender instanceof Player
            && !sender.isOp()
            && !sender.getName().equalsIgnoreCase(targetPlayerName)) {
          sender.sendMessage(MessageManager.getInstance().getMessage("no-permission"));
          return;
        }
      }

      // Player not found solo si el target no está online y no es el propio sender
      Player targetPlayer = Bukkit.getPlayer(targetPlayerName);
      if (targetPlayer == null
          && !(sender instanceof Player && sender.getName().equalsIgnoreCase(targetPlayerName))) {
        sender.sendMessage(MessageManager.getInstance().getMessage("command.player-not-found"));
        return;
      }

      java.util.List<com.mrh00k.hopperfilterx.managers.DatabaseManager.HopperData> allHoppers =
          com.mrh00k.hopperfilterx.managers.DatabaseManager.loadAllHopperData();

      java.util.List<com.mrh00k.hopperfilterx.managers.DatabaseManager.HopperData> playerHoppers =
          new java.util.ArrayList<>();

      for (com.mrh00k.hopperfilterx.managers.DatabaseManager.HopperData data : allHoppers) {
        if (data.getOwner().equalsIgnoreCase(targetPlayerName)) {
          playerHoppers.add(data);
        }
      }

      if (playerHoppers.isEmpty()) {
        // Si es operador y está buscando a otro jugador, mensaje personalizado
        if (sender.isOp() && !(sender.getName().equalsIgnoreCase(targetPlayerName))) {
          sender.sendMessage(
              MessageManager.getInstance()
                  .getMessage("command.list-no-hoppers", "player", targetPlayerName));
        } else {
          // Si es el propio usuario, mensaje "que te pertenezcan"
          sender.sendMessage(
              MessageManager.getInstance().getMessage("command.list-no-hoppers-user"));
        }
        return;
      }

      sender.sendMessage(
          MessageManager.getInstance()
              .getMessage("command.list-header", "player", targetPlayerName));

      for (com.mrh00k.hopperfilterx.managers.DatabaseManager.HopperData data : playerHoppers) {
        // Crear el componente UUID con click to copy y hover
        TextComponent uuidComponent = new TextComponent(data.getId());
        uuidComponent.setColor(net.md_5.bungee.api.ChatColor.WHITE);
        uuidComponent.setClickEvent(
            new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, data.getId()));

        // Crear el texto de hover
        TextComponent hoverText =
            new TextComponent(
                MessageManager.getInstance().getMessageWithoutPrefix("command.click-to-copy"));
        hoverText.setColor(net.md_5.bungee.api.ChatColor.WHITE);
        uuidComponent.setHoverEvent(
            new HoverEvent(
                HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder(hoverText.getText())
                    .color(net.md_5.bungee.api.ChatColor.WHITE)
                    .create()));

        // Crear el texto de estado
        String statusText;
        if (data.isPlaced()) {
          String world =
              data.getLocation().getWorld() != null ? data.getLocation().getWorld().getName() : "?";

          statusText =
              MessageManager.getInstance()
                  .getMessageWithoutPrefix(
                      "command.list-entry-placed",
                      "world",
                      world,
                      "x",
                      data.getLocation().getBlockX(),
                      "y",
                      data.getLocation().getBlockY(),
                      "z",
                      data.getLocation().getBlockZ());
        } else {
          statusText =
              MessageManager.getInstance().getMessageWithoutPrefix("command.list-entry-inventory");
        }

        // Construir el mensaje completo
        TextComponent linePrefix = new TextComponent("\n- ");
        TextComponent statusComponent = new TextComponent("\n" + statusText);

        TextComponent completeMessage = new TextComponent("");
        completeMessage.addExtra(linePrefix);
        completeMessage.addExtra(uuidComponent);
        completeMessage.addExtra(statusComponent);

        sender.spigot().sendMessage(completeMessage);
      }
    } catch (Exception e) {
      sender.sendMessage(
          MessageManager.getInstance().getMessage("error.unexpected", "error", e.getMessage()));

      logger.error("Error executing hopper list command: " + e.getMessage());
    }
  }
}
