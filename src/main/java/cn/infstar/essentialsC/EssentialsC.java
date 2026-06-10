package cn.infstar.essentialsC;

import cn.infstar.essentialsC.admin.AdminModeManager;
import cn.infstar.essentialsC.commands.BaseCommand;
import cn.infstar.essentialsC.commands.CommandRegistry;
import cn.infstar.essentialsC.commands.HelpCommand;
import cn.infstar.essentialsC.maintenance.MaintenanceListener;
import cn.infstar.essentialsC.maintenance.MaintenanceManager;
import cn.infstar.essentialsC.tpsbar.TpsBarService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;

public final class EssentialsC extends JavaPlugin {

    private static LangManager langManager;
    private ModuleManager moduleManager;
    private AdminModeManager adminModeManager;
    private MaintenanceManager maintenanceManager;
    private TpsBarService tpsBarManager;

    @Override
    public void onEnable() {
        langManager = new LangManager(this);
        moduleManager = new ModuleManager(this);

        if (moduleManager.isEnabled(ModuleManager.ADMIN_MODE)) {
            adminModeManager = new AdminModeManager(this);
            getServer().getPluginManager().registerEvents(adminModeManager, this);
        }

        if (moduleManager.isEnabled(ModuleManager.MAINTENANCE)) {
            maintenanceManager = new MaintenanceManager(this);
            getServer().getPluginManager().registerEvents(new MaintenanceListener(this), this);
        }

        if (moduleManager.isEnabled(ModuleManager.TPSBAR)) {
            tpsBarManager = createOptionalService("cn.infstar.essentialsC.tpsbar.TpsBarManager", TpsBarService.class);
        }
        if (tpsBarManager instanceof Listener listener) {
            getServer().getPluginManager().registerEvents(listener, this);
        }
        registerPluginChannels();
        registerListeners();
        registerCommands();

        getLogger().info("EssentialsC 已启用，版本: " + getDescription().getVersion());
    }

    @Override
    public void onDisable() {
        if (tpsBarManager != null) {
            tpsBarManager.shutdown();
        }
        if (adminModeManager != null) {
            adminModeManager.shutdown();
        }
        if (maintenanceManager != null) {
            maintenanceManager.shutdown();
        }
        getLogger().info("EssentialsC 已禁用。");
    }

    public static LangManager getLangManager() {
        return langManager;
    }

    public AdminModeManager getAdminModeManager() {
        return adminModeManager;
    }

    public ModuleManager getModuleManager() {
        return moduleManager;
    }

    public MaintenanceManager getMaintenanceManager() {
        return maintenanceManager;
    }

    public TpsBarService getTpsBarManager() {
        return tpsBarManager;
    }

    private void registerPluginChannels() {
        if (!moduleManager.isEnabled(ModuleManager.JEI_SYNC)) {
            return;
        }
        org.bukkit.plugin.messaging.Messenger messenger = getServer().getMessenger();
        messenger.registerOutgoingPluginChannel(this, "fabric:recipe_sync");
        messenger.registerOutgoingPluginChannel(this, "neoforge:recipe_content");
    }

    private void registerListeners() {
        if (moduleManager.isEnabled(ModuleManager.BLOCKS)
            && registerListener("cn.infstar.essentialsC.listeners.ShulkerBoxListener")) {
            getLogger().info("- 潜影盒模块");
        }

        if (moduleManager.isEnabled(ModuleManager.JEI_SYNC)
            && registerListener("cn.infstar.essentialsC.listeners.JeiRecipeSyncListener")) {
            getLogger().info("- JEI 配方同步");
        }

        if (moduleManager.isEnabled(ModuleManager.MOB_DROPS)
            && registerListener("cn.infstar.essentialsC.listeners.MobDropListener")) {
            createOptionalInstance("cn.infstar.essentialsC.listeners.MobDropMenuListener");
            getLogger().info("- 生物掉落控制");
        }
    }

    private boolean registerListener(String className) {
        try {
            Class<?> listenerClass = Class.forName(className);
            Object listenerInstance = listenerClass.getConstructor(EssentialsC.class).newInstance(this);
            getServer().getPluginManager().registerEvents((org.bukkit.event.Listener) listenerInstance, this);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private void createOptionalInstance(String className) {
        try {
            Class<?> targetClass = Class.forName(className);
            targetClass.getConstructor(EssentialsC.class).newInstance(this);
        } catch (Exception ignored) {
        }
    }

    private <T> T createOptionalService(String className, Class<T> serviceType) {
        try {
            Class<?> targetClass = Class.forName(className);
            Object instance = targetClass.getConstructor(EssentialsC.class).newInstance(this);
            return serviceType.cast(instance);
        } catch (Exception ignored) {
            return null;
        }
    }

    private void registerCommands() {
        try {
            Field bukkitCommandMap = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            bukkitCommandMap.setAccessible(true);
            org.bukkit.command.CommandMap commandMap = (org.bukkit.command.CommandMap) bukkitCommandMap.get(Bukkit.getServer());

            for (CommandRegistry.CommandSpec spec : CommandRegistry.getCommandSpecs()) {
                if (!spec.standalone()) {
                    continue;
                }
                BaseCommand executor = CommandRegistry.getCommand(spec.name());
                if (executor == null) {
                    continue;
                }
                registerCommandWithAliases(commandMap, spec.name(), executor, spec.aliases().toArray(String[]::new));
            }

            registerCommandWithAliases(commandMap, "essentialsc", new HelpCommand(), "essc");
        } catch (Exception e) {
            getLogger().severe("注册命令失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void registerCommandWithAliases(org.bukkit.command.CommandMap commandMap, String name, BaseCommand executor, String... aliases) {
        String fallbackPrefix = getName().toLowerCase(java.util.Locale.ROOT);
        Command command = new Command(name) {
            @Override
            public boolean execute(CommandSender sender, String commandLabel, String[] args) {
                if (CommandRegistry.resolveCommandName(name) != null && !CommandRegistry.isAvailable(name)) {
                    sender.sendMessage(EssentialsC.getLangManager().getPrefixedString("messages.module-disabled"));
                    return true;
                }
                return executor.onCommand(sender, this, commandLabel, args);
            }

            @Override
            public java.util.List<String> tabComplete(CommandSender sender, String alias, String[] args) throws IllegalArgumentException {
                if (executor instanceof org.bukkit.command.TabCompleter completer) {
                    return completer.onTabComplete(sender, this, alias, args);
                }
                return super.tabComplete(sender, alias, args);
            }
        };

        command.setPermission(executor.getPermission());
        commandMap.register(fallbackPrefix, command);

        for (String alias : aliases) {
            Command aliasCmd = new Command(alias) {
                @Override
                public boolean execute(CommandSender sender, String commandLabel, String[] args) {
                    if (CommandRegistry.resolveCommandName(name) != null && !CommandRegistry.isAvailable(name)) {
                        sender.sendMessage(EssentialsC.getLangManager().getPrefixedString("messages.module-disabled"));
                        return true;
                    }
                    return executor.onCommand(sender, this, commandLabel, args);
                }

                @Override
                public java.util.List<String> tabComplete(CommandSender sender, String label, String[] args) throws IllegalArgumentException {
                    if (executor instanceof org.bukkit.command.TabCompleter completer) {
                        return completer.onTabComplete(sender, this, label, args);
                    }
                    return super.tabComplete(sender, label, args);
                }
            };
            aliasCmd.setPermission(executor.getPermission());
            commandMap.register(fallbackPrefix, aliasCmd);
        }
    }
}
