package dev.tomle.phoenixlogin.manager;

import dev.tomle.phoenixlogin.PhoenixLogin;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class InventoryManager {

    private final PhoenixLogin plugin;
    private final File inventoryDir;

    public InventoryManager(PhoenixLogin plugin) {
        this.plugin = plugin;
        this.inventoryDir = new File(plugin.getDataFolder(), "userdata" + File.separator + "inventories");
        if (!inventoryDir.exists()) {
            inventoryDir.mkdirs();
        }
    }

    public void cacheAndClearInventory(Player player) {
        if (hasBackup(player)) {
            plugin.getLogger().warning("Backup already exists for " + player.getName()
                    + ", preserving original items. Clearing current inventory.");
            player.getInventory().clear();
            player.getInventory().setArmorContents(null);
            return;
        }

        saveInventoryToFile(player);

        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
    }

    public void restoreInventory(Player player) {
        if (!hasBackup(player)) {
            return;
        }

        try {
            File file = getPlayerFile(player);
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

            List<ItemStack> contentList = (List<ItemStack>) config.getList("inventory");
            ItemStack[] contents = contentList.toArray(new ItemStack[0]);

            List<ItemStack> armorList = (List<ItemStack>) config.getList("armor");
            ItemStack[] armor = armorList.toArray(new ItemStack[0]);

            player.getInventory().setContents(contents);
            player.getInventory().setArmorContents(armor);

            // Restore stats
            if (config.contains("gamemode")) {
                try {
                    player.setGameMode(org.bukkit.GameMode.valueOf(config.getString("gamemode")));
                } catch (IllegalArgumentException e) {
                    // Ignore invalid gamemode
                }
            }
            if (config.contains("health")) {
                double health = config.getDouble("health");
                double maxHealth = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue();
                if (health > 0 && health <= maxHealth) {
                    player.setHealth(health);
                }
            }
            if (config.contains("food")) {
                player.setFoodLevel(config.getInt("food"));
            }
            if (config.contains("level")) {
                player.setLevel(config.getInt("level"));
            }
            if (config.contains("exp")) {
                player.setExp((float) config.getDouble("exp"));
            }

            // Delete file after successful restore
            file.delete();

        } catch (Exception e) {
            plugin.getLogger().severe("Failed to restore inventory for " + player.getName());
            plugin.getLogger().severe(e.getMessage());
        }
    }

    private void saveInventoryToFile(Player player) {
        try {
            File file = getPlayerFile(player);
            YamlConfiguration config = new YamlConfiguration();

            config.set("inventory", player.getInventory().getContents());
            config.set("armor", player.getInventory().getArmorContents());

            // Save stats
            config.set("gamemode", player.getGameMode().name());
            config.set("health", player.getHealth());
            config.set("food", player.getFoodLevel());
            config.set("level", player.getLevel());
            config.set("exp", player.getExp());

            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save inventory for: " + player.getName());
            plugin.getLogger().severe(e.getMessage());
        }
    }

    private boolean hasBackup(Player player) {
        return getPlayerFile(player).exists();
    }

    private File getPlayerFile(Player player) {
        return new File(inventoryDir, player.getUniqueId().toString() + ".yml");
    }
}
