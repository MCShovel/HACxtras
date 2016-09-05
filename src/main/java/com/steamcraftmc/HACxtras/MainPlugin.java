package com.steamcraftmc.HACxtras;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.steamcraftmc.HACxtras.utils.PlayerData;
 
public class MainPlugin extends JavaPlugin {
	public final   Logger  _logger;
	private WorldEvents _listener;
	public Boolean _exLogging;
	public final MainConfig Config;
	public final PlayerStorage Store;

	public MainPlugin() {
		_exLogging = true;
		_logger = getLogger();
		_logger.setLevel(Level.ALL);
		_logger.log(Level.CONFIG, "Plugin initializing...");
		
		Config = new MainConfig(this);
		Config.load();
		
		Store = new PlayerStorage(this);
		Store.load();
	}

	public void log(Level level, String text) {
		_logger.log(Level.INFO, text);
	}

    @Override
    public void onEnable() {
        new com.steamcraftmc.HACxtras.Commands.CmdHacx(this);

    	_listener = new WorldEvents(this);
    	_listener.start();
        log(Level.INFO, "Plugin listening for events.");
    }

    @Override
    public void onDisable() {
    	_listener.stop();
    }

	public void reload() {
    	_listener.stop();
    	Config.load();
    	Store.load();
    	_listener.start();
	}

	public PlayerData getPlayerByName(String name) {
		return _listener.getPlayer(name);
	}

}
