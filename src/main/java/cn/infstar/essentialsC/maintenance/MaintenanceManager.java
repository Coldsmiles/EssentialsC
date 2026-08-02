package cn.infstar.essentialsC.maintenance;

import cn.infstar.essentialsC.EssentialsC;
import cn.infstar.essentialsC.util.AtomicYamlWriter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public final class MaintenanceManager {

    private static final int CURRENT_CONFIG_VERSION = 1;
    private static final LegacyComponentSerializer LEGACY_AMPERSAND = LegacyComponentSerializer.legacyAmpersand();
    private static final LegacyComponentSerializer LEGACY_SECTION = LegacyComponentSerializer.legacySection();

    private final EssentialsC plugin;
    private final File configFile;
    private FileConfiguration config;
    private BossBar bossBar;
    private volatile AccessSnapshot accessSnapshot = AccessSnapshot.disabled();

    public MaintenanceManager(EssentialsC plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "maintenance.yml");
        reload();
    }

    public void reload() {
        if (!configFile.exists()) {
            plugin.saveResource("maintenance.yml", false);
        }

        config = loadConfiguration();
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
        publishAccessSnapshot();
        refreshBossBar();
        if (isEnabled()) {
            kickUnauthorizedPlayers();
        }
    }

    public boolean isEnabled() {
        return config.getBoolean("enabled", false);
    }

    public OperationResult setEnabled(boolean enabled) {
        if (isEnabled() == enabled) {
            return OperationResult.UNCHANGED;
        }
        boolean previous = isEnabled();
        config.set("enabled", enabled);
        if (!save()) {
            config.set("enabled", previous);
            return OperationResult.SAVE_FAILED;
        }
        publishAccessSnapshot();
        refreshBossBar();
        if (enabled) {
            kickUnauthorizedPlayers();
        }
        return OperationResult.SUCCESS;
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

    public AccessSnapshot getAccessSnapshot() {
        return accessSnapshot;
    }

    public boolean isWhitelisted(Player player) {
        if (player == null) {
            return false;
        }

        return getWhitelistUuids().contains(player.getUniqueId().toString().toLowerCase(Locale.ROOT))
            || getWhitelistNames().contains(normalizeName(player.getName()));
    }

    public OperationResult addWhitelistEntry(String input) {
        String normalized = normalizeEntry(input);
        if (normalized.isEmpty()) {
            return OperationResult.UNCHANGED;
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

    public OperationResult removeWhitelistEntry(String input) {
        String normalized = normalizeEntry(input);
        if (normalized.isEmpty()) {
            return OperationResult.UNCHANGED;
        }

        UUID uuid = parseUuid(normalized);
        if (uuid != null) {
            return removeWhitelistUuid(uuid.toString());
        }

        Player onlinePlayer = Bukkit.getPlayerExact(normalized);
        if (onlinePlayer != null) {
            OperationResult uuidResult = removeWhitelistUuid(onlinePlayer.getUniqueId().toString());
            if (uuidResult != OperationResult.UNCHANGED) {
                return uuidResult;
            }
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

    public Component getMotd() {
        return colorizeLines(config.getStringList("motd.lines"));
    }

    public Component getKickMessage() {
        return colorizeLines(config.getStringList("kick-message"));
    }

    public void refreshBossBar() {
        clearBossBar();
        if (!isEnabled() || !isBossBarEnabled()) {
            return;
        }

        bossBar = plugin.getServer().createBossBar(
            LEGACY_SECTION.serialize(colorize(config.getString("bossbar.title", "&c服务器正在维护"))),
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
        Component message = getKickMessage();
        for (Player player : List.copyOf(plugin.getServer().getOnlinePlayers())) {
            if (!canJoin(player)) {
                player.kick(message);
            }
        }
    }

    public void shutdown() {
        accessSnapshot = AccessSnapshot.disabled();
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

    private OperationResult addWhitelistUuid(String uuid) {
        return updateWhitelist("whitelist.uuids", uuid.toLowerCase(Locale.ROOT), true);
    }

    private OperationResult addWhitelistName(String name) {
        return updateWhitelist("whitelist.names", normalizeName(name), true);
    }

    private OperationResult removeWhitelistUuid(String uuid) {
        return updateWhitelist("whitelist.uuids", uuid.toLowerCase(Locale.ROOT), false);
    }

    private OperationResult removeWhitelistName(String name) {
        return updateWhitelist("whitelist.names", normalizeName(name), false);
    }

    private OperationResult updateWhitelist(String path, String value, boolean add) {
        if (value == null || value.isBlank()) {
            return OperationResult.UNCHANGED;
        }

        List<String> previousEntries = new ArrayList<>(config.getStringList(path));
        Set<String> entries = path.endsWith(".names") ? getWhitelistNames() : getWhitelistUuids();
        boolean changed;
        if (add) {
            changed = entries.add(value);
        } else {
            changed = entries.remove(value);
        }

        if (!changed) {
            return OperationResult.UNCHANGED;
        }

        config.set(path, new ArrayList<>(entries));
        if (!save()) {
            config.set(path, previousEntries);
            return OperationResult.SAVE_FAILED;
        }
        publishAccessSnapshot();
        refreshBossBar();
        if (isEnabled()) {
            kickUnauthorizedPlayers();
        }
        return OperationResult.SUCCESS;
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

    private Component colorizeLines(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return Component.empty();
        }

        return colorize(String.join("\n", lines));
    }

    private Component colorize(String text) {
        return LEGACY_AMPERSAND.deserialize(text == null ? "" : text);
    }

    private void publishAccessSnapshot() {
        Set<UUID> whitelistUuids = new LinkedHashSet<>();
        for (String value : getWhitelistUuids()) {
            UUID uuid = parseUuid(value);
            if (uuid != null) {
                whitelistUuids.add(uuid);
            }
        }
        accessSnapshot = new AccessSnapshot(
            isEnabled(),
            getBypassPermission(),
            whitelistUuids,
            getWhitelistNames(),
            getKickMessage(),
            isNotifyEnabled(),
            getNotifyPermission(),
            shouldIncludeAddressInNotification()
        );
    }

    private boolean save() {
        try {
            AtomicYamlWriter.save(config, configFile);
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("保存 maintenance.yml 失败: " + e.getMessage());
            return false;
        }
    }

    private YamlConfiguration loadConfiguration() {
        YamlConfiguration loaded = new YamlConfiguration();
        loaded.options().parseComments(true);
        try {
            loaded.load(configFile);
        } catch (IOException | InvalidConfigurationException exception) {
            plugin.getLogger().severe("加载 maintenance.yml 失败: " + exception.getMessage());
            throw new IllegalStateException("无法加载 maintenance.yml，请修复配置格式后重试。", exception);
        }
        return loaded;
    }

    public record AccessSnapshot(boolean enabled, String bypassPermission, Set<UUID> whitelistUuids,
                                 Set<String> whitelistNames, Component kickMessage, boolean notifyEnabled,
                                 String notifyPermission, boolean includeAddress) {

        public AccessSnapshot {
            bypassPermission = bypassPermission == null ? "" : bypassPermission;
            whitelistUuids = Set.copyOf(whitelistUuids);
            whitelistNames = Set.copyOf(whitelistNames);
            kickMessage = kickMessage == null ? Component.empty() : kickMessage;
            notifyPermission = notifyPermission == null ? "" : notifyPermission;
        }

        public boolean isWhitelisted(UUID uniqueId, String playerName) {
            return whitelistUuids.contains(uniqueId)
                || whitelistNames.contains(playerName == null ? "" : playerName.trim().toLowerCase(Locale.ROOT));
        }

        private static AccessSnapshot disabled() {
            return new AccessSnapshot(false, "", Set.of(), Set.of(), Component.empty(), false, "", false);
        }
    }

    public enum OperationResult {
        SUCCESS,
        UNCHANGED,
        SAVE_FAILED
    }
}
