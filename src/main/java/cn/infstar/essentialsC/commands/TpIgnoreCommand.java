package cn.infstar.essentialsC.commands;

import cn.infstar.essentialsC.teleport.TeleportRequestManager;
import org.bukkit.entity.Player;

public final class TpIgnoreCommand extends BaseCommand {

    public TpIgnoreCommand() {
        super("essentialsc.command.tpignore");
    }

    @Override
    protected boolean execute(Player player, String[] args) {
        TeleportRequestManager manager = plugin.getTeleportRequestManager();
        if (manager == null) {
            player.sendMessage(getLang().getPrefixedComponent("messages.module-disabled"));
            return true;
        }

        boolean ignoring = manager.toggleIgnoringRequests(player);
        player.sendMessage(getLang().getPrefixedComponent(ignoring
            ? "tpa.messages.ignore-enabled"
            : "tpa.messages.ignore-disabled"));
        return true;
    }
}
