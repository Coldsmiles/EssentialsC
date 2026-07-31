package cn.infstar.essentialsC.commands;

import cn.infstar.essentialsC.EssentialsC;
import cn.infstar.essentialsC.util.AtomicYamlWriter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class VanishCommand extends BaseCommand {

    private static final Set<UUID> vanishedPlayers = new HashSet<>();
    public static final String SEE_PERMISSION = "essentialsc.vanish.see";
    private static File stateFile;

    public VanishCommand() {
        super("essentialsc.command.vanish");
    }

    @Override
    protected boolean execute(Player player, String[] args) {
        UUID uuid = player.getUniqueId();

        if (vanishedPlayers.contains(uuid)) {
            vanishedPlayers.remove(uuid);
            showPlayerToAll(plugin, player);
            player.sendMessage(getLang().getPrefixedString("messages.vanish-disabled"));
        } else {
            vanishedPlayers.add(uuid);
            hidePlayerFromAll(plugin, player);
            player.sendMessage(getLang().getPrefixedString("messages.vanish-enabled"));
        }
        saveState(plugin);
        return true;
    }

    public static void loadState(EssentialsC plugin) {
        stateFile = new File(plugin.getDataFolder(), "vanished-players.yml");
        vanishedPlayers.clear();
        if (!stateFile.exists()) {
            return;
        }
        FileConfiguration state = YamlConfiguration.loadConfiguration(stateFile);
        for (String value : state.getStringList("players")) {
            try {
                vanishedPlayers.add(UUID.fromString(value));
            } catch (IllegalArgumentException ignored) {
                plugin.getLogger().warning("忽略无效的隐身玩家 UUID: " + value);
            }
        }
    }

    public static void hideVanishedPlayersFrom(EssentialsC plugin, Player observer) {
        if (observer.hasPermission(SEE_PERMISSION)) {
            return;
        }
        for (Player vanished : plugin.getServer().getOnlinePlayers()) {
            if (!observer.equals(vanished) && isVanished(vanished)) {
                observer.hidePlayer(plugin, vanished);
            }
        }
    }

    public static void restoreVisibility(EssentialsC plugin, Player player, boolean notify) {
        if (!vanishedPlayers.remove(player.getUniqueId())) {
            return;
        }
        showPlayerToAll(plugin, player);
        saveState(plugin);
        if (notify) {
            player.sendMessage(EssentialsC.getLangManager().getPrefixedString("messages.vanish-permission-removed"));
        }
    }

    public static void applyHiddenState(EssentialsC plugin, Player player) {
        if (isVanished(player)) {
            hidePlayerFromAll(plugin, player);
        }
    }

    public static void clearAll(EssentialsC plugin) {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (isVanished(player)) {
                showPlayerToAll(plugin, player);
            }
        }
        vanishedPlayers.clear();
    }

    private static void hidePlayerFromAll(EssentialsC plugin, Player player) {
        for (Player online : player.getServer().getOnlinePlayers()) {
            if (online != player && !online.hasPermission(SEE_PERMISSION)) {
                online.hidePlayer(plugin, player);
            }
        }
    }

    private static void showPlayerToAll(EssentialsC plugin, Player player) {
        for (Player online : player.getServer().getOnlinePlayers()) {
            if (online != player) {
                online.showPlayer(plugin, player);
            }
        }
    }

    public static boolean isVanished(Player player) {
        return vanishedPlayers.contains(player.getUniqueId());
    }

    private static void saveState(EssentialsC plugin) {
        if (stateFile == null) {
            stateFile = new File(plugin.getDataFolder(), "vanished-players.yml");
        }
        FileConfiguration state = new YamlConfiguration();
        state.set("players", vanishedPlayers.stream().map(UUID::toString).sorted().toList());
        try {
            AtomicYamlWriter.save(state, stateFile);
        } catch (Exception exception) {
            plugin.getLogger().warning("保存 vanished-players.yml 失败: " + exception.getMessage());
        }
    }
}
