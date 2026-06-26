package dev.tomle.phoenixlogin.manager;

import dev.tomle.phoenixlogin.PhoenixLogin;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.List;
import java.util.UUID;

/**
 * Saves and restores player inventories during authentication.
 * Uses file-based backups to prevent item loss even on crashes.
 */
public class InventoryManager {

    private final PhoenixLogin plugin;
    private final File backupFolder;

    public InventoryManager(PhoenixLogin plugin) {
        this.plugin = plugin;
        this.backupFolder = new File(plugin.getDataFolder(), "inventories");
        if (!backupFolder.exists()) {
            backupFolder.mkdirs();
        }
    }

    /**
     * Saves the player's inventory to file and clears it.
     */
    public void cacheAndClearInventory(Player player) {
        File file = getInventoryFile(player.getUniqueId());

        // Don't overwrite if backup already exists (player disconnected before
        // restoring)
        if (file.exists()) {
            plugin.getLogger().info("Inventory backup already exists for " + player.getName() + ", skipping save.");
            player.getInventory().clear();
            return;
        }

        try {
            YamlConfiguration config = new YamlConfiguration();
            config.set("inventory", player.getInventory().getContents());
            config.set("armor", player.getInventory().getArmorContents());
            config.set("offhand", player.getInventory().getItemInOffHand());

            // Save player stats
            config.set("health", player.getHealth());

            // Use reflection to get max health for version compatibility
            double maxHealth = getMaxHealth(player);
            config.set("max-health", maxHealth);

            config.set("food", player.getFoodLevel());
            config.set("saturation", player.getSaturation());
            config.set("exp", player.getExp());
            config.set("level", player.getLevel());
            config.set("fire-ticks", player.getFireTicks());

            config.save(file);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to save inventory for " + player.getName());
            e.printStackTrace();
            return; // Don't clear inventory if save failed!
        }

        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[4]);
        player.getInventory().setItemInOffHand(null);

        player.setHealth(maxHealthSafe(player));
        player.setFoodLevel(20);
        player.setSaturation(20f);
        player.setExp(0f);
        player.setLevel(0);
        player.setFireTicks(0);
    }

    /**
     * Restores the player's inventory from file backup.
     */
    public void restoreInventory(Player player) {
        File file = getInventoryFile(player.getUniqueId());
        if (!file.exists())
            return;

        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

            // Null safety — check all lists before converting
            List<?> contentList = config.getList("inventory");
            if (contentList != null) {
                @SuppressWarnings("unchecked")
                List<ItemStack> items = (List<ItemStack>) contentList;
                player.getInventory().setContents(items.toArray(new ItemStack[0]));
            }

            List<?> armorList = config.getList("armor");
            if (armorList != null) {
                @SuppressWarnings("unchecked")
                List<ItemStack> armor = (List<ItemStack>) armorList;
                player.getInventory().setArmorContents(armor.toArray(new ItemStack[0]));
            }

            ItemStack offhand = config.getItemStack("offhand");
            if (offhand != null) {
                player.getInventory().setItemInOffHand(offhand);
            }

            // Restore stats safely
            double maxHealth = getMaxHealth(player);
            double savedHealth = config.getDouble("health", maxHealth);
            player.setHealth(Math.min(savedHealth, maxHealth));

            player.setFoodLevel(config.getInt("food", 20));
            player.setSaturation((float) config.getDouble("saturation", 20.0));
            player.setExp((float) config.getDouble("exp", 0.0));
            player.setLevel(config.getInt("level", 0));
            player.setFireTicks(config.getInt("fire-ticks", 0));

        } catch (Exception e) {
            plugin.getLogger().severe("Failed to restore inventory for " + player.getName() +
                    " — backup file may be corrupted.");
            e.printStackTrace();
        }

        // Delete backup after successful restore
        if (!file.delete()) {
            plugin.getLogger().warning("Could not delete inventory backup for " + player.getName());
        }
    }

    /**
     * Gets max health using reflection for version compatibility.
     * In 1.21.2+ Attribute.GENERIC_MAX_HEALTH was renamed to Attribute.MAX_HEALTH.
     */
    private double getMaxHealth(Player player) {
        try {
            // Try modern name first (1.21.2+)
            org.bukkit.attribute.Attribute attr = org.bukkit.attribute.Attribute.valueOf("MAX_HEALTH");
            org.bukkit.attribute.AttributeInstance instance = player.getAttribute(attr);
            if (instance != null)
                return instance.getValue();
        } catch (Exception ignored) {
        }

        try {
            // Fallback to legacy name
            org.bukkit.attribute.Attribute attr = org.bukkit.attribute.Attribute.valueOf("GENERIC_MAX_HEALTH");
            org.bukkit.attribute.AttributeInstance instance = player.getAttribute(attr);
            if (instance != null)
                return instance.getValue();
        } catch (Exception ignored) {
        }

        // Ultimate fallback
        return 20.0;
    }

    private double maxHealthSafe(Player player) {
        return getMaxHealth(player);
    }

    private File getInventoryFile(UUID uuid) {
        return new File(backupFolder, uuid.toString() + ".yml");
    }
}
