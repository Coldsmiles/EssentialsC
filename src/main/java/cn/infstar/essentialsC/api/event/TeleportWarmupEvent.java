package cn.infstar.essentialsC.api.event;

import cn.infstar.essentialsC.teleport.TeleportRequestManager.TeleportRequest;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class TeleportWarmupEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final TeleportRequest request;
    private final int durationSeconds;
    private boolean cancelled;

    public TeleportWarmupEvent(Player player, TeleportRequest request, int durationSeconds) {
        this.player = player;
        this.request = request;
        this.durationSeconds = durationSeconds;
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

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
