package com.steamcraftmc.HACxtras.utils;

import java.util.*;
import java.util.Map.Entry;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import com.steamcraftmc.HACxtras.MainPlugin;

public class PlayerData {

	private final MainPlugin plugin;
	public final UUID uuid;
	public final String name;
	private final HashMap<String, Integer> violations;
	private Long quitTime;
	public boolean hasWarned, hasBranded;
	
	public PlayerData(MainPlugin plugin, Player player) {
		this.plugin = plugin;
		this.uuid = player.getUniqueId();
		this.name = player.getName();
		this.violations = new HashMap<String, Integer>();
		this.quitTime = null;
	}

	public void join() {
		quitTime = null;
	}

	public void quit() {
		quitTime = System.currentTimeMillis();
	}
	
	public int getCounter(String hack) {
		Integer counter = violations.get(hack);
		return counter == null ? 0 : counter;
	}
	
	public ConfigurationSection increment(String hack) {
		Integer counter = violations.get(hack);
		if (counter == null) counter = 0;
		counter += 1;
		violations.put(hack, counter);
		
		int warnLevel = plugin.Config.getInt("rules." + hack + ".warning_level", 1);
		if (counter < warnLevel || warnLevel < 0) {
			return null;
		}
		if (counter == warnLevel) {
			if (!hasWarned) {
				hasWarned = true;
				return plugin.getConfig().getConfigurationSection("warning");
			}
			return null;
		}

		int actLevel = plugin.Config.getInt("rules." + hack + ".action_level", 2);
		if (counter < actLevel || actLevel < 0) {
			return null;
		}
		if (counter == actLevel) {
			if (!hasBranded) {
				hasBranded = true;
				return plugin.getConfig().getConfigurationSection("action");
			}
			return null;
		}

		int banLevel = plugin.Config.getInt("rules." + hack + ".ban_level", 3);
		if (counter < banLevel || banLevel < 0) {
			return null;
		}

		return plugin.getConfig().getConfigurationSection("ban");
	}
	
	public boolean whitelisted() {
		return plugin.Store.getRaw(uuid + ".whitelisted") == "true";
	}
	
	public String getTeam() {
		return plugin.Store.getRaw(uuid + ".team");
	}

	public void setTeam(String team, String reason) {
		plugin.Store.Write(uuid + ".warn_reason", reason);
		plugin.Store.Write(uuid + ".time", System.currentTimeMillis());
		plugin.Store.Write(uuid + ".name", this.name);
		plugin.Store.Write(uuid + ".team", team);

		for (Entry<String, Integer> e : this.violations.entrySet()) {
			plugin.Store.Write(uuid + ".hack." + e.getKey(), e.getValue());
		}
	}
	
	public void banned(String msg) {
		plugin.Store.Write(uuid + ".banned_reason", msg);
		plugin.Store.Write(uuid + ".time", System.currentTimeMillis());
		plugin.Store.Write(uuid + ".name", this.name);

		for (Entry<String, Integer> e : this.violations.entrySet()) {
			plugin.Store.Write(uuid + ".hack." + e.getKey(), e.getValue());
		}
	}

	public boolean reduce() {
		ArrayList<String> remove = null;
		for (Entry<String, Integer> e : this.violations.entrySet()) {
			Integer val = e.getValue();
			if (val > 0) {
				e.setValue(val - 1);
			}
			else {
				if (remove == null) remove = new ArrayList<String>();
				remove.add(e.getKey());
			}
		}
		
		if (remove != null) {
			for (String k : remove) {
				this.violations.remove(k);
			}
		}
		
		if (this.violations.size() == 0 && this.quitTime != null && 
				(System.currentTimeMillis() - this.quitTime) > 300000) {
			return true;
		}
		
		return false;
	}
}
