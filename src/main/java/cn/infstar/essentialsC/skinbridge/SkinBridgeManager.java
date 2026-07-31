package cn.infstar.essentialsC.skinbridge;

import cn.infstar.essentialsC.EssentialsC;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

public final class SkinBridgeManager implements Listener {

    private final EssentialsC plugin;
    private final HttpClient httpClient;
    private final ExecutorService executor;
    private final ConcurrentMap<UUID, CachedLookup> cache = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, Long> pendingLookups = new ConcurrentHashMap<>();
    private final AtomicLong configurationGeneration = new AtomicLong();

    private volatile List<SkinProvider> providers = List.of();
    private volatile SkinBridgeGateway gateway;
    private volatile boolean enabled;
    private volatile boolean debug;
    private volatile boolean sendPlayerMessage;
    private volatile int requestTimeoutSeconds;
    private volatile int cacheMinutes;
    private volatile long joinDelayTicks;

    public SkinBridgeManager(EssentialsC plugin) {
        this.plugin = plugin;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();
        this.executor = Executors.newFixedThreadPool(2, new SkinBridgeThreadFactory());
        reload();
    }

    public void reload() {
        configurationGeneration.incrementAndGet();
        FileConfiguration config = plugin.getFeatureConfigManager().getSkinBridgeConfig();
        addDefaults(config);
        config.options().copyDefaults(true);
        plugin.getFeatureConfigManager().saveSkinBridgeConfig();

        enabled = config.getBoolean("enabled", false);
        debug = plugin.getConfig().getBoolean("debug", false);
        sendPlayerMessage = config.getBoolean("send-player-message", true);
        requestTimeoutSeconds = clamp(config.getInt("profile-request-timeout-seconds", 5), 1, 30);
        String mineSkinEndpoint = config.getString("mineskin.endpoint", "https://api.mineskin.org");
        String mineSkinApiKey = config.getString("mineskin.api-key", "").trim();
        String mineSkinVisibility = config.getString("mineskin.visibility", "unlisted");
        int mineSkinTimeoutSeconds = clamp(config.getInt("mineskin.request-timeout-seconds", 30), 10, 180);
        cacheMinutes = clamp(config.getInt("cache-minutes", 120), 5, 10080);
        joinDelayTicks = clamp(config.getLong("join-delay-ticks", 20L), 0, 200);
        providers = loadProviders(config);
        cache.clear();
        gateway = loadGateway(mineSkinEndpoint, mineSkinApiKey, mineSkinVisibility, mineSkinTimeoutSeconds);

        if (enabled && gateway == null) {
            plugin.getLogger().warning("SkinBridge 已启用，但未配置有效的 MineSkin API Key，皮肤同步不会执行。");
        }
    }

    public void shutdown() {
        configurationGeneration.incrementAndGet();
        cache.clear();
        pendingLookups.clear();
        executor.shutdownNow();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        queueSync(event.getPlayer(), false);
    }

    public SyncResult queueSync(Player player, boolean force) {
        if (!enabled) {
            return SyncResult.FEATURE_DISABLED;
        }
        if (gateway == null) {
            return SyncResult.DEPENDENCY_MISSING;
        }
        if (providers.isEmpty()) {
            return SyncResult.NO_PROVIDERS;
        }

        UUID playerId = player.getUniqueId();
        if (force) {
            cache.remove(playerId);
        }

        CachedLookup cached = cache.get(playerId);
        if (!force && cached != null && !cached.hasExpired()) {
            if (cached.skin() != null) {
                applySkin(playerId, cached, configurationGeneration.get());
            }
            return SyncResult.CACHED;
        }

        long lookupGeneration = configurationGeneration.get();
        if (!registerPendingLookup(playerId, lookupGeneration)) {
            return SyncResult.ALREADY_RUNNING;
        }

        String playerName = player.getName();
        Bukkit.getScheduler().runTaskLater(plugin, () -> startLookup(playerId, playerName, lookupGeneration), joinDelayTicks);
        sendPlayerNotification(playerId, "skin-bridge.notifications.detecting", Map.of());
        return SyncResult.QUEUED;
    }

