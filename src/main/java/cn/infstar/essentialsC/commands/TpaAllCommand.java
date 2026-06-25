package cn.infstar.essentialsC.commands;

import cn.infstar.essentialsC.teleport.TeleportRequestManager;
import org.bukkit.entity.Player;

import java.util.Map;

public final class TpaAllCommand extends BaseCommand {

    public TpaAllCommand() {
        super("essentialsc.command.tpaall");
    }

    @Override
    protected boolean execute(Player player, String[] args) {
        if (args.length != 0) {
            player.sendMessage(getLang().getPrefixedString("tpa.messages.usage-tpaall"));
            return true;
        }

        TeleportRequestManager manager = plugin.getTeleportRequestManager();
        if (manager == null) {
            player.sendMessage(getLang().getPrefixedString("messages.module-disabled"));
            return true;
        }
        if (manager.isIgnoringRequests(player)) {
            player.sendMessage(getLang().getPrefixedString("tpa.messages.ignoring-requests"));
            return true;
        }

        int sent = manager.sendTeleportAllRequest(player, target -> !VanishCommand.isVanished(target));
        if (sent < 0) {
            player.sendMessage(getLang().getPrefixedString("tpa.messages.send-cooldown",
                Map.of("seconds", String.valueOf(manager.getSendCooldownSeconds(player)))));
            return true;
        }
        if (sent <= 0) {
            player.sendMessage(getLang().getPrefixedString("tpa.messages.tpaall-no-targets"));
            return true;
        }

        player.sendMessage(getLang().getPrefixedString("tpa.messages.tpaall-sent"));
        return true;
    }
}
