package cn.infstar.essentialsC.tpsbar;

import cn.infstar.essentialsC.EssentialsC;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.CommandMap;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class TpsBarManager implements Listener, TpsBarService {

    private static final double MAX_TPS = 20.0D;
    private static final int UPDATE_INTERVAL_TICKS = 20;
    private static final BarStyle BAR_STYLE = BarStyle.SEGMENTED_20;
    private static final double TPS_WARN_THRESHOLD = 18.0D;
    private static final double TPS_CRITICAL_THRESHOLD = 15.0D;
    private static final double MSPT_WARN_THRESHOLD = 40.0D;
    private static final double MSPT_CRITICAL_THRESHOLD = 50.0D;
    private static final double PING_WARN_THRESHOLD = 100.0D;
    private static final double PING_CRITICAL_THRESHOLD = 200.0D;

    private final EssentialsC plugin;
    private final Set<UUID> enabledPlayers = new LinkedHashSet<>();
    private final Map<UUID, BossBar> activeBars = new java.util.HashMap<>();

    private BukkitTask updateTask;
    private Mode mode;
    private boolean pluginCommandEnabled;
    private boolean nativeCommandAvailable;
    private String titleFormat;
    private String enabledSelfMessage;
    private String disabledSelfMessage;
    private String enabledOtherMessage;
    private String disabledOtherMessage;
    private String usageMessage;
    private String playerNotFoundMessage;
    private String noTargetsMessage;
    private String nativeDetectedMessage;
    private String pluginEnabledMessage;
    private String pluginForcedButNativeExistsMessage;
    private String modeChangedReloadMessage;

    public TpsBarManager(EssentialsC plugin) {
        this.plugin = plugin;
        addConfigDefaults();
        reloadSettings();
    }

    @Override
    public boolean isPluginCommandEnabled() {
        return pluginCommandEnabled;
    }

    @Override
    public boolean isNativeCommandAvailable() {
        return nativeCommandAvailable;
    }

    @Override
    public void reloadSettings() {
        FileConfiguration config = plugin.getConfig();
        var lang = EssentialsC.getLangManager();
        Mode previousMode = this.mode;

        this.mode = Mode.fromString(config.getString("tpsbar.mode", "auto"));
        this.titleFormat = lang.getString("tpsbar.title-format");
        this.enabledSelfMessage = lang.getString("tpsbar.messages.enabled-self");
        this.disabledSelfMessage = lang.getString("tpsbar.messages.disabled-self");
        this.enabledOtherMessage = lang.getString("tpsbar.messages.enabled-other");
        this.disabledOtherMessage = lang.getString("tpsbar.messages.disabled-other");
        this.usageMessage = lang.getString("tpsbar.messages.usage");
        this.playerNotFoundMessage = lang.getString("tpsbar.messages.player-not-found");
        this.noTargetsMessage = lang.getString("tpsbar.messages.no-targets");
        this.nativeDetectedMessage = lang.getString("tpsbar.messages.native-detected");
        this.pluginEnabledMessage = lang.getString("tpsbar.messages.plugin-enabled");
        this.pluginForcedButNativeExistsMessage = lang.getString("tpsbar.messages.plugin-forced-but-native-exists");
        this.modeChangedReloadMessage = lang.getString("tpsbar.messages.mode-changed-reload");

        this.nativeCommandAvailable = detectNativeTpsBar();
        this.pluginCommandEnabled = switch (mode) {
            case OFF -> false;
            case AUTO -> !nativeCommandAvailable;
            case ON -> !nativeCommandAvailable;
        };

        if (previousMode != null && previousMode != mode) {
            plugin.getLogger().info(stripColor(modeChangedReloadMessage));
        }

        if (nativeCommandAvailable) {
            if (mode == Mode.AUTO) {
                plugin.getLogger().info(stripColor(nativeDetectedMessage));
            } else if (mode == Mode.ON) {
                plugin.getLogger().warning(stripColor(pluginForcedButNativeExistsMessage));
            }
        } else if (pluginCommandEnabled) {
            plugin.getLogger().info(stripColor(pluginEnabledMessage));
        }

        if (!pluginCommandEnabled) {
            clearActiveBars();
            return;
        }

        restartTaskIfNeeded();
        refreshBars();
    }

    @Override
    public void shutdown() {
        clearActiveBars();
    }

    @Override
    public boolean toggle(Player target) {
        if (enabledPlayers.contains(target.getUniqueId())) {
            disable(target);
            return false;
        }

        enable(target);
        return true;
    }

    @Override
    public void sendToggleMessage(Player actor, Player target, boolean enabled) {
        if (actor.getUniqueId().equals(target.getUniqueId())) {
            actor.sendMessage(prefixed(enabled ? enabledSelfMessage : disabledSelfMessage));
            return;
        }

        actor.sendMessage(prefixed(applyPlaceholders(
            enabled ? enabledOtherMessage : disabledOtherMessage,
            Map.of("player", target.getName())
        )));
    }

    @Override
    public String getUsageMessage() {
        return prefixed(usageMessage);
    }

    @Override
    public String getPlayerNotFoundMessage(String input) {
        return prefixed(applyPlaceholders(playerNotFoundMessage, Map.of("player", input)));
    }

    @Override
    public String getNoTargetsMessage() {
        return prefixed(noTargetsMessage);
    }

    @Override
    public Collection<Player> resolveTargets(Player sender, String input) {
        Set<Player> targets = new LinkedHashSet<>();
        try {
            for (Entity entity : Bukkit.selectEntities(sender, input)) {
                if (entity instanceof Player target) {
                    targets.add(target);
                }
            }
        } catch (IllegalArgumentException ignored) {
        }

        if (!targets.isEmpty()) {
            return targets;
        }

        Player target = Bukkit.getPlayerExact(input);
        if (target != null) {
            targets.add(target);
        }
        return targets;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        disable(event.getPlayer());
    }

    private void clearActiveBars() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }

        for (BossBar bossBar : activeBars.values()) {
            bossBar.removeAll();
        }
        activeBars.clear();
        enabledPlayers.clear();
    }

    private void enable(Player player) {
        enabledPlayers.add(player.getUniqueId());

        BossBar bossBar = activeBars.computeIfAbsent(player.getUniqueId(), uuid ->
            Bukkit.createBossBar("", BarColor.GREEN, BAR_STYLE)
        );
        bossBar.setVisible(true);
        bossBar.addPlayer(player);

        updateBar(player, bossBar);
        startTaskIfNeeded();
    }

    private void disable(Player player) {
        UUID uuid = player.getUniqueId();
        enabledPlayers.remove(uuid);

        BossBar bossBar = activeBars.remove(uuid);
        if (bossBar != null) {
            bossBar.removeAll();
        }

        stopTaskIfIdle();
    }

    private void startTaskIfNeeded() {
        if (updateTask != null || enabledPlayers.isEmpty()) {
            return;
        }

        updateTask = Bukkit.getScheduler().runTaskTimer(plugin, this::refreshBars, 0L, UPDATE_INTERVAL_TICKS);
    }

    private void restartTaskIfNeeded() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
        startTaskIfNeeded();
    }

    private void stopTaskIfIdle() {
        if (!enabledPlayers.isEmpty() || updateTask == null) {
            return;
        }

        updateTask.cancel();
        updateTask = null;
    }

    private void refreshBars() {
        List<UUID> stalePlayers = new ArrayList<>();
        for (UUID uuid : enabledPlayers) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) {
                stalePlayers.add(uuid);
                continue;
            }

            BossBar bossBar = activeBars.computeIfAbsent(uuid, ignored ->
                Bukkit.createBossBar("", BarColor.GREEN, BAR_STYLE)
            );
            if (!bossBar.getPlayers().contains(player)) {
                bossBar.addPlayer(player);
            }
            updateBar(player, bossBar);
        }

        for (UUID uuid : stalePlayers) {
            enabledPlayers.remove(uuid);
            BossBar bossBar = activeBars.remove(uuid);
            if (bossBar != null) {
                bossBar.removeAll();
            }
        }

        stopTaskIfIdle();
    }

    private void updateBar(Player player, BossBar bossBar) {
        double tps = clampTps(plugin.getServer().getTPS()[0]);
        double mspt = clampMspt(plugin.getServer().getAverageTickTime());
        int ping = Math.max(0, player.getPing());

        bossBar.setTitle(buildTitle(tps, mspt, ping));
        bossBar.setColor(resolveBarColor(tps, mspt, ping));
        bossBar.setStyle(BAR_STYLE);
        bossBar.setProgress(clamp(tps / MAX_TPS, 0.0D, 1.0D));
    }

    private String buildTitle(double tps, double mspt, int ping) {
        return ChatColor.translateAlternateColorCodes('&', applyPlaceholders(titleFormat, Map.of(
            "tps_1m", formatDouble(tps),
            "mspt", formatDouble(mspt),
            "ping", Integer.toString(ping)
        )));
    }

    private BarColor resolveBarColor(double tps, double mspt, int ping) {
        if (tps <= TPS_CRITICAL_THRESHOLD || mspt >= MSPT_CRITICAL_THRESHOLD || ping >= PING_CRITICAL_THRESHOLD) {
            return BarColor.RED;
        }
        if (tps <= TPS_WARN_THRESHOLD || mspt >= MSPT_WARN_THRESHOLD || ping >= PING_WARN_THRESHOLD) {
            return BarColor.YELLOW;
        }
        return BarColor.GREEN;
    }

    private boolean detectNativeTpsBar() {
        try {
            CommandMap commandMap = Bukkit.getCommandMap();
            org.bukkit.command.Command command = commandMap.getCommand("tpsbar");
            return command != null;
        } catch (Exception ignored) {
            return false;
        }
    }

    private void addConfigDefaults() {
        FileConfiguration config = plugin.getConfig();
        config.addDefault("tpsbar.mode", "auto");
        config.options().copyDefaults(true);
        plugin.saveConfig();
    }

    private String prefixed(String message) {
        return EssentialsC.getLangManager().getPrefix() + ChatColor.translateAlternateColorCodes('&', message);
    }

    private String applyPlaceholders(String text, Map<String, String> placeholders) {
        String result = text == null ? "" : text;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }

    private String stripColor(String message) {
        return ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', message));
    }

    private String formatDouble(double value) {
        return String.format(Locale.US, "%.2f", value);
    }

    private double clampTps(double value) {
        if (!Double.isFinite(value)) {
            return 0.0D;
        }
        return clamp(value, 0.0D, MAX_TPS);
    }

    private double clampMspt(double value) {
        if (!Double.isFinite(value)) {
            return 0.0D;
        }
        return Math.max(0.0D, value);
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private enum Mode {
        OFF,
        AUTO,
        ON;

        private static Mode fromString(String value) {
            if (value == null) {
                return AUTO;
            }
            return switch (value.toLowerCase(Locale.ROOT)) {
                case "off", "false", "disabled" -> OFF;
                case "on", "enabled", "plugin" -> ON;
                default -> AUTO;
            };
        }
    }
}
