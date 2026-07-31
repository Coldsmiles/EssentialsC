package cn.infstar.essentialsC.teleport;

import cn.infstar.essentialsC.EssentialsC;
import cn.infstar.essentialsC.api.event.TeleportEvent;
import cn.infstar.essentialsC.api.event.TeleportRequestReceiveEvent;
import cn.infstar.essentialsC.api.event.TeleportRequestReplyEvent;
import cn.infstar.essentialsC.api.event.TeleportRequestSendEvent;
import cn.infstar.essentialsC.api.event.TeleportWarmupCancelledEvent;
import cn.infstar.essentialsC.api.event.TeleportWarmupEvent;
import cn.infstar.essentialsC.commands.VanishCommand;
import cn.infstar.essentialsC.util.AtomicYamlWriter;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

public final class TeleportRequestManager implements Listener {

    public static final String BYPASS_WARMUP_PERMISSION = "essentialsc.tpa.bypass-warmup";
    public static final String BYPASS_COOLDOWN_PERMISSION = "essentialsc.tpa.bypass-cooldown";
    public static final String WARMUP_PERMISSION_PREFIX = "essentialsc.tpa.warmup.";
    private static final double MOVEMENT_THRESHOLD = 0.1D;

    private final EssentialsC plugin;
    private final Map<UUID, Deque<TeleportRequest>> requests = new HashMap<>();
    private final Map<UUID, PendingTeleport> warmups = new HashMap<>();
    private final Map<UUID, Long> sendCooldowns = new HashMap<>();
    private final Map<UUID, Long> acceptCooldowns = new HashMap<>();
    private final Map<UUID, BukkitTask> invulnerabilityTasks = new HashMap<>();
    private final Set<UUID> warmupDamagedPlayers = new HashSet<>();
    private final Set<UUID> invulnerablePlayers = new HashSet<>();
    private final Set<UUID> ignoringRequests = new HashSet<>();
    private final File ignoreFile;
    private final File cooldownFile;
    private BukkitTask cleanupTask;

    private int timeoutSeconds;
    private int maxPendingRequests;
    private boolean strictTpaRequests;
    private boolean strictTpaHereRequests;
    private int warmupSeconds;
    private boolean cancelWarmupOnMove;
    private boolean cancelWarmupOnDamage;
    private boolean cooldownsEnabled;
    private int sendCooldownSeconds;
    private int acceptCooldownSeconds;
    private String warmupDisplay;
    private int teleportInvulnerabilitySeconds;
    private boolean teleportAsync;
    private boolean soundsEnabled;
    private String requestSound;
    private String warmupSound;
    private String cancelSound;
    private String completeSound;

    public TeleportRequestManager(EssentialsC plugin) {
        this.plugin = plugin;
        this.ignoreFile = new File(plugin.getDataFolder(), "teleport-ignore.yml");
        this.cooldownFile = new File(plugin.getDataFolder(), "teleport-cooldowns.yml");
        reload();
        loadCooldowns();
        cleanupTask = Bukkit.getScheduler().runTaskTimer(plugin, this::cleanupExpiredState, 1200L, 1200L);
    }

