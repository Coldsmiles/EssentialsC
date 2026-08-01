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

        TeleportRequestManager.ToggleIgnoreResult result = manager.toggleIgnoringRequests(player);
        String messagePath = switch (result) {
            case ENABLED -> "tpa.messages.ignore-enabled";
            case DISABLED -> "tpa.messages.ignore-disabled";
            case SAVE_FAILED -> "tpa.messages.ignore-save-failed";
        };
        player.sendMessage(getLang().getPrefixedComponent(messagePath));
        return true;
    }
}
