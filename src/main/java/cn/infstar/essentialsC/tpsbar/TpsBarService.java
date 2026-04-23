package cn.infstar.essentialsC.tpsbar;

import org.bukkit.entity.Player;

import java.util.Collection;

public interface TpsBarService {

    boolean isPluginCommandEnabled();

    boolean isNativeCommandAvailable();

    void reloadSettings();

    void shutdown();

    boolean toggle(Player target);

    void sendToggleMessage(Player actor, Player target, boolean enabled);

    String getUsageMessage();

    String getPlayerNotFoundMessage(String input);

    String getNoTargetsMessage();

    Collection<Player> resolveTargets(Player sender, String input);
}