    public Status getStatus(Player player) {
        CachedLookup cached = cache.get(player.getUniqueId());
        if (cached != null && !cached.hasExpired()) {
            return new Status(cached.state(), cached.providerId());
        }
        if (pendingLookups.containsKey(player.getUniqueId())) {
            return new Status(State.PENDING, null);
        }
        return new Status(State.UNKNOWN, null);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isSkinGatewayAvailable() {
        return gateway != null;
    }

    public int getProviderCount() {
        return providers.size();
    }

    public String getModuleDetail() {
        if (!enabled) {
            return "配置未启用";
        }
        if (gateway == null) {
            return "缺少 MineSkin API Key 或配置无效";
        }
        if (providers.isEmpty()) {
            return "未配置有效 Provider";
        }
        return providers.size() + " 个 Provider 已就绪";
    }

    private void startLookup(UUID playerId, String playerName, long lookupGeneration) {
        if (executor.isShutdown() || lookupGeneration != configurationGeneration.get()) {
            pendingLookups.remove(playerId, lookupGeneration);
            return;
        }
        executor.execute(() -> {
            try {
                CachedLookup resolved = resolve(playerId, playerName);
                if (lookupGeneration != configurationGeneration.get()) {
                    return;
                }
                cache.put(playerId, resolved);
                if (resolved.skin() != null) {
                    applySkin(playerId, resolved, lookupGeneration);
                } else {
                    sendPlayerNotification(playerId, "skin-bridge.notifications.not-external", Map.of());
                    if (debug) {
                        plugin.getLogger().info("SkinBridge 未识别到外置登录玩家: " + playerName);
                    }
                }
            } catch (Exception exception) {
                plugin.getLogger().warning("SkinBridge 查询 " + playerName + " 的皮肤资料失败: " + exception.getMessage());
                sendPlayerNotification(playerId, "skin-bridge.notifications.failed", Map.of());
                if (debug) {
                    plugin.getLogger().warning("SkinBridge 异常类型: " + exception.getClass().getName());
                }
            } finally {
                pendingLookups.remove(playerId, lookupGeneration);
            }
        });
    }

    private boolean registerPendingLookup(UUID playerId, long lookupGeneration) {
        while (true) {
            Long runningGeneration = pendingLookups.putIfAbsent(playerId, lookupGeneration);
            if (runningGeneration == null) {
                return true;
            }
            if (runningGeneration == lookupGeneration) {
                return false;
            }
            if (pendingLookups.replace(playerId, runningGeneration, lookupGeneration)) {
                return true;
            }
        }
    }

    private CachedLookup resolve(UUID playerId, String playerName) throws Exception {
        Exception lastFailure = null;
        for (SkinProvider provider : providers) {
            Optional<ProviderProfile> profile;
            try {
                profile = queryProfile(provider, playerId, playerName);
            } catch (Exception exception) {
                lastFailure = exception;
                plugin.getLogger().warning("SkinBridge Provider " + provider.id() + " 查询失败: " + exception.getMessage());
                continue;
            }
            if (profile.isEmpty()) {
                continue;
            }

            SkinBridgeGateway currentGateway = gateway;
            if (currentGateway == null) {
                throw new IllegalStateException("MineSkin 网关在查询期间不可用。");
            }

            ProviderProfile matchedProfile = profile.get();
            GeneratedSkin generatedSkin = currentGateway.generateSkin(matchedProfile.skinUrl(), matchedProfile.model());
            return cached(provider.id(), generatedSkin, State.EXTERNAL);
        }

        if (lastFailure != null) {
            throw new IllegalStateException("所有可用 Provider 均未能完成确认。", lastFailure);
        }
        return cached(null, null, State.NOT_EXTERNAL);
    }

    private Optional<ProviderProfile> queryProfile(SkinProvider provider, UUID playerId, String playerName) throws Exception {
        URI requestUri = URI.create(provider.resolveProfileUrl(playerId));
        HttpRequest request = HttpRequest.newBuilder(requestUri)
            .timeout(Duration.ofSeconds(requestTimeoutSeconds))
            .header("Accept", "application/json")
            .header("User-Agent", "EssentialsC/" + plugin.getDescription().getVersion() + " SkinBridge")
            .GET()
            .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        if (response.statusCode() == 204 || response.statusCode() == 404) {
            return Optional.empty();
        }
        if (response.statusCode() != 200) {
            throw new IllegalStateException(provider.id() + " 返回 HTTP " + response.statusCode());
        }
        if (response.body().length() > 1_048_576) {
            throw new IllegalStateException(provider.id() + " 返回的 profile 超过 1 MiB 限制。");
        }

        JsonObject profile = JsonParser.parseString(response.body()).getAsJsonObject();
        String profileId = requireString(profile, "id");
        String profileName = requireString(profile, "name");
        if (!normalizeUuid(profileId).equals(normalizeUuid(playerId.toString())) || !profileName.equalsIgnoreCase(playerName)) {
            if (debug) {
                plugin.getLogger().warning("SkinBridge 忽略 " + provider.id() + " 的不匹配 profile: " + profileName + " / " + profileId);
            }
            return Optional.empty();
        }

        JsonObject textureData = findTextureData(profile);
        JsonObject skin = textureData.getAsJsonObject("textures").getAsJsonObject("SKIN");
        String skinUrl = requireString(skin, "url");
        URI skinUri = URI.create(skinUrl);
        if (!"https".equalsIgnoreCase(skinUri.getScheme()) && !"http".equalsIgnoreCase(skinUri.getScheme())) {
            throw new IllegalStateException(provider.id() + " 返回了不支持的皮肤 URL 协议。");
        }

        SkinModel model = SkinModel.CLASSIC;
        JsonObject metadata = skin.has("metadata") && skin.get("metadata").isJsonObject()
            ? skin.getAsJsonObject("metadata")
            : null;
        if (metadata != null && "slim".equalsIgnoreCase(metadata.has("model") ? metadata.get("model").getAsString() : "")) {
            model = SkinModel.SLIM;
        }
        return Optional.of(new ProviderProfile(skinUrl, model));
    }

    private JsonObject findTextureData(JsonObject profile) {
        JsonArray properties = profile.has("properties") && profile.get("properties").isJsonArray()
            ? profile.getAsJsonArray("properties")
            : new JsonArray();
        for (JsonElement element : properties) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject property = element.getAsJsonObject();
            if (!"textures".equals(property.has("name") ? property.get("name").getAsString() : "")) {
                continue;
            }
            String encodedValue = requireString(property, "value");
            String decodedValue = new String(Base64.getDecoder().decode(encodedValue), StandardCharsets.UTF_8);
            JsonObject textureData = JsonParser.parseString(decodedValue).getAsJsonObject();
            if (textureData.has("textures")
                && textureData.get("textures").isJsonObject()
                && textureData.getAsJsonObject("textures").has("SKIN")
                && textureData.getAsJsonObject("textures").get("SKIN").isJsonObject()) {
                return textureData;
            }
        }
        throw new IllegalStateException("profile 不包含有效的皮肤 textures 属性。");
    }

