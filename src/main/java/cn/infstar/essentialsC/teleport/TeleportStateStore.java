package cn.infstar.essentialsC.teleport;

import cn.infstar.essentialsC.EssentialsC;
import cn.infstar.essentialsC.util.AtomicYamlWriter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

final class TeleportStateStore {

    private final EssentialsC plugin;
    private final File cooldownFile;
    private final File ignoreFile;
    private boolean cooldownWritable = true;
    private boolean ignoreWritable = true;

    TeleportStateStore(EssentialsC plugin) {
        this.plugin = plugin;
        this.cooldownFile = new File(plugin.getDataFolder(), "teleport-cooldowns.yml");
        this.ignoreFile = new File(plugin.getDataFolder(), "teleport-ignore.yml");
    }

    CooldownState loadCooldowns() {
        Map<UUID, Long> send = new HashMap<>();
        Map<UUID, Long> accept = new HashMap<>();
        if (!cooldownFile.exists()) {
            return new CooldownState(send, accept);
        }

        YamlConfiguration config = new YamlConfiguration();
        try {
            config.load(cooldownFile);
        } catch (IOException | InvalidConfigurationException exception) {
            cooldownWritable = false;
            plugin.getLogger().severe("加载 teleport-cooldowns.yml 失败，已禁止覆盖原文件: "
                + exception.getMessage());
            return new CooldownState(send, accept);
        }
        loadCooldownMap(config, "send", send);
        loadCooldownMap(config, "accept", accept);
        return new CooldownState(send, accept);
    }

    void saveCooldowns(Map<UUID, Long> send, Map<UUID, Long> accept) {
        if (!cooldownWritable) {
            return;
        }
        YamlConfiguration config = new YamlConfiguration();
        long now = System.currentTimeMillis();
        saveCooldownMap(config, "send", send, now);
        saveCooldownMap(config, "accept", accept, now);
        try {
            AtomicYamlWriter.save(config, cooldownFile);
        } catch (Exception exception) {
            plugin.getLogger().warning("保存 teleport-cooldowns.yml 失败: " + exception.getMessage());
        }
    }

    Set<UUID> loadIgnoringRequests() {
        Set<UUID> ignoredPlayers = new HashSet<>();
        if (!ignoreFile.exists()) {
            return ignoredPlayers;
        }

        YamlConfiguration config = new YamlConfiguration();
        try {
            config.load(ignoreFile);
        } catch (IOException | InvalidConfigurationException exception) {
            ignoreWritable = false;
            plugin.getLogger().severe("加载 teleport-ignore.yml 失败，已禁止覆盖原文件: "
                + exception.getMessage());
            return ignoredPlayers;
        }
        ConfigurationSection ignored = config.getConfigurationSection("ignored");
        if (ignored == null) {
            return ignoredPlayers;
        }
        for (String key : ignored.getKeys(false)) {
            if (!ignored.getBoolean(key, false)) {
                continue;
            }
            try {
                ignoredPlayers.add(UUID.fromString(key));
            } catch (IllegalArgumentException exception) {
                plugin.getLogger().warning("忽略无效的 TPA 忽略记录 UUID: " + key);
            }
        }
        return ignoredPlayers;
    }

    boolean saveIgnoringRequests(Set<UUID> ignoredPlayers) {
        if (!ignoreWritable) {
            return false;
        }
        YamlConfiguration config = new YamlConfiguration();
        for (UUID playerId : ignoredPlayers) {
            config.set("ignored." + playerId, true);
        }
        try {
            AtomicYamlWriter.save(config, ignoreFile);
            return true;
        } catch (Exception exception) {
            plugin.getLogger().warning("保存 teleport-ignore.yml 失败: " + exception.getMessage());
            return false;
        }
    }

    private void loadCooldownMap(YamlConfiguration config, String path, Map<UUID, Long> destination) {
        ConfigurationSection section = config.getConfigurationSection(path);
        if (section == null) {
            return;
        }
        long now = System.currentTimeMillis();
        for (String key : section.getKeys(false)) {
            try {
                UUID playerId = UUID.fromString(key);
                long expiresAt = section.getLong(key, 0L);
                if (expiresAt > now) {
                    destination.put(playerId, expiresAt);
                }
            } catch (IllegalArgumentException exception) {
                plugin.getLogger().warning("忽略无效的 TPA 冷却记录 UUID: " + key);
            }
        }
    }

    private void saveCooldownMap(YamlConfiguration config, String path, Map<UUID, Long> source, long now) {
        source.forEach((playerId, expiresAt) -> {
            if (expiresAt > now) {
                config.set(path + "." + playerId, expiresAt);
            }
        });
    }

    record CooldownState(Map<UUID, Long> send, Map<UUID, Long> accept) {
    }
}
