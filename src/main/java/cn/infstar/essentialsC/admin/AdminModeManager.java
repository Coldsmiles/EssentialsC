package cn.infstar.essentialsC.admin;

import cn.infstar.essentialsC.EssentialsC;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class AdminModeManager implements Listener {

    private static final float VANILLA_FLY_SPEED = 0.1F;

    private final EssentialsC plugin;
    private final File dataFile;
    private final YamlConfiguration data;
    private final Set<UUID> activePlayers = new HashSet<>();

    private BukkitTask actionBarTask;

    public AdminModeManager(EssentialsC plugin) {
        this.plugin = plugin;
        addConfigDefaults();
        this.dataFile = new File(plugin.getDataFolder(), "admin-mode.yml");
        this.data = YamlConfiguration.loadConfiguration(dataFile);
    }

    public boolean isAdminMode(Player player) {
        return activePlayers.contains(player.getUniqueId());
    }

    public void toggle(Player player) {
        if (isAdminMode(player)) {
            disable(player, true);
        } else {
            enable(player);
        }
    }

    public void shutdown() {
        for (UUID uuid : new ArrayList<>(activePlayers)) {
            Player player = plugin.getServer().getPlayer(uuid);
            if (player != null) {
                disable(player, false);
            }
        }

        if (actionBarTask != null) {
            actionBarTask.cancel();
            actionBarTask = null;
        }
        saveData();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String playerPath = getPlayerPath(player);
        if (!data.getBoolean(playerPath + ".active", false)) {
            return;
        }

        saveProfile(player, playerPath + ".admin");
        restoreNormalProfile(player);
        data.set(playerPath + ".active", false);
        saveData();
        sendLangMessage(player, "admin-mode.messages.crash-restored");
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (isAdminMode(player)) {
            disable(player, false);
        }
    }

    private void enable(Player player) {
        String playerPath = getPlayerPath(player);
        player.closeInventory();
        saveProfile(player, playerPath + ".normal");

        if (!loadProfile(player, playerPath + ".admin")) {
            clearInventory(player);
        }

        activePlayers.add(player.getUniqueId());
        data.set(playerPath + ".active", true);

        player.setGameMode(GameMode.CREATIVE);
        player.setAllowFlight(true);
        player.setFlying(true);
        player.setFlySpeed(getAdminFlySpeed());
        saveData();

        sendLangMessage(player, "admin-mode.messages.enabled");
        sendActionBar(player);
        startActionBarTask();
    }

    private void disable(Player player, boolean notify) {
        String playerPath = getPlayerPath(player);
        player.closeInventory();
        saveProfile(player, playerPath + ".admin");
        restoreNormalProfile(player);

        activePlayers.remove(player.getUniqueId());
        data.set(playerPath + ".active", false);
        saveData();

        if (notify) {
            sendLangMessage(player, "admin-mode.messages.disabled");
        }
        stopActionBarTaskIfIdle();
    }

    private void restoreNormalProfile(Player player) {
        String playerPath = getPlayerPath(player);
        if (!loadProfile(player, playerPath + ".normal")) {
            clearInventory(player);
            player.setGameMode(GameMode.SURVIVAL);
            player.setAllowFlight(false);
            player.setFlying(false);
            player.setFlySpeed(VANILLA_FLY_SPEED);
        }
    }

    private void saveProfile(Player player, String path) {
        PlayerInventory inventory = player.getInventory();
        data.set(path + ".storage", Arrays.asList(inventory.getStorageContents()));
        data.set(path + ".armor", Arrays.asList(inventory.getArmorContents()));
        data.set(path + ".extra", Arrays.asList(inventory.getExtraContents()));
        data.set(path + ".cursor", player.getItemOnCursor());
        data.set(path + ".held-slot", inventory.getHeldItemSlot());
        data.set(path + ".game-mode", player.getGameMode().name());
        data.set(path + ".allow-flight", player.getAllowFlight());
        data.set(path + ".flying", player.isFlying());
        data.set(path + ".fly-speed", player.getFlySpeed());
        data.set(path + ".health", player.getHealth());
        data.set(path + ".food-level", player.getFoodLevel());
        data.set(path + ".saturation", player.getSaturation());
        data.set(path + ".exhaustion", player.getExhaustion());
        data.set(path + ".exp", player.getExp());
        data.set(path + ".level", player.getLevel());
        data.set(path + ".total-experience", player.getTotalExperience());
        data.set(path + ".fire-ticks", player.getFireTicks());
    }

    private boolean loadProfile(Player player, String path) {
        if (!data.contains(path)) {
            return false;
        }

        PlayerInventory inventory = player.getInventory();
        clearInventory(player);
        inventory.setStorageContents(readItemArray(path + ".storage", inventory.getStorageContents().length));
        inventory.setArmorContents(readItemArray(path + ".armor", inventory.getArmorContents().length));
        inventory.setExtraContents(readItemArray(path + ".extra", inventory.getExtraContents().length));
        inventory.setHeldItemSlot(clampHeldSlot(data.getInt(path + ".held-slot", inventory.getHeldItemSlot())));
        player.setItemOnCursor(readItem(path + ".cursor"));

        player.setGameMode(readGameMode(path + ".game-mode", player.getGameMode()));
        player.setAllowFlight(data.getBoolean(path + ".allow-flight", player.getAllowFlight()));
        player.setFlying(data.getBoolean(path + ".flying", false) && player.getAllowFlight());
        player.setFlySpeed(clampFlySpeed(data.getDouble(path + ".fly-speed", VANILLA_FLY_SPEED)));
        player.setHealth(readHealth(player, path + ".health"));
        player.setFoodLevel(clampFoodLevel(data.getInt(path + ".food-level", player.getFoodLevel())));
        player.setSaturation(clampSaturation(data.getDouble(path + ".saturation", player.getSaturation())));
        player.setExhaustion(clampExhaustion(data.getDouble(path + ".exhaustion", player.getExhaustion())));
        player.setExp(clampExp(data.getDouble(path + ".exp", player.getExp())));
        player.setLevel(Math.max(0, data.getInt(path + ".level", player.getLevel())));
        player.setTotalExperience(Math.max(0, data.getInt(path + ".total-experience", player.getTotalExperience())));
        player.setFireTicks(Math.max(0, data.getInt(path + ".fire-ticks", player.getFireTicks())));
        return true;
    }

    private ItemStack[] readItemArray(String path, int size) {
        ItemStack[] items = new ItemStack[size];
        List<?> list = data.getList(path);
        if (list == null) {
            return items;
        }

        for (int index = 0; index < Math.min(size, list.size()); index++) {
            Object value = list.get(index);
            if (value instanceof ItemStack itemStack) {
                items[index] = itemStack;
            }
        }
        return items;
    }

    private GameMode readGameMode(String path, GameMode fallback) {
        try {
            return GameMode.valueOf(data.getString(path, fallback.name()));
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }

    private double readHealth(Player player, String path) {
        double maxHealth = player.getAttribute(Attribute.MAX_HEALTH) != null
            ? player.getAttribute(Attribute.MAX_HEALTH).getValue()
            : player.getHealth();
        double health = data.getDouble(path, player.getHealth());
        if (!Double.isFinite(health)) {
            return Math.min(Math.max(1.0D, player.getHealth()), maxHealth);
        }
        return Math.min(Math.max(1.0D, health), maxHealth);
    }

    private ItemStack readItem(String path) {
        Object value = data.get(path);
        if (value instanceof ItemStack itemStack) {
            return itemStack;
        }
        return new ItemStack(Material.AIR);
    }

    private void clearInventory(Player player) {
        PlayerInventory inventory = player.getInventory();
        inventory.clear();
        inventory.setArmorContents(new ItemStack[inventory.getArmorContents().length]);
        inventory.setExtraContents(new ItemStack[inventory.getExtraContents().length]);
        player.setItemOnCursor(new ItemStack(Material.AIR));
    }

    private void startActionBarTask() {
        if (actionBarTask != null) {
            return;
        }

        int interval = Math.max(10, plugin.getConfig().getInt("admin-mode.actionbar.interval-ticks", 40));
        actionBarTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            for (UUID uuid : new ArrayList<>(activePlayers)) {
                Player player = plugin.getServer().getPlayer(uuid);
                if (player != null && player.isOnline()) {
                    sendActionBar(player);
                }
            }
            stopActionBarTaskIfIdle();
        }, 0L, interval);
    }

    private void stopActionBarTaskIfIdle() {
        if (!activePlayers.isEmpty() || actionBarTask == null) {
            return;
        }

        actionBarTask.cancel();
        actionBarTask = null;
    }

    private void sendActionBar(Player player) {
        String text = EssentialsC.getLangManager().getString("admin-mode.actionbar");
        Component component = LegacyComponentSerializer.legacyAmpersand().deserialize(text);
        player.sendActionBar(component);
    }

    private float getAdminFlySpeed() {
        double speed = plugin.getConfig().getDouble("admin-mode.fly-speed", 0.2D);
        return clampFlySpeed(speed);
    }

    private float clampFlySpeed(double speed) {
        if (!Double.isFinite(speed)) {
            return VANILLA_FLY_SPEED;
        }
        return (float) Math.max(-1.0D, Math.min(1.0D, speed));
    }

    private int clampHeldSlot(int slot) {
        return Math.max(0, Math.min(8, slot));
    }

    private int clampFoodLevel(int foodLevel) {
        return Math.max(0, Math.min(20, foodLevel));
    }

    private float clampSaturation(double saturation) {
        if (!Double.isFinite(saturation)) {
            return 0.0F;
        }
        return (float) Math.max(0.0D, Math.min(20.0D, saturation));
    }

    private float clampExhaustion(double exhaustion) {
        if (!Double.isFinite(exhaustion)) {
            return 0.0F;
        }
        return (float) Math.max(0.0D, exhaustion);
    }

    private float clampExp(double exp) {
        if (!Double.isFinite(exp)) {
            return 0.0F;
        }
        return (float) Math.max(0.0D, Math.min(1.0D, exp));
    }

    private String getPlayerPath(Player player) {
        return "players." + player.getUniqueId();
    }

    private void sendLangMessage(Player player, String path) {
        player.sendMessage(EssentialsC.getLangManager().getPrefixedString(path));
    }

    private void saveData() {
        try {
            data.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save admin-mode.yml: " + e.getMessage());
        }
    }

    private void addConfigDefaults() {
        plugin.getConfig().addDefault("admin-mode.fly-speed", 0.2D);
        plugin.getConfig().addDefault("admin-mode.actionbar.interval-ticks", 40);
        plugin.getConfig().options().copyDefaults(true);
        plugin.saveConfig();
    }
}