    public void reload() {
        plugin.getConfig().addDefault("tpa.timeout-seconds", 60);
        plugin.getConfig().addDefault("tpa.max-pending-requests", 5);
        plugin.getConfig().addDefault("tpa.strict-tpa-requests", false);
        plugin.getConfig().addDefault("tpa.strict-tpahere-requests", true);
        plugin.getConfig().addDefault("tpa.warmup-seconds", 5);
        plugin.getConfig().addDefault("tpa.cancel-warmup-on-move", true);
        plugin.getConfig().addDefault("tpa.cancel-warmup-on-damage", true);
        plugin.getConfig().addDefault("tpa.cooldowns.enabled", true);
        plugin.getConfig().addDefault("tpa.cooldowns.cooldown-times.SEND_TELEPORT_REQUEST", 0);
        plugin.getConfig().addDefault("tpa.cooldowns.cooldown-times.ACCEPT_TELEPORT_REQUEST", 0);
        plugin.getConfig().addDefault("tpa.warmup-display", "actionbar");
        plugin.getConfig().addDefault("tpa.teleport-invulnerability-seconds", 0);
        plugin.getConfig().addDefault("tpa.teleport-async", true);
        plugin.getConfig().addDefault("tpa.sounds.enabled", true);
        plugin.getConfig().addDefault("tpa.sounds.request-received", "entity.experience_orb.pickup");
        plugin.getConfig().addDefault("tpa.sounds.warmup", "block.note_block.banjo");
        plugin.getConfig().addDefault("tpa.sounds.cancelled", "entity.item.break");
        plugin.getConfig().addDefault("tpa.sounds.complete", "entity.enderman.teleport");
        plugin.getConfig().set("tpa.warmup-move-threshold", null);
        plugin.getConfig().options().copyDefaults(true);

        timeoutSeconds = Math.max(0, plugin.getConfig().getInt("tpa.timeout-seconds", 60));
        maxPendingRequests = Math.max(1, Math.min(100,
            plugin.getConfig().getInt("tpa.max-pending-requests", 5)));
        strictTpaRequests = plugin.getConfig().getBoolean("tpa.strict-tpa-requests", false);
        strictTpaHereRequests = plugin.getConfig().getBoolean("tpa.strict-tpahere-requests", true);
        warmupSeconds = Math.max(0, plugin.getConfig().getInt("tpa.warmup-seconds", 5));
        cancelWarmupOnMove = plugin.getConfig().getBoolean("tpa.cancel-warmup-on-move", true);
        cancelWarmupOnDamage = plugin.getConfig().getBoolean("tpa.cancel-warmup-on-damage", true);
        cooldownsEnabled = plugin.getConfig().getBoolean("tpa.cooldowns.enabled", true);
        sendCooldownSeconds = Math.max(0, plugin.getConfig().getInt("tpa.cooldowns.cooldown-times.SEND_TELEPORT_REQUEST", 0));
        acceptCooldownSeconds = Math.max(0, plugin.getConfig().getInt("tpa.cooldowns.cooldown-times.ACCEPT_TELEPORT_REQUEST", 0));
        warmupDisplay = plugin.getConfig().getString("tpa.warmup-display", "actionbar").toLowerCase(Locale.ROOT);
        teleportInvulnerabilitySeconds = Math.max(0,
            plugin.getConfig().getInt("tpa.teleport-invulnerability-seconds", 0));
        teleportAsync = plugin.getConfig().getBoolean("tpa.teleport-async", true);
        soundsEnabled = plugin.getConfig().getBoolean("tpa.sounds.enabled", true);
        requestSound = plugin.getConfig().getString("tpa.sounds.request-received", "entity.experience_orb.pickup");
        warmupSound = plugin.getConfig().getString("tpa.sounds.warmup", "block.note_block.banjo");
        cancelSound = plugin.getConfig().getString("tpa.sounds.cancelled", "entity.item.break");
        completeSound = plugin.getConfig().getString("tpa.sounds.complete", "entity.enderman.teleport");
        loadIgnoringRequests();
    }

