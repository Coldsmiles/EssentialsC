package cn.infstar.essentialsC.skinbridge;

import cn.infstar.essentialsC.EssentialsC;
import cn.infstar.essentialsC.util.AtomicYamlWriter;
import com.destroystokyo.paper.profile.ProfileProperty;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public final class SkinBridgeManager implements Listener {

    private static final int WORKER_THREADS = 2;
    private static final int MAX_PENDING_LOOKUPS = 100;

    private final EssentialsC plugin;
    private final HttpClient httpClient;
    private final ThreadPoolExecutor executor;
    private final File generatedCacheFile;
    private final ConcurrentMap<UUID, CachedLookup> cache = new ConcurrentHashMap<>();
    private final ConcurrentMap<SkinCacheKey, GeneratedCacheEntry> generatedSkinCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, Long> pendingLookups = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, Long> forceRefreshCooldowns = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, String> loginSkinUrls = new ConcurrentHashMap<>();
    private final AtomicLong configurationGeneration = new AtomicLong();

    private volatile List<SkinProvider> providers = List.of();
    private volatile SkinBridgeGateway gateway;
    private volatile boolean debug;
    private volatile boolean sendPlayerMessage;
    private volatile boolean logDetectionResults;
    private volatile int requestTimeoutSeconds;
    private volatile int cacheMinutes;
    private volatile int forceRefreshCooldownSeconds;
    private volatile boolean requireCurrentTextureMatch;
    private volatile long joinDelayTicks;
    private volatile Set<String> excludedUuids = Set.of();
    private volatile Set<String> excludedNames = Set.of();

    public SkinBridgeManager(EssentialsC plugin) {
        this.plugin = plugin;
        this.generatedCacheFile = new File(plugin.getDataFolder(), "skin-cache.yml");
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();
        this.executor = new ThreadPoolExecutor(
            WORKER_THREADS,
            WORKER_THREADS,
            0L,
            TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(MAX_PENDING_LOOKUPS),
            new SkinBridgeThreadFactory(),
            new ThreadPoolExecutor.AbortPolicy()
        );
        reload();
        loadGeneratedSkinCache();
    }

    public void reload() {
        configurationGeneration.incrementAndGet();
        FileConfiguration config = plugin.getConfig();

        debug = plugin.getConfig().getBoolean("debug", false);
        sendPlayerMessage = config.getBoolean("skin-bridge.send-player-message", true);
        logDetectionResults = config.getBoolean("skin-bridge.log-detection-results", true);
        requestTimeoutSeconds = clamp(config.getInt("skin-bridge.profile-request-timeout-seconds", 5), 1, 30);
        String mineSkinEndpoint = config.getString("skin-bridge.mineskin.endpoint", "https://api.mineskin.org");
        String mineSkinApiKey = config.getString("skin-bridge.mineskin.api-key", "").trim();
        String mineSkinVisibility = config.getString("skin-bridge.mineskin.visibility", "unlisted");
        int mineSkinTimeoutSeconds = clamp(config.getInt("skin-bridge.mineskin.request-timeout-seconds", 30), 10, 180);
        long minimumSubmitIntervalMillis = clamp(
            config.getLong("skin-bridge.mineskin.minimum-submit-interval-millis", 1000L), 0L, 10000L);
        cacheMinutes = clamp(config.getInt("skin-bridge.cache-minutes", 120), 5, 10080);
        forceRefreshCooldownSeconds = clamp(config.getInt("skin-bridge.force-refresh-cooldown-seconds", 30), 0, 3600);
        requireCurrentTextureMatch = config.getBoolean("skin-bridge.require-current-texture-match", true);
        joinDelayTicks = clamp(config.getLong("skin-bridge.join-delay-ticks", 20L), 0, 200);
        excludedUuids = loadNormalizedValues(config, "skin-bridge.exclusions.uuids");
        excludedNames = loadNormalizedValues(config, "skin-bridge.exclusions.names");
        providers = loadProviders(config);
        cache.clear();
        gateway = loadGateway(mineSkinEndpoint, mineSkinApiKey, mineSkinVisibility,
            mineSkinTimeoutSeconds, minimumSubmitIntervalMillis);

        if (gateway == null) {
            plugin.getLogger().warning("SkinBridge 已启用，但未配置有效的 MineSkin API Key，皮肤同步不会执行。");
        }
    }

    public void shutdown() {
        configurationGeneration.incrementAndGet();
        executor.shutdownNow();
        saveGeneratedSkinCache();
        cache.clear();
        generatedSkinCache.clear();
        pendingLookups.clear();
        forceRefreshCooldowns.clear();
        loginSkinUrls.clear();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        getCurrentSkinUrl(event.getPlayer()).ifPresentOrElse(
            skinUrl -> loginSkinUrls.put(playerId, skinUrl),
            () -> loginSkinUrls.remove(playerId)
        );
        queueSync(event.getPlayer(), false);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        loginSkinUrls.remove(event.getPlayer().getUniqueId());
    }

    public SyncResult queueSync(Player player, boolean force) {
        UUID playerId = player.getUniqueId();
        if (isExcluded(player)) {
            cache.put(playerId, cached(null, null, State.EXCLUDED));
            sendPlayerNotification(playerId, "skin-bridge.notifications.excluded", Map.of());
            if (logDetectionResults) {
                plugin.getLogger().info("SkinBridge 已根据排除名单跳过玩家: " + player.getName());
            }
            return SyncResult.EXCLUDED;
        }
        if (gateway == null) {
            return SyncResult.DEPENDENCY_MISSING;
        }
        if (providers.isEmpty()) {
            return SyncResult.NO_PROVIDERS;
        }

        if (force) {
            long now = System.currentTimeMillis();
            long expiresAt = forceRefreshCooldowns.getOrDefault(playerId, 0L);
            if (expiresAt > now) {
                return SyncResult.REFRESH_COOLDOWN;
            }
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
        if (!pendingLookups.containsKey(playerId) && pendingLookups.size() >= MAX_PENDING_LOOKUPS) {
            plugin.getLogger().warning("SkinBridge 查询队列已满，已跳过玩家: " + player.getName());
            sendPlayerNotification(playerId, "skin-bridge.notifications.queue-full", Map.of());
            return SyncResult.QUEUE_FULL;
        }
        if (!registerPendingLookup(playerId, lookupGeneration)) {
            return SyncResult.ALREADY_RUNNING;
        }
        if (force && forceRefreshCooldownSeconds > 0) {
            forceRefreshCooldowns.put(playerId,
                System.currentTimeMillis() + forceRefreshCooldownSeconds * 1000L);
        }

        String playerName = player.getName();
        String loginSkinUrl = loginSkinUrls.computeIfAbsent(playerId,
            ignored -> getCurrentSkinUrl(player).orElse(""));
        String currentSkinUrl = loginSkinUrl.isEmpty() ? null : loginSkinUrl;
        Bukkit.getScheduler().runTaskLater(plugin,
            () -> startLookup(playerId, playerName, currentSkinUrl, lookupGeneration), joinDelayTicks);
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

    public int getRemainingForceRefreshCooldownSeconds(Player player) {
        long remaining = forceRefreshCooldowns.getOrDefault(player.getUniqueId(), 0L) - System.currentTimeMillis();
        return remaining <= 0L ? 0 : (int) Math.ceil(remaining / 1000.0D);
    }

    public boolean isSkinGatewayAvailable() {
        return gateway != null;
    }

    public int getProviderCount() {
        return providers.size();
    }

    public String getModuleDetail() {
        if (gateway == null) {
            return "缺少 MineSkin API Key 或配置无效";
        }
        if (providers.isEmpty()) {
            return "未配置有效 Provider";
        }
        return providers.size() + " 个 Provider 已就绪";
    }

    private void startLookup(UUID playerId, String playerName, String currentSkinUrl, long lookupGeneration) {
        if (executor.isShutdown() || lookupGeneration != configurationGeneration.get()) {
            pendingLookups.remove(playerId, lookupGeneration);
            return;
        }
        try {
            executor.execute(() -> {
                try {
                    CachedLookup resolved = resolve(playerId, playerName, currentSkinUrl);
                    if (lookupGeneration != configurationGeneration.get()) {
                        return;
                    }
                    cache.put(playerId, resolved);
                    if (resolved.skin() != null) {
                        applySkin(playerId, resolved, lookupGeneration);
                    } else {
                        sendPlayerNotification(playerId, "skin-bridge.notifications.not-external", Map.of());
                        if (logDetectionResults) {
                            plugin.getLogger().info("SkinBridge 未匹配到外置皮肤站，已保留玩家皮肤: " + playerName);
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
        } catch (RejectedExecutionException exception) {
            pendingLookups.remove(playerId, lookupGeneration);
            plugin.getLogger().warning("SkinBridge 查询队列拒绝了玩家任务: " + playerName);
            sendPlayerNotification(playerId, "skin-bridge.notifications.queue-full", Map.of());
        }
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

    private CachedLookup resolve(UUID playerId, String playerName, String currentSkinUrl) throws Exception {
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
            if (requireCurrentTextureMatch
                && (currentSkinUrl == null || !currentSkinUrl.equals(matchedProfile.skinUrl()))) {
                if (debug) {
                    plugin.getLogger().info("SkinBridge 已忽略与当前登录纹理不一致的 Provider: " + provider.name());
                }
                continue;
            }
            if (logDetectionResults) {
                plugin.getLogger().info("SkinBridge 已识别玩家 " + playerName + " 的皮肤来源: " + provider.name());
            }
            SkinCacheKey cacheKey = new SkinCacheKey(matchedProfile.skinUrl(), matchedProfile.model());
            GeneratedCacheEntry generatedEntry = generatedSkinCache.get(cacheKey);
            GeneratedSkin generatedSkin;
            if (generatedEntry != null && !generatedEntry.hasExpired()) {
                generatedSkin = generatedEntry.skin();
            } else {
                generatedSkinCache.remove(cacheKey);
                generatedSkin = currentGateway.generateSkin(matchedProfile.skinUrl(), matchedProfile.model());
                generatedSkinCache.put(cacheKey, new GeneratedCacheEntry(generatedSkin,
                    System.currentTimeMillis() + Duration.ofMinutes(cacheMinutes).toMillis()));
                saveGeneratedSkinCache();
            }
            return cached(provider.name(), generatedSkin, State.EXTERNAL);
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
        if (!"https".equalsIgnoreCase(skinUri.getScheme())) {
            throw new IllegalStateException(provider.id() + " 返回了非 HTTPS 皮肤 URL。");
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

    private Optional<String> getCurrentSkinUrl(Player player) {
        try {
            for (ProfileProperty property : player.getPlayerProfile().getProperties()) {
                if (!"textures".equals(property.getName())) {
                    continue;
                }
                String decoded = new String(Base64.getDecoder().decode(property.getValue()), StandardCharsets.UTF_8);
                JsonObject textureData = JsonParser.parseString(decoded).getAsJsonObject();
                if (!textureData.has("textures") || !textureData.get("textures").isJsonObject()) {
                    continue;
                }
                JsonObject textures = textureData.getAsJsonObject("textures");
                if (!textures.has("SKIN") || !textures.get("SKIN").isJsonObject()) {
                    continue;
                }
                String skinUrl = requireString(textures.getAsJsonObject("SKIN"), "url");
                if ("https".equalsIgnoreCase(URI.create(skinUrl).getScheme())) {
                    return Optional.of(skinUrl);
                }
            }
        } catch (RuntimeException exception) {
            if (debug) {
                plugin.getLogger().warning("SkinBridge 无法解析玩家当前纹理: " + exception.getMessage());
            }
        }
        return Optional.empty();
    }

    private void loadGeneratedSkinCache() {
        if (!generatedCacheFile.exists()) {
            return;
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(generatedCacheFile);
        ConfigurationSection entries = config.getConfigurationSection("entries");
        if (entries == null) {
            return;
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
                    generatedSkinCache.put(new SkinCacheKey(skinUrl, model),
                        new GeneratedCacheEntry(new GeneratedSkin(value, signature), expiresAt));
                }
            } catch (IllegalArgumentException ignored) {
                plugin.getLogger().warning("忽略无效的 SkinBridge 缓存记录: " + id);
            }
        }
    }

    private synchronized void saveGeneratedSkinCache() {
        FileConfiguration config = new YamlConfiguration();
        long now = System.currentTimeMillis();
        generatedSkinCache.entrySet().removeIf(entry -> entry.getValue().expiresAtMillis() <= now);
        generatedSkinCache.forEach((key, entry) -> {
            String path = "entries." + cacheId(key);
            config.set(path + ".skin-url", key.skinUrl());
            config.set(path + ".model", key.model().name());
            config.set(path + ".expires-at", entry.expiresAtMillis());
            config.set(path + ".value", entry.skin().value());
            config.set(path + ".signature", entry.skin().signature());
        });
        try {
            AtomicYamlWriter.save(config, generatedCacheFile);
        } catch (Exception exception) {
            plugin.getLogger().warning("保存 skin-cache.yml 失败: " + exception.getMessage());
        }
    }

    private String cacheId(SkinCacheKey key) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((key.skinUrl() + "\n" + key.model().name()).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("当前 Java 环境不支持 SHA-256。", exception);
        }
    }

    private List<SkinProvider> loadProviders(FileConfiguration config) {
        ConfigurationSection providersSection = config.getConfigurationSection("skin-bridge.providers");
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
                URI profileUri = URI.create(profileUrl.replace("{uuid}", "00000000000000000000000000000000")
                    .replace("{uuid-dashed}", "00000000-0000-0000-0000-000000000000"));
                if (!"https".equalsIgnoreCase(profileUri.getScheme())) {
                    plugin.getLogger().warning("SkinBridge Provider " + key + " 必须使用 HTTPS，已跳过。");
                    continue;
                }
                String configuredName = providerSection.getString("name", key);
                String providerName = configuredName == null ? key : configuredName.trim();
                if (providerName.isEmpty()) {
                    providerName = key;
                }
                loadedProviders.add(new SkinProvider(key, providerName, profileUrl,
                    providerSection.getInt("priority", 100)));
            } catch (IllegalArgumentException exception) {
                plugin.getLogger().warning("SkinBridge Provider " + key + " 的 profile-url 无效，已跳过。");
            }
        }
        loadedProviders.sort(Comparator.comparingInt(SkinProvider::priority).thenComparing(SkinProvider::id));
        return List.copyOf(loadedProviders);
    }

    private SkinBridgeGateway loadGateway(String endpoint, String apiKey, String visibility, int timeoutSeconds,
                                          long minimumSubmitIntervalMillis) {
        if (apiKey.isBlank()) {
            return null;
        }
        try {
            return new MineSkinGateway(httpClient, endpoint, apiKey, visibility, timeoutSeconds,
                minimumSubmitIntervalMillis,
                "EssentialsC/" + plugin.getDescription().getVersion() + " SkinBridge");
        } catch (Exception | LinkageError exception) {
            plugin.getLogger().warning("加载 MineSkin SkinBridge 适配器失败: " + exception.getMessage());
            return null;
        }
    }

    private Set<String> loadNormalizedValues(FileConfiguration config, String path) {
        Set<String> values = ConcurrentHashMap.newKeySet();
        for (String value : config.getStringList(path)) {
            String normalized = value == null ? "" : value.trim().toLowerCase(java.util.Locale.ROOT);
            if (!normalized.isEmpty()) {
                values.add(normalized);
            }
        }
        return Set.copyOf(values);
    }

    private boolean isExcluded(Player player) {
        return excludedUuids.contains(player.getUniqueId().toString().toLowerCase(java.util.Locale.ROOT))
            || excludedNames.contains(player.getName().toLowerCase(java.util.Locale.ROOT));
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

    private record SkinProvider(String id, String name, String profileUrl, int priority) {
        private String resolveProfileUrl(UUID playerId) {
            return profileUrl.replace("{uuid}", playerId.toString().replace("-", ""))
                .replace("{uuid-dashed}", playerId.toString());
        }
    }

    private record ProviderProfile(String skinUrl, SkinModel model) {
    }

    private record SkinCacheKey(String skinUrl, SkinModel model) {
    }

    private record GeneratedCacheEntry(GeneratedSkin skin, long expiresAtMillis) {
        private boolean hasExpired() {
            return System.currentTimeMillis() >= expiresAtMillis;
        }
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
        EXCLUDED,
        QUEUE_FULL,
        DEPENDENCY_MISSING,
        NO_PROVIDERS,
        REFRESH_COOLDOWN
    }

    public record Status(State state, String providerId) {
    }

    public enum State {
        EXTERNAL,
        EXCLUDED,
        NOT_EXTERNAL,
        PENDING,
        UNKNOWN
    }
}
