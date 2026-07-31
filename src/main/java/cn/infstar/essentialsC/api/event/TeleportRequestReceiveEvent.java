package cn.infstar.essentialsC.api.event;

import cn.infstar.essentialsC.teleport.TeleportRequestManager.TeleportRequest;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class TeleportRequestReceiveEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player recipient;
    private final TeleportRequest request;
    private boolean cancelled;

    public TeleportRequestReceiveEvent(Player recipient, TeleportRequest request) {
        this.recipient = recipient;
        this.request = request;
    }

    public Player getRecipient() {
        return recipient;
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
