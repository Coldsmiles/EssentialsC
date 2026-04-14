package cn.infstar.essentialsC.listeners;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 使用 ProtocolLib 修改 Inventory 标题
 * 通过拦截 OPEN_WINDOW 数据包实现自定义标题
 */
public class InventoryTitleListener extends PacketAdapter {
    
    // 存储需要修改标题的玩家和对应的新标题
    private final Map<UUID, String> pendingTitleChanges = new HashMap<>();
    
    public InventoryTitleListener(Plugin plugin) {
        super(plugin, ListenerPriority.NORMAL, PacketType.Play.Server.OPEN_WINDOW);
    }
    
    /**
     * 标记玩家需要修改下一个打开的 inventory 标题
     * @param player 玩家
     * @param title 新标题（支持颜色代码）
     */
    public void markForTitleChange(Player player, String title) {
        pendingTitleChanges.put(player.getUniqueId(), title);
    }
    
    @Override
    public void onPacketSending(PacketEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        // 检查该玩家是否有待处理的标题修改
        String newTitle = pendingTitleChanges.remove(playerId);
        if (newTitle == null) {
            return;
        }
        
        try {
            PacketContainer packet = event.getPacket();
            
            // Paper 1.21+ 使用 WrappedChatComponent 作为标题
            // 将颜色代码 & 转换为 §
            String formattedTitle = newTitle.replace('&', '§');
            
            // 创建聊天组件
            WrappedChatComponent titleComponent = WrappedChatComponent.fromText(formattedTitle);
            
            // 修改数据包中的标题字段
            // 在 1.21+ 中，标题是第二个字段（索引1）
            packet.getChatComponents().write(0, titleComponent);
            
        } catch (Exception e) {
            // 如果修改失败，记录错误但不影响正常流程
            Bukkit.getLogger().warning("[EssentialsC] 修改 inventory 标题失败: " + e.getMessage());
        }
    }
    
    /**
     * 清理所有待处理的标题修改（防止内存泄漏）
     */
    public void cleanup() {
        pendingTitleChanges.clear();
    }
}