    public void shutdown() {
        if (cleanupTask != null) {
            cleanupTask.cancel();
            cleanupTask = null;
        }
        saveCooldowns();
        requests.clear();
        sendCooldowns.clear();
        acceptCooldowns.clear();
        cancelAllWarmups();
        clearAllInvulnerability();
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

        if (applyCooldown) {
            TeleportRequestSendEvent sendEvent = new TeleportRequestSendEvent(requester, request);
            Bukkit.getPluginManager().callEvent(sendEvent);
            if (sendEvent.isCancelled()) {
                return new CreateRequestResult(CreateRequestStatus.CANCELLED, request);
            }
        }

        if (isIgnoringRequests(target) || isVanished(target)) {
            request.setStatus(TeleportRequest.Status.IGNORED);
            if (applyCooldown) {
                startCooldown(requester, sendCooldowns, sendCooldownSeconds);
            }
            return new CreateRequestResult(CreateRequestStatus.IGNORED, request);
        }

        Optional<TeleportRequest> existingRequest = findIncoming(target, requester.getName());
        if (existingRequest.isPresent()
            && existingRequest.get().type() == type
            && !existingRequest.get().hasExpired()) {
            if (applyCooldown) {
                startCooldown(requester, sendCooldowns, sendCooldownSeconds);
            }
            return new CreateRequestResult(CreateRequestStatus.DUPLICATE, existingRequest.get());
        }

        TeleportRequestReceiveEvent receiveEvent = new TeleportRequestReceiveEvent(target, request);
        Bukkit.getPluginManager().callEvent(receiveEvent);
        if (receiveEvent.isCancelled()) {
            return new CreateRequestResult(CreateRequestStatus.CANCELLED, request);
        }
        Deque<TeleportRequest> targetRequests = requests.computeIfAbsent(target.getUniqueId(), ignored -> new ArrayDeque<>());
        pruneExpiredRequests(targetRequests);
        TeleportRequestQueuePolicy.addFirstBounded(targetRequests, request, maxPendingRequests);
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

        removeIncomingByRequester(target, request.requesterName());
        if (request.hasExpired()) {
            return TeleportResult.EXPIRED;
        }

        request.setStatus(TeleportRequest.Status.ACCEPTED);
        TeleportRequestReplyEvent replyEvent = new TeleportRequestReplyEvent(target, request);
        Bukkit.getPluginManager().callEvent(replyEvent);
        if (replyEvent.isCancelled()) {
            return TeleportResult.CANCELLED;
        }
        Map<String, String> placeholders = placeholders(request);
        target.sendMessage(plugin.getLangManager().getPrefixedComponent("tpa.messages.accepted-target", placeholders));

        Player requester = Bukkit.getPlayer(request.requesterId());
        if (requester == null || !requester.isOnline()) {
            target.sendMessage(plugin.getLangManager().getPrefixedComponent("tpa.messages.player-offline", placeholders));
            return TeleportResult.PLAYER_OFFLINE;
        }
        requester.sendMessage(plugin.getLangManager().getPrefixedComponent("tpa.messages.accepted-sender", placeholders));

        TeleportPlan plan = createTeleportPlan(request, requester, target);
        Player teleporter = plan.teleporter();
        int playerWarmupSeconds = getTeleportWarmupSeconds(teleporter);
        if (playerWarmupSeconds <= 0 || teleporter.hasPermission(BYPASS_WARMUP_PERMISSION)) {
            return executeTeleport(plan, true);
        }

        if (warmups.containsKey(teleporter.getUniqueId())) {
            teleporter.sendMessage(plugin.getLangManager().getPrefixedComponent("tpa.messages.already-warming-up"));
            return TeleportResult.ACCEPTED_WITHOUT_TELEPORT;
        }
        OptionalIntCooldown teleporterCooldown = getRemainingCooldown(teleporter, acceptCooldowns);
        if (teleporterCooldown.active()) {
            sendAcceptCooldown(teleporter, teleporterCooldown.seconds());
            return TeleportResult.ACCEPTED_WITHOUT_TELEPORT;
        }
        if (isMoving(teleporter)) {
            teleporter.sendMessage(plugin.getLangManager().getPrefixedComponent("tpa.messages.warmup-stand-still"));
            return TeleportResult.ACCEPTED_WITHOUT_TELEPORT;
        }

        return startWarmup(plan, playerWarmupSeconds)
            ? TeleportResult.WARMING_UP
            : TeleportResult.CANCELLED;
    }

    public TeleportResult deny(Player target, TeleportRequest request) {
        if (!removeIncomingByRequester(target, request.requesterName())) {
            return TeleportResult.EXPIRED;
        }
        if (request.hasExpired()) {
            return TeleportResult.EXPIRED;
        }
        request.setStatus(TeleportRequest.Status.DECLINED);
        TeleportRequestReplyEvent replyEvent = new TeleportRequestReplyEvent(target, request);
        Bukkit.getPluginManager().callEvent(replyEvent);
        if (replyEvent.isCancelled()) {
            return TeleportResult.CANCELLED;
        }
        Map<String, String> placeholders = placeholders(request);
        target.sendMessage(plugin.getLangManager().getPrefixedComponent("tpa.messages.denied-target", placeholders));
        Player requester = Bukkit.getPlayer(request.requesterId());
        if (requester != null && requester.isOnline()) {
            requester.sendMessage(plugin.getLangManager().getPrefixedComponent("tpa.messages.denied-sender", placeholders));
        } else {
            target.sendMessage(plugin.getLangManager().getPrefixedComponent("tpa.messages.player-offline", placeholders));
        }
        return TeleportResult.SUCCESS;
    }

