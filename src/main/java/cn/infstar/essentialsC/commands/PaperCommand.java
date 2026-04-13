package cn.infstar.essentialsC.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public class PaperCommand extends Command {
    
    private final BaseCommand baseCommand;
    
    public PaperCommand(String name, BaseCommand baseCommand) {
        super(name);
        this.baseCommand = baseCommand;
        this.setPermission(baseCommand.permission);
    }
    
    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        return baseCommand.onCommand(sender, this, commandLabel, args);
    }
}
