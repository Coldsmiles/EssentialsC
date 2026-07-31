package cn.infstar.essentialsC.api.event;

import cn.infstar.essentialsC.teleport.TeleportRequestManager.TeleportRequest;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class TeleportEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final TeleportRequest request;
    private Location destination;
    private boolean cancelled;

    public TeleportEvent(Player player, TeleportRequest request, Location destination) {
        this.player = player;
        this.request = request;
        this.destination = destination.clone();
    }

    public Player getPlayer() {
        return player;
    }

    public TeleportRequest getRequest() {
        return request;
    }

    public Location getDestination() {
        return destination.clone();
    }

    public void setDestination(Location destination) {
        this.destination = destination.clone();
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
