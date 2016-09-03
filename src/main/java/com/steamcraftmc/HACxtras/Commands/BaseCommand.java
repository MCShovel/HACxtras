package com.steamcraftmc.HACxtras.Commands;

import java.util.logging.Level;

import com.steamcraftmc.HACxtras.MainPlugin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public abstract class BaseCommand implements CommandExecutor {

    protected final MainPlugin plugin;
    protected final String permission;
    protected final String cmdName;
    private final int minArgs;
    private final int maxArgs;
    
    public BaseCommand(MainPlugin plugin, String command) {
    	this(plugin, command, "essentials." + command, 0, 0);
	}

    public BaseCommand(MainPlugin plugin, String command, int maxArg) {
    	this(plugin, command, "essentials." + command, 0, maxArg);
	}

    public BaseCommand(MainPlugin plugin, String command, int minArg, int maxArg) {
    	this(plugin, command, "essentials." + command, minArg, maxArg);
	}

    public BaseCommand(MainPlugin plugin, String command, String permission, int minArg, int maxArg) {
        this.plugin = plugin;
        this.permission = permission;
        this.cmdName = command;
        this.minArgs = minArg;
        this.maxArgs = maxArg;
    	this.plugin.getCommand(this.cmdName).setExecutor(this);
    	this.plugin.getCommand(this.cmdName).setPermission(permission);
    	if (this instanceof TabCompleter) {
    		this.plugin.getCommand(this.cmdName).setTabCompleter((TabCompleter)this);    		
    	}
    }

	protected abstract boolean doPlayerCommand(Player player, Command cmd, String commandLabel, String[] args) throws Exception;
    
    protected boolean doConsoleCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) throws Exception {
    	sender.sendMessage(plugin.Config.get("messages.no-console", "&4Command should be used by a player."));
    	return true;
    }
    
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        
        if (cmd == null || !cmdName.equalsIgnoreCase(cmd.getName()) || 
        	args == null || args.length < minArgs || args.length > maxArgs) {
            return false;
        }

    	try {
	        if (sender instanceof Player) {
		        Player player = (Player) sender;
		        
		        if (!sender.hasPermission(permission)) {
		            sender.sendMessage(plugin.Config.NoAccess());
		            plugin.log(Level.SEVERE, "The user " + player.getName() + " need permission " + permission + " to access to the command " + cmdName);
		            return true;
		        }
		        
					return doPlayerCommand(player, cmd, commandLabel, args);
	        }
	        else {
	        	return doConsoleCommand(sender, cmd, commandLabel, args);
	        }
		} catch (Exception e) {
			plugin.log(Level.SEVERE, e.toString());
			e.printStackTrace();
			sender.sendMessage(e.getMessage());
			return true;
		}
    }
}
