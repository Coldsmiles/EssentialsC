package cn.infstar.essentialsC.teleport;

import cn.infstar.essentialsC.EssentialsC;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

public final class TeleportRequestManager implements Listener {

    public static final String BYPASS_WARMUP_PERMISSION = "essentialsc.tpa.bypass-warmup";
    public static final String BYPASS_COOLDOWN_PERMISSION = "essentialsc.tpa.bypass-cooldown";

    private final EssentialsC plugin;
    private final Map<UUID, Deque<TeleportRequest>> requests = new HashMap<>();
    private final Map<UUID, PendingTeleport> warmups = new HashMap<>();
    private final Map<UUID, Long> sendCooldowns = new HashMap<>();
    private final Map<UUID, Long> acceptCooldowns = new HashMap<>();
    private final Set<UUID> ignoringRequests = new HashSet<>();
    private final File ignoreFile;
    private BukkitTask cleanupTask;

    private int timeoutSeconds;
    private boolean strictTpaRequests;
    private boolean strictTpaHereRequests;
    private int warmupSeconds;
    private boolean cancelWarmupOnMove;
    private boolean cancelWarmupOnDamage;
    private boolean cooldownsEnabled;
    private int sendCooldownSeconds;
    private int acceptCooldownSeconds;
    private double warmupMoveThreshold;
    private String warmupDisplay;
    private boolean soundsEnabled;
    private String requestSound;
    private String warmupSound;
    private String cancelSound;
    private String completeSound;

    public TeleportRequestManager(EssentialsC plugin) {
        this.plugin = plugin;
        this.ignoreFile = new File(plugin.getDataFolder(), "teleport-ignore.yml");
        reload();
    }

    public void reload() {
        plugin.getConfig().addDefault("tpa.timeout-seconds", 60);
        plugin.getConfig().addDefault("tpa.strict-tpa-requests", false);
        plugin.getConfig().addDefault("tpa.strict-tpahere-requests", true);
        plugin.getConfig().addDefault("tpa.warmup-seconds", 5);
        plugin.getConfig().addDefault("tpa.cancel-warmup-on-move", true);
        plugin.getConfig().addDefault("tpa.cancel-warmup-on-damage", true);
        plugin.getConfig().addDefault("tpa.cooldowns.enabled", true);
        plugin.getConfig().addDefault("tpa.cooldowns.cooldown-times.SEND_TELEPORT_REQUEST", 0);
        plugin.getConfig().addDefault("tpa.cooldowns.cooldown-times.ACCEPT_TELEPORT_REQUEST", 0);
        plugin.getConfig().addDefault("tpa.warmup-move-threshold", 0.1D);
        plugin.getConfig().addDefault("tpa.warmup-display", "actionbar");
        plugin.getConfig().addDefault("tpa.sounds.enabled", true);
        plugin.getConfig().addDefault("tpa.sounds.request-received", "entity.experience_orb.pickup");
        plugin.getConfig().addDefault("tpa.sounds.warmup", "block.note_block.banjo");
        plugin.getConfig().addDefault("tpa.sounds.cancelled", "entity.item.break");
        plugin.getConfig().addDefault("tpa.sounds.complete", "entity.enderman.teleport");
        plugin.getConfig().options().copyDefaults(true);
        plugin.saveConfig();

        timeoutSeconds = Math.max(5, plugin.getConfig().getInt("tpa.timeout-seconds", 60));
        strictTpaRequests = plugin.getConfig().getBoolean("tpa.strict-tpa-requests", false);
        strictTpaHereRequests = plugin.getConfig().getBoolean("tpa.strict-tpahere-requests", true);
        warmupSeconds = Math.max(0, plugin.getConfig().getInt("tpa.warmup-seconds", 5));
        cancelWarmupOnMove = plugin.getConfig().getBoolean("tpa.cancel-warmup-on-move", true);
        cancelWarmupOnDamage = plugin.getConfig().getBoolean("tpa.cancel-warmup-on-damage", true);
        cooldownsEnabled = plugin.getConfig().getBoolean("tpa.cooldowns.enabled", true);
        sendCooldownSeconds = Math.max(0, plugin.getConfig().getInt("tpa.cooldowns.cooldown-times.SEND_TELEPORT_REQUEST", 0));
        acceptCooldownSeconds = Math.max(0, plugin.getConfig().getInt("tpa.cooldowns.cooldown-times.ACCEPT_TELEPORT_REQUEST", 0));
        warmupMoveThreshold = Math.max(0.0D, plugin.getConfig().getDouble("tpa.warmup-move-threshold", 0.1D));
        warmupDisplay = plugin.getConfig().getString("tpa.warmup-display", "actionbar").toLowerCase();
        soundsEnabled = plugin.getConfig().getBoolean("tpa.sounds.enabled", true);
        requestSound = plugin.getConfig().getString("tpa.sounds.request-received", "entity.experience_orb.pickup");
        warmupSound = plugin.getConfig().getString("tpa.sounds.warmup", "block.note_block.banjo");
        cancelSound = plugin.getConfig().getString("tpa.sounds.cancelled", "entity.item.break");
        completeSound = plugin.getConfig().getString("tpa.sounds.complete", "entity.enderman.teleport");
        loadIgnoringRequests();
        startCleanupTask();
    }

