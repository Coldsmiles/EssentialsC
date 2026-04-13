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
        // 初始化语言管理器
        langManager = new LangManager(this);
        
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
    
    private void registerCommands() {
        try {
            // 获取 CommandMap
            Field bukkitCommandMap = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            bukkitCommandMap.setAccessible(true);
            org.bukkit.command.CommandMap commandMap = (org.bukkit.command.CommandMap) bukkitCommandMap.get(Bukkit.getServer());
            
            // 注册所有命令
            registerCommand(commandMap, "workbench", new WorkbenchCommand());
            registerCommand(commandMap, "anvil", new AnvilCommand());
            registerCommand(commandMap, "enchantingtable", new EnchantingTableCommand());
            registerCommand(commandMap, "cartographytable", new CartographyTableCommand());
            registerCommand(commandMap, "grindstone", new GrindstoneCommand());
            registerCommand(commandMap, "loom", new LoomCommand());
            registerCommand(commandMap, "smithingtable", new SmithingTableCommand());
            registerCommand(commandMap, "stonecutter", new StonecutterCommand());
            registerCommand(commandMap, "enderchest", new EnderChestCommand());
            registerCommand(commandMap, "hat", new HatCommand());
            registerCommand(commandMap, "suicide", new SuicideCommand());
            registerCommand(commandMap, "fly", new FlyCommand());
            registerCommand(commandMap, "heal", new HealCommand());
            registerCommand(commandMap, "vanish", new VanishCommand());
            registerCommand(commandMap, "seen", new SeenCommand());
            registerCommand(commandMap, "admin", new AdminMenuCommand());
            registerCommand(commandMap, "feed", new FeedCommand());
            registerCommand(commandMap, "repair", new RepairCommand());
            registerCommand(commandMap, "essentialsc", new HelpCommand());
            
            getLogger().info("成功注册 18 个命令！");
        } catch (Exception e) {
            getLogger().severe("无法注册命令: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void registerCommand(org.bukkit.command.CommandMap commandMap, String name, cn.infstar.essentialsC.commands.BaseCommand executor) {
        Command command = new Command(name) {
            @Override
            public boolean execute(CommandSender sender, String commandLabel, String[] args) {
                return executor.onCommand(sender, this, commandLabel, args);
            }
        };
        
        // 为 essentialsc 命令添加简化别名
        if (name.equals("essentialsc")) {
            command.setAliases(java.util.Arrays.asList("essc"));
        }
        
        command.setPermission(executor.getPermission());
        // 注册到默认命名空间，使玩家可以直接使用 /workbench 而不是 /essentialsc:workbench
        commandMap.register("", command);
    }
}
