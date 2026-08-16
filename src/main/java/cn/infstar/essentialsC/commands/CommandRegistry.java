package cn.infstar.essentialsC.commands;

import cn.infstar.essentialsC.EssentialsC;
import cn.infstar.essentialsC.ModuleManager;
import cn.infstar.essentialsC.tpsbar.TpsBarService;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public final class CommandRegistry {

    private static final Map<String, CommandSpec> COMMANDS = new LinkedHashMap<>();
    private static final Map<String, String> ALIAS_TO_COMMAND = new HashMap<>();
    private static final Map<String, BaseCommand> COMMAND_CACHE = new HashMap<>();
    private static final Set<String> UNAVAILABLE_COMMANDS = new java.util.HashSet<>();

    static {
        register("workbench", "essentialsc.command.workbench", ModuleManager.BLOCKS, WorkbenchCommand::new, "wb");
        register("anvil", "essentialsc.command.anvil", ModuleManager.BLOCKS, AnvilCommand::new);
        register("cartographytable", "essentialsc.command.cartographytable", ModuleManager.BLOCKS, CartographyTableCommand::new, "ct", "cartography");
        register("grindstone", "essentialsc.command.grindstone", ModuleManager.BLOCKS, GrindstoneCommand::new, "gs");
        register("loom", "essentialsc.command.loom", ModuleManager.BLOCKS, LoomCommand::new);
        register("smithingtable", "essentialsc.command.smithingtable", ModuleManager.BLOCKS, SmithingTableCommand::new, "st", "smithing");
        register("stonecutter", "essentialsc.command.stonecutter", ModuleManager.BLOCKS, StonecutterCommand::new, "sc");
        register("enderchest", "essentialsc.command.enderchest", ModuleManager.BLOCKS, EnderChestCommand::new, "ec");
        register("blocks", "essentialsc.command.blocks", ModuleManager.BLOCKS, BlocksMenuCommand::new);
        registerCore("hat", "essentialsc.command.hat", HatCommand::new);
        registerCore("suicide", "essentialsc.command.suicide", SuicideCommand::new, "die");
        registerCore("fly", "essentialsc.command.fly", FlyCommand::new);
        registerCore("nightvision", "essentialsc.command.nightvision", NightVisionCommand::new, "nv");
        registerCore("glow", "essentialsc.command.glow", GlowCommand::new);
        registerCore("heal", "essentialsc.command.heal", HealCommand::new);
        registerCore("vanish", "essentialsc.command.vanish", VanishCommand::new, "v");
        registerCore("seen", "essentialsc.command.seen", SeenCommand::new, "info");
        registerCore("feed", "essentialsc.command.feed", FeedCommand::new);
        registerCore("repair", "essentialsc.command.repair", RepairCommand::new, "rep");
        registerCore("tpa", "essentialsc.command.tpa", TpaCommand::new);
        registerCore("tpahere", "essentialsc.command.tpahere", TpaHereCommand::new);
        registerCore("tpaall", "essentialsc.command.tpaall", TpaAllCommand::new);
        registerCore("tpaccept", "essentialsc.command.tpaccept", TpAcceptCommand::new, "tpyes");
        registerCore("tpdeny", "essentialsc.command.tpdeny", TpDenyCommand::new, "tpdecline", "tpno");
        registerCore("tpignore", "essentialsc.command.tpignore", TpIgnoreCommand::new);
        register("tpsbar", "essentialsc.command.tpsbar", ModuleManager.TPSBAR, TpsBarCommand::new);
        register("mobdrops", "essentialsc.mobdrops.enderman", ModuleManager.MOB_DROPS, MobDropCommand::new);
        registerSubCommand("admin", "essentialsc.command.admin", ModuleManager.ADMIN_MODE, AdminCommand::new);
        registerSubCommand("skin", "essentialsc.command.skin", ModuleManager.SKIN_BRIDGE, SkinBridgeCommand::new);
    }

    private CommandRegistry() {
    }

    private static void registerCore(String name, String permission, Supplier<BaseCommand> factory, String... aliases) {
        register(name, permission, null, factory, aliases);
    }

    private static void register(String name, String permission, String moduleKey, Supplier<BaseCommand> factory, String... aliases) {
        register(name, permission, moduleKey, factory, true, aliases);
    }

    private static void registerSubCommand(String name, String permission, String moduleKey, Supplier<BaseCommand> factory, String... aliases) {
        register(name, permission, moduleKey, factory, false, aliases);
    }

    private static void register(String name, String permission, String moduleKey, Supplier<BaseCommand> factory,
                                 boolean standalone, String... aliases) {
        List<String> aliasList = List.of(aliases);
        CommandSpec spec = new CommandSpec(name, permission, moduleKey, factory, aliasList, standalone);
        COMMANDS.put(name, spec);
        ALIAS_TO_COMMAND.put(name, name);
        for (String alias : aliasList) {
            ALIAS_TO_COMMAND.put(alias, name);
        }
    }

    public static Collection<CommandSpec> getCommandSpecs() {
        return Collections.unmodifiableCollection(COMMANDS.values());
    }

    public static String resolveCommandName(String input) {
        if (input == null) {
            return null;
        }
        return ALIAS_TO_COMMAND.get(input.toLowerCase());
    }

    public static boolean isAvailable(String name) {
        String resolvedName = resolveCommandName(name);
        return resolvedName != null && !isRuntimeDisabled(resolvedName) && getRegisteredCommand(resolvedName) != null;
    }

    public static String getPermission(String name) {
        CommandSpec spec = COMMANDS.get(name);
        return spec == null ? null : spec.permission();
    }

    public static BaseCommand getCommand(String name) {
        String resolvedName = resolveCommandName(name);
        if (resolvedName == null) {
            return null;
        }

        if (isRuntimeDisabled(resolvedName)) {
            return null;
        }

        return getRegisteredCommand(resolvedName);
    }

    public static BaseCommand getRegisteredCommand(String name) {
        String resolvedName = resolveCommandName(name);
        if (resolvedName == null) {
            return null;
        }

        BaseCommand cached = COMMAND_CACHE.get(resolvedName);
        if (cached != null) {
            return cached;
        }
        if (UNAVAILABLE_COMMANDS.contains(resolvedName)) {
            return null;
        }

        CommandSpec spec = COMMANDS.get(resolvedName);
        if (spec == null) {
            return null;
        }

        try {
            BaseCommand command = spec.factory().get();
            COMMAND_CACHE.put(resolvedName, command);
            return command;
        } catch (RuntimeException | LinkageError exception) {
            UNAVAILABLE_COMMANDS.add(resolvedName);
            try {
                EssentialsC.getPlugin(EssentialsC.class).getLogger()
                    .warning("初始化命令 /" + resolvedName + " 失败: " + exception.getMessage());
            } catch (IllegalStateException ignored) {
            }
            return null;
        }
    }

    private static boolean isRuntimeDisabled(String resolvedName) {
        CommandSpec spec = COMMANDS.get(resolvedName);
        if (spec == null) {
            return true;
        }

        try {
            EssentialsC plugin = EssentialsC.getPlugin(EssentialsC.class);
            ModuleManager moduleManager = plugin.getModuleManager();
            if (moduleManager != null && !moduleManager.isEnabled(spec.moduleKey())) {
                return true;
            }
        } catch (IllegalStateException ignored) {
            return false;
        }

        if (!"tpsbar".equals(resolvedName)) {
            return false;
        }

        try {
            EssentialsC plugin = EssentialsC.getPlugin(EssentialsC.class);
            TpsBarService tpsBarService = plugin.getTpsBarManager();
            return tpsBarService == null || !tpsBarService.isPluginCommandEnabled();
        } catch (IllegalStateException ignored) {
            return false;
        }
    }

    public static void clearInitializationFailures() {
        UNAVAILABLE_COMMANDS.clear();
    }

    public record CommandSpec(String name, String permission, String moduleKey, Supplier<BaseCommand> factory,
                               List<String> aliases, boolean standalone) {
    }
}
