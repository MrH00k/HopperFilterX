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
import com.mrh00k.hopperfilterx.managers.ChunkHopperManager;
import com.mrh00k.hopperfilterx.managers.DatabaseManager;
import com.mrh00k.hopperfilterx.managers.DatabaseManager.HopperData;
import com.mrh00k.hopperfilterx.managers.MessageManager;
import com.mrh00k.hopperfilterx.managers.SoundManager;
import com.mrh00k.hopperfilterx.utils.HopperUtils;
import com.mrh00k.hopperfilterx.utils.Logger;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class HopperListener implements Listener {
  private final Logger logger = Logger.getInstance();

  private final NamespacedKey filteredHopperKey;

  private final ChunkHopperManager chunkHopperManager = new ChunkHopperManager();

  private int operationCounter = 0;

  private static final int OPTIMIZATION_FREQUENCY = 100;

  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  public HopperListener(Main plugin) {
    this.filteredHopperKey = new NamespacedKey(plugin, "filtered_hopper");
    try {
      List<HopperData> persisted = DatabaseManager.loadAllHopperData();

      int loadedCount = 0;

      for (HopperData data : persisted) {
        if (data.isPlaced()) {
          chunkHopperManager.addFilteredHopper(data);

          loadedCount++;
        } else {
          logger.debug(
              "Skipping non-placed hopper UUID " + data.getId() + " owned by " + data.getOwner());
        }
      }

      logger.info(
          "Loaded "
              + loadedCount
              + " placed filtered hoppers from database ("
              + (persisted.size() - loadedCount)
              + " non-placed hoppers remain in database)");
    } catch (SQLException e) {
      logger.error("Failed to load filtered hoppers from database: " + e.getMessage());
    }
  }

  @EventHandler
  public void onBlockPlace(BlockPlaceEvent event) {
    try {
      if (event == null || event.getPlayer() == null || event.getItemInHand() == null) {
        logger.warning("BlockPlaceEvent has null components; skipping processing");

        return;
      }

      ItemStack item = event.getItemInHand();

      if (item.getType() != Material.HOPPER) {
        return;
      }

      if (HopperUtils.isFilteredHopper(item, filteredHopperKey)) {
        if (!event.getPlayer().hasPermission("hopperfilterx.use")) {
          event.setCancelled(true);

          SoundManager.playErrorSound(event.getPlayer());

          event
              .getPlayer()
              .sendMessage(MessageManager.getInstance().getMessage("hopper.not-owner"));

          return;
        }

        Block block = event.getBlockPlaced();

        Location loc = block.getLocation().clone();

        String placingPlayer = event.getPlayer().getName();

        String existingUuid = HopperUtils.getUuidFromFilteredHopper(item, filteredHopperKey);

        final String[] owner = new String[] {placingPlayer};

        if (existingUuid != null) {
          // Try to fetch the original owner from the database
          String dbOwner = null;
          try {
            List<DatabaseManager.HopperData> all = DatabaseManager.loadAllHopperData();
            for (DatabaseManager.HopperData d : all) {
              if (existingUuid.equals(d.getId())) {
                dbOwner = d.getOwner();
                break;
              }
            }
          } catch (Exception ex) {
            logger.error(
                "Failed to fetch original owner for UUID " + existingUuid + ": " + ex.getMessage());
          }
          if (dbOwner != null && !placingPlayer.equals(dbOwner)) {
            // Check if the placing player has permission from the owner
            boolean hasPerm = false;
            try {
              hasPerm = DatabaseManager.hasHopperPermission(dbOwner, placingPlayer, existingUuid);
            } catch (Exception ex) {
              logger.error("Failed to check permission for hopper placement: " + ex.getMessage());
            }
            if (hasPerm) {
              owner[0] = dbOwner; // Preserve the original owner
            } else {
              // No permission, cancel placement
              event.setCancelled(true);
              SoundManager.playErrorSound(event.getPlayer());
              event
                  .getPlayer()
                  .sendMessage(MessageManager.getInstance().getMessage("hopper.not-owner"));
              return;
            }
          } else if (dbOwner != null) {
            owner[0] = dbOwner; // Placing player is the owner
          }

          if (HopperUtils.isPlayerInCreative(event.getPlayer())) {
            logger.info(
                "Creative mode placement - generating new UUID instead of reusing "
                    + existingUuid
                    + " by "
                    + owner[0]
                    + " at "
                    + loc);

            JavaPlugin.getPlugin(Main.class)
                .getServer()
                .getScheduler()
                .runTaskAsynchronously(
                    JavaPlugin.getPlugin(Main.class),
                    () -> {
                      String newId = DatabaseManager.insertFilteredHopper(loc, owner[0]);

                      if (newId != null) {
                        HopperData data = new HopperData(newId, loc, owner[0], true);

                        JavaPlugin.getPlugin(Main.class)
                            .getServer()
                            .getScheduler()
                            .runTask(
                                JavaPlugin.getPlugin(Main.class),
                                () -> {
                                  chunkHopperManager.addFilteredHopper(data);
                                  triggerOptimizationIfNeeded();
                                });
                      }
                    });

            event.getPlayer().sendMessage(MessageManager.getInstance().getMessage("hopper.placed"));
          } else {
            logger.info(
                "Re-placing filtered hopper with existing UUID "
                    + existingUuid
                    + " by "
                    + owner[0]
                    + " at "
                    + loc);

            JavaPlugin.getPlugin(Main.class)
                .getServer()
                .getScheduler()
                .runTaskAsynchronously(
                    JavaPlugin.getPlugin(Main.class),
                    () -> {
                      DatabaseManager.markHopperAsPlaced(existingUuid, loc);

                      JavaPlugin.getPlugin(Main.class)
                          .getServer()
                          .getScheduler()
                          .runTask(
                              JavaPlugin.getPlugin(Main.class),
                              () -> {
                                chunkHopperManager.removeFilteredHopper(loc);

                                HopperData data = new HopperData(existingUuid, loc, owner[0], true);

                                chunkHopperManager.addFilteredHopper(data);

                                triggerOptimizationIfNeeded();
                              });
                    });

            event
                .getPlayer()
                .sendMessage(MessageManager.getInstance().getMessage("hopper.replaced"));
          }
        } else {
          logger.info("Placing new filtered hopper by " + owner[0] + " at " + loc);

          JavaPlugin.getPlugin(Main.class)
              .getServer()
              .getScheduler()
              .runTaskAsynchronously(
                  JavaPlugin.getPlugin(Main.class),
                  () -> {
                    String id = DatabaseManager.insertFilteredHopper(loc, owner[0]);

                    if (id != null) {
                      HopperData data = new HopperData(id, loc, owner[0], true);

                      JavaPlugin.getPlugin(Main.class)
                          .getServer()
                          .getScheduler()
                          .runTask(
                              JavaPlugin.getPlugin(Main.class),
                              () -> {
                                chunkHopperManager.addFilteredHopper(data);

                                triggerOptimizationIfNeeded();
                              });
                    }
                  });

          event.getPlayer().sendMessage(MessageManager.getInstance().getMessage("hopper.placed"));
        }

        SoundManager.playHopperPlacedSound(event.getPlayer(), block.getLocation());

        logger.success(
            "Filtered hopper placed by '"
                + event.getPlayer().getName()
                + "' at "
                + block.getLocation()
                + (existingUuid != null
                    ? " (re-placed with UUID " + existingUuid + ")"
                    : " (new)"));
      }
    } catch (Exception e) {
      logger.error("Error handling block place event: " + e.getMessage());

      ItemStack itemInHand = event.getItemInHand();

      Block blockPlaced = event.getBlockPlaced();

      logger.debug(
          "Block place error details - player: "
              + event.getPlayer().getName()
              + ", item: "
              + itemInHand.getType()
              + ", location: "
              + blockPlaced.getLocation());

      event
          .getPlayer()
          .sendMessage(
              MessageManager.getInstance().getMessage("error.unexpected", "error", e.getMessage()));
    }
  }

  @EventHandler
  public void onBlockBreak(BlockBreakEvent event) {
    try {
      if (event.getPlayer() == null || event.getBlock() == null) {
        logger.debug("Block break event validation failed - null player or block");

        return;
      }

      Block block = event.getBlock();

      if (block.getType() != Material.HOPPER) {
        return;
      }

      Location location = block.getLocation();

      if (chunkHopperManager.hasFilteredHopper(location)) {
        HopperData data = chunkHopperManager.getHopperData(location);

        if (data == null
            || !(event.getPlayer().getName().equals(data.getOwner())
                || hasHopperPermission(
                    data.getOwner(), event.getPlayer().getName(), data.getId()))) {
          event.setCancelled(true);
          SoundManager.playErrorSound(event.getPlayer());
          event
              .getPlayer()
              .sendMessage(MessageManager.getInstance().getMessage("hopper.not-owner"));
          return;
        }

        event.setDropItems(false);

        if (block.getState() instanceof org.bukkit.block.Hopper) {
          org.bukkit.block.Hopper hopperState = (org.bukkit.block.Hopper) block.getState();

          org.bukkit.inventory.Inventory hopperInventory = hopperState.getInventory();
          if (hopperInventory != null) {
            org.bukkit.World world = location.getWorld();
            if (world != null) {
              ItemStack[] contents = hopperInventory.getContents();
              if (contents != null) {
                for (ItemStack item : contents) {
                  if (item != null && item.getType() != Material.AIR) {
                    world.dropItemNaturally(location, item);
                  }
                }
              }
            }
          }
        }

        Player player = event.getPlayer();

        String hopperId = data.getId();

        if (HopperUtils.isPlayerInSurvival(player)) {
          logger.info("Survival mode break detected - preserving hopper data for UUID " + hopperId);

          JavaPlugin.getPlugin(Main.class)
              .getServer()
              .getScheduler()
              .runTaskAsynchronously(
                  JavaPlugin.getPlugin(Main.class),
                  () -> DatabaseManager.markHopperAsNotPlaced(hopperId));

          ItemStack filteredHopper = HopperUtils.createFilteredHopper(filteredHopperKey);
          filteredHopper =
              HopperUtils.addUuidToFilteredHopper(
                  filteredHopper, hopperId, player.getName(), filteredHopperKey);

          org.bukkit.World world = location.getWorld();
          if (world != null) {
            world.dropItemNaturally(location, filteredHopper);

            logger.debug(
                "Dropped filtered hopper with UUID " + hopperId + " for survival mode break");
          }

          data.setPlaced(false);

          event
              .getPlayer()
              .sendMessage(MessageManager.getInstance().getMessage("hopper.broken.survival"));

        } else if (HopperUtils.isPlayerInCreative(player)) {
          logger.info(
              "Creative mode break detected - removing hopper data completely for UUID "
                  + hopperId);

          chunkHopperManager.removeFilteredHopper(location);

          JavaPlugin.getPlugin(Main.class)
              .getServer()
              .getScheduler()
              .runTaskAsynchronously(
                  JavaPlugin.getPlugin(Main.class),
                  () -> DatabaseManager.deleteFilteredHopper(hopperId));

          logger.debug("No hopper drop for creative mode break - removed from database");

          event
              .getPlayer()
              .sendMessage(MessageManager.getInstance().getMessage("hopper.broken.creative"));

        } else {
          logger.info(
              "Non-standard game mode break detected ("
                  + player.getGameMode()
                  + ") - treating as survival mode for UUID "
                  + hopperId);

          JavaPlugin.getPlugin(Main.class)
              .getServer()
              .getScheduler()
              .runTaskAsynchronously(
                  JavaPlugin.getPlugin(Main.class),
                  () -> DatabaseManager.markHopperAsNotPlaced(hopperId));
          ItemStack filteredHopper = HopperUtils.createFilteredHopper(filteredHopperKey);
          filteredHopper =
              HopperUtils.addUuidToFilteredHopper(
                  filteredHopper, hopperId, player.getName(), filteredHopperKey);

          org.bukkit.World world = location.getWorld();
          if (world != null) {
            world.dropItemNaturally(location, filteredHopper);
          }

          data.setPlaced(false);

          event.getPlayer().sendMessage(MessageManager.getInstance().getMessage("hopper.broken"));
        }

        triggerOptimizationIfNeeded();

        SoundManager.playHopperBrokenSound(event.getPlayer(), location);

        logger.success(
            "Filtered hopper broken by '"
                + event.getPlayer().getName()
                + "' ("
                + event.getPlayer().getGameMode()
                + " mode) at "
                + location);
      }
    } catch (Exception e) {
      logger.error("Error handling block break event: " + e.getMessage());

      logger.debug(
          "Block break error details - player: "
              + event.getPlayer().getName()
              + ", block: "
              + event.getBlock().getType()
              + ", location: "
              + event.getBlock().getLocation());

      event
          .getPlayer()
          .sendMessage(
              MessageManager.getInstance().getMessage("error.unexpected", "error", e.getMessage()));
    }
  }

  @EventHandler
  public void onPlayerInteract(PlayerInteractEvent event) {
    try {
      if (event == null || event.getPlayer() == null) {
        logger.debug("Player interact event validation failed - null event components");

        return;
      }

      if (event.getHand() != EquipmentSlot.HAND) {
        return;
      }

      Block block = event.getClickedBlock();

      if (block == null || block.getType() != Material.HOPPER) {
        return;
      }

      if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
        return;
      }

      // Prevent Filtered Hopper Chest GUI from opening if player is trying to place an inventory
      // block
      // This allows normal placement of chests, barrels, shulkers, etc. on a filtered hopper
      ItemStack hand = event.getItem();
      if (hand != null && HopperUtils.isInventoryBlock(hand.getType())) {
        // Do not open the Filtered Hopper Chest GUI, let vanilla placement happen
        return;
      }

      if (!event.getPlayer().isSneaking()) {
        return;
      }

      ItemStack itemInHand = event.getItem();

      if (itemInHand != null && itemInHand.getType() == Material.HOPPER) {
        Block targetBlock = block.getRelative(event.getBlockFace());

        if (targetBlock.getType() == Material.AIR
            || targetBlock.getType().equals(Material.WATER)
            || targetBlock.getType().equals(Material.LAVA)) {
          return;
        }
      }

      if (chunkHopperManager.hasFilteredHopper(block.getLocation())) {
        Player player = event.getPlayer();
        HopperData data = chunkHopperManager.getHopperData(block.getLocation());
        if (data == null) {
          // No data, no acción
          return;
        }
        if (!(player.isOp()
            || player.getName().equals(data.getOwner())
            || hasHopperPermission(data.getOwner(), player.getName(), data.getId()))) {
          event.setCancelled(true);
          SoundManager.playErrorSound(player);
          player.sendMessage(MessageManager.getInstance().getMessage("hopper.not-owner"));
          return;
        }

        event.setCancelled(true);

        SoundManager.playHopperConfigSound(player, block.getLocation());

        player.sendMessage(MessageManager.getInstance().getMessage("hopper.config-opening"));

        java.util.List<org.bukkit.inventory.ItemStack> items = java.util.Collections.emptyList();
        try {
          items =
              com.mrh00k.hopperfilterx.managers.DatabaseManager.loadFilteredHopperItems(
                  data.getId());
        } catch (java.sql.SQLException e) {
          logger.error(
              "Failed to load filtered hopper items for UUID "
                  + data.getId()
                  + ": "
                  + e.getMessage());
        }

        com.mrh00k.hopperfilterx.gui.HopperChestGUI.open(player, items);

        player.setMetadata(
            "filtered_hopper_uuid",
            new org.bukkit.metadata.FixedMetadataValue(
                JavaPlugin.getPlugin(com.mrh00k.hopperfilterx.Main.class), data.getId()));
        logger.debug(
            "Filtered hopper GUI opened for player '"
                + player.getName()
                + "' at "
                + block.getLocation());
        logger.info(
            "Configuration GUI opened for filtered hopper id="
                + data.getId()
                + " at "
                + block.getLocation());
        return;
      }
    } catch (Exception e) {
      logger.error("Error handling player interact event: " + e.getMessage());

      Block clickedBlock = event.getClickedBlock();

      String blockType = (clickedBlock != null) ? clickedBlock.getType().toString() : "null";

      String blockLocation =
          (clickedBlock != null) ? clickedBlock.getLocation().toString() : "null";

      logger.debug(
          "Player interact error details - player: "
              + event.getPlayer().getName()
              + ", action: "
              + event.getAction()
              + ", block: "
              + blockType
              + ", location: "
              + blockLocation);

      event
          .getPlayer()
          .sendMessage(
              MessageManager.getInstance().getMessage("error.unexpected", "error", e.getMessage()));
    }
  }

  @EventHandler
  public void onInventoryMoveItem(InventoryMoveItemEvent event) {
    try {
      InventoryHolder sourceHolder = event.getSource().getHolder();
      if (sourceHolder instanceof org.bukkit.block.Hopper) {
        org.bukkit.block.Hopper hopper = (org.bukkit.block.Hopper) sourceHolder;
        Location loc = hopper.getLocation();
        if (chunkHopperManager.hasFilteredHopper(loc)) {
          if (!isItemAllowedByFilter(loc, event.getItem())) {
            event.setCancelled(true);
            logger.debug(
                "Filtered hopper at "
                    + loc
                    + " blocked item from source: "
                    + event.getItem().getType());
            return;
          }
        }
      }

      InventoryHolder destHolder = event.getDestination().getHolder();
      if (destHolder instanceof org.bukkit.block.Hopper) {
        org.bukkit.block.Hopper hopper = (org.bukkit.block.Hopper) destHolder;
        Location loc = hopper.getLocation();
        if (chunkHopperManager.hasFilteredHopper(loc)) {
          if (!isItemAllowedByFilter(loc, event.getItem())) {
            event.setCancelled(true);
            logger.debug(
                "Filtered hopper at "
                    + loc
                    + " blocked item from destination: "
                    + event.getItem().getType());
            // Cache sourceInv and destInv to avoid possible null dereference in async task
            final org.bukkit.inventory.Inventory sourceInv = event.getSource();
            final org.bukkit.inventory.Inventory destInv = event.getDestination();
            if (sourceInv == null || destInv == null) {
              return;
            }
            new BukkitRunnable() {
              @Override
              public void run() {
                if (loc == null || !chunkHopperManager.hasFilteredHopper(loc)) {
                  return;
                }
                org.bukkit.inventory.Inventory src = sourceInv;
                org.bukkit.inventory.Inventory dst = destInv;
                if (src == null || dst == null) {
                  return;
                }
                org.bukkit.inventory.ItemStack[] contents = src.getContents();
                if (contents != null) {
                  for (org.bukkit.inventory.ItemStack s : contents) {
                    if (s != null && s.getType() != Material.AIR && isItemAllowedByFilter(loc, s)) {
                      org.bukkit.inventory.ItemStack toMove = s.clone();
                      toMove.setAmount(1);
                      src.removeItem(toMove);
                      dst.addItem(toMove);
                      break;
                    }
                  }
                }
              }
            }.runTaskLater(JavaPlugin.getPlugin(Main.class), 1L);
            return;
          }
        }
      }
    } catch (Exception e) {
      logger.error("Error in filtered hopper item move filter: " + e.getMessage());
    }
  }

  private boolean isItemAllowedByFilter(Location loc, ItemStack moving) {
    DatabaseManager.HopperData data = chunkHopperManager.getHopperData(loc);
    if (data == null) return true;
    String uuid = data.getId();
    if (uuid == null) return true;
    java.util.List<ItemStack> filterItems;
    try {
      filterItems = DatabaseManager.loadFilteredHopperItems(uuid);
    } catch (Exception e) {
      logger.error("Failed to load filter items for hopper UUID " + uuid + ": " + e.getMessage());
      return true;
    }
    if (filterItems == null || filterItems.isEmpty()) {
      return true;
    }
    for (ItemStack filter : filterItems) {
      if (filter == null || filter.getType() == Material.AIR) continue;
      if (filter.isSimilar(moving)) {
        return true;
      }
    }
    return false;
  }

  private void triggerOptimizationIfNeeded() {
    operationCounter++;

    if (operationCounter >= OPTIMIZATION_FREQUENCY) {
      operationCounter = 0;

      chunkHopperManager.optimize();

      logger.debug("Triggered automatic ChunkHopperManager optimization");
    }
  }

  @EventHandler
  void onBlockExplode(BlockExplodeEvent event) {
    for (Block block : new ArrayList<>(event.blockList())) {
      if (block.getType() == Material.HOPPER
          && chunkHopperManager.hasFilteredHopper(block.getLocation())) {
        DatabaseManager.HopperData data = chunkHopperManager.getHopperData(block.getLocation());

        if (data != null) {
          event.blockList().remove(block);

          double dropChance = 0.25;

          boolean isDestroyed = false;

          if (SECURE_RANDOM.nextDouble() < dropChance) {
            ItemStack filteredHopper = HopperUtils.createFilteredHopper(filteredHopperKey);

            filteredHopper =
                HopperUtils.addUuidToFilteredHopper(
                    filteredHopper, data.getId(), data.getOwner(), filteredHopperKey);

            if (block.getWorld() != null) {
              block.getWorld().dropItemNaturally(block.getLocation(), filteredHopper);
            }

            logger.info(
                "Filtered hopper at "
                    + block.getLocation()
                    + " dropped with metadata after explosion (lucky!)");
          } else {
            isDestroyed = true;
            logger.info(
                "Filtered hopper at "
                    + block.getLocation()
                    + " was destroyed by explosion (no drop)");
          }

          block.setType(Material.AIR);

          chunkHopperManager.removeFilteredHopper(block.getLocation());

          if (!isDestroyed) {
            JavaPlugin.getPlugin(Main.class)
                .getServer()
                .getScheduler()
                .runTaskAsynchronously(
                    JavaPlugin.getPlugin(Main.class),
                    () -> DatabaseManager.markHopperAsNotPlaced(data.getId()));
          } else {
            JavaPlugin.getPlugin(Main.class)
                .getServer()
                .getScheduler()
                .runTaskAsynchronously(
                    JavaPlugin.getPlugin(Main.class),
                    () -> DatabaseManager.deleteFilteredHopper(data.getId()));
          }
        }
      }
    }
  }

  @EventHandler
  void onEntityExplode(EntityExplodeEvent event) {
    for (Block block : new ArrayList<>(event.blockList())) {
      if (block.getType() == Material.HOPPER
          && chunkHopperManager.hasFilteredHopper(block.getLocation())) {
        DatabaseManager.HopperData data = chunkHopperManager.getHopperData(block.getLocation());

        if (data != null) {
          event.blockList().remove(block);

          double dropChance = 0.25;

          boolean isDestroyed = false;

          if (event.getEntity() instanceof org.bukkit.entity.Creeper) {
            dropChance = 0.15; // 15% for Creepers
          } else if (event.getEntity() instanceof org.bukkit.entity.Fireball) {
            dropChance = 0.20; // 20% for Fireballs (Ghast, Blaze)
          } else if (event.getEntity() instanceof org.bukkit.entity.TNTPrimed) {
            dropChance = 0.30; // 30% for TNT
          } else if (event.getEntity() instanceof org.bukkit.entity.EnderDragon) {
            dropChance = 0.10; // 10% for Ender Dragon
          } else if (event.getEntity() instanceof org.bukkit.entity.Wither) {
            dropChance = 0.05; // 5% for Wither
          } else if (event.getEntity() instanceof org.bukkit.entity.Minecart) {
            dropChance = 0.18; // 18% for Exploding Minecarts
          } else if (event.getEntity() instanceof org.bukkit.entity.LightningStrike) {
            dropChance = 0.12; // 12% for Lightning (rare, but possible)
          }

          if (SECURE_RANDOM.nextDouble() < dropChance) {
            ItemStack filteredHopper = HopperUtils.createFilteredHopper(filteredHopperKey);

            filteredHopper =
                HopperUtils.addUuidToFilteredHopper(
                    filteredHopper, data.getId(), data.getOwner(), filteredHopperKey);

            if (block.getWorld() != null) {
              block.getWorld().dropItemNaturally(block.getLocation(), filteredHopper);
            }

            logger.info(
                "Filtered hopper at "
                    + block.getLocation()
                    + " dropped with metadata after entity explosion (lucky!)");
          } else {
            isDestroyed = true;
            logger.info(
                "Filtered hopper at "
                    + block.getLocation()
                    + " was destroyed by entity explosion (no drop)");
          }

          block.setType(Material.AIR);

          chunkHopperManager.removeFilteredHopper(block.getLocation());

          if (!isDestroyed) {
            JavaPlugin.getPlugin(Main.class)
                .getServer()
                .getScheduler()
                .runTaskAsynchronously(
                    JavaPlugin.getPlugin(Main.class),
                    () -> DatabaseManager.markHopperAsNotPlaced(data.getId()));
          } else {
            JavaPlugin.getPlugin(Main.class)
                .getServer()
                .getScheduler()
                .runTaskAsynchronously(
                    JavaPlugin.getPlugin(Main.class),
                    () -> DatabaseManager.deleteFilteredHopper(data.getId()));
          }
        }
      }
    }
  }

  @EventHandler
  void onEntityDamage(final EntityDamageEvent event) {
    if (!(event.getEntity() instanceof Item)) return;

    final Item itemEntity = (Item) event.getEntity();

    ItemStack stack = itemEntity.getItemStack();

    if (!HopperUtils.isFilteredHopper(stack, filteredHopperKey)) return;

    String uuid = HopperUtils.getUuidFromFilteredHopper(stack, filteredHopperKey);

    if (uuid == null) return;

    new BukkitRunnable() {
      @Override
      public void run() {
        if (itemEntity.isDead() || !itemEntity.isValid()) {
          JavaPlugin.getPlugin(Main.class)
              .getServer()
              .getScheduler()
              .runTaskAsynchronously(
                  JavaPlugin.getPlugin(Main.class),
                  () -> DatabaseManager.deleteFilteredHopper(uuid));

          logger.info(
              "Filtered hopper item destroyed by damage ("
                  + event.getCause()
                  + "), removed UUID "
                  + uuid
                  + " from database");
        }
      }
    }.runTaskLater(JavaPlugin.getPlugin(Main.class), 1L);
  }

  @EventHandler
  void onEntityCombust(final EntityCombustEvent event) {
    if (!(event.getEntity() instanceof Item)) return;

    final Item itemEntity = (Item) event.getEntity();

    ItemStack stack = itemEntity.getItemStack();

    if (!HopperUtils.isFilteredHopper(stack, filteredHopperKey)) return;

    String uuid = HopperUtils.getUuidFromFilteredHopper(stack, filteredHopperKey);

    if (uuid == null) return;

    new BukkitRunnable() {
      @Override
      public void run() {
        if (itemEntity.isDead() || !itemEntity.isValid()) {
          JavaPlugin.getPlugin(Main.class)
              .getServer()
              .getScheduler()
              .runTaskAsynchronously(
                  JavaPlugin.getPlugin(Main.class),
                  () -> DatabaseManager.deleteFilteredHopper(uuid));

          logger.info(
              "Filtered hopper item destroyed by combustion, removed UUID "
                  + uuid
                  + " from database");
        }
      }
    }.runTaskLater(JavaPlugin.getPlugin(Main.class), 1L);
  }

  @EventHandler
  void onItemDespawn(final ItemDespawnEvent event) {
    Item itemEntity = event.getEntity();

    ItemStack stack = itemEntity.getItemStack();

    if (!HopperUtils.isFilteredHopper(stack, filteredHopperKey)) return;

    String uuid = HopperUtils.getUuidFromFilteredHopper(stack, filteredHopperKey);

    if (uuid == null) return;

    JavaPlugin.getPlugin(Main.class)
        .getServer()
        .getScheduler()
        .runTaskAsynchronously(
            JavaPlugin.getPlugin(Main.class), () -> DatabaseManager.deleteFilteredHopper(uuid));

    logger.info(
        "Filtered hopper item destroyed by despawn, removed UUID " + uuid + " from database");
  }

  @EventHandler
  public void onInventoryClickFilteredHopper(org.bukkit.event.inventory.InventoryClickEvent event) {
    try {
      org.bukkit.inventory.InventoryView view = event.getView();
      org.bukkit.inventory.Inventory topInv = view.getTopInventory();
      org.bukkit.inventory.InventoryHolder topHolder = topInv.getHolder();
      if (!(topHolder instanceof org.bukkit.block.Hopper)) return;
      org.bukkit.block.Hopper hopperBlock = (org.bukkit.block.Hopper) topHolder;
      org.bukkit.Location loc = hopperBlock.getLocation();
      if (!chunkHopperManager.hasFilteredHopper(loc)) return;
      org.bukkit.inventory.ItemStack moving = null;
      if (event.isShiftClick()) {
        moving = event.getCurrentItem();
      } else if (event.getClickedInventory() == topInv) {
        if (event.getClick() == org.bukkit.event.inventory.ClickType.NUMBER_KEY) {
          moving = view.getBottomInventory().getItem(event.getHotbarButton());
        } else {
          moving = event.getCursor();
        }
      } else {
        return;
      }
      if (moving != null
          && moving.getType() != org.bukkit.Material.AIR
          && !isItemAllowedByFilter(loc, moving)) {
        event.setCancelled(true);
        if (event.getWhoClicked() instanceof org.bukkit.entity.Player) {
          org.bukkit.entity.Player player = (org.bukkit.entity.Player) event.getWhoClicked();
          SoundManager.playErrorSound(player);
          player.sendMessage(MessageManager.getInstance().getMessage("hopper.filter-denied"));
        }
      }
    } catch (Exception e) {
      logger.error("Error handling hopper inventory click filter: " + e.getMessage());
    }
  }

  private boolean hasHopperPermission(String owner, String permitted, String hopperUuid) {
    try {
      return com.mrh00k.hopperfilterx.managers.DatabaseManager.hasHopperPermission(
          owner, permitted, hopperUuid);
    } catch (Exception e) {
      Logger.getInstance().error("Failed to check hopper permission: " + e.getMessage());
      return false;
    }
  }
}
