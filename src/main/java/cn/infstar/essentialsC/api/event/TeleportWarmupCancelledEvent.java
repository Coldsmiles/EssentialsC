package cn.infstar.essentialsC.api.event;

import cn.infstar.essentialsC.teleport.TeleportRequestManager.TeleportRequest;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class TeleportWarmupCancelledEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final TeleportRequest request;
    private final int durationSeconds;
    private final int elapsedSeconds;
    private final Reason reason;

    public TeleportWarmupCancelledEvent(Player player, TeleportRequest request, int durationSeconds,
                                        int elapsedSeconds, Reason reason) {
        this.player = player;
        this.request = request;
        this.durationSeconds = durationSeconds;
        this.elapsedSeconds = elapsedSeconds;
        this.reason = reason;
    }

    public Player getPlayer() {
        return player;
    }

    public TeleportRequest getRequest() {
        return request;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public int getElapsedSeconds() {
        return elapsedSeconds;
    }

    public Reason getReason() {
        return reason;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    public enum Reason {
        PLAYER_MOVE,
        PLAYER_DAMAGE,
        PLAYER_QUIT,
        PLUGIN_SHUTDOWN
    }
}
