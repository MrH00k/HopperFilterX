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
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

public class SoundManager {

  private static final Logger logger = Logger.getInstance();

  private static void spawnParticle(
      Particle particle,
      Location location,
      int count,
      double offsetX,
      double offsetY,
      double offsetZ,
      double extra) {
    org.bukkit.World world = location.getWorld();
    if (world != null) {
      world.spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, extra);
    }
  }

  private static void play(
      Player player,
      Location location,
      Sound sound,
      SoundCategory category,
      float volume,
      float pitch) {
    if (player != null && location != null && sound != null && category != null) {
      player.playSound(location, sound, category, volume, pitch);
    }
  }

  public static void playHopperPlacedSound(Player player, Location location) {
    try {
      play(player, location, Sound.BLOCK_STONE_PLACE, SoundCategory.BLOCKS, 0.8f, 1.2f);
      play(player, location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 0.3f, 1.5f);
      spawnParticle(
          Particle.HAPPY_VILLAGER, location.clone().add(0.5, 0.8, 0.5), 8, 0.3, 0.2, 0.3, 0.0);

      logger.debug("Played hopper placed sound and effects for player " + player.getName());
    } catch (Exception e) {
      logger.error("Failed to play hopper placed sound: " + e.getMessage());
    }
  }

  public static void playHopperBrokenSound(Player player, Location location) {
    try {
      play(player, location, Sound.BLOCK_STONE_BREAK, SoundCategory.BLOCKS, 0.9f, 0.8f);

      play(player, location, Sound.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS, 0.4f, 0.9f);

      spawnParticle(Particle.FLAME, location.clone().add(0.5, 0.5, 0.5), 6, 0.2, 0.2, 0.2, 0.0);

      logger.debug("Played hopper broken sound and effects for player " + player.getName());
    } catch (Exception e) {
      logger.error("Failed to play hopper broken sound: " + e.getMessage());
    }
  }

  public static void playHopperConfigSound(Player player, Location location) {
    try {
      play(player, location, Sound.BLOCK_CHEST_OPEN, SoundCategory.BLOCKS, 0.6f, 1.3f);
      play(player, location, Sound.BLOCK_NOTE_BLOCK_PLING, SoundCategory.BLOCKS, 0.4f, 2.0f);

      spawnParticle(Particle.CRIT, location.clone().add(0.5, 0.8, 0.5), 12, 0.4, 0.3, 0.4, 0.5);

      logger.debug("Played hopper config sound and effects for player " + player.getName());
    } catch (Exception e) {
      logger.error("Failed to play hopper config sound: " + e.getMessage());
    }
  }

  public static void playHopperGivenSound(Player sender, Player receiver, int amount) {
    try {
      if (sender != null) {
        play(
            sender,
            sender.getLocation(),
            Sound.ENTITY_PLAYER_LEVELUP,
            SoundCategory.PLAYERS,
            0.3f,
            1.8f);

        logger.debug("Played hopper given success sound for sender " + sender.getName());
      }

      float pitch = Math.min(2.0f, 1.0f + (amount * 0.1f));

      play(
          receiver,
          receiver.getLocation(),
          Sound.ENTITY_ITEM_PICKUP,
          SoundCategory.PLAYERS,
          0.5f,
          pitch);
      play(
          receiver,
          receiver.getLocation(),
          Sound.BLOCK_ENCHANTMENT_TABLE_USE,
          SoundCategory.PLAYERS,
          0.3f,
          1.5f);
      spawnParticle(
          Particle.FLAME, receiver.getLocation().clone().add(0, 1, 0), 8, 0.5, 0.5, 0.5, 0.1);

      logger.debug(
          "Played hopper received sound and effects for player "
              + receiver.getName()
              + " (amount: "
              + amount
              + ")");
    } catch (Exception e) {
      logger.error("Failed to play hopper given sound: " + e.getMessage());
    }
  }

  public static void playErrorSound(Player player) {
    try {
      play(
          player,
          player.getLocation(),
          Sound.BLOCK_NOTE_BLOCK_BASS,
          SoundCategory.PLAYERS,
          0.4f,
          0.5f);

      play(
          player,
          player.getLocation(),
          Sound.ENTITY_VILLAGER_NO,
          SoundCategory.PLAYERS,
          0.2f,
          1.0f);

      logger.debug("Played error sound for player " + player.getName());
    } catch (Exception e) {
      logger.error("Failed to play error sound: " + e.getMessage());
    }
  }

  public static void playInventoryManagementSound(Player player, boolean isReplacement) {
    try {
      if (isReplacement) {
        play(
            player,
            player.getLocation(),
            Sound.ENTITY_ITEM_PICKUP,
            SoundCategory.PLAYERS,
            0.3f,
            0.8f);

        play(
            player,
            player.getLocation(),
            Sound.BLOCK_DISPENSER_DISPENSE,
            SoundCategory.BLOCKS,
            0.2f,
            1.2f);

        logger.debug("Played inventory replacement sound for player " + player.getName());
      } else {

        play(
            player,
            player.getLocation(),
            Sound.ENTITY_ITEM_PICKUP,
            SoundCategory.PLAYERS,
            0.4f,
            1.1f);

        logger.debug("Played inventory addition sound for player " + player.getName());
      }
    } catch (Exception e) {
      logger.error("Failed to play inventory management sound: " + e.getMessage());
    }
  }

  public static void playChestCloseSound(Player player, Location location) {
    try {
      play(player, location, Sound.BLOCK_CHEST_CLOSE, SoundCategory.BLOCKS, 0.6f, 1.0f);
      logger.debug("Played chest close sound for player " + player.getName());
    } catch (Exception e) {
      logger.error("Failed to play chest close sound: " + e.getMessage());
    }
  }
}