    public boolean isIgnoringRequests(Player player) {
        return ignoringRequests.contains(player.getUniqueId());
    }

    public boolean isVanished(Player player) {
        if (VanishCommand.isVanished(player)) {
            return true;
        }
        return player.hasMetadata("vanished") && player.getMetadata("vanished").stream()
            .map(metadata -> metadata.asBoolean())
            .findFirst()
            .orElse(false);
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
        playConfiguredSound(player, requestSound, 1.0F, 1.0F);
    }

    public int sendTeleportAllRequest(Player requester) {
        int recipients = 0;
        for (Player target : Bukkit.getOnlinePlayers()) {
            if (target.getUniqueId().equals(requester.getUniqueId())) {
                continue;
            }
            recipients++;
            CreateRequestResult result = createRequest(requester, target, TeleportRequest.Type.TPAHERE, false);
            if (result.status() != CreateRequestStatus.SUCCESS) {
                continue;
            }
            Map<String, String> placeholders = placeholders(result.request());
            target.sendMessage(plugin.getLangManager().getPrefixedComponent("tpa.messages.received-tpahere", placeholders));
            sendResponseHint(target, placeholders);
            playRequestReceivedSound(target);
        }
        return recipients;
    }

    public int getRemainingSendCooldownSeconds(Player player) {
        return getRemainingCooldown(player, sendCooldowns).seconds();
    }

    public Optional<Player> findOnlinePlayer(String playerName, Predicate<Player> filter) {
        Optional<Player> exactMatch = Bukkit.getOnlinePlayers().stream()
            .map(Player.class::cast)
            .filter(filter)
            .filter(player -> player.getName().equalsIgnoreCase(playerName))
            .findFirst();
        if (exactMatch.isPresent()) {
            return exactMatch;
        }

        String playerNameLower = playerName.toLowerCase();
        return Bukkit.getOnlinePlayers().stream()
            .map(Player.class::cast)
            .filter(filter)
            .filter(player -> player.getName().toLowerCase().startsWith(playerNameLower))
            .findFirst();
    }

