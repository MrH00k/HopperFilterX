![Banner](https://cdn.modrinth.com/data/cached_images/a223e1919e952a6c928c0da2f681161748ea0cd2.jpeg)

# HopperFilterX

A lightweight, multilingual Minecraft plugin that lets you filter what hoppers can pick up using a simple in-game GUI. Designed for performance, survival, and technical servers.

---

## 🔧 Features

- `/hopper give <player> [amount]`: Give special filter-enabled hoppers to players.
- `/hopper remove <player> <uuid>`: Remove a filtered hopper from a player or the world.
- `/hopper list <player>`: List all filtered hoppers owned by a player.
- `/hopper addperm <player> <uuid>`: Grant players access to your filtered hoppers.
- `/hopper removeperm <player> <uuid>`: Remove player access from your filtered hoppers.
- `/hopper reload`: Reload the plugin's configuration and language files.
- 🔍 **Filter System**: Shift + right-click a HopperFilterX hopper to open a GUI.
- 📥 **Drag and Drop Items**: Insert any items into the GUI to define the hopper’s filter.
- ✅ **Selective Item Movement**: Hopper will only move items matching the filter.
- 🔄 **Fallback Behavior**: If no filter is defined, it behaves like a regular vanilla hopper.
- 🧊 **Seamless vanilla integration**: Works with all vanilla mechanics and hoppers.
- 🗃️ **Persistent Filters**: Filter settings are saved and restored even after server restarts.
- 🛡️ **Owner Protection**: Only the owner can pick up or configure their filtered hoppers.
- 🛠️ **Creative/Survival Support**: Handles creative/survival mode transitions safely.
- 🗣️ **Multilanguage**: See below for details.

## 🗂️ Supported Minecraft Versions (uses PaperLib for compatibility)

- **Paper/Spigot 1.14.x**
- **Paper/Spigot 1.15.x**
- **Paper/Spigot 1.16.x**
- **Paper/Spigot 1.17.x**
- **Paper/Spigot 1.18.x**
- **Paper/Spigot 1.19.x**
- **Paper/Spigot 1.20.x**
- **Paper/Spigot 1.21.x**

## 🌐 Multilanguage Support

HopperFilterX is fully translated and ready for international servers. It currently supports:

- English
- Spanish
- German
- French
- Portuguese
- Russian
- Simplified Chinese

The plugin automatically detects the player’s language based on their Minecraft client settings. All messages are customizable in `lang.yml`.

## ⚙️ Configuration

- `config.yml` allows enabling debug mode for detailed logging.
- `lang.yml` contains all translatable messages and can be edited for custom language support.

## 🛠️ Commands

| Command                                 | Description                                      |
|-----------------------------------------|--------------------------------------------------|
| `/hopper give <player> [amount]`        | Give filter hoppers to a player                   |
| `/hopper remove <player> <uuid>`        | Remove a filtered hopper from a player/world      |
| `/hopper list <player>`                 | List all filtered hoppers owned by a player       |
| `/hopper addperm <player> <uuid>`       | Grant a player access to your filtered hoppers    |
| `/hopper removeperm <player> <uuid>`    | Remove a player's access from your filtered hoppers |
| `/hopper reload`                        | Reload plugin configuration and language          |

## 🔐 Permissions

| Node                      | Description                                         |
|---------------------------|-----------------------------------------------------|
| `hopperfilterx.use`       | Allows players to use the filter GUI                |
| `hopperfilterx.give`      | Allows use of the `/hopper give` command            |
| `hopperfilterx.remove`    | Allows use of the `/hopper remove` command          |
| `hopperfilterx.list`      | Allows use of the `/hopper list` command            |
| `hopperfilterx.addperm`   | Allows use of the `/hopper addperm` command         |
| `hopperfilterx.removeperm`| Allows use of the `/hopper removeperm` command      |
| `hopperfilterx.reload`    | Allows reloading the plugin via command             |
| `hopperfilterx.*`         | Grants all HopperFilterX permissions                |

## 🎮 How it Works

1. Use `/hopper give <player> [amount]` to give a player a special hopper.
2. Place the hopper like any normal one.
3. **Shift + Right-Click** on it to open the filter GUI.
4. Add items you want the hopper to accept.
5. The hopper will now only move the selected items. If the filter is empty, it works like vanilla.

## 🔗 Permission System

HopperFilterX includes a built-in permission system that allows you to share access to your filtered hoppers with other players:

- **Grant Access**: Use `/hopper addperm <player> <uuid>` to allow another player to use your filtered hoppers
  - Without UUID: Grants access to all your current and future filtered hoppers
  - With UUID: Grants access only to a specific hopper
- **Revoke Access**: Use `/hopper removeperm <player> <uuid>` to remove a player's access
- **Owner Protection**: Only the hopper owner can modify filters and manage permissions
- **Persistent**: Permissions are saved in the database and persist across server restarts

## 💡 Use Cases

- Create automated item sorters.
- Prevent specific items from being picked up in complex redstone systems.
- Set up refined farms and storage systems with ease.

## 📝 Technical & Legal

- **License:** GPL-3.0-only (see LICENSE)
- **Author:** MrH00k
- **Database:** Uses SQLite for persistent filter storage.
- **No suppression of static analysis warnings:** All warnings remain visible for maintainability.
- **No external dependencies required** (except PaperLib for version compatibility).

---

**Start organizing your hoppers like a pro — with HopperFilterX!**