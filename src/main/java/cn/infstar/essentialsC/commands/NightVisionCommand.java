package cn.infstar.essentialsC.commands;

import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class NightVisionCommand extends BaseCommand {

    public NightVisionCommand() {
        super("essentialsc.command.nightvision");
    }

    @Override
    protected boolean execute(Player player, String[] args) {
        boolean currentState = isPluginNightVisionEnabled(player);
        Boolean targetState = resolveTargetState(currentState, args);
        if (targetState == null) {
            player.sendMessage(getLang().getPrefixedString("messages.nightvision-usage"));
            return true;
        }

        if (targetState) {
            plugin.getPlayerStateManager().enableNightVision(player);
            playShortcutSound(player, Sound.BLOCK_BEACON_POWER_SELECT);
            player.sendMessage(getLang().getPrefixedString("messages.nightvision-enabled"));
        } else {
            plugin.getPlayerStateManager().disableNightVision(player);
            playShortcutSound(player, Sound.BLOCK_BEACON_DEACTIVATE);
            player.sendMessage(getLang().getPrefixedString("messages.nightvision-disabled"));
        }
        return true;
    }

    private Boolean resolveTargetState(boolean currentState, String[] args) {
        if (args.length == 0) {
            return !currentState;
        }

        return switch (args[0].toLowerCase()) {
            case "on", "true", "enable", "enabled" -> true;
            case "off", "false", "disable", "disabled" -> false;
            case "toggle" -> !currentState;
            default -> null;
        };
    }

    private boolean isPluginNightVisionEnabled(Player player) {
        return plugin.getPlayerStateManager().isNightVisionEnabled(player);
    }
}
