package cn.infstar.essentialsC.skinbridge;

import org.bukkit.entity.Player;

interface SkinBridgeGateway {

    GeneratedSkin generateSkin(String skinUrl, SkinModel model) throws Exception;

    void applySkin(Player player, GeneratedSkin skin) throws Exception;
}

enum SkinModel {
    CLASSIC,
    SLIM
}

record GeneratedSkin(String value, String signature) {
}
