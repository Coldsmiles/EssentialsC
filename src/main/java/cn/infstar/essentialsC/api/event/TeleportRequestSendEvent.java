package cn.infstar.essentialsC.api.event;

import cn.infstar.essentialsC.teleport.TeleportRequestManager.TeleportRequest;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class TeleportRequestSendEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player sender;
    private final TeleportRequest request;
    private boolean cancelled;

    public TeleportRequestSendEvent(Player sender, TeleportRequest request) {
        this.sender = sender;
        this.request = request;
    }

    public Player getSender() {
        return sender;
    }

    public TeleportRequest getRequest() {
        return request;
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
