package com.steamcraftmc.HACxtras.Commands;

import java.util.UUID;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import com.steamcraftmc.HACxtras.MainPlugin;
import com.steamcraftmc.HACxtras.utils.PlayerData;

public class CmdHacx extends BaseCommand {

	public CmdHacx(MainPlugin plugin) {
		super(plugin, "hacx", 1, 2);
	}

	@Override
	protected boolean doConsoleCommand(CommandSender sender, Command cmd, String commandLabel, String[] args)
			throws Exception {
		if (args.length < 1)
			return false;

		PlayerData player;
		switch (args[0].toLowerCase()) {
		case "pardon":
			player = getPlayer(sender, args);
			if (player == null) break;
			player.pardon();
			sender.sendMessage(plugin.Config.get("messages.pardon", "&6Player has been pardoned."));
			break;
		case "whitelist":
			player = getPlayer(sender, args);
			if (player == null) break;
			player.whitelist(true);
			sender.sendMessage(plugin.Config.get("messages.whitelisted", "&6Player has been whitelisted."));
			break;
		case "reload":
			plugin.reload();
			sender.sendMessage(plugin.Config.get("messages.config-reloaded", "&6Configuration reloaded."));
			break;
		default:
			return false;
		}
		
		return true;
	}

	private PlayerData getPlayer(CommandSender sender, String[] args) {
		if (args.length < 2)
			return null;
		String name = args[1];
		PlayerData pd = plugin.getPlayerByName(name);
		if (pd != null) {
			return pd;
		}
		
		for (ConfigurationSection section : plugin.Store.getSections()) {
			if (section.getString("name", "").equalsIgnoreCase(name)) {
				return new PlayerData(plugin, UUID.fromString(section.getCurrentPath()), section.getString("name"));
			}
		}
		
		sender.sendMessage(plugin.Config.PlayerNotFound(name));
		return null;
	}

	@Override
	protected boolean doPlayerCommand(Player player, Command cmd, String commandLabel, String[] args) throws Exception {
		return doConsoleCommand(player, cmd, commandLabel, args);
	}
}
