package cn.infstar.essentialsC.maintenance;

import cn.infstar.essentialsC.EssentialsC;
import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

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
        config.addDefault("motd.enabled", true);
        config.addDefault("motd.lines", List.of("&c服务器维护中", "&7请稍后再试"));
        config.addDefault("kick-message", List.of("&c服务器正在维护", "&7请稍后再进入"));
        config.addDefault("bossbar.enabled", true);
        config.addDefault("bossbar.title", "&c服务器正在维护");
        config.addDefault("bossbar.color", "RED");
        config.addDefault("bossbar.style", "SOLID");
        config.addDefault("bossbar.progress", 1.0D);
        config.options().copyDefaults(true);
        save();
        refreshBossBar();
    }

    public boolean isEnabled() {
        return config.getBoolean("enabled", false);
    }

    public void setEnabled(boolean enabled) {
        config.set("enabled", enabled);
        save();
        refreshBossBar();
    }

    public String getBypassPermission() {
        return config.getString("bypass-permission", "essentialsc.maintenance.bypass");
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
        if (player.hasPermission(getBypassPermission())) {
            bossBar.removePlayer(player);
            return;
        }
        bossBar.addPlayer(player);
    }

    public void removeBossBarPlayer(Player player) {
        if (bossBar != null && player != null) {
            bossBar.removePlayer(player);
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
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().warning("保存 maintenance.yml 失败: " + e.getMessage());
        }
    }
}