    public void shutdown() {
        requests.clear();
        sendCooldowns.clear();
        acceptCooldowns.clear();
        cancelAllWarmups();
        cancelCleanupTask();
        saveIgnoringRequests();
    }

    public CreateRequestResult createRequest(Player requester, Player target, TeleportRequest.Type type) {
        return createRequest(requester, target, type, true);
    }

    private CreateRequestResult createRequest(Player requester, Player target, TeleportRequest.Type type, boolean applyCooldown) {
        OptionalIntCooldown sendCooldown = getRemainingCooldown(requester, sendCooldowns);
        if (applyCooldown && sendCooldown.active()) {
            return new CreateRequestResult(CreateRequestStatus.ON_COOLDOWN, null, sendCooldown.seconds());
        }

        Optional<TeleportRequest> existingRequest = findIncoming(target, requester.getName());
        if (existingRequest.isPresent()
            && existingRequest.get().type() == type
            && !existingRequest.get().hasExpired()) {
            return new CreateRequestResult(CreateRequestStatus.DUPLICATE, existingRequest.get());
        }

        TeleportRequest request = new TeleportRequest(
            requester.getUniqueId(),
            requester.getName(),
            requester.getLocation().clone(),
            target.getUniqueId(),
            target.getName(),
            target.getLocation().clone(),
            type,
            Instant.now().plusSeconds(timeoutSeconds).toEpochMilli(),
            TeleportRequest.Status.PENDING
        );

        if (isIgnoringRequests(target)) {
            request.setStatus(TeleportRequest.Status.IGNORED);
            if (applyCooldown) {
                startCooldown(requester, sendCooldowns, sendCooldownSeconds);
            }
            return new CreateRequestResult(CreateRequestStatus.IGNORED, request);
        }

        Deque<TeleportRequest> targetRequests = requests.computeIfAbsent(target.getUniqueId(), ignored -> new ArrayDeque<>());
        targetRequests.addFirst(request);
        if (applyCooldown) {
            startCooldown(requester, sendCooldowns, sendCooldownSeconds);
        }
        return new CreateRequestResult(CreateRequestStatus.SUCCESS, request, 0);
    }

    public Optional<TeleportRequest> findIncoming(Player target, String requesterName) {
        Deque<TeleportRequest> targetRequests = requests.getOrDefault(target.getUniqueId(), new ArrayDeque<>());
        if (requesterName == null) {
            return targetRequests.stream().findFirst();
        }

        Optional<TeleportRequest> unexpired = targetRequests.stream()
            .filter(request -> request.requesterName().equalsIgnoreCase(requesterName))
            .filter(request -> !request.hasExpired())
            .findFirst();
        if (unexpired.isPresent()) {
            return unexpired;
        }

        return targetRequests.stream()
            .filter(request -> request.requesterName().equalsIgnoreCase(requesterName))
            .findFirst();
    }

    public Optional<TeleportRequest> findOutgoing(Player requester, String targetName) {
        return requests.values().stream()
            .flatMap(Deque::stream)
            .filter(request -> request.requesterId().equals(requester.getUniqueId()))
            .filter(request -> !request.hasExpired())
            .filter(request -> targetName == null || request.targetName().equalsIgnoreCase(targetName))
            .max(Comparator.comparingLong(TeleportRequest::expiresAtMillis));
    }

