package cn.infstar.essentialsC;

import cn.infstar.essentialsC.admin.AdminModeManager;
import cn.infstar.essentialsC.commands.BaseCommand;
import cn.infstar.essentialsC.commands.CommandRegistry;
import cn.infstar.essentialsC.commands.HelpCommand;
import cn.infstar.essentialsC.tpsbar.TpsBarService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;

public final class EssentialsC extends JavaPlugin {

    private static LangManager langManager;
    private AdminModeManager adminModeManager;
    private TpsBarService tpsBarManager;

    @Override
    public void onEnable() {
        langManager = new LangManager(this);
        adminModeManager = new AdminModeManager(this);
        getServer().getPluginManager().registerEvents(adminModeManager, this);
        tpsBarManager = createOptionalService("cn.infstar.essentialsC.tpsbar.TpsBarManager", TpsBarService.class);
        if (tpsBarManager instanceof Listener listener) {
            getServer().getPluginManager().registerEvents(listener, this);
        }
        registerPluginChannels();
        registerListeners();
        registerCommands();

        getLogger().info("EssentialsC enabled. Version: " + getDescription().getVersion());
    }

    @Override
    public void onDisable() {
        if (tpsBarManager != null) {
            tpsBarManager.shutdown();
        }
        if (adminModeManager != null) {
            adminModeManager.shutdown();
        }
        getLogger().info("EssentialsC disabled.");
    }

    public static LangManager getLangManager() {
        return langManager;
    }

    public AdminModeManager getAdminModeManager() {
        return adminModeManager;
    }

    public TpsBarService getTpsBarManager() {
        return tpsBarManager;
    }

    private void registerPluginChannels() {
        org.bukkit.plugin.messaging.Messenger messenger = getServer().getMessenger();
        messenger.registerOutgoingPluginChannel(this, "fabric:recipe_sync");
        messenger.registerOutgoingPluginChannel(this, "neoforge:recipe_content");
    }

    private void registerListeners() {
        if (registerListener("cn.infstar.essentialsC.listeners.ShulkerBoxListener")) {
            getLogger().info("- Shulker box module");
        }

        if (registerListener("cn.infstar.essentialsC.listeners.JeiRecipeSyncListener")) {
            getLogger().info("- JEI recipe sync");
        }

        if (registerListener("cn.infstar.essentialsC.listeners.MobDropListener")) {
            createOptionalInstance("cn.infstar.essentialsC.listeners.MobDropMenuListener");
            getLogger().info("- Mob drop control");
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
            getLogger().severe("Failed to register commands: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void registerCommandWithAliases(org.bukkit.command.CommandMap commandMap, String name, BaseCommand executor, String... aliases) {
        Command command = new Command(name) {
            @Override
            public boolean execute(CommandSender sender, String commandLabel, String[] args) {
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
        commandMap.register("", command);

        for (String alias : aliases) {
            Command aliasCmd = new Command(alias) {
                @Override
                public boolean execute(CommandSender sender, String commandLabel, String[] args) {
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
            commandMap.register("", aliasCmd);
        }
    }
}
