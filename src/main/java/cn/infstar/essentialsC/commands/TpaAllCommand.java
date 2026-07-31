package cn.infstar.essentialsC.commands;

import cn.infstar.essentialsC.teleport.TeleportRequestManager;
import org.bukkit.entity.Player;

public final class TpaAllCommand extends BaseCommand {

    public TpaAllCommand() {
        super("essentialsc.command.tpaall");
    }

    @Override
    protected boolean execute(Player player, String[] args) {
        if (args.length != 0) {
            player.sendMessage(getLang().getPrefixedComponent("tpa.messages.usage-tpaall"));
            return true;
        }

        TeleportRequestManager manager = plugin.getTeleportRequestManager();
        if (manager == null) {
            player.sendMessage(getLang().getPrefixedComponent("messages.module-disabled"));
            return true;
        }
        if (manager.isIgnoringRequests(player)) {
            player.sendMessage(getLang().getPrefixedComponent("tpa.messages.ignoring-requests"));
            return true;
        }

        int sent = manager.sendTeleportAllRequest(player);
        if (sent <= 0) {
            player.sendMessage(getLang().getPrefixedComponent("tpa.messages.tpaall-no-targets"));
            return true;
        }

        player.sendMessage(getLang().getPrefixedComponent("tpa.messages.tpaall-sent"));
        return true;
    }
}
