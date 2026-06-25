package cn.infstar.essentialsC.commands;

import cn.infstar.essentialsC.teleport.TeleportRequestManager;
import org.bukkit.entity.Player;

public final class TpaHereCommand extends TpaCommand {

    public TpaHereCommand() {
        super();
        this.permission = "essentialsc.command.tpahere";
    }

    @Override
    protected boolean execute(Player player, String[] args) {
        return sendRequest(player, args, TeleportRequestManager.TeleportRequest.Type.TPAHERE);
    }
}