    public List<String> getIncomingRequesterNames(Player target, String partial) {
        String partialLower = partial == null ? "" : partial.toLowerCase();
        return requests.getOrDefault(target.getUniqueId(), new ArrayDeque<>()).stream()
            .filter(request -> !request.hasExpired())
            .map(TeleportRequest::requesterName)
            .filter(name -> name.toLowerCase().startsWith(partialLower))
            .distinct()
            .sorted(String.CASE_INSENSITIVE_ORDER)
            .toList();
    }

    public List<String> getOutgoingTargetNames(Player requester, String partial) {
        String partialLower = partial == null ? "" : partial.toLowerCase();
        return requests.values().stream()
            .flatMap(Deque::stream)
            .filter(request -> request.requesterId().equals(requester.getUniqueId()))
            .filter(request -> !request.hasExpired())
            .map(TeleportRequest::targetName)
            .filter(name -> name.toLowerCase().startsWith(partialLower))
            .distinct()
            .sorted(String.CASE_INSENSITIVE_ORDER)
            .toList();
    }

    public TeleportResult accept(Player target, TeleportRequest request) {
        OptionalIntCooldown acceptCooldown = getRemainingCooldown(target, acceptCooldowns);
        if (acceptCooldown.active()) {
            return TeleportResult.onCooldown(acceptCooldown.seconds());
        }

        if (request.hasExpired()) {
            removeIncomingByRequester(target, request.requesterName());
            return TeleportResult.EXPIRED;
        }

        Player requester = Bukkit.getPlayer(request.requesterId());
        Player currentTarget = Bukkit.getPlayer(request.targetId());
        if (requester == null || currentTarget == null || !requester.isOnline() || !currentTarget.isOnline()) {
            return TeleportResult.PLAYER_OFFLINE;
        }

        TeleportPlan plan = createTeleportPlan(request, requester, currentTarget);
        Player teleporter = plan.teleporter();
        if (warmups.containsKey(teleporter.getUniqueId())) {
            return TeleportResult.ALREADY_WARMING_UP;
        }

        removeIncomingByRequester(target, request.requesterName());
        request.setStatus(TeleportRequest.Status.ACCEPTED);
        if (warmupSeconds <= 0 || teleporter.hasPermission(BYPASS_WARMUP_PERMISSION)) {
            TeleportResult result = executeTeleport(plan, true);
            if (result.status() == TeleportResult.Status.SUCCESS) {
                startCooldown(target, acceptCooldowns, acceptCooldownSeconds);
            }
            return result;
        }

        startWarmup(plan);
        startCooldown(target, acceptCooldowns, acceptCooldownSeconds);
        return TeleportResult.WARMING_UP;
    }

    public TeleportResult deny(Player target, TeleportRequest request) {
        if (!removeIncomingByRequester(target, request.requesterName())) {
            return TeleportResult.EXPIRED;
        }
        if (request.hasExpired()) {
            return TeleportResult.EXPIRED;
        }
        request.setStatus(TeleportRequest.Status.DECLINED);
        return TeleportResult.SUCCESS;
    }

    public boolean cancel(TeleportRequest request) {
        Deque<TeleportRequest> targetRequests = requests.get(request.targetId());
        if (targetRequests == null) {
            return false;
        }

        boolean removed = targetRequests.removeIf(candidate ->
            candidate.requesterName().equalsIgnoreCase(request.requesterName()));
        if (targetRequests.isEmpty()) {
            requests.remove(request.targetId());
        }
        return removed;
    }

    public boolean isIgnoringRequests(Player player) {
        return ignoringRequests.contains(player.getUniqueId());
    }

    public boolean toggleIgnoringRequests(Player player) {
        UUID uuid = player.getUniqueId();
        boolean nowIgnoring;
        if (ignoringRequests.contains(uuid)) {
            ignoringRequests.remove(uuid);
            nowIgnoring = false;
        } else {
            ignoringRequests.add(uuid);
            nowIgnoring = true;
        }
        saveIgnoringRequests();
        return nowIgnoring;
    }

    public void playRequestReceivedSound(Player player) {
        playConfiguredSound(player, requestSound, 0.7F, 1.0F);
    }

    public int sendTeleportAllRequest(Player requester) {
        return sendTeleportAllRequest(requester, ignored -> true);
    }

