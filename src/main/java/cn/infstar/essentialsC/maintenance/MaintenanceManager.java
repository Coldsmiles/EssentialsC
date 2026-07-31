package cn.infstar.essentialsC.maintenance;

import cn.infstar.essentialsC.EssentialsC;
import cn.infstar.essentialsC.util.AtomicYamlWriter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public final class MaintenanceManager {

    private static final int CURRENT_CONFIG_VERSION = 1;

    private final EssentialsC plugin;
    private final File configFile;
    private FileConfiguration config;
    private BossBar bossBar;

    public MaintenanceManager(EssentialsC plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "maintenance.yml");
        reload();
    }

    public void reload() {
        if (!configFile.exists()) {
            plugin.saveResource("maintenance.yml", false);
        }

        config = YamlConfiguration.loadConfiguration(configFile);
        config.addDefault("config-version", CURRENT_CONFIG_VERSION);
        config.addDefault("enabled", false);
        config.addDefault("bypass-permission", "essentialsc.maintenance.bypass");
        config.addDefault("notify.enabled", true);
        config.addDefault("notify.permission", "essentialsc.maintenance.notify");
        config.addDefault("notify.include-address", false);
        config.addDefault("whitelist.uuids", List.of());
        config.addDefault("whitelist.names", List.of());
        config.addDefault("motd.enabled", true);
        config.addDefault("motd.lines", List.of("&c服务器维护中", "&7请稍后再试"));
        config.addDefault("kick-message", List.of("&c服务器正在维护", "&7请稍后再进入"));
        config.addDefault("bossbar.enabled", true);
        config.addDefault("bossbar.title", "&c服务器正在维护");
        config.addDefault("bossbar.color", "RED");
        config.addDefault("bossbar.style", "SOLID");
        config.addDefault("bossbar.progress", 1.0D);
        config.options().copyDefaults(true);
        refreshBossBar();
        if (isEnabled()) {
            kickUnauthorizedPlayers();
        }
    }

    public boolean isEnabled() {
        return config.getBoolean("enabled", false);
    }

    public void setEnabled(boolean enabled) {
        config.set("enabled", enabled);
        save();
        refreshBossBar();
        if (enabled) {
            kickUnauthorizedPlayers();
        }
    }

    public String getBypassPermission() {
        return config.getString("bypass-permission", "essentialsc.maintenance.bypass");
    }

    public boolean isNotifyEnabled() {
        return config.getBoolean("notify.enabled", true);
    }

    public String getNotifyPermission() {
        return config.getString("notify.permission", "essentialsc.maintenance.notify");
    }

    public boolean shouldIncludeAddressInNotification() {
        return config.getBoolean("notify.include-address", false);
    }

    public boolean canJoin(Player player) {
        return player != null && (player.hasPermission(getBypassPermission()) || isWhitelisted(player));
    }

    public boolean isWhitelisted(Player player) {
        if (player == null) {
            return false;
        }

        return getWhitelistUuids().contains(player.getUniqueId().toString().toLowerCase(Locale.ROOT))
            || getWhitelistNames().contains(normalizeName(player.getName()));
    }

    public boolean addWhitelistEntry(String input) {
        String normalized = normalizeEntry(input);
        if (normalized.isEmpty()) {
            return false;
        }

        UUID uuid = parseUuid(normalized);
        if (uuid != null) {
            return addWhitelistUuid(uuid.toString());
        }

        Player onlinePlayer = Bukkit.getPlayerExact(normalized);
        if (onlinePlayer != null) {
            return addWhitelistUuid(onlinePlayer.getUniqueId().toString());
        }

        return addWhitelistName(input);
    }

    public boolean removeWhitelistEntry(String input) {
        String normalized = normalizeEntry(input);
        if (normalized.isEmpty()) {
            return false;
        }

        UUID uuid = parseUuid(normalized);
        if (uuid != null) {
            return removeWhitelistUuid(uuid.toString());
        }

        Player onlinePlayer = Bukkit.getPlayerExact(normalized);
        if (onlinePlayer != null && removeWhitelistUuid(onlinePlayer.getUniqueId().toString())) {
            return true;
        }

        return removeWhitelistName(input);
    }

    public List<String> getWhitelistEntries() {
        List<String> entries = new ArrayList<>();
        entries.addAll(getWhitelistNames());
        entries.addAll(getWhitelistUuids());
        return entries;
    }

    public int getWhitelistCount() {
        return getWhitelistEntries().size();
    }

    public boolean isMotdEnabled() {
        return config.getBoolean("motd.enabled", true);
    }

    public String getMotd() {
        return colorizeLines(config.getStringList("motd.lines"));
    }

    public String getKickMessage() {
        return colorizeLines(config.getStringList("kick-message"));
    }

    public void refreshBossBar() {
        clearBossBar();
        if (!isEnabled() || !isBossBarEnabled()) {
            return;
        }

        bossBar = plugin.getServer().createBossBar(
            colorize(config.getString("bossbar.title", "&c服务器正在维护")),
            readBossBarColor(),
            readBossBarStyle()
        );
        bossBar.setProgress(readBossBarProgress());
        bossBar.setVisible(true);

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            addBossBarPlayer(player);
        }
    }

    public void addBossBarPlayer(Player player) {
        if (bossBar == null || player == null || !player.isOnline()) {
            return;
        }
        bossBar.addPlayer(player);
    }

    public void removeBossBarPlayer(Player player) {
        if (bossBar != null && player != null) {
            bossBar.removePlayer(player);
        }
    }

    private void kickUnauthorizedPlayers() {
        String message = getKickMessage();
        for (Player player : List.copyOf(plugin.getServer().getOnlinePlayers())) {
            if (!canJoin(player)) {
                player.kickPlayer(message);
            }
        }
    }

    public void shutdown() {
        clearBossBar();
    }

    private void clearBossBar() {
        if (bossBar == null) {
            return;
        }

        bossBar.removeAll();
        bossBar = null;
    }

    private boolean isBossBarEnabled() {
        return config.getBoolean("bossbar.enabled", true);
    }

    private BarColor readBossBarColor() {
        try {
            return BarColor.valueOf(config.getString("bossbar.color", "RED").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return BarColor.RED;
        }
    }

    private BarStyle readBossBarStyle() {
        try {
            return BarStyle.valueOf(config.getString("bossbar.style", "SOLID").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return BarStyle.SOLID;
        }
    }

    private double readBossBarProgress() {
        double progress = config.getDouble("bossbar.progress", 1.0D);
        if (!Double.isFinite(progress)) {
            return 1.0D;
        }
        return Math.max(0.0D, Math.min(1.0D, progress));
    }

    private boolean addWhitelistUuid(String uuid) {
        return updateWhitelist("whitelist.uuids", uuid.toLowerCase(Locale.ROOT), true);
    }

    private boolean addWhitelistName(String name) {
        return updateWhitelist("whitelist.names", normalizeName(name), true);
    }

    private boolean removeWhitelistUuid(String uuid) {
        return updateWhitelist("whitelist.uuids", uuid.toLowerCase(Locale.ROOT), false);
    }

    private boolean removeWhitelistName(String name) {
        return updateWhitelist("whitelist.names", normalizeName(name), false);
    }

    private boolean updateWhitelist(String path, String value, boolean add) {
        if (value == null || value.isBlank()) {
            return false;
        }

        Set<String> entries = path.endsWith(".names") ? getWhitelistNames() : getWhitelistUuids();
        boolean changed;
        if (add) {
            changed = entries.add(value);
        } else {
            changed = entries.remove(value);
        }

        if (changed) {
            config.set(path, new ArrayList<>(entries));
            save();
            refreshBossBar();
            if (isEnabled()) {
                kickUnauthorizedPlayers();
            }
        }
        return changed;
    }

    private Set<String> getWhitelistUuids() {
        Set<String> entries = new LinkedHashSet<>();
        for (String value : config.getStringList("whitelist.uuids")) {
            String normalized = normalizeEntry(value).toLowerCase(Locale.ROOT);
            if (!normalized.isEmpty()) {
                entries.add(normalized);
            }
        }
        return entries;
    }

    private Set<String> getWhitelistNames() {
        Set<String> entries = new LinkedHashSet<>();
        for (String value : config.getStringList("whitelist.names")) {
            String normalized = normalizeName(value);
            if (!normalized.isEmpty()) {
                entries.add(normalized);
            }
        }
        return entries;
    }

    private UUID parseUuid(String input) {
        try {
            return UUID.fromString(input);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private String normalizeEntry(String input) {
        return input == null ? "" : input.trim();
    }

    private String normalizeName(String input) {
        return normalizeEntry(input).toLowerCase(Locale.ROOT);
    }

    private String colorizeLines(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return "";
        }

        return String.join("\n", lines.stream()
            .map(this::colorize)
            .toList());
    }

    private String colorize(String text) {
        return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text);
    }

    private void save() {
        try {
            AtomicYamlWriter.save(config, configFile);
        } catch (Exception e) {
            plugin.getLogger().warning("保存 maintenance.yml 失败: " + e.getMessage());
        }
    }
}
