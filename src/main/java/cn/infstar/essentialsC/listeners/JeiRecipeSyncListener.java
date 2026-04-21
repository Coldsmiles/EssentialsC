package cn.infstar.essentialsC.listeners;

import cn.infstar.essentialsC.EssentialsC;
import io.netty.buffer.Unpooled;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.DiscardedPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.item.crafting.RecipeSerializer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * JEI 配方同步监听器
 * 解决 Minecraft 1.21.2+ 配方不同步问题
 * 支持 Fabric 和 NeoForge 客户端
 */
public class JeiRecipeSyncListener implements Listener {
    
    private final EssentialsC plugin;
    private final boolean enabled;
    private final boolean debug;
    private final boolean sendPlayerMessage;
    
    public JeiRecipeSyncListener(EssentialsC plugin) {
        this.plugin = plugin;
        
        FileConfiguration config = plugin.getConfig();
        this.enabled = config.getBoolean("jei-sync.enabled", true);
        this.debug = config.getBoolean("jei-sync.debug", false);
        this.sendPlayerMessage = config.getBoolean("jei-sync.send-player-message", true);
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!enabled) {
            return;
        }
        
        Player player = event.getPlayer();
        String clientBrand = player.getClientBrandName();
        
        if (debug) {
            plugin.getLogger().info("========================================");
            plugin.getLogger().info("玩家 " + player.getName() + " 加入");
            plugin.getLogger().info("客户端品牌: '" + (clientBrand != null ? clientBrand : "null") + "'");
            plugin.getLogger().info("JEI 同步功能: " + (enabled ? "启用" : "禁用"));
            plugin.getLogger().info("========================================");
        }
        
        if (clientBrand == null || clientBrand.isEmpty()) {
            if (debug) {
                plugin.getLogger().info("跳过 " + player.getName() + "：客户端品牌为空");
            }
            return;
        }
        
        // 统一转换为小写进行比较，支持更多变体
        String brandLower = clientBrand.toLowerCase();
        
