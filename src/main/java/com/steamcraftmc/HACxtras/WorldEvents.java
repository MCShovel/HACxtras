package com.steamcraftmc.HACxtras;

import java.util.*;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent.Result;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.heirteir.hac.api.event.PlayerHACViolationEvent;
import com.steamcraftmc.HACxtras.utils.PlayerData;

public class WorldEvents implements Listener, PluginMessageListener, Runnable {
	private final MainPlugin plugin;
	private final HashMap<String, Team> teams;
	private final HashMap<UUID, PlayerData> players;
	private boolean bungeecord;
	private Scoreboard board;
	private int taskId;

	public WorldEvents(MainPlugin plugin) {
		this.plugin = plugin;
		this.teams = new HashMap<String, Team>();
		this.players = new HashMap<UUID, PlayerData>();
		this.taskId = -1;
	}
	
	public void start() {
		bungeecord = plugin.Config.getBoolean("options.use_bungeecord", true);
		if (bungeecord) {
	        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, "BungeeCord");
	        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, "BungeeCord", this);
		}
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
		int reduceRate = plugin.Config.getInt("options.reduce_ticks", 1200);
		if (reduceRate > 0) {
			this.taskId = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, this, reduceRate, reduceRate);
		}
	}
	
	private void createTeams() {
		try {
			if (this.board == null && plugin.Config.getBoolean("options.use_scoreboard_teams", true)) {
				this.board = Bukkit.getScoreboardManager().getNewScoreboard();
				//objective = board.registerNewObjective("haxor", "dummy");
				Team team;
				for (String key : new String[] { "warning", "action", "ban" }) {
					String t1 = plugin.Config.getRaw(key + ".scoreboard_team");
					if (t1 != null && t1.length() > 0) {
						String nm = t1.replaceAll("[^\\w]+", "");
						this.teams.put(t1, team = board.registerNewTeam(nm));
						team.setDisplayName("Team " + nm);
						team.setSuffix(" " + ChatColor.translateAlternateColorCodes('&', t1));
						team.setCanSeeFriendlyInvisibles(false);
						team.setAllowFriendlyFire(true);
						team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);
					}
				}
			}
		}
		catch(Exception ex) {
			ex.printStackTrace();
		}
	}

	public void stop() {
    	HandlerList.unregisterAll(this);
        plugin.getServer().getMessenger().unregisterOutgoingPluginChannel(plugin, "BungeeCord");
        plugin.getServer().getMessenger().unregisterIncomingPluginChannel(plugin, "BungeeCord", this);
        if (taskId != -1) {
        	plugin.getServer().getScheduler().cancelTask(this.taskId);
        	this.taskId = -1;
        }
	}

	public PlayerData getPlayer(Player player) {
		PlayerData pdata = players.get(player.getUniqueId());
		if (pdata == null) {
			players.put(player.getUniqueId(), pdata = new PlayerData(plugin, player));
		}
		return pdata;
	}	

	public PlayerData getPlayer(String name) {
		for (PlayerData pd : this.players.values()) {
			if (pd.name.equalsIgnoreCase(name)) {
				return pd;
			}
		}
			
		Player p = plugin.getServer().getPlayer(name);
		if (p != null) {
			return getPlayer(p);
		}

		return null;
	}	
	
	@EventHandler
	public void onPlayerPreLogin(AsyncPlayerPreLoginEvent e) {
		String banned = plugin.Store.getRaw(e.getUniqueId() + ".banned_reason");
		if (banned != null && banned.length() > 0) {
			e.disallow(Result.KICK_BANNED, banned);
		}
	}
		
	@EventHandler
	public void onPlayerLogon(PlayerJoinEvent e) {
		try {
			createTeams();
			if (this.board != null) {
				e.getPlayer().setScoreboard(this.board);
			}
			Player p = e.getPlayer();
			PlayerData pdata = getPlayer(p);
			pdata.join();
			
			for (Team t : teams.values()) {
				t.removePlayer(p);
			}

			String team = pdata.getTeam();
			if (team != null && team.length() > 0 && teams.containsKey(team)) {
				teams.get(team).addPlayer(p);
			}
		}
		catch(Exception ex) {
			ex.printStackTrace();
		}
	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent e) {
		getPlayer(e.getPlayer()).quit();
	}

	public void onResetTeams() {
		for (Player p : plugin.getServer().getOnlinePlayers()) {
			for (Team t : teams.values()) {
				t.removePlayer(p);
			}

			PlayerData pdata = getPlayer(p);
			if (pdata != null) {
				String team = pdata.getTeam();
				if (team != null && team.length() > 0 && teams.containsKey(team)) {
					teams.get(team).addPlayer(p);
				}
			}
		}
	}
	
	@Override
	public void run() {
		PlayerData[] all = new PlayerData[this.players.size()]; 
		all = this.players.values().toArray(all);
		for (PlayerData pdata : all) {
			try {
				if (pdata.reduce()) {
					this.players.remove(pdata.uuid);
				}
			}
			catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}
	
	//Bad Packets, Criticals, Fast Eat, Fast Place, Fly, Glide, Headless, Impossible Interaction, 
	//Impossible Movements, Jesus, Kill Aura, No Fall, Spam, Speed, Step
	@EventHandler
	public void onPlayerHACViolation(PlayerHACViolationEvent e) {
		final Player p = e.getPlayer();
		final Location loc = p.getLocation();
		
		final PlayerData pdata = getPlayer(p);
		final String hacName = e.getHackType();

		if (p.hasPermission("hacx.whitelisted") || pdata.whitelisted())
			return;

		int ourCount = pdata.getCounter(e.getHackType()) + 1;
		plugin.log(Level.WARNING, 
				plugin.Config.format("messages.hac-console", 
						"{player}({x},{y},{z},{ping}ping,{tps}tps) VIOLOATION: #{id} {type} {severity}", 
						"player", p.getName(),
						"x", loc.getBlockX(), "y", loc.getBlockY(), "z", loc.getBlockZ(),
						"ping", e.getPlayerPing(),
						"tps", e.getTPS(),
						"id", e.getId(),
						"type", e.getHackType(),
						"severity", ourCount // e.getThreshold()
					)
				);

		if (pdata.lastViloationAt != null && pdata.lastViloationAt.distanceSquared(loc) <= 2 && ourCount < 5) {
			if (hacName.equals("Impossible Interaction") || hacName.equals("Impossible Movements") || hacName.equals("Fly")) {
				e.setCancelled(true);
				return;
			}
		}
		if (ourCount < 5 && hacName.equals("Fast Place")) {
			e.setCancelled(true);
		}

		pdata.lastViloationAt = loc;

		try {
			final int hacId = e.getId();
			final ConfigurationSection actions = pdata.increment(hacName);
			final HashMap<String, Team> teams = this.teams;
			if (actions != null) {
				plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
					@Override
					public void run() {
						sendFormatted(actions, "staff", pdata.name, hacId, hacName);
						sendFormatted(actions, "announce", pdata.name, hacId, hacName);
						String msg = getFormatted(actions, "message", pdata.name, hacId, hacName);
						
						if (teams.size() > 0) {
							String team = actions.getString("scoreboard_team");
							if (team != null && team.length() > 0) {
								pdata.setTeam(team, msg);
							}
							if (team != null && teams.containsKey(team) && p.isOnline()) {
								for (Team t : teams.values()) {
									t.removePlayer(p);
								}
								teams.get(team).addPlayer(p);
							}
						}
						if (msg == null || msg.length() <= 0) {
							// Do Nothing
						}
						else if (actions.getBoolean("ban", false)) {
							pdata.banned(msg);
							if (p.isOnline())
								p.kickPlayer(msg);
						}
						else if (actions.getBoolean("kick", false)) {
							if (p.isOnline())
								p.kickPlayer(msg);
						}
						else {
							if (p.isOnline())
								p.sendMessage(msg);
						}
					}});
			}
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	private String getFormatted(ConfigurationSection cfg, String key, String player, int id, String hack) {
		String message = cfg.getString(key);
		if (message != null && message.length() > 0) {
			message = ChatColor.translateAlternateColorCodes('&', message);
			message = message.replace("{player}", player);
			message = message.replace("{id}", String.valueOf(id));
			message = message.replace("{hack}", hack);
		}
		return message;
	}
	
	private void sendFormatted(ConfigurationSection cfg, String key, String player, int id, String hack) {
		String message = getFormatted(cfg, key, player, id, hack);
		if (message != null && message.length() > 0) {
			String perm = plugin.Config.get("options." + key + "_permission", "hacx." + key);
			sendMessage(perm, message);
		}
	}

	private void sendMessage(String permission, String message) {
		for (Player p : plugin.getServer().getOnlinePlayers()) {
			if (p.hasPermission(permission)) {
				p.sendMessage(message);
			}
		}
		
		if (bungeecord) {
	        ByteArrayDataOutput out = ByteStreams.newDataOutput();
	        
	        out.writeUTF("Forward");
	        out.writeUTF("ONLINE");
	        out.writeUTF("HACX");

	        ByteArrayDataOutput msg = ByteStreams.newDataOutput();
	        msg.writeUTF(permission + ":" + message);
	        out.writeShort(msg.toByteArray().length);
	        out.write(msg.toByteArray());
			for (Player p : plugin.getServer().getOnlinePlayers()) {
	        	p.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
	        	break;
			}
		}
	}

	@Override
	public void onPluginMessageReceived(String channel, Player player, byte[] message) {
		if (!channel.equals("BungeeCord")) {
			return;
		}
		ByteArrayDataInput in = ByteStreams.newDataInput(message);
		String subchannel = in.readUTF();

		if (subchannel.equals("HACX")) {
			int size = in.readShort();
			byte[] msgbytes = new byte[size];
			in.readFully(msgbytes);
			ByteArrayDataInput msg = ByteStreams.newDataInput(msgbytes);
			String cmd = msg.readUTF();
			int offset = cmd.indexOf(':');
			if (offset > 0) {
		        plugin.log(Level.INFO, "RECIEVE HACX: " + cmd);
		        String permission = cmd.substring(0, offset);
		        String txtMessage = cmd.substring(1 + offset);

				for (Player p : plugin.getServer().getOnlinePlayers()) {
					if (p.hasPermission(permission)) {
						p.sendMessage(txtMessage);
					}
				}
			}
		}
	}

}
