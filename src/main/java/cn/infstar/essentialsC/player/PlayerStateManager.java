package cn.infstar.essentialsC.player;

import cn.infstar.essentialsC.EssentialsC;
import org.bukkit.GameMode;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

public final class PlayerStateManager implements Listener {

    public static final String FLY_PERMISSION = "essentialsc.command.fly";
    public static final String NIGHT_VISION_PERMISSION = "essentialsc.command.nightvision";
    public static final String GLOW_PERMISSION = "essentialsc.command.glow";

    private final EssentialsC plugin;
    private final NamespacedKey flyEnabledKey;
    private final NamespacedKey flyPreviousAllowKey;
    private final NamespacedKey flyPreviousFlyingKey;
    private final NamespacedKey nightVisionEnabledKey;
    private final NamespacedKey glowEnabledKey;
    private final BukkitTask permissionTask;

    public PlayerStateManager(EssentialsC plugin) {
        this.plugin = plugin;
        this.flyEnabledKey = new NamespacedKey(plugin, "fly_enabled");
        this.flyPreviousAllowKey = new NamespacedKey(plugin, "fly_previous_allow");
        this.flyPreviousFlyingKey = new NamespacedKey(plugin, "fly_previous_flying");
        this.nightVisionEnabledKey = new NamespacedKey(plugin, "nightvision_enabled");
        this.glowEnabledKey = new NamespacedKey(plugin, "glow_enabled");
        this.permissionTask = plugin.getServer().getScheduler()
            .runTaskTimer(plugin, this::reconcileOnlinePlayers, 40L, 40L);
    }

    public boolean isFlyEnabled(Player player) {
        return isMarked(player, flyEnabledKey);
    }

    public void enableFly(Player player) {
        if (isFlyEnabled(player)) {
            player.setAllowFlight(true);
            player.setFlying(true);
            return;
        }

        PersistentDataContainer data = player.getPersistentDataContainer();
        setBoolean(data, flyPreviousAllowKey, player.getAllowFlight());
        setBoolean(data, flyPreviousFlyingKey, player.isFlying());
        setBoolean(data, flyEnabledKey, true);
        player.setAllowFlight(true);
        player.setFlying(true);
    }

    public void disableFly(Player player) {
        PersistentDataContainer data = player.getPersistentDataContainer();
        if (!isMarked(data, flyEnabledKey)) {
            return;
        }

        boolean previousAllow = getBoolean(data, flyPreviousAllowKey);
        boolean previousFlying = getBoolean(data, flyPreviousFlyingKey);
        clearFlyMarkers(data);

        boolean allowFlight = previousAllow || hasGameModeFlight(player);
        player.setFlying(previousFlying && allowFlight);
        player.setAllowFlight(allowFlight);
    }

    public boolean isNightVisionEnabled(Player player) {
        return isMarked(player, nightVisionEnabledKey);
    }

    public void enableNightVision(Player player) {
        if (!player.hasPotionEffect(PotionEffectType.NIGHT_VISION)) {
            player.addPotionEffect(new PotionEffect(
                PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, false, false, false));
            setBoolean(player.getPersistentDataContainer(), nightVisionEnabledKey, true);
        }
    }

    public void disableNightVision(Player player) {
        if (!isNightVisionEnabled(player)) {
            return;
        }
        player.removePotionEffect(PotionEffectType.NIGHT_VISION);
        player.getPersistentDataContainer().remove(nightVisionEnabledKey);
    }

    public boolean isGlowEnabled(Player player) {
        return isMarked(player, glowEnabledKey);
    }

    public void enableGlow(Player player) {
        if (!player.isGlowing()) {
            player.setGlowing(true);
            setBoolean(player.getPersistentDataContainer(), glowEnabledKey, true);
        }
    }

    public void disableGlow(Player player) {
        if (!isGlowEnabled(player)) {
            return;
        }
        player.setGlowing(false);
        player.getPersistentDataContainer().remove(glowEnabledKey);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.getServer().getScheduler().runTask(plugin, () -> reconcilePlayer(event.getPlayer(), false));
    }

    public void shutdown() {
        permissionTask.cancel();
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            releaseOwnedStates(player, false);
        }
    }

    private void reconcileOnlinePlayers() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            reconcilePlayer(player, true);
        }
    }

    private void reconcilePlayer(Player player, boolean notify) {
        if (isFlyEnabled(player)) {
            if (!player.hasPermission(FLY_PERMISSION)) {
                disableFly(player);
                notifyPermissionRemoval(player, "messages.fly-permission-removed", notify);
            } else if (!player.getAllowFlight()) {
                player.setAllowFlight(true);
            }
        }

        if (isNightVisionEnabled(player)) {
            if (!player.hasPermission(NIGHT_VISION_PERMISSION)) {
                disableNightVision(player);
                notifyPermissionRemoval(player, "messages.nightvision-permission-removed", notify);
            } else if (!player.hasPotionEffect(PotionEffectType.NIGHT_VISION)) {
                player.addPotionEffect(new PotionEffect(
                    PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, false, false, false));
            }
        }

        if (isGlowEnabled(player)) {
            if (!player.hasPermission(GLOW_PERMISSION)) {
                disableGlow(player);
                notifyPermissionRemoval(player, "messages.glow-permission-removed", notify);
            } else if (!player.isGlowing()) {
                player.setGlowing(true);
            }
        }
    }

    private void releaseOwnedStates(Player player, boolean notify) {
        if (isFlyEnabled(player)) {
            disableFly(player);
            notifyPermissionRemoval(player, "messages.fly-permission-removed", notify);
        }
        if (isNightVisionEnabled(player)) {
            disableNightVision(player);
            notifyPermissionRemoval(player, "messages.nightvision-permission-removed", notify);
        }
        if (isGlowEnabled(player)) {
            disableGlow(player);
            notifyPermissionRemoval(player, "messages.glow-permission-removed", notify);
        }
    }

    private void notifyPermissionRemoval(Player player, String path, boolean notify) {
        if (notify) {
            player.sendMessage(EssentialsC.getLangManager().getPrefixedString(path));
        }
    }

    private boolean isMarked(Player player, NamespacedKey key) {
        return isMarked(player.getPersistentDataContainer(), key);
    }

    private boolean isMarked(PersistentDataContainer data, NamespacedKey key) {
        Byte value = data.get(key, PersistentDataType.BYTE);
        return value != null && value == (byte) 1;
    }

    private boolean getBoolean(PersistentDataContainer data, NamespacedKey key) {
        return isMarked(data, key);
    }

    private void setBoolean(PersistentDataContainer data, NamespacedKey key, boolean value) {
        data.set(key, PersistentDataType.BYTE, value ? (byte) 1 : (byte) 0);
    }

    private void clearFlyMarkers(PersistentDataContainer data) {
        data.remove(flyEnabledKey);
        data.remove(flyPreviousAllowKey);
        data.remove(flyPreviousFlyingKey);
    }

    private boolean hasGameModeFlight(Player player) {
        return player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR;
    }
}