    public int sendTeleportAllRequest(Player requester, Predicate<Player> targetFilter) {
        OptionalIntCooldown sendCooldown = getRemainingCooldown(requester, sendCooldowns);
        if (sendCooldown.active()) {
            return -sendCooldown.seconds();
        }

        int recipients = 0;
        for (Player target : Bukkit.getOnlinePlayers()) {
            if (target.getUniqueId().equals(requester.getUniqueId())) {
                continue;
            }
            if (!targetFilter.test(target)) {
                continue;
            }
            recipients++;
            CreateRequestResult result = createRequest(requester, target, TeleportRequest.Type.TPAHERE, false);
            if (result.status() != CreateRequestStatus.SUCCESS) {
                continue;
            }
            Map<String, String> placeholders = placeholders(result.request());
            target.sendMessage(plugin.getLangManager().getPrefixedString("tpa.messages.received-tpahere", placeholders));
            sendResponseHint(target, result.request(), placeholders);
            playRequestReceivedSound(target);
        }
        if (recipients > 0) {
            startCooldown(requester, sendCooldowns, sendCooldownSeconds);
        }
        return recipients;
    }

    public int getSendCooldownSeconds(Player player) {
        return getRemainingCooldown(player, sendCooldowns).seconds();
    }

    private void sendResponseHint(Player target, TeleportRequest request, Map<String, String> placeholders) {
        String requesterName = request.requesterName();
        String acceptCommand = "/tpaccept " + requesterName;
        String denyCommand = "/tpdeny " + requesterName;

        Component hint = LegacyComponentSerializer.legacySection()
            .deserialize(plugin.getLangManager().getPrefixedString("tpa.messages.response-hint", placeholders));
        Component accept = LegacyComponentSerializer.legacySection()
            .deserialize(plugin.getLangManager().getString("tpa.messages.accept-button"))
            .clickEvent(ClickEvent.runCommand(acceptCommand))
            .hoverEvent(HoverEvent.showText(Component.text(acceptCommand, NamedTextColor.GREEN)));
        Component deny = LegacyComponentSerializer.legacySection()
            .deserialize(plugin.getLangManager().getString("tpa.messages.deny-button"))
            .clickEvent(ClickEvent.runCommand(denyCommand))
            .hoverEvent(HoverEvent.showText(Component.text(denyCommand, NamedTextColor.RED)));

        target.sendMessage(hint.append(Component.space()).append(accept).append(Component.space()).append(deny));
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    private OptionalIntCooldown getRemainingCooldown(Player player, Map<UUID, Long> cooldowns) {
        if (player.hasPermission(BYPASS_COOLDOWN_PERMISSION)) {
            return OptionalIntCooldown.inactive();
        }
        if (!cooldownsEnabled) {
            return OptionalIntCooldown.inactive();
        }

        long expiresAt = cooldowns.getOrDefault(player.getUniqueId(), 0L);
        long now = System.currentTimeMillis();
        if (expiresAt <= now) {
            cooldowns.remove(player.getUniqueId());
            return OptionalIntCooldown.inactive();
        }

        int seconds = (int) Math.ceil((expiresAt - now) / 1000.0D);
        return new OptionalIntCooldown(true, Math.max(1, seconds));
    }

    private void startCooldown(Player player, Map<UUID, Long> cooldowns, int seconds) {
        if (!cooldownsEnabled || seconds <= 0 || player.hasPermission(BYPASS_COOLDOWN_PERMISSION)) {
            return;
        }
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis() + seconds * 1000L);
    }

    private TeleportPlan createTeleportPlan(TeleportRequest request, Player requester, Player currentTarget) {
        if (request.type() == TeleportRequest.Type.TPA) {
            return new TeleportPlan(
                requester,
                strictTpaRequests ? currentTarget.getLocation().clone() : null,
                strictTpaRequests ? null : currentTarget.getUniqueId()
            );
        }
        return new TeleportPlan(
            currentTarget,
            strictTpaHereRequests ? request.requesterLocation().clone() : null,
            strictTpaHereRequests ? null : requester.getUniqueId()
        );
    }

