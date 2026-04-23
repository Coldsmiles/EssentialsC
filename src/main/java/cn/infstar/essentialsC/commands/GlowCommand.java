package cn.infstar.essentialsC.commands;

import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

public class GlowCommand extends BaseCommand {

    private final NamespacedKey enabledKey;

    public GlowCommand() {
        super("essentialsc.command.glow");
        this.enabledKey = new NamespacedKey(plugin, "glow_enabled");
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
            player.setGlowing(true);
            player.getPersistentDataContainer().set(enabledKey, PersistentDataType.BYTE, (byte) 1);
            playShortcutSound(player, Sound.BLOCK_AMETHYST_BLOCK_CHIME);
            player.sendMessage(getLang().getPrefixedString("messages.glow-enabled"));
        } else {
            if (currentState) {
                player.setGlowing(false);
                player.getPersistentDataContainer().remove(enabledKey);
            }
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
        Byte value = player.getPersistentDataContainer().get(enabledKey, PersistentDataType.BYTE);
        return value != null && value == (byte) 1;
    }
}
