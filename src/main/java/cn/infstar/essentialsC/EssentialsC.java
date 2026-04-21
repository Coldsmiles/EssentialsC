package cn.infstar.essentialsC;

import cn.infstar.essentialsC.commands.*;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;

public final class EssentialsC extends JavaPlugin {

    private static LangManager langManager;

    @Override
    public void onEnable() {
        langManager = new LangManager(this);
        registerPluginChannels();
        registerListeners();
        registerCommands();
        
        getLogger().info("插件已启用！版本: " + getDescription().getVersion());
    }

    @Override
    public void onDisable() {
        getLogger().info("EssentialsC 插件已禁用！");
    }
    
    public static LangManager getLangManager() {
        return langManager;
    }
    
    /**
     * 注册 JEI 配方同步所需的插件频道
     */
    private void registerPluginChannels() {
        org.bukkit.plugin.messaging.Messenger messenger = getServer().getMessenger();
        // 注册 Fabric 和 NeoForge 的配方同步频道
        messenger.registerOutgoingPluginChannel(this, "fabric:recipe_sync");
        messenger.registerOutgoingPluginChannel(this, "neoforge:recipe_content");
    }
    
    private void registerListeners() {
        if (registerListener("cn.infstar.essentialsC.listeners.ShulkerBoxListener")) {
            getLogger().info("- 潜影盒模块");
        }
        
        if (registerListener("cn.infstar.essentialsC.listeners.JeiRecipeSyncListener")) {
            getLogger().info("- JEI 配方同步");
        }
        
        if (registerListener("cn.infstar.essentialsC.listeners.MobDropListener")) {
            try {
                Class.forName("cn.infstar.essentialsC.listeners.MobDropMenuListener");
                new cn.infstar.essentialsC.listeners.MobDropMenuListener(this);
            } catch (ClassNotFoundException e) {
            }
            getLogger().info("- 生物掉落控制");
        }
    }
    