    private List<SkinProvider> loadProviders(FileConfiguration config) {
        ConfigurationSection providersSection = config.getConfigurationSection("providers");
        if (providersSection == null) {
            return List.of();
        }

        List<SkinProvider> loadedProviders = new ArrayList<>();
        for (String key : providersSection.getKeys(false)) {
            ConfigurationSection providerSection = providersSection.getConfigurationSection(key);
            if (providerSection == null || !providerSection.getBoolean("enabled", false)) {
                continue;
            }
            String profileUrl = providerSection.getString("profile-url", "").trim();
            if (!profileUrl.contains("{uuid}") && !profileUrl.contains("{uuid-dashed}")) {
                plugin.getLogger().warning("SkinBridge Provider " + key + " 缺少 {uuid} 或 {uuid-dashed} 占位符，已跳过。");
                continue;
            }
            try {
                URI.create(profileUrl.replace("{uuid}", "00000000000000000000000000000000")
                    .replace("{uuid-dashed}", "00000000-0000-0000-0000-000000000000"));
                loadedProviders.add(new SkinProvider(key, profileUrl, providerSection.getInt("priority", 100)));
            } catch (IllegalArgumentException exception) {
                plugin.getLogger().warning("SkinBridge Provider " + key + " 的 profile-url 无效，已跳过。");
            }
        }
        loadedProviders.sort(Comparator.comparingInt(SkinProvider::priority).thenComparing(SkinProvider::id));
        return List.copyOf(loadedProviders);
    }

