package cn.infstar.essentialsC.commands;

import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.persistence.PersistentDataType;

public class NightVisionCommand extends BaseCommand {

    private final NamespacedKey enabledKey;

    public NightVisionCommand() {
        super("essentialsc.command.nightvision");
        this.enabledKey = new NamespacedKey(plugin, "nightvision_enabled");
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
            player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, false, false, false));
            player.getPersistentDataContainer().set(enabledKey, PersistentDataType.BYTE, (byte) 1);
            playShortcutSound(player, Sound.BLOCK_BEACON_POWER_SELECT);
            player.sendMessage(getLang().getPrefixedString("messages.nightvision-enabled"));
        } else {
            if (currentState) {
                player.removePotionEffect(PotionEffectType.NIGHT_VISION);
                player.getPersistentDataContainer().remove(enabledKey);
            }
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
        Byte value = player.getPersistentDataContainer().get(enabledKey, PersistentDataType.BYTE);
        return value != null && value == (byte) 1;
    }
}
