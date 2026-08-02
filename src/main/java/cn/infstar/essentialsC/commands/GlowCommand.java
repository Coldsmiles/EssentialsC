package cn.infstar.essentialsC.commands;

import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class GlowCommand extends BaseCommand {

    public GlowCommand() {
        super("essentialsc.command.glow");
    }

    @Override
    protected boolean execute(Player player, String[] args) {
        boolean currentState = isPluginGlowEnabled(player);
        Boolean targetState = resolveTargetState(currentState, args);
        if (targetState == null) {
            player.sendMessage(getLang().getPrefixedString("messages.glow-usage"));
            return true;
        }

        if (targetState) {
            plugin.getPlayerStateManager().enableGlow(player);
            playShortcutSound(player, Sound.BLOCK_AMETHYST_BLOCK_CHIME);
            player.sendMessage(getLang().getPrefixedString("messages.glow-enabled"));
        } else {
            plugin.getPlayerStateManager().disableGlow(player);
            playShortcutSound(player, Sound.BLOCK_AMETHYST_CLUSTER_FALL);
            player.sendMessage(getLang().getPrefixedString("messages.glow-disabled"));
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

    private boolean isPluginGlowEnabled(Player player) {
        return plugin.getPlayerStateManager().isGlowEnabled(player);
    }
}
