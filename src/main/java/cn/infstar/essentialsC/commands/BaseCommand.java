package cn.infstar.essentialsC.commands;

import cn.infstar.essentialsC.EssentialsC;
import cn.infstar.essentialsC.LangManager;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundGroup;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

public abstract class BaseCommand implements CommandExecutor {

    protected String permission;
    protected static EssentialsC plugin;

    public BaseCommand(String permission) {
        this.permission = permission;
        if (plugin == null) {
            plugin = EssentialsC.getPlugin(EssentialsC.class);
        }
    }

    public String getPermission() {
        return permission;
    }

    protected LangManager getLang() {
        return EssentialsC.getLangManager();
    }

    protected void playBlockShortcutSound(Player player, Material material, Sound fallbackSound) {
        Sound sound = resolvePlaceSound(material);
        if (sound == null) {
            sound = fallbackSound;
        }

        if (sound != null) {
            player.playSound(player.getLocation(), sound, 1.0F, 1.0F);
        }
    }

    protected void playShortcutSound(Player player, Sound sound) {
        if (sound != null) {
            player.playSound(player.getLocation(), sound, 1.0F, 1.0F);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player player) {
            if (!player.hasPermission(permission)) {
                player.sendMessage(getLang().getPrefixedString("messages.no-permission",
                    Map.of("permission", permission)));
                return true;
            }
            return execute(player, args);
        }

        return executeConsole(sender, args);
    }

    protected abstract boolean execute(Player player, String[] args);

    protected boolean executeConsole(CommandSender sender, String[] args) {
        sender.sendMessage(getLang().getPrefixedString("messages.player-only"));
        return true;
    }

    private Sound resolvePlaceSound(Material material) {
        if (material == null || !material.isBlock()) {
            return null;
        }

        try {
            SoundGroup soundGroup = material.createBlockData().getSoundGroup();
            return soundGroup.getPlaceSound();
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
