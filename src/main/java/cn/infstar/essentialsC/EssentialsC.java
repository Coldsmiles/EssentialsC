package cn.infstar.essentialsC;

import cn.infstar.essentialsC.admin.AdminModeManager;
import cn.infstar.essentialsC.commands.BaseCommand;
import cn.infstar.essentialsC.commands.CommandRegistry;
import cn.infstar.essentialsC.commands.HelpCommand;
import cn.infstar.essentialsC.commands.PaperCommand;
import cn.infstar.essentialsC.commands.VanishCommand;
import cn.infstar.essentialsC.listeners.JeiRecipeSyncListener;
import cn.infstar.essentialsC.listeners.MobDropListener;
import cn.infstar.essentialsC.listeners.MobDropMenuListener;
import cn.infstar.essentialsC.listeners.ShulkerBoxListener;
import cn.infstar.essentialsC.listeners.VanishListener;
import cn.infstar.essentialsC.maintenance.MaintenanceListener;
import cn.infstar.essentialsC.maintenance.MaintenanceManager;
import cn.infstar.essentialsC.teleport.TeleportRequestManager;
import cn.infstar.essentialsC.tpsbar.TpsBarManager;
import cn.infstar.essentialsC.tpsbar.TpsBarService;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.messaging.Messenger;
import org.bukkit.plugin.java.JavaPlugin;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EssentialsC extends JavaPlugin {

    private static LangManager langManager;
    private ModuleManager moduleManager;
    private AdminModeManager adminModeManager;
    private MaintenanceManager maintenanceManager;
    private TeleportRequestManager teleportRequestManager;
    private MaintenanceListener maintenanceListener;
    private TpsBarService tpsBarManager;
    private ShulkerBoxListener shulkerBoxListener;
    private JeiRecipeSyncListener jeiRecipeSyncListener;
    private MobDropListener mobDropListener;
    private MobDropMenuListener mobDropMenuListener;
    private VanishListener vanishListener;
    private boolean commandsRegistered;
    private final Map<String, String> moduleStatus = new LinkedHashMap<>();

    @Override
    public void onEnable() {
        langManager = new LangManager(this);
        moduleManager = new ModuleManager(this);

        reloadRuntimeModules();
        registerCommands();

        logStartupSummary();
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
        if (teleportRequestManager != null) {
            teleportRequestManager.shutdown();
        }
        VanishCommand.clearAll(this);
        unregisterRuntimeListeners();
        unregisterPluginChannels();
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

    public TeleportRequestManager getTeleportRequestManager() {
        return teleportRequestManager;
    }

    public TpsBarService getTpsBarManager() {
        return tpsBarManager;
    }

    public void reloadRuntimeModules() {
        moduleStatus.clear();
        refreshPlayer();
        refreshAdminMode();
        refreshMaintenance();
        refreshTpsBar();
        refreshBlocks();
        refreshJeiSync();
        refreshMobDrops();
    }

    private void refreshPlayer() {
        if (!moduleManager.isEnabled(ModuleManager.PLAYER)) {
            if (teleportRequestManager != null) {
                teleportRequestManager.shutdown();
                HandlerList.unregisterAll(teleportRequestManager);
                teleportRequestManager = null;
            }
            VanishCommand.clearAll(this);
            if (vanishListener != null) {
                HandlerList.unregisterAll(vanishListener);
                vanishListener = null;
            }
            setModuleStatus("玩家功能", false, "已禁用");
            return;
        }

        if (vanishListener == null) {
            vanishListener = new VanishListener(this);
            getServer().getPluginManager().registerEvents(vanishListener, this);
        }
        if (teleportRequestManager == null) {
            teleportRequestManager = new TeleportRequestManager(this);
            getServer().getPluginManager().registerEvents(teleportRequestManager, this);
        } else {
            teleportRequestManager.reload();
        }
        setModuleStatus("玩家功能", true, "命令可用");
    }

    private void refreshAdminMode() {
        if (!moduleManager.isEnabled(ModuleManager.ADMIN_MODE)) {
            if (adminModeManager != null) {
                adminModeManager.shutdown();
                HandlerList.unregisterAll(adminModeManager);
                adminModeManager = null;
            }
            setModuleStatus("管理模式", false, "已禁用");
            return;
        }

        if (adminModeManager == null) {
            adminModeManager = new AdminModeManager(this);
            getServer().getPluginManager().registerEvents(adminModeManager, this);
        }
        setModuleStatus("管理模式", true, "监听器已注册");
    }

    private void refreshMaintenance() {
        if (!moduleManager.isEnabled(ModuleManager.MAINTENANCE)) {
            if (maintenanceManager != null) {
                maintenanceManager.shutdown();
                maintenanceManager = null;
            }
            if (maintenanceListener != null) {
                HandlerList.unregisterAll(maintenanceListener);
                maintenanceListener = null;
            }
            setModuleStatus("维护模式", false, "已禁用");
            return;
        }

        if (maintenanceManager == null) {
            maintenanceManager = new MaintenanceManager(this);
        } else {
            maintenanceManager.reload();
        }
        if (maintenanceListener == null) {
            maintenanceListener = new MaintenanceListener(this);
            getServer().getPluginManager().registerEvents(maintenanceListener, this);
        }
        setModuleStatus("维护模式", true, maintenanceManager.isEnabled() ? "当前开启" : "当前关闭");
    }

    private void refreshTpsBar() {
        if (!moduleManager.isEnabled(ModuleManager.TPSBAR)) {
            if (tpsBarManager != null) {
                tpsBarManager.shutdown();
                if (tpsBarManager instanceof Listener listener) {
                    HandlerList.unregisterAll(listener);
                }
                tpsBarManager = null;
            }
            setModuleStatus("TPSBar", false, "模块已禁用");
            return;
        }

        if (tpsBarManager == null) {
            tpsBarManager = new TpsBarManager(this);
            if (tpsBarManager instanceof Listener listener) {
                getServer().getPluginManager().registerEvents(listener, this);
            }
        } else {
            tpsBarManager.reloadSettings();
        }
        if (tpsBarManager == null) {
            setModuleStatus("TPSBar", false, "初始化失败");
        } else if (!tpsBarManager.isPluginCommandEnabled()) {
            setModuleStatus("TPSBar", false, tpsBarManager.isNativeCommandAvailable() ? "使用服务端原生命令" : "插件命令关闭");
        } else {
            setModuleStatus("TPSBar", true, "插件命令已启用");
        }
    }

    private void refreshBlocks() {
        if (!moduleManager.isEnabled(ModuleManager.BLOCKS)) {
            if (shulkerBoxListener != null) {
                HandlerList.unregisterAll(shulkerBoxListener);
                shulkerBoxListener = null;
            }
            setModuleStatus("便捷方块", false, "已禁用");
            return;
        }

        if (shulkerBoxListener == null) {
            shulkerBoxListener = new ShulkerBoxListener(this);
            getServer().getPluginManager().registerEvents(shulkerBoxListener, this);
        }
        setModuleStatus("便捷方块", true, "命令和潜影盒监听器已启用");
    }

    private void refreshJeiSync() {
        if (jeiRecipeSyncListener != null) {
            HandlerList.unregisterAll(jeiRecipeSyncListener);
            jeiRecipeSyncListener = null;
        }
        unregisterPluginChannels();

        if (!moduleManager.isEnabled(ModuleManager.JEI_SYNC)) {
            setModuleStatus("JEI 同步", false, "已禁用");
            return;
        }

        registerPluginChannels();
        jeiRecipeSyncListener = new JeiRecipeSyncListener(this);
        getServer().getPluginManager().registerEvents(jeiRecipeSyncListener, this);
        setModuleStatus("JEI 同步", true, "插件消息通道已注册");
    }

    private void refreshMobDrops() {
        if (!moduleManager.isEnabled(ModuleManager.MOB_DROPS)) {
            if (mobDropListener != null) {
                HandlerList.unregisterAll(mobDropListener);
                mobDropListener = null;
            }
            if (mobDropMenuListener != null) {
                HandlerList.unregisterAll(mobDropMenuListener);
                mobDropMenuListener = null;
            }
            setModuleStatus("生物掉落", false, "已禁用");
            return;
        }

        if (mobDropListener == null) {
            mobDropListener = new MobDropListener(this);
            getServer().getPluginManager().registerEvents(mobDropListener, this);
        } else {
            mobDropListener.reload();
        }
        if (mobDropMenuListener == null) {
            mobDropMenuListener = new MobDropMenuListener(this);
        }
        setModuleStatus("生物掉落", true, "末影人掉落控制已启用");
    }

    private void registerPluginChannels() {
        Messenger messenger = getServer().getMessenger();
        messenger.registerOutgoingPluginChannel(this, "fabric:recipe_sync");
        messenger.registerOutgoingPluginChannel(this, "neoforge:recipe_content");
    }

    private void unregisterPluginChannels() {
        Messenger messenger = getServer().getMessenger();
        messenger.unregisterOutgoingPluginChannel(this, "fabric:recipe_sync");
        messenger.unregisterOutgoingPluginChannel(this, "neoforge:recipe_content");
    }

    private void unregisterRuntimeListeners() {
        unregisterListener(adminModeManager);
        unregisterListener(maintenanceListener);
        unregisterListener(shulkerBoxListener);
        unregisterListener(jeiRecipeSyncListener);
        unregisterListener(mobDropListener);
        unregisterListener(mobDropMenuListener);
        unregisterListener(vanishListener);
        unregisterListener(teleportRequestManager);
        if (tpsBarManager instanceof Listener listener) {
            unregisterListener(listener);
        }
    }

    private void unregisterListener(Listener listener) {
        if (listener != null) {
            HandlerList.unregisterAll(listener);
        }
    }

    private void setModuleStatus(String moduleName, boolean enabled, String detail) {
        moduleStatus.put(moduleName, (enabled ? "启用" : "关闭") + " - " + detail);
    }

    private void logStartupSummary() {
        long enabledCount = moduleStatus.values().stream()
            .filter(status -> status.startsWith("启用"))
            .count();
        long disabledCount = moduleStatus.size() - enabledCount;

        getLogger().info("EssentialsC v" + getDescription().getVersion()
            + " 已启用 | 模块: " + enabledCount + " 启用, " + disabledCount + " 关闭");

        if (!getConfig().getBoolean("debug", false)) {
            return;
        }

        getLogger().info("模块明细:");
        for (Map.Entry<String, String> entry : moduleStatus.entrySet()) {
            getLogger().info("  " + entry.getKey() + ": " + entry.getValue());
        }
    }

    private void registerCommands() {
        if (commandsRegistered) {
            return;
        }
        commandsRegistered = true;
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            for (CommandRegistry.CommandSpec spec : CommandRegistry.getCommandSpecs()) {
                if (!spec.standalone()) {
                    continue;
                }
                BaseCommand executor = CommandRegistry.getCommand(spec.name());
                if (executor == null) {
                    continue;
                }
                event.registrar().register(
                    spec.name(),
                    spec.name(),
                    spec.aliases(),
                    new EssentialsBasicCommand(spec.name(), executor)
                );
            }

            event.registrar().register(
                "essentialsc",
                "essentialsc",
                List.of("essc"),
                new EssentialsBasicCommand("essentialsc", new HelpCommand())
            );
        });
    }

    private static final class EssentialsBasicCommand implements BasicCommand {

        private final String name;
        private final BaseCommand executor;
        private final PaperCommand commandAdapter;

        private EssentialsBasicCommand(String name, BaseCommand executor) {
            this.name = name;
            this.executor = executor;
            this.commandAdapter = new PaperCommand(name, executor);
        }

        @Override
        public void execute(CommandSourceStack commandSourceStack, String[] args) {
            CommandSender sender = commandSourceStack.getSender();
            if (CommandRegistry.resolveCommandName(name) != null && !CommandRegistry.isAvailable(name)) {
                sender.sendMessage(EssentialsC.getLangManager().getPrefixedString("messages.module-disabled"));
                return;
            }
            executor.onCommand(sender, commandAdapter, name, args);
        }

        @Override
        public Collection<String> suggest(CommandSourceStack commandSourceStack, String[] args) {
            if (executor instanceof TabCompleter completer) {
                return completer.onTabComplete(commandSourceStack.getSender(), commandAdapter, name, args);
            }
            return List.of();
        }
    }
}