    private void startWarmup(TeleportPlan plan) {
        Player teleporter = plan.teleporter();
        UUID uuid = teleporter.getUniqueId();
        PendingTeleport pendingTeleport = new PendingTeleport(
            plan,
            teleporter.getLocation().clone(),
            warmupSeconds
        );
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> tickWarmup(uuid), 0L, 20L);
        pendingTeleport.setTask(task);
        warmups.put(uuid, pendingTeleport);
        teleporter.sendMessage(plugin.getLangManager().getPrefixedString("tpa.messages.warmup-start",
            Map.of("seconds", String.valueOf(warmupSeconds))));
    }

    private void tickWarmup(UUID teleporterId) {
        PendingTeleport pendingTeleport = warmups.get(teleporterId);
        if (pendingTeleport == null) {
            return;
        }

        Player teleporter = Bukkit.getPlayer(teleporterId);
        if (teleporter == null || !teleporter.isOnline()) {
            cancelWarmup(teleporterId, null, false);
            return;
        }

        if (pendingTeleport.remainingSeconds() <= 0) {
            cancelWarmup(teleporterId, null, false);
            TeleportResult result = executeTeleport(pendingTeleport.plan(), true);
            if (result.status() == TeleportResult.Status.PLAYER_OFFLINE) {
                teleporter.sendMessage(plugin.getLangManager().getPrefixedString("tpa.messages.player-offline"));
            }
            return;
        }

        sendWarmupStatus(teleporter, pendingTeleport.remainingSeconds());
        playConfiguredSound(teleporter, warmupSound, 0.5F, 1.0F);
        pendingTeleport.decrementRemainingSeconds();
    }

    private TeleportResult executeTeleport(TeleportPlan plan, boolean notifyCompletion) {
        Player teleporter = plan.teleporter();
        if (!teleporter.isOnline()) {
            return TeleportResult.PLAYER_OFFLINE;
        }

        Location destination = plan.fixedDestination();
        if (destination == null && plan.dynamicTargetId() != null) {
            Player dynamicTarget = Bukkit.getPlayer(plan.dynamicTargetId());
            if (dynamicTarget == null || !dynamicTarget.isOnline()) {
                return TeleportResult.PLAYER_OFFLINE;
            }
            destination = dynamicTarget.getLocation().clone();
        }

        if (destination == null) {
            return TeleportResult.PLAYER_OFFLINE;
        }

        teleporter.teleportAsync(destination).thenAccept(success -> {
            if (!success || !notifyCompletion) {
                return;
            }
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (teleporter.isOnline()) {
                    teleporter.sendMessage(plugin.getLangManager().getPrefixedString("tpa.messages.teleport-complete"));
                    playConfiguredSound(teleporter, completeSound, 0.7F, 1.0F);
                }
            });
        });
        return TeleportResult.SUCCESS;
    }

    private void sendWarmupStatus(Player player, int seconds) {
        String message = plugin.getLangManager().getString("tpa.messages.warmup-status",
            Map.of("seconds", String.valueOf(seconds)));
        if ("actionbar".equalsIgnoreCase(warmupDisplay)) {
            player.sendActionBar(LegacyComponentSerializer.legacySection().deserialize(message));
            return;
        }
        if (!"none".equalsIgnoreCase(warmupDisplay)) {
            player.sendMessage(message);
        }
    }

    private void cancelWarmup(UUID teleporterId, String messagePath, boolean playSound) {
        PendingTeleport pendingTeleport = warmups.remove(teleporterId);
        if (pendingTeleport == null) {
            return;
        }
        if (pendingTeleport.task() != null) {
            pendingTeleport.task().cancel();
        }

        Player player = Bukkit.getPlayer(teleporterId);
        if (player != null && player.isOnline() && messagePath != null) {
            player.sendMessage(plugin.getLangManager().getPrefixedString(messagePath));
            if (playSound) {
                playConfiguredSound(player, cancelSound, 0.7F, 1.0F);
            }
        }
    }

    private void cancelAllWarmups() {
        List<UUID> warmingPlayers = new ArrayList<>(warmups.keySet());
        for (UUID warmingPlayer : warmingPlayers) {
            cancelWarmup(warmingPlayer, null, false);
        }
    }

    private void playConfiguredSound(Player player, String sound, float volume, float pitch) {
        if (!soundsEnabled || sound == null || sound.isBlank()) {
            return;
        }
        try {
            player.playSound(player.getLocation(), sound, volume, pitch);
        } catch (IllegalArgumentException ignored) {
            plugin.getLogger().warning("无效的 TPA 音效配置: " + sound);
        }
    }

    private boolean removeIncomingByRequester(Player target, String requesterName) {
        Deque<TeleportRequest> targetRequests = requests.get(target.getUniqueId());
        if (targetRequests == null) {
            return false;
        }

        boolean removed = targetRequests.removeIf(request ->
            request.requesterName().equalsIgnoreCase(requesterName));
        if (targetRequests.isEmpty()) {
            requests.remove(target.getUniqueId());
        }
        return removed;
    }

    public void purgeExpired() {
        List<UUID> emptyQueues = new ArrayList<>();
        for (Map.Entry<UUID, Deque<TeleportRequest>> entry : requests.entrySet()) {
            entry.getValue().removeIf(TeleportRequest::hasExpired);
            if (entry.getValue().isEmpty()) {
                emptyQueues.add(entry.getKey());
            }
        }
        emptyQueues.forEach(requests::remove);
    }

    public Map<String, String> placeholders(TeleportRequest request) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("requester", request.requesterName());
        placeholders.put("target", request.targetName());
        placeholders.put("seconds", String.valueOf(timeoutSeconds));
        placeholders.put("type", request.type().name().toLowerCase());
        return placeholders;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onPlayerMove(PlayerMoveEvent event) {
        if (!cancelWarmupOnMove || !warmups.containsKey(event.getPlayer().getUniqueId())) {
            return;
        }

        PendingTeleport pendingTeleport = warmups.get(event.getPlayer().getUniqueId());
        Location start = pendingTeleport.startLocation();
        Location to = event.getTo();
        if (to == null || !sameWorld(start, to)) {
            cancelWarmup(event.getPlayer().getUniqueId(), "tpa.messages.warmup-cancelled-move", true);
            return;
        }

        double distance = Math.abs(start.getX() - to.getX())
            + Math.abs(start.getY() - to.getY())
            + Math.abs(start.getZ() - to.getZ());
        if (distance > warmupMoveThreshold) {
            cancelWarmup(event.getPlayer().getUniqueId(), "tpa.messages.warmup-cancelled-move", true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onEntityDamage(EntityDamageEvent event) {
        if (cancelWarmupOnDamage && event.getEntity() instanceof Player player && warmups.containsKey(player.getUniqueId())) {
            cancelWarmup(player.getUniqueId(), "tpa.messages.warmup-cancelled-damage", true);
        }
    }

    @EventHandler
    private void onPlayerJoin(PlayerJoinEvent event) {
        if (!isIgnoringRequests(event.getPlayer())) {
            return;
        }

        event.getPlayer().sendMessage(plugin.getLangManager().getPrefixedString("tpa.messages.ignore-notification"));
    }

    @EventHandler
    private void onPlayerQuit(PlayerQuitEvent event) {
        cancelWarmup(event.getPlayer().getUniqueId(), null, false);
    }

    private void startCleanupTask() {
        if (cleanupTask != null) {
            return;
        }
        cleanupTask = Bukkit.getScheduler().runTaskTimer(plugin, this::purgeExpired, 20L * 60L, 20L * 60L);
    }

    private void cancelCleanupTask() {
        if (cleanupTask == null) {
            return;
        }
        cleanupTask.cancel();
        cleanupTask = null;
    }

    private boolean sameWorld(Location first, Location second) {
        return first.getWorld() != null
            && second.getWorld() != null
            && first.getWorld().getUID().equals(second.getWorld().getUID());
    }

    private void loadIgnoringRequests() {
        ignoringRequests.clear();
        if (!ignoreFile.exists()) {
            return;
        }

        FileConfiguration ignoreConfig = YamlConfiguration.loadConfiguration(ignoreFile);
        if (!ignoreConfig.isConfigurationSection("ignored")) {
            return;
        }

        for (String key : ignoreConfig.getConfigurationSection("ignored").getKeys(false)) {
            if (!ignoreConfig.getBoolean("ignored." + key, false)) {
                continue;
            }
            try {
                ignoringRequests.add(UUID.fromString(key));
            } catch (IllegalArgumentException ignored) {
                plugin.getLogger().warning("忽略无效的 TPA 忽略记录 UUID: " + key);
            }
        }
    }

    private void saveIgnoringRequests() {
        FileConfiguration ignoreConfig = new YamlConfiguration();
        for (UUID uuid : ignoringRequests) {
            ignoreConfig.set("ignored." + uuid, true);
        }
        try {
            ignoreConfig.save(ignoreFile);
        } catch (IOException e) {
            plugin.getLogger().warning("保存 teleport-ignore.yml 失败: " + e.getMessage());
        }
    }

    public enum CreateRequestStatus {
        SUCCESS,
        DUPLICATE,
        IGNORED,
        ON_COOLDOWN
    }

    public record TeleportResult(Status status, int cooldownSeconds) {
        public static final TeleportResult SUCCESS = new TeleportResult(Status.SUCCESS, 0);
        public static final TeleportResult WARMING_UP = new TeleportResult(Status.WARMING_UP, 0);
        public static final TeleportResult EXPIRED = new TeleportResult(Status.EXPIRED, 0);
        public static final TeleportResult PLAYER_OFFLINE = new TeleportResult(Status.PLAYER_OFFLINE, 0);
        public static final TeleportResult ALREADY_WARMING_UP = new TeleportResult(Status.ALREADY_WARMING_UP, 0);

        public static TeleportResult onCooldown(int seconds) {
            return new TeleportResult(Status.ON_COOLDOWN, seconds);
        }

        public enum Status {
            SUCCESS,
            WARMING_UP,
            EXPIRED,
            PLAYER_OFFLINE,
            ALREADY_WARMING_UP,
            ON_COOLDOWN
        }
    }

    public record CreateRequestResult(CreateRequestStatus status, TeleportRequest request, int cooldownSeconds) {
        public CreateRequestResult(CreateRequestStatus status, TeleportRequest request) {
            this(status, request, 0);
        }
    }

    public static final class TeleportRequest {
        private final UUID requesterId;
        private final String requesterName;
        private final Location requesterLocation;
        private final UUID targetId;
        private final String targetName;
        private final Location targetLocation;
        private final Type type;
        private final long expiresAtMillis;
        private Status status;

        public TeleportRequest(UUID requesterId, String requesterName, Location requesterLocation,
                               UUID targetId, String targetName, Location targetLocation,
                               Type type, long expiresAtMillis, Status status) {
            this.requesterId = requesterId;
            this.requesterName = requesterName;
            this.requesterLocation = requesterLocation;
            this.targetId = targetId;
            this.targetName = targetName;
            this.targetLocation = targetLocation;
            this.type = type;
            this.expiresAtMillis = expiresAtMillis;
            this.status = status;
        }

        public UUID requesterId() {
            return requesterId;
        }

        public String requesterName() {
            return requesterName;
        }

        public Location requesterLocation() {
            return requesterLocation;
        }

        public UUID targetId() {
            return targetId;
        }

        public String targetName() {
            return targetName;
        }

        public Location targetLocation() {
            return targetLocation;
        }

        public Type type() {
            return type;
        }

        public long expiresAtMillis() {
            return expiresAtMillis;
        }

        public Status status() {
            return status;
        }

        public void setStatus(Status status) {
            this.status = status;
        }

        public boolean hasExpired() {
            return Instant.now().isAfter(Instant.ofEpochMilli(expiresAtMillis));
        }

        public enum Type {
            TPA,
            TPAHERE
        }

        public enum Status {
            PENDING,
            ACCEPTED,
            DECLINED,
            IGNORED
        }
    }

    private record TeleportPlan(Player teleporter, Location fixedDestination, UUID dynamicTargetId) {
    }

    private record OptionalIntCooldown(boolean active, int seconds) {
        private static OptionalIntCooldown inactive() {
            return new OptionalIntCooldown(false, 0);
        }
    }

    private static final class PendingTeleport {
        private final TeleportPlan plan;
        private final Location startLocation;
        private int remainingSeconds;
        private BukkitTask task;

        private PendingTeleport(TeleportPlan plan, Location startLocation, int remainingSeconds) {
            this.plan = plan;
            this.startLocation = startLocation;
            this.remainingSeconds = remainingSeconds;
        }

        private TeleportPlan plan() {
            return plan;
        }

        private Location startLocation() {
            return startLocation;
        }

        private int remainingSeconds() {
            return remainingSeconds;
        }

        private void decrementRemainingSeconds() {
            remainingSeconds--;
        }

        private BukkitTask task() {
            return task;
        }

        private void setTask(BukkitTask task) {
            this.task = task;
        }
    }
}
