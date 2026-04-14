package cn.infstar.essentialsC;

import cn.infstar.essentialsC.commands.*;
import cn.infstar.essentialsC.listeners.ShulkerBoxListener;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;

public final class EssentialsC extends JavaPlugin {

    private static LangManager langManager;

    @Override
    public void onEnable() {
        // 初始化语言管理器
        langManager = new LangManager(this);
        
        // 注册监听器
        registerListeners();
        
        // 注册命令
        registerCommands();
        
        getLogger().info("EssentialsC 插件已启用！");
        getLogger().info("当前语言: " + langManager.getCurrentLanguage());
    }

    @Override
    public void onDisable() {
        getLogger().info("EssentialsC 插件已禁用！");
    }
    
    /**
     * 获取语言管理器实例
     */
    public static LangManager getLangManager() {
        return langManager;
    }
    
    /**
     * 注册所有监听器
     */
    private void registerListeners() {
        // 注册潜影盒右键打开监听器
        new ShulkerBoxListener(this);
        getLogger().info("成功注册监听器！");
    }
    
    private void registerCommands() {
        try {
            // 获取 CommandMap
            Field bukkitCommandMap = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            bukkitCommandMap.setAccessible(true);
            org.bukkit.command.CommandMap commandMap = (org.bukkit.command.CommandMap) bukkitCommandMap.get(Bukkit.getServer());
            
            // 注册所有命令（使用 CMI 风格：独立命令 + 别名）
            registerCommandWithAliases(commandMap, "workbench", new WorkbenchCommand(), "wb");
            registerCommandWithAliases(commandMap, "anvil", new AnvilCommand());
            registerCommandWithAliases(commandMap, "cartographytable", new CartographyTableCommand(), "ct", "cartography");
            registerCommandWithAliases(commandMap, "grindstone", new GrindstoneCommand(), "gs");
            registerCommandWithAliases(commandMap, "loom", new LoomCommand());
            registerCommandWithAliases(commandMap, "smithingtable", new SmithingTableCommand(), "st", "smithing");
            registerCommandWithAliases(commandMap, "stonecutter", new StonecutterCommand(), "sc");
            registerCommandWithAliases(commandMap, "enderchest", new EnderChestCommand(), "ec");
            registerCommandWithAliases(commandMap, "hat", new HatCommand());
            registerCommandWithAliases(commandMap, "suicide", new SuicideCommand(), "die");
            registerCommandWithAliases(commandMap, "fly", new FlyCommand());
            registerCommandWithAliases(commandMap, "heal", new HealCommand());
            registerCommandWithAliases(commandMap, "vanish", new VanishCommand(), "v");
            registerCommandWithAliases(commandMap, "seen", new SeenCommand(), "info");
            registerCommandWithAliases(commandMap, "feed", new FeedCommand());
            registerCommandWithAliases(commandMap, "repair", new RepairCommand(), "rep");
            registerCommandWithAliases(commandMap, "essentialsc", new HelpCommand(), "essc");
            
            getLogger().info("成功注册所有命令！");
        } catch (Exception e) {
            getLogger().severe("无法注册命令: " + e.getMessage());
            e.printStackTrace();
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