    private boolean registerListener(String className) {
        try {
            Class<?> listenerClass = Class.forName(className);
            Object listenerInstance = listenerClass.getConstructor(EssentialsC.class).newInstance(this);
            getServer().getPluginManager().registerEvents((org.bukkit.event.Listener) listenerInstance, this);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    private void registerCommands() {
        try {
            Field bukkitCommandMap = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            bukkitCommandMap.setAccessible(true);
            org.bukkit.command.CommandMap commandMap = (org.bukkit.command.CommandMap) bukkitCommandMap.get(Bukkit.getServer());
            
            int commandCount = 0;
            
            if (classExists("cn.infstar.essentialsC.commands.WorkbenchCommand")) {
                registerCommandWithAliases(commandMap, "workbench", new WorkbenchCommand(), "wb");
                commandCount++;
            }
            if (classExists("cn.infstar.essentialsC.commands.AnvilCommand")) {
                registerCommandWithAliases(commandMap, "anvil", new AnvilCommand());
                commandCount++;
            }
            if (classExists("cn.infstar.essentialsC.commands.CartographyTableCommand")) {
                registerCommandWithAliases(commandMap, "cartographytable", new CartographyTableCommand(), "ct", "cartography");
                commandCount++;
            }
            if (classExists("cn.infstar.essentialsC.commands.GrindstoneCommand")) {
                registerCommandWithAliases(commandMap, "grindstone", new GrindstoneCommand(), "gs");
                commandCount++;
            }
            if (classExists("cn.infstar.essentialsC.commands.LoomCommand")) {
                registerCommandWithAliases(commandMap, "loom", new LoomCommand());
                commandCount++;
            }
            if (classExists("cn.infstar.essentialsC.commands.SmithingTableCommand")) {
                registerCommandWithAliases(commandMap, "smithingtable", new SmithingTableCommand(), "st", "smithing");
                commandCount++;
            }
            if (classExists("cn.infstar.essentialsC.commands.StonecutterCommand")) {
                registerCommandWithAliases(commandMap, "stonecutter", new StonecutterCommand(), "sc");
                commandCount++;
            }
            if (classExists("cn.infstar.essentialsC.commands.EnderChestCommand")) {
                registerCommandWithAliases(commandMap, "enderchest", new EnderChestCommand(), "ec");
                commandCount++;
            }
            if (classExists("cn.infstar.essentialsC.commands.BlocksMenuCommand")) {
                registerCommandWithAliases(commandMap, "blocks", new BlocksMenuCommand());
                commandCount++;
            }
            
            if (classExists("cn.infstar.essentialsC.commands.FlyCommand")) {
                registerCommandWithAliases(commandMap, "fly", new FlyCommand());
                commandCount++;
            }
            if (classExists("cn.infstar.essentialsC.commands.HealCommand")) {
                registerCommandWithAliases(commandMap, "heal", new HealCommand());
                commandCount++;
            }
            if (classExists("cn.infstar.essentialsC.commands.FeedCommand")) {
                registerCommandWithAliases(commandMap, "feed", new FeedCommand());
                commandCount++;
            }
            if (classExists("cn.infstar.essentialsC.commands.VanishCommand")) {
                registerCommandWithAliases(commandMap, "vanish", new VanishCommand(), "v");
                commandCount++;
            }
            if (classExists("cn.infstar.essentialsC.commands.SeenCommand")) {
                registerCommandWithAliases(commandMap, "seen", new SeenCommand(), "info");
                commandCount++;
            }
            if (classExists("cn.infstar.essentialsC.commands.HatCommand")) {
                registerCommandWithAliases(commandMap, "hat", new HatCommand());
                commandCount++;
            }
            if (classExists("cn.infstar.essentialsC.commands.SuicideCommand")) {
                registerCommandWithAliases(commandMap, "suicide", new SuicideCommand(), "die");
                commandCount++;
            }
            if (classExists("cn.infstar.essentialsC.commands.RepairCommand")) {
                registerCommandWithAliases(commandMap, "repair", new RepairCommand(), "rep");
                commandCount++;
            }
            
            if (classExists("cn.infstar.essentialsC.commands.MobDropCommand")) {
                registerCommandWithAliases(commandMap, "mobdrops", new MobDropCommand());
                commandCount++;
            }
            
            registerCommandWithAliases(commandMap, "essentialsc", new HelpCommand(), "essc");
            commandCount++;
        } catch (Exception e) {
            getLogger().severe("无法注册命令: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private boolean classExists(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    /**
     * 注册命令并支持别名
     * @param commandMap Bukkit CommandMap
     * @param name 主命令名
     * @param executor 命令执行器
     * @param aliases 别名列表（可选）
     */
    private void registerCommandWithAliases(org.bukkit.command.CommandMap commandMap, String name, cn.infstar.essentialsC.commands.BaseCommand executor, String... aliases) {
        Command command = new Command(name) {
            @Override
            public boolean execute(CommandSender sender, String commandLabel, String[] args) {
                return executor.onCommand(sender, this, commandLabel, args);
            }
            
            @Override
            public java.util.List<String> tabComplete(CommandSender sender, String alias, String[] args) throws IllegalArgumentException {
                if (executor instanceof org.bukkit.command.TabCompleter) {
                    return ((org.bukkit.command.TabCompleter) executor).onTabComplete(sender, this, alias, args);
                }
                return super.tabComplete(sender, alias, args);
            }
        };
        
        command.setPermission(executor.getPermission());
        // 注册到默认命名空间，使玩家可以直接使用 /workbench 而不是 /essentialsc:workbench
        commandMap.register("", command);
        
        // 注册别名
        for (String alias : aliases) {
            Command aliasCmd = new Command(alias) {
                @Override
                public boolean execute(CommandSender sender, String commandLabel, String[] args) {
                    return executor.onCommand(sender, this, commandLabel, args);
                }
                
                @Override
                public java.util.List<String> tabComplete(CommandSender sender, String alias, String[] args) throws IllegalArgumentException {
                    if (executor instanceof org.bukkit.command.TabCompleter) {
                        return ((org.bukkit.command.TabCompleter) executor).onTabComplete(sender, this, alias, args);
                    }
                    return super.tabComplete(sender, alias, args);
                }
            };
            aliasCmd.setPermission(executor.getPermission());
            commandMap.register("", aliasCmd);
        }
    }
}
