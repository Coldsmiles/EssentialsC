package cn.infstar.essentialsC.admin;

import cn.infstar.essentialsC.EssentialsC;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class AdminModeManager implements Listener {

    private static final float VANILLA_FLY_SPEED = 0.1F;

    private final EssentialsC plugin;
    private final AdminModeStore store;
    private final Set<UUID> activePlayers = new HashSet<>();

    private BukkitTask actionBarTask;

    public AdminModeManager(EssentialsC plugin) {
        this.plugin = plugin;
        addConfigDefaults();
        this.store = new AdminModeStore(plugin);
    }

    public void reload() {
        if (actionBarTask != null) {
            actionBarTask.cancel();
            actionBarTask = null;
        }
        float flySpeed = getAdminFlySpeed();
        for (UUID uuid : new ArrayList<>(activePlayers)) {
            Player player = plugin.getServer().getPlayer(uuid);
            if (player != null && player.isOnline()) {
                player.setFlySpeed(flySpeed);
            }
        }
        if (!activePlayers.isEmpty()) {
            startActionBarTask();
        }
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
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        YamlConfiguration data = store.load(player.getUniqueId());
        if (data == null) {
            sendLangMessage(player, "admin-mode.messages.save-failed");
            return;
        }
        if (!data.getBoolean("active", false)) {
            return;
        }

        saveProfile(player, data, "admin");
        if (!saveData(player, data)) {
            activePlayers.add(player.getUniqueId());
            startActionBarTask();
            sendLangMessage(player, "admin-mode.messages.save-failed");
            return;
        }
        restoreNormalProfile(player, data);
        data.set("active", false);
        if (!saveData(player, data)) {
            data.set("active", true);
            loadProfile(player, data, "admin");
            activePlayers.add(player.getUniqueId());
            startActionBarTask();
            sendLangMessage(player, "admin-mode.messages.save-failed");
            return;
        }
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
        YamlConfiguration data = store.load(player.getUniqueId());
        if (data == null) {
            sendLangMessage(player, "admin-mode.messages.save-failed");
            return;
        }
        player.closeInventory();
        saveProfile(player, data, "normal");
        data.set("active", true);
        if (!saveData(player, data)) {
            sendLangMessage(player, "admin-mode.messages.save-failed");
            data.set("active", false);
            return;
        }

        if (!loadProfile(player, data, "admin")) {
            clearInventory(player);
        }

        activePlayers.add(player.getUniqueId());

        player.setGameMode(GameMode.CREATIVE);
        player.setAllowFlight(true);
        player.setFlying(true);
        player.setFlySpeed(getAdminFlySpeed());

        sendLangMessage(player, "admin-mode.messages.enabled");
        sendActionBar(player);
        startActionBarTask();
    }

    private boolean disable(Player player, boolean notify) {
        YamlConfiguration data = store.load(player.getUniqueId());
        if (data == null) {
            if (notify) {
                sendLangMessage(player, "admin-mode.messages.save-failed");
            }
            return false;
        }
        player.closeInventory();
        saveProfile(player, data, "admin");
        data.set("active", true);
        if (!saveData(player, data)) {
            if (notify) {
                sendLangMessage(player, "admin-mode.messages.save-failed");
            }
            return false;
        }
        restoreNormalProfile(player, data);

        data.set("active", false);
        if (!saveData(player, data)) {
            data.set("active", true);
            loadProfile(player, data, "admin");
            activePlayers.add(player.getUniqueId());
            if (notify) {
                sendLangMessage(player, "admin-mode.messages.save-failed");
            }
            return false;
        }

        activePlayers.remove(player.getUniqueId());

        if (notify) {
            sendLangMessage(player, "admin-mode.messages.disabled");
        }
        stopActionBarTaskIfIdle();
        return true;
    }

    private void restoreNormalProfile(Player player, YamlConfiguration data) {
        if (!loadProfile(player, data, "normal")) {
            clearInventory(player);
            player.setGameMode(GameMode.SURVIVAL);
            player.setAllowFlight(false);
            player.setFlying(false);
            player.setFlySpeed(VANILLA_FLY_SPEED);
        }
    }

    private void saveProfile(Player player, YamlConfiguration data, String path) {
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

    private boolean loadProfile(Player player, YamlConfiguration data, String path) {
        if (!data.contains(path)) {
            return false;
        }

        PlayerInventory inventory = player.getInventory();
        clearInventory(player);
        inventory.setStorageContents(readItemArray(data, path + ".storage", inventory.getStorageContents().length));
        inventory.setArmorContents(readItemArray(data, path + ".armor", inventory.getArmorContents().length));
        inventory.setExtraContents(readItemArray(data, path + ".extra", inventory.getExtraContents().length));
        inventory.setHeldItemSlot(clampHeldSlot(data.getInt(path + ".held-slot", inventory.getHeldItemSlot())));
        player.setItemOnCursor(readItem(data, path + ".cursor"));

        player.setGameMode(readGameMode(data, path + ".game-mode", player.getGameMode()));
        player.setAllowFlight(data.getBoolean(path + ".allow-flight", player.getAllowFlight()));
        player.setFlying(data.getBoolean(path + ".flying", false) && player.getAllowFlight());
        player.setFlySpeed(clampFlySpeed(data.getDouble(path + ".fly-speed", VANILLA_FLY_SPEED)));
        player.setHealth(readHealth(player, data, path + ".health"));
        player.setFoodLevel(clampFoodLevel(data.getInt(path + ".food-level", player.getFoodLevel())));
        player.setSaturation(clampSaturation(data.getDouble(path + ".saturation", player.getSaturation())));
        player.setExhaustion(clampExhaustion(data.getDouble(path + ".exhaustion", player.getExhaustion())));
        player.setExp(clampExp(data.getDouble(path + ".exp", player.getExp())));
        player.setLevel(Math.max(0, data.getInt(path + ".level", player.getLevel())));
        player.setTotalExperience(Math.max(0, data.getInt(path + ".total-experience", player.getTotalExperience())));
        player.setFireTicks(Math.max(0, data.getInt(path + ".fire-ticks", player.getFireTicks())));
        return true;
    }

    private ItemStack[] readItemArray(YamlConfiguration data, String path, int size) {
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

    private GameMode readGameMode(YamlConfiguration data, String path, GameMode fallback) {
        try {
            return GameMode.valueOf(data.getString(path, fallback.name()));
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }

    private double readHealth(Player player, YamlConfiguration data, String path) {
        double maxHealth = player.getAttribute(Attribute.MAX_HEALTH) != null
            ? player.getAttribute(Attribute.MAX_HEALTH).getValue()
            : player.getHealth();
        double health = data.getDouble(path, player.getHealth());
        if (!Double.isFinite(health)) {
            return Math.min(Math.max(1.0D, player.getHealth()), maxHealth);
        }
        return Math.min(Math.max(1.0D, health), maxHealth);
    }

    private ItemStack readItem(YamlConfiguration data, String path) {
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
        player.sendActionBar(EssentialsC.getLangManager().getComponent("admin-mode.actionbar"));
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

    private void sendLangMessage(Player player, String path) {
        player.sendMessage(EssentialsC.getLangManager().getPrefixedString(path));
    }

    private boolean saveData(Player player, YamlConfiguration data) {
        return store.save(player.getUniqueId(), data);
    }

    private void addConfigDefaults() {
        plugin.getConfig().addDefault("admin-mode.fly-speed", 0.2D);
        plugin.getConfig().addDefault("admin-mode.actionbar.interval-ticks", 40);
        plugin.getConfig().options().copyDefaults(true);
    }
}
