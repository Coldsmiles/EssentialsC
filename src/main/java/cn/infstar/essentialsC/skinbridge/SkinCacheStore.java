package cn.infstar.essentialsC.skinbridge;

import cn.infstar.essentialsC.EssentialsC;
import cn.infstar.essentialsC.util.AtomicYamlWriter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

final class SkinCacheStore {

    private final EssentialsC plugin;
    private final File cacheFile;

    SkinCacheStore(EssentialsC plugin) {
        this.plugin = plugin;
        this.cacheFile = new File(plugin.getDataFolder(), "skin-cache.yml");
    }

    Map<Key, Entry> load() {
        Map<Key, Entry> loaded = new HashMap<>();
        if (!cacheFile.exists()) {
            return loaded;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(cacheFile);
        ConfigurationSection entries = config.getConfigurationSection("entries");
        if (entries == null) {
            return loaded;
        }
        long now = System.currentTimeMillis();
        for (String id : entries.getKeys(false)) {
            String path = "entries." + id;
            try {
                String skinUrl = config.getString(path + ".skin-url", "");
                SkinModel model = SkinModel.valueOf(config.getString(path + ".model", "CLASSIC"));
                long expiresAt = config.getLong(path + ".expires-at", 0L);
                String value = config.getString(path + ".value", "");
                String signature = config.getString(path + ".signature", "");
                if (expiresAt > now && !skinUrl.isBlank() && !value.isBlank() && !signature.isBlank()) {
                    loaded.put(new Key(skinUrl, model),
                        new Entry(new GeneratedSkin(value, signature), expiresAt));
                }
            } catch (IllegalArgumentException exception) {
                plugin.getLogger().warning("忽略无效的 SkinBridge 缓存记录: " + id);
            }
        }
        return loaded;
    }

    boolean save(Map<Key, Entry> entries) {
        YamlConfiguration config = new YamlConfiguration();
        entries.forEach((key, entry) -> {
            String path = "entries." + cacheId(key);
            config.set(path + ".skin-url", key.skinUrl());
            config.set(path + ".model", key.model().name());
            config.set(path + ".expires-at", entry.expiresAtMillis());
            config.set(path + ".value", entry.skin().value());
            config.set(path + ".signature", entry.skin().signature());
        });
        try {
            AtomicYamlWriter.save(config, cacheFile);
            return true;
        } catch (Exception exception) {
            plugin.getLogger().warning("保存 skin-cache.yml 失败: " + exception.getMessage());
            return false;
        }
    }

    private String cacheId(Key key) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((key.skinUrl() + "\n" + key.model().name())
                .getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("当前 Java 环境不支持 SHA-256。", exception);
        }
    }

    record Key(String skinUrl, SkinModel model) {
    }

    record Entry(GeneratedSkin skin, long expiresAtMillis) {
        boolean hasExpired() {
            return System.currentTimeMillis() >= expiresAtMillis;
        }
    }
}
