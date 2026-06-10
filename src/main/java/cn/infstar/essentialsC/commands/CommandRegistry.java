package cn.infstar.essentialsC.commands;

import cn.infstar.essentialsC.EssentialsC;
import cn.infstar.essentialsC.ModuleManager;
import cn.infstar.essentialsC.tpsbar.TpsBarService;

import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class CommandRegistry {

    private static final Map<String, CommandSpec> COMMANDS = new LinkedHashMap<>();
    private static final Map<String, String> ALIAS_TO_COMMAND = new HashMap<>();
    private static final Map<String, BaseCommand> COMMAND_CACHE = new HashMap<>();
    private static final Set<String> UNAVAILABLE_COMMANDS = new java.util.HashSet<>();

    static {
        register("workbench", "essentialsc.command.workbench", ModuleManager.BLOCKS, "cn.infstar.essentialsC.commands.WorkbenchCommand", "wb");
        register("anvil", "essentialsc.command.anvil", ModuleManager.BLOCKS, "cn.infstar.essentialsC.commands.AnvilCommand");
        register("cartographytable", "essentialsc.command.cartographytable", ModuleManager.BLOCKS, "cn.infstar.essentialsC.commands.CartographyTableCommand", "ct", "cartography");
        register("grindstone", "essentialsc.command.grindstone", ModuleManager.BLOCKS, "cn.infstar.essentialsC.commands.GrindstoneCommand", "gs");
        register("loom", "essentialsc.command.loom", ModuleManager.BLOCKS, "cn.infstar.essentialsC.commands.LoomCommand");
        register("smithingtable", "essentialsc.command.smithingtable", ModuleManager.BLOCKS, "cn.infstar.essentialsC.commands.SmithingTableCommand", "st", "smithing");
        register("stonecutter", "essentialsc.command.stonecutter", ModuleManager.BLOCKS, "cn.infstar.essentialsC.commands.StonecutterCommand", "sc");
        register("enderchest", "essentialsc.command.enderchest", ModuleManager.BLOCKS, "cn.infstar.essentialsC.commands.EnderChestCommand", "ec");
        register("blocks", "essentialsc.command.blocks", ModuleManager.BLOCKS, "cn.infstar.essentialsC.commands.BlocksMenuCommand");
        register("hat", "essentialsc.command.hat", ModuleManager.PLAYER, "cn.infstar.essentialsC.commands.HatCommand");
        register("suicide", "essentialsc.command.suicide", ModuleManager.PLAYER, "cn.infstar.essentialsC.commands.SuicideCommand", "die");
        register("fly", "essentialsc.command.fly", ModuleManager.PLAYER, "cn.infstar.essentialsC.commands.FlyCommand");
        register("nightvision", "essentialsc.command.nightvision", ModuleManager.PLAYER, "cn.infstar.essentialsC.commands.NightVisionCommand", "nv");
        register("glow", "essentialsc.command.glow", ModuleManager.PLAYER, "cn.infstar.essentialsC.commands.GlowCommand");
        register("heal", "essentialsc.command.heal", ModuleManager.PLAYER, "cn.infstar.essentialsC.commands.HealCommand");
        register("vanish", "essentialsc.command.vanish", ModuleManager.PLAYER, "cn.infstar.essentialsC.commands.VanishCommand", "v");
        register("seen", "essentialsc.command.seen", ModuleManager.PLAYER, "cn.infstar.essentialsC.commands.SeenCommand", "info");
        register("feed", "essentialsc.command.feed", ModuleManager.PLAYER, "cn.infstar.essentialsC.commands.FeedCommand");
        register("repair", "essentialsc.command.repair", ModuleManager.PLAYER, "cn.infstar.essentialsC.commands.RepairCommand", "rep");
        register("tpsbar", "essentialsc.command.tpsbar", ModuleManager.TPSBAR, "cn.infstar.essentialsC.commands.TpsBarCommand");
        register("mobdrops", "essentialsc.mobdrops.enderman", ModuleManager.MOB_DROPS, "cn.infstar.essentialsC.commands.MobDropCommand");
        register("maintenance", "essentialsc.command.maintenance", ModuleManager.MAINTENANCE, "cn.infstar.essentialsC.commands.MaintenanceCommand", "maint");
        registerSubCommand("admin", "essentialsc.command.admin", ModuleManager.ADMIN_MODE, "cn.infstar.essentialsC.commands.AdminCommand");
    }

    private CommandRegistry() {
    }

    private static void register(String name, String permission, String moduleKey, String className, String... aliases) {
        register(name, permission, moduleKey, className, true, aliases);
    }

    private static void registerSubCommand(String name, String permission, String moduleKey, String className, String... aliases) {
        register(name, permission, moduleKey, className, false, aliases);
    }

    private static void register(String name, String permission, String moduleKey, String className, boolean standalone, String... aliases) {
        List<String> aliasList = List.of(aliases);
        CommandSpec spec = new CommandSpec(name, permission, moduleKey, className, aliasList, standalone);
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
        return getCommand(name) != null;
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
            Class<?> rawClass = Class.forName(spec.className());
            if (!BaseCommand.class.isAssignableFrom(rawClass)) {
                UNAVAILABLE_COMMANDS.add(resolvedName);
                return null;
            }

            Constructor<? extends BaseCommand> constructor = rawClass.asSubclass(BaseCommand.class).getDeclaredConstructor();
            BaseCommand command = constructor.newInstance();
            COMMAND_CACHE.put(resolvedName, command);
            return command;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            UNAVAILABLE_COMMANDS.add(resolvedName);
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

    public static void clearCache() {
        COMMAND_CACHE.clear();
        UNAVAILABLE_COMMANDS.clear();
    }

    public record CommandSpec(String name, String permission, String moduleKey, String className, List<String> aliases, boolean standalone) {
    }
}