    private SkinBridgeGateway loadGateway(String endpoint, String apiKey, String visibility, int timeoutSeconds) {
        if (apiKey.isBlank()) {
            return null;
        }
        try {
            return new MineSkinGateway(httpClient, endpoint, apiKey, visibility, timeoutSeconds,
                "EssentialsC/" + plugin.getDescription().getVersion() + " SkinBridge");
        } catch (Exception | LinkageError exception) {
            plugin.getLogger().warning("加载 MineSkin SkinBridge 适配器失败: " + exception.getMessage());
            return null;
        }
    }

    private void addDefaults(FileConfiguration config) {
        config.addDefault("config-version", 1);
        config.addDefault("enabled", false);
        config.addDefault("send-player-message", true);
        config.addDefault("profile-request-timeout-seconds", 5);
        config.addDefault("mineskin.endpoint", "https://api.mineskin.org");
        config.addDefault("mineskin.api-key", "");
        config.addDefault("mineskin.visibility", "unlisted");
        config.addDefault("mineskin.request-timeout-seconds", 30);
        config.addDefault("cache-minutes", 120);
        config.addDefault("join-delay-ticks", 20);
    }

    private void applySkin(UUID playerId, CachedLookup resolved, long lookupGeneration) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (lookupGeneration != configurationGeneration.get()) {
                return;
            }
            Player player = Bukkit.getPlayer(playerId);
            SkinBridgeGateway currentGateway = gateway;
            if (player == null || !player.isOnline() || currentGateway == null || resolved.skin() == null) {
                return;
            }
            try {
                currentGateway.applySkin(player, resolved.skin());
                sendPlayerNotification(playerId, "skin-bridge.notifications.synced",
                    Map.of("provider", resolved.providerId()));
                if (debug) {
                    plugin.getLogger().info("SkinBridge 已应用 " + player.getName() + " 的 " + resolved.providerId() + " 皮肤。");
                }
            } catch (Exception exception) {
                plugin.getLogger().warning("SkinBridge 应用 " + player.getName() + " 的皮肤失败: " + exception.getMessage());
                sendPlayerNotification(playerId, "skin-bridge.notifications.failed", Map.of());
            }
        });
    }

    private void sendPlayerNotification(UUID playerId, String messagePath, Map<String, String> placeholders) {
        if (!sendPlayerMessage) {
            return;
        }

        Runnable notification = () -> {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                player.sendMessage(EssentialsC.getLangManager().getPrefixedString(messagePath, placeholders));
            }
        };
        if (Bukkit.isPrimaryThread()) {
            notification.run();
        } else {
            Bukkit.getScheduler().runTask(plugin, notification);
        }
    }

    private CachedLookup cached(String providerId, GeneratedSkin skin, State state) {
        return new CachedLookup(providerId, skin, state, System.currentTimeMillis() + Duration.ofMinutes(cacheMinutes).toMillis());
    }

    private static String requireString(JsonObject object, String key) {
        if (!object.has(key) || !object.get(key).isJsonPrimitive()) {
            throw new IllegalStateException("缺少字符串字段: " + key);
        }
        String value = object.get(key).getAsString();
        if (value.isBlank()) {
            throw new IllegalStateException("字符串字段为空: " + key);
        }
        return value;
    }

    private static String normalizeUuid(String value) {
        return value.replace("-", "").toLowerCase(java.util.Locale.ROOT);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static long clamp(long value, long min, long max) {
        return Math.max(min, Math.min(max, value));
    }

    private record SkinProvider(String id, String profileUrl, int priority) {
        private String resolveProfileUrl(UUID playerId) {
            return profileUrl.replace("{uuid}", playerId.toString().replace("-", ""))
                .replace("{uuid-dashed}", playerId.toString());
        }
    }

    private record ProviderProfile(String skinUrl, SkinModel model) {
    }

    private record CachedLookup(String providerId, GeneratedSkin skin, State state, long expiresAtMillis) {
        private boolean hasExpired() {
            return System.currentTimeMillis() >= expiresAtMillis;
        }
    }

    private static final class SkinBridgeThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "EssentialsC-SkinBridge");
            thread.setDaemon(true);
            return thread;
        }
    }

    public enum SyncResult {
        QUEUED,
        CACHED,
        ALREADY_RUNNING,
        FEATURE_DISABLED,
        DEPENDENCY_MISSING,
        NO_PROVIDERS
    }

    public record Status(State state, String providerId) {
    }

    public enum State {
        EXTERNAL,
        NOT_EXTERNAL,
        PENDING,
        UNKNOWN
    }
}