        if (brandLower.contains("fabric")) {
            if (debug) {
                plugin.getLogger().info("检测到 Fabric 客户端，开始发送配方同步...");
            }
            sendPlayerMessage(player, "Fabric");
            sendFabricRecipeSync(player);
        } else if (brandLower.contains("neoforge") || brandLower.contains("forge")) {
            if (debug) {
                plugin.getLogger().info("检测到 NeoForge/Forge 客户端，开始发送配方同步...");
            }
            sendPlayerMessage(player, "NeoForge");
            sendNeoForgeRecipeSync(player);
        } else {
            if (debug) {
                plugin.getLogger().info("跳过 " + player.getName() + "：不支持的客户端类型 '" + clientBrand + "'");
            }
        }
    }
    
    /**
     * 发送提示消息给玩家
     */
    private void sendPlayerMessage(Player player, String clientType) {
        if (!sendPlayerMessage) {
            return;
        }
        
        String messageKey;
        if (clientType.equalsIgnoreCase("fabric")) {
            messageKey = "messages.jei-sync-fabric";
        } else if (clientType.equalsIgnoreCase("neoforge")) {
            messageKey = "messages.jei-sync-neoforge";
        } else {
            return;
        }
        
        // 使用统一前缀 + 消息内容
        String prefix = EssentialsC.getLangManager().getString("prefix");
        String message = EssentialsC.getLangManager().getString(messageKey);
        String fullMessage = prefix + " " + message;
        
        net.kyori.adventure.text.Component component = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand().deserialize(fullMessage);
        player.sendMessage(component);
    }
    
    /**
     * 发送 Fabric 格式的配方同步数据包
     */
    @SuppressWarnings({"unchecked", "deprecation"})
    private void sendFabricRecipeSync(Player player) {
        try {
            ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
            MinecraftServer server = serverPlayer.level().getServer();
            if (server == null) {
                if (debug) plugin.getLogger().warning("服务器实例为 null");
                return;
            }
            
            RecipeMap recipeMap = server.getRecipeManager().recipes;
            
            if (debug) {
                plugin.getLogger().info("开始构建 Fabric 配方数据");
            }
            
            // 创建 Fabric Payload（与参考插件完全一致）
            var list = new ArrayList<FabricRecipeEntry>();
            var seen = new HashSet<RecipeSerializer<?>>();
            
            for (RecipeSerializer<?> serializer : BuiltInRegistries.RECIPE_SERIALIZER) {
                if (!seen.add(serializer)) continue;
                
                List<RecipeHolder<?>> recipes = new ArrayList<>();
                for (RecipeHolder<?> holder : recipeMap.values()) {
                    if (holder.value().getSerializer() == serializer) {
                        recipes.add(holder);
                    }
                }
                
                if (!recipes.isEmpty()) {
                    RecipeSerializer<?> entrySerializer = recipes.get(0).value().getSerializer();
                    list.add(new FabricRecipeEntry(entrySerializer, recipes));
                }
            }
            
            var payload = new FabricRecipeSyncPayload(list);
            
            if (debug) {
                plugin.getLogger().info("Fabric 配方条目数: " + list.size());
            }
            
            // 构建 buffer
            RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(
                Unpooled.buffer(), 
                server.registryAccess()
            );
            
            // 使用 CODEC 编码（与参考插件完全一致）
            var codec = getFabricCodec();
            codec.encode(buffer, payload);
            
            // 发送数据包
            byte[] bytes = new byte[buffer.writerIndex()];
            buffer.getBytes(0, bytes);
            
            Identifier id = Identifier.fromNamespaceAndPath("fabric", "recipe_sync");
            DiscardedPayload discardedPayload = new DiscardedPayload(id, bytes);
            serverPlayer.connection.send(new ClientboundCustomPayloadPacket(discardedPayload));
            
            if (debug) {
                plugin.getLogger().info("已发送 Fabric 配方同步 [" + id + "], 大小: " + bytes.length + " bytes");
            }
            
        } catch (Exception e) {
            plugin.getLogger().warning("发送 Fabric 配方同步失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 获取 Fabric Codec
     */
    @SuppressWarnings({"unchecked", "deprecation"})
    private net.minecraft.network.codec.StreamCodec<RegistryFriendlyByteBuf, FabricRecipeSyncPayload> getFabricCodec() {
        return FabricRecipeEntry.CODEC.apply(net.minecraft.network.codec.ByteBufCodecs.list())
            .map(FabricRecipeSyncPayload::new, FabricRecipeSyncPayload::entries);
    }
    
    /**
     * 发送 NeoForge 格式的配方同步数据包
     */
    @SuppressWarnings({"unchecked", "deprecation"})
    private void sendNeoForgeRecipeSync(Player player) {
        try {
            ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
            MinecraftServer server = serverPlayer.level().getServer();
            if (server == null) {
                if (debug) plugin.getLogger().warning("服务器实例为 null");
                return;
            }
            
            RecipeMap recipeMap = server.getRecipeManager().recipes;
            
            if (debug) {
                plugin.getLogger().info("开始构建 NeoForge 配方数据");
            }
            
            // 获取所有配方类型
            java.util.List<net.minecraft.world.item.crafting.RecipeType<?>> allRecipeTypes = 
                BuiltInRegistries.RECIPE_TYPE.stream().toList();
            
            if (debug) {
                plugin.getLogger().info("NeoForge 配方类型数: " + allRecipeTypes.size());
            }
            
            // 创建 NeoForge Payload（与参考插件完全一致）
            var payload = createNeoForgePayload(allRecipeTypes, recipeMap);
            
            // 构建 buffer
            RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(
                Unpooled.buffer(), 
                server.registryAccess()
            );
            
            // 使用 STREAM_CODEC 编码（与参考插件完全一致）
            var streamCodec = getNeoForgeStreamCodec();
            streamCodec.encode(buffer, payload);
            
            // 发送数据包
            byte[] bytes = new byte[buffer.writerIndex()];
            buffer.getBytes(0, bytes);
            
            Identifier id = Identifier.fromNamespaceAndPath("neoforge", "recipe_content");
            DiscardedPayload discardedPayload = new DiscardedPayload(id, bytes);
            serverPlayer.connection.send(new ClientboundCustomPayloadPacket(discardedPayload));
            
            // 发送 Tags 同步（NeoForge 需要）
            serverPlayer.connection.send(new net.minecraft.network.protocol.common.ClientboundUpdateTagsPacket(
                net.minecraft.tags.TagNetworkSerialization.serializeTagsToNetwork(server.registries())
            ));
            
            if (debug) {
                plugin.getLogger().info("已发送 NeoForge 配方同步 [" + id + "], 大小: " + bytes.length + " bytes");
            }
            
        } catch (Exception e) {
            plugin.getLogger().warning("发送 NeoForge 配方同步失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 创建 NeoForge 配载对象
     */
    private NeoForgeRecipeSyncPayload createNeoForgePayload(
            java.util.List<net.minecraft.world.item.crafting.RecipeType<?>> recipeTypes,
            RecipeMap recipeMap) {
        var recipeTypeSet = new java.util.HashSet<>(recipeTypes);
        
        if (recipeTypeSet.isEmpty()) {
            return new NeoForgeRecipeSyncPayload(recipeTypeSet, java.util.List.of());
        } else {
            var recipeSubset = recipeMap.values().stream()
                .filter(h -> recipeTypeSet.contains(h.value().getType()))
                .toList();
            return new NeoForgeRecipeSyncPayload(recipeTypeSet, recipeSubset);
        }
    }
    
    /**
     * 获取 NeoForge StreamCodec
     */
    @SuppressWarnings({"unchecked", "deprecation"})
    private net.minecraft.network.codec.StreamCodec<RegistryFriendlyByteBuf, NeoForgeRecipeSyncPayload> getNeoForgeStreamCodec() {
        return net.minecraft.network.codec.StreamCodec.composite(
            net.minecraft.network.codec.ByteBufCodecs.registry(net.minecraft.core.registries.Registries.RECIPE_TYPE)
                .apply(net.minecraft.network.codec.ByteBufCodecs.collection(java.util.HashSet::new)),
            NeoForgeRecipeSyncPayload::recipeTypes,
            RecipeHolder.STREAM_CODEC.apply(net.minecraft.network.codec.ByteBufCodecs.list()),
            NeoForgeRecipeSyncPayload::recipes,
            NeoForgeRecipeSyncPayload::new
        );
    }
    
    /**
     * Fabric 配方条目
     */
    @SuppressWarnings("deprecation")
    private static class FabricRecipeEntry {
        final Object serializer;  // 使用 Object 避免 NMS 类型不兼容
        final List<RecipeHolder<?>> recipes;
        
        FabricRecipeEntry(Object serializer, List<RecipeHolder<?>> recipes) {
            this.serializer = serializer;
            this.recipes = recipes;
        }
        
        static final net.minecraft.network.codec.StreamCodec<RegistryFriendlyByteBuf, FabricRecipeEntry> CODEC = 
            net.minecraft.network.codec.StreamCodec.ofMember(
                FabricRecipeEntry::write,
                FabricRecipeEntry::read
            );
        
        @SuppressWarnings("unchecked")
        private static FabricRecipeEntry read(RegistryFriendlyByteBuf buf) {
            Identifier recipeSerializerId = buf.readIdentifier();
            RecipeSerializer<?> recipeSerializer = BuiltInRegistries.RECIPE_SERIALIZER.getValue(recipeSerializerId);
            
            if (recipeSerializer == null) {
                throw new RuntimeException("Tried syncing unsupported packet serializer '" + recipeSerializerId + "'!");
            }
            
            int count = buf.readVarInt();
            var list = new ArrayList<RecipeHolder<?>>();
            
            for (int i = 0; i < count; i++) {
                net.minecraft.resources.ResourceKey<net.minecraft.world.item.crafting.Recipe<?>> id = 
                    buf.readResourceKey(net.minecraft.core.registries.Registries.RECIPE);
                
                // 使用反射获取 streamCodec，避免 NMS 类型不兼容
                try {
                    var streamCodecMethod = recipeSerializer.getClass().getMethod("streamCodec");
                    var streamCodec = streamCodecMethod.invoke(recipeSerializer);
                    net.minecraft.world.item.crafting.Recipe<?> recipe = 
                        ((net.minecraft.network.codec.StreamCodec<RegistryFriendlyByteBuf, net.minecraft.world.item.crafting.Recipe<?>>) streamCodec)
                            .decode(buf);
                    list.add(new RecipeHolder<>(id, recipe));
                } catch (Exception e) {
                    throw new RuntimeException("Failed to decode recipe: " + e.getMessage(), e);
                }
            }
            
            return new FabricRecipeEntry(recipeSerializer, list);
        }
        
        private void write(RegistryFriendlyByteBuf buf) {
            // 使用反射获取 key，避免 NMS 类型不兼容
            try {
                var getKeyMethod = BuiltInRegistries.RECIPE_SERIALIZER.getClass().getMethod("getKey", Object.class);
                Identifier identifier = (Identifier) getKeyMethod.invoke(BuiltInRegistries.RECIPE_SERIALIZER, this.serializer);
                buf.writeIdentifier(identifier);
            } catch (Exception e) {
                throw new RuntimeException("Failed to get serializer key: " + e.getMessage(), e);
            }
            
            buf.writeVarInt(this.recipes.size());
            
            // 使用反射获取 streamCodec，避免 NMS 类型不兼容
            try {
                var streamCodecMethod = this.serializer.getClass().getMethod("streamCodec");
                @SuppressWarnings("unchecked")
                var codec = (net.minecraft.network.codec.StreamCodec<RegistryFriendlyByteBuf, net.minecraft.world.item.crafting.Recipe<?>>) 
                    streamCodecMethod.invoke(this.serializer);
                
                for (RecipeHolder<?> recipe : this.recipes) {
                    buf.writeResourceKey(recipe.id());
                    codec.encode(buf, recipe.value());
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to encode recipe: " + e.getMessage(), e);
            }
        }
    }
    
    /**
     * Fabric 配方同步 Payload
     */
    private record FabricRecipeSyncPayload(List<FabricRecipeEntry> entries) {
    }
    
    /**
     * NeoForge 配方同步 Payload
     */
    private record NeoForgeRecipeSyncPayload(
            java.util.Set<net.minecraft.world.item.crafting.RecipeType<?>> recipeTypes,
            java.util.List<RecipeHolder<?>> recipes) {
    }
}