    public void sendResponseHint(Player target, Map<String, String> placeholders) {
        target.sendMessage(plugin.getLangManager().getPrefixedComponent("tpa.messages.response-buttons", placeholders));
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
                strictTpaRequests ? null : currentTarget.getUniqueId(),
                request
            );
        }
        return new TeleportPlan(
            currentTarget,
            strictTpaHereRequests ? request.requesterLocation().clone() : null,
            strictTpaHereRequests ? null : requester.getUniqueId(),
            request
        );
    }

    private int getTeleportWarmupSeconds(Player player) {
        return player.getEffectivePermissions().stream()
            .filter(PermissionAttachmentInfo::getValue)
            .map(PermissionAttachmentInfo::getPermission)
            .filter(permission -> permission.startsWith(WARMUP_PERMISSION_PREFIX))
            .map(permission -> permission.substring(WARMUP_PERMISSION_PREFIX.length()))
            .mapToInt(this::parseWarmupPermission)
            .filter(seconds -> seconds >= 0)
            .max()
            .orElse(warmupSeconds);
    }

    private int parseWarmupPermission(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private boolean startWarmup(TeleportPlan plan, int playerWarmupSeconds) {
        Player teleporter = plan.teleporter();
        UUID uuid = teleporter.getUniqueId();
        TeleportWarmupEvent warmupEvent = new TeleportWarmupEvent(teleporter, plan.request(), playerWarmupSeconds);
        Bukkit.getPluginManager().callEvent(warmupEvent);
        if (warmupEvent.isCancelled()) {
            return false;
        }
        PendingTeleport pendingTeleport = new PendingTeleport(
            plan,
            teleporter.getLocation().clone(),
            playerWarmupSeconds
        );
        warmupDamagedPlayers.remove(uuid);
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> tickWarmup(uuid), 0L, 20L);
        pendingTeleport.setTask(task);
        warmups.put(uuid, pendingTeleport);
        teleporter.sendMessage(plugin.getLangManager().getPrefixedComponent("tpa.messages.warmup-start",
            Map.of("seconds", String.valueOf(playerWarmupSeconds))));
        return true;
    }

    private void tickWarmup(UUID teleporterId) {
        PendingTeleport pendingTeleport = warmups.get(teleporterId);
        if (pendingTeleport == null) {
            return;
        }

        Player teleporter = Bukkit.getPlayer(teleporterId);
        if (teleporter == null || !teleporter.isOnline()) {
            cancelWarmup(teleporterId, null, false, TeleportWarmupCancelledEvent.Reason.PLAYER_QUIT);
            return;
        }

        if (cancelWarmupOnDamage && warmupDamagedPlayers.contains(teleporterId)) {
            cancelWarmup(teleporterId, "tpa.messages.warmup-cancelled-damage", true,
                TeleportWarmupCancelledEvent.Reason.PLAYER_DAMAGE);
            return;
        }
        if (cancelWarmupOnMove && hasMoved(pendingTeleport.startLocation(), teleporter.getLocation())) {
            cancelWarmup(teleporterId, "tpa.messages.warmup-cancelled-move", true,
                TeleportWarmupCancelledEvent.Reason.PLAYER_MOVE);
            return;
        }

        if (pendingTeleport.remainingSeconds() <= 0) {
            sendWarmupStatus(teleporter, "tpa.messages.warmup-processing", Map.of());
            cancelWarmup(teleporterId, null, false);
            TeleportResult result = executeTeleport(pendingTeleport.plan(), true);
            if (result.status() == TeleportResult.Status.ON_COOLDOWN) {
                sendAcceptCooldown(teleporter, result.cooldownSeconds());
            }
            return;
        }

        sendWarmupStatus(teleporter, "tpa.messages.warmup-status",
            Map.of("seconds", String.valueOf(pendingTeleport.remainingSeconds())));
        playConfiguredSound(teleporter, warmupSound, 1.0F, 1.0F);
        pendingTeleport.decrementRemainingSeconds();
    }

    private TeleportResult executeTeleport(TeleportPlan plan, boolean notifyCompletion) {
        Player teleporter = plan.teleporter();
        if (!teleporter.isOnline()) {
            return TeleportResult.PLAYER_OFFLINE;
        }

        OptionalIntCooldown acceptCooldown = getRemainingCooldown(teleporter, acceptCooldowns);
        if (acceptCooldown.active()) {
            return TeleportResult.onCooldown(acceptCooldown.seconds());
        }

        Location destination = plan.fixedDestination();
        if (destination == null && plan.dynamicTargetId() != null) {
            Player dynamicTarget = Bukkit.getPlayer(plan.dynamicTargetId());
            if (dynamicTarget == null || !dynamicTarget.isOnline()) {
                teleporter.sendMessage(plugin.getLangManager().getPrefixedComponent("tpa.messages.target-offline"));
                return TeleportResult.TARGET_OFFLINE;
            }
            destination = dynamicTarget.getLocation().clone();
        }

        if (destination == null) {
            teleporter.sendMessage(plugin.getLangManager().getPrefixedComponent("tpa.messages.target-offline"));
            return TeleportResult.TARGET_OFFLINE;
        }

        TeleportEvent teleportEvent = new TeleportEvent(teleporter, plan.request(), destination);
        Bukkit.getPluginManager().callEvent(teleportEvent);
        if (teleportEvent.isCancelled()) {
            return TeleportResult.CANCELLED;
        }
        destination = teleportEvent.getDestination();

        teleporter.leaveVehicle();
        teleporter.eject();
        teleporter.setFallDistance(0.0F);
        startCooldown(teleporter, acceptCooldowns, acceptCooldownSeconds);
        if (teleportAsync) {
            try {
                teleporter.teleportAsync(destination, PlayerTeleportEvent.TeleportCause.PLUGIN)
                    .whenComplete((success, throwable) -> {
                        if (!plugin.isEnabled()) {
                            return;
                        }
                        Bukkit.getScheduler().runTask(plugin, () -> handleTeleportCompletion(
                            teleporter, Boolean.TRUE.equals(success), throwable, notifyCompletion));
                    });
            } catch (RuntimeException exception) {
                handleTeleportCompletion(teleporter, false, exception, notifyCompletion);
            }
        } else {
            try {
                boolean success = teleporter.teleport(destination, PlayerTeleportEvent.TeleportCause.PLUGIN);
                handleTeleportCompletion(teleporter, success, null, notifyCompletion);
            } catch (RuntimeException exception) {
                handleTeleportCompletion(teleporter, false, exception, notifyCompletion);
            }
        }
        return TeleportResult.SUCCESS;
    }

    private void handleTeleportCompletion(Player teleporter, boolean success, Throwable throwable,
                                          boolean notifyCompletion) {
        if (!success) {
            if (teleporter.isOnline()) {
                teleporter.sendMessage(plugin.getLangManager().getPrefixedComponent("tpa.messages.teleport-failed"));
            }
            if (throwable != null) {
                String detail = throwable.getMessage() == null
                    ? throwable.getClass().getSimpleName()
                    : throwable.getMessage();
                plugin.getLogger().warning("TPA 传送执行失败: " + detail);
            }
            return;
        }

        applyTeleportInvulnerability(teleporter);
        if (notifyCompletion && teleporter.isOnline()) {
            teleporter.sendMessage(plugin.getLangManager().getPrefixedComponent("tpa.messages.teleport-complete"));
            playConfiguredSound(teleporter, completeSound, 1.0F, 1.0F);
        }
    }

    private void applyTeleportInvulnerability(Player player) {
        if (teleportInvulnerabilitySeconds <= 0 || !player.isOnline()) {
            return;
        }

        UUID uuid = player.getUniqueId();
        BukkitTask previousTask = invulnerabilityTasks.remove(uuid);
        if (previousTask != null) {
            previousTask.cancel();
        }
        if (player.isInvulnerable() && !invulnerablePlayers.contains(uuid)) {
            return;
        }

        invulnerablePlayers.add(uuid);
        player.setInvulnerable(true);
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin,
            () -> clearInvulnerability(uuid), teleportInvulnerabilitySeconds * 20L);
        invulnerabilityTasks.put(uuid, task);
    }

    private void clearInvulnerability(UUID uuid) {
        BukkitTask task = invulnerabilityTasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }
        if (!invulnerablePlayers.remove(uuid)) {
            return;
        }
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            player.setInvulnerable(false);
        }
    }

    private void clearAllInvulnerability() {
        for (BukkitTask task : invulnerabilityTasks.values()) {
            task.cancel();
        }
        invulnerabilityTasks.clear();
        for (UUID uuid : new HashSet<>(invulnerablePlayers)) {
            clearInvulnerability(uuid);
        }
    }

    private void sendAcceptCooldown(Player player, int seconds) {
        player.sendMessage(plugin.getLangManager().getPrefixedComponent("tpa.messages.accept-cooldown",
            Map.of("seconds", String.valueOf(seconds))));
    }

    private void sendWarmupStatus(Player player, String messagePath, Map<String, String> placeholders) {
        net.kyori.adventure.text.Component message = plugin.getLangManager().getPrefixedComponent(messagePath, placeholders);
        switch (warmupDisplay.replace("-", "_")) {
            case "actionbar", "action_bar" -> player.sendActionBar(message);
            case "title" -> player.showTitle(Title.title(message, net.kyori.adventure.text.Component.empty(),
                Title.Times.times(Duration.ZERO, Duration.ofSeconds(1), Duration.ofMillis(250))));
            case "subtitle" -> player.showTitle(Title.title(net.kyori.adventure.text.Component.empty(), message,
                Title.Times.times(Duration.ZERO, Duration.ofSeconds(1), Duration.ofMillis(250))));
            case "none" -> {
            }
            default -> player.sendMessage(message);
        }
    }

    private void cancelWarmup(UUID teleporterId, String messagePath, boolean playSound) {
        cancelWarmup(teleporterId, messagePath, playSound, null);
    }

    private void cancelWarmup(UUID teleporterId, String messagePath, boolean playSound,
                              TeleportWarmupCancelledEvent.Reason reason) {
        PendingTeleport pendingTeleport = warmups.remove(teleporterId);
        warmupDamagedPlayers.remove(teleporterId);
        if (pendingTeleport == null) {
            return;
        }
        if (pendingTeleport.task() != null) {
            pendingTeleport.task().cancel();
        }

        Player player = Bukkit.getPlayer(teleporterId);
        if (player != null && player.isOnline() && messagePath != null) {
            player.sendMessage(plugin.getLangManager().getPrefixedComponent(messagePath));
            sendWarmupStatus(player, "tpa.messages.warmup-cancelled-actionbar", Map.of());
            if (playSound) {
                playConfiguredSound(player, cancelSound, 1.0F, 1.0F);
            }
        }
        if (player != null && reason != null) {
            int duration = pendingTeleport.initialSeconds();
            Bukkit.getPluginManager().callEvent(new TeleportWarmupCancelledEvent(
                player, pendingTeleport.plan().request(), duration,
                Math.max(0, duration - pendingTeleport.remainingSeconds()), reason));
        }
    }

    private void cancelAllWarmups() {
        List<UUID> warmingPlayers = new ArrayList<>(warmups.keySet());
        for (UUID warmingPlayer : warmingPlayers) {
            cancelWarmup(warmingPlayer, null, false, TeleportWarmupCancelledEvent.Reason.PLUGIN_SHUTDOWN);
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

    public Map<String, String> placeholders(TeleportRequest request) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("requester", request.requesterName());
        placeholders.put("target", request.targetName());
        placeholders.put("seconds", String.valueOf(timeoutSeconds));
        placeholders.put("type", request.type().name().toLowerCase());
        return placeholders;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onEntityDamage(EntityDamageEvent event) {
        if (event.getDamage() <= 0) {
            return;
        }
        if (event.getEntity() instanceof Player player && warmups.containsKey(player.getUniqueId())) {
            warmupDamagedPlayers.add(player.getUniqueId());
        }
    }

    @EventHandler
    private void onPlayerJoin(PlayerJoinEvent event) {
        if (!isIgnoringRequests(event.getPlayer())) {
            return;
        }

        event.getPlayer().sendMessage(plugin.getLangManager().getPrefixedComponent("tpa.messages.ignore-notification"));
    }

    @EventHandler
    private void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        cancelWarmup(uuid, null, false, TeleportWarmupCancelledEvent.Reason.PLAYER_QUIT);
        clearInvulnerability(uuid);
        requests.remove(uuid);
        requests.values().removeIf(targetRequests -> {
            targetRequests.removeIf(request -> request.requesterId().equals(uuid));
            return targetRequests.isEmpty();
        });
    }

    private boolean hasMoved(Location start, Location current) {
        if (start.getWorld() == null || current.getWorld() == null
            || !start.getWorld().getUID().equals(current.getWorld().getUID())) {
            return true;
        }
        double distance = Math.abs(start.getX() - current.getX())
            + Math.abs(start.getY() - current.getY())
            + Math.abs(start.getZ() - current.getZ());
        return distance > MOVEMENT_THRESHOLD;
    }

    private boolean isMoving(Player player) {
        return player.getVelocity().length() >= MOVEMENT_THRESHOLD;
    }

    private void pruneExpiredRequests(Deque<TeleportRequest> targetRequests) {
        long retentionMillis = Math.max(60_000L, timeoutSeconds * 1000L);
        long now = System.currentTimeMillis();
        targetRequests.removeIf(request -> TeleportRequestQueuePolicy.shouldPrune(
            request.expiresAtMillis(), now, retentionMillis));
    }

    private void cleanupExpiredState() {
        requests.entrySet().removeIf(entry -> {
            pruneExpiredRequests(entry.getValue());
            return entry.getValue().isEmpty();
        });
        long now = System.currentTimeMillis();
        sendCooldowns.values().removeIf(expiresAt -> expiresAt <= now);
        acceptCooldowns.values().removeIf(expiresAt -> expiresAt <= now);
        saveCooldowns();
    }

    private void loadCooldowns() {
        if (!cooldownFile.exists()) {
            return;
        }
        FileConfiguration cooldownConfig = YamlConfiguration.loadConfiguration(cooldownFile);
        loadCooldownMap(cooldownConfig, "send", sendCooldowns);
        loadCooldownMap(cooldownConfig, "accept", acceptCooldowns);
    }

    private void loadCooldownMap(FileConfiguration config, String path, Map<UUID, Long> destination) {
        if (!config.isConfigurationSection(path)) {
            return;
        }
        long now = System.currentTimeMillis();
        for (String key : config.getConfigurationSection(path).getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                long expiresAt = config.getLong(path + "." + key, 0L);
                if (expiresAt > now) {
                    destination.put(uuid, expiresAt);
                }
            } catch (IllegalArgumentException ignored) {
                plugin.getLogger().warning("忽略无效的 TPA 冷却记录 UUID: " + key);
            }
        }
    }

    private void saveCooldowns() {
        FileConfiguration cooldownConfig = new YamlConfiguration();
        long now = System.currentTimeMillis();
        saveCooldownMap(cooldownConfig, "send", sendCooldowns, now);
        saveCooldownMap(cooldownConfig, "accept", acceptCooldowns, now);
        try {
            AtomicYamlWriter.save(cooldownConfig, cooldownFile);
        } catch (Exception exception) {
            plugin.getLogger().warning("保存 teleport-cooldowns.yml 失败: " + exception.getMessage());
        }
    }

    private void saveCooldownMap(FileConfiguration config, String path, Map<UUID, Long> source, long now) {
        source.forEach((uuid, expiresAt) -> {
            if (expiresAt > now) {
                config.set(path + "." + uuid, expiresAt);
            }
        });
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
            AtomicYamlWriter.save(ignoreConfig, ignoreFile);
        } catch (Exception e) {
            plugin.getLogger().warning("保存 teleport-ignore.yml 失败: " + e.getMessage());
        }
    }

    public enum CreateRequestStatus {
        SUCCESS,
        DUPLICATE,
        IGNORED,
        ON_COOLDOWN,
        CANCELLED
    }

    public record TeleportResult(Status status, int cooldownSeconds) {
        public static final TeleportResult SUCCESS = new TeleportResult(Status.SUCCESS, 0);
        public static final TeleportResult WARMING_UP = new TeleportResult(Status.WARMING_UP, 0);
        public static final TeleportResult EXPIRED = new TeleportResult(Status.EXPIRED, 0);
        public static final TeleportResult PLAYER_OFFLINE = new TeleportResult(Status.PLAYER_OFFLINE, 0);
        public static final TeleportResult TARGET_OFFLINE = new TeleportResult(Status.TARGET_OFFLINE, 0);
        public static final TeleportResult ALREADY_WARMING_UP = new TeleportResult(Status.ALREADY_WARMING_UP, 0);
        public static final TeleportResult ACCEPTED_WITHOUT_TELEPORT = new TeleportResult(Status.ACCEPTED_WITHOUT_TELEPORT, 0);
        public static final TeleportResult CANCELLED = new TeleportResult(Status.CANCELLED, 0);

        public static TeleportResult onCooldown(int seconds) {
            return new TeleportResult(Status.ON_COOLDOWN, seconds);
        }

        public enum Status {
            SUCCESS,
            WARMING_UP,
            EXPIRED,
            PLAYER_OFFLINE,
            TARGET_OFFLINE,
            ALREADY_WARMING_UP,
            ACCEPTED_WITHOUT_TELEPORT,
            ON_COOLDOWN,
            CANCELLED
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

    private record TeleportPlan(Player teleporter, Location fixedDestination, UUID dynamicTargetId,
                                TeleportRequest request) {
    }

    private record OptionalIntCooldown(boolean active, int seconds) {
        private static OptionalIntCooldown inactive() {
            return new OptionalIntCooldown(false, 0);
        }
    }

    private static final class PendingTeleport {
        private final TeleportPlan plan;
        private final Location startLocation;
        private final int initialSeconds;
        private int remainingSeconds;
        private BukkitTask task;

        private PendingTeleport(TeleportPlan plan, Location startLocation, int remainingSeconds) {
            this.plan = plan;
            this.startLocation = startLocation;
            this.initialSeconds = remainingSeconds;
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

        private int initialSeconds() {
            return initialSeconds;
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
