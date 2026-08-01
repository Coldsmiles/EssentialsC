package cn.infstar.essentialsC.compat.jei;

import org.bukkit.entity.Player;

import java.util.logging.Logger;

public interface JeiRecipeSyncAdapter {

    void sendFabricRecipeSync(Player player, Logger logger, boolean debug) throws Exception;

    void sendNeoForgeRecipeSync(Player player, Logger logger, boolean debug) throws Exception;

    void clearCache();
}
