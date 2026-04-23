package cn.infstar.essentialsC.commands;

import cn.infstar.essentialsC.EssentialsC;
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
        register("workbench", "essentialsc.command.workbench", "cn.infstar.essentialsC.commands.WorkbenchCommand", "wb");
        register("anvil", "essentialsc.command.anvil", "cn.infstar.essentialsC.commands.AnvilCommand");
        register("cartographytable", "essentialsc.command.cartographytable", "cn.infstar.essentialsC.commands.CartographyTableCommand", "ct", "cartography");
        register("grindstone", "essentialsc.command.grindstone", "cn.infstar.essentialsC.commands.GrindstoneCommand", "gs");
        register("loom", "essentialsc.command.loom", "cn.infstar.essentialsC.commands.LoomCommand");
        register("smithingtable", "essentialsc.command.smithingtable", "cn.infstar.essentialsC.commands.SmithingTableCommand", "st", "smithing");
        register("stonecutter", "essentialsc.command.stonecutter", "cn.infstar.essentialsC.commands.StonecutterCommand", "sc");
        register("enderchest", "essentialsc.command.enderchest", "cn.infstar.essentialsC.commands.EnderChestCommand", "ec");
        register("blocks", "essentialsc.command.blocks", "cn.infstar.essentialsC.commands.BlocksMenuCommand");
        register("hat", "essentialsc.command.hat", "cn.infstar.essentialsC.commands.HatCommand");
        register("suicide", "essentialsc.command.suicide", "cn.infstar.essentialsC.commands.SuicideCommand", "die");
        register("fly", "essentialsc.command.fly", "cn.infstar.essentialsC.commands.FlyCommand");
        register("nightvision", "essentialsc.command.nightvision", "cn.infstar.essentialsC.commands.NightVisionCommand", "nv");
        register("glow", "essentialsc.command.glow", "cn.infstar.essentialsC.commands.GlowCommand");
        register("heal", "essentialsc.command.heal", "cn.infstar.essentialsC.commands.HealCommand");
        register("vanish", "essentialsc.command.vanish", "cn.infstar.essentialsC.commands.VanishCommand", "v");
        register("seen", "essentialsc.command.seen", "cn.infstar.essentialsC.commands.SeenCommand", "info");
        register("feed", "essentialsc.command.feed", "cn.infstar.essentialsC.commands.FeedCommand");
        register("repair", "essentialsc.command.repair", "cn.infstar.essentialsC.commands.RepairCommand", "rep");
        register("tpsbar", "essentialsc.command.tpsbar", "cn.infstar.essentialsC.commands.TpsBarCommand");
        register("mobdrops", "essentialsc.mobdrops.enderman", "cn.infstar.essentialsC.commands.MobDropCommand");
        registerSubCommand("admin", "essentialsc.command.admin", "cn.infstar.essentialsC.commands.AdminCommand");
    }

    private CommandRegistry() {
    }

    private static void register(String name, String permission, String className, String... aliases) {
        register(name, permission, className, true, aliases);
    }

    private static void registerSubCommand(String name, String permission, String className, String... aliases) {
        register(name, permission, className, false, aliases);
    }

    private static void register(String name, String permission, String className, boolean standalone, String... aliases) {
        List<String> aliasList = List.of(aliases);
        CommandSpec spec = new CommandSpec(name, permission, className, aliasList, standalone);
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

        BaseCommand cached = COMMAND_CACHE.get(resolvedName);
        if (cached != null) {
            return cached;
        }
        if (UNAVAILABLE_COMMANDS.contains(resolvedName)) {
            return null;
        }
        if (isRuntimeDisabled(resolvedName)) {
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

    public record CommandSpec(String name, String permission, String className, List<String> aliases, boolean standalone) {
    }
}
