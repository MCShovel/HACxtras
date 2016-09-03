package com.steamcraftmc.HACxtras;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
 
public class MainPlugin extends JavaPlugin {
	public final   Logger  _logger;
	private WorldEvents _listener;
	public Boolean _exLogging;
	public final MainConfig Config;
	public com.steamcraftmc.HACxtras.Commands.CmdGod god;
	public com.steamcraftmc.HACxtras.Commands.CmdFireball fb;

	public MainPlugin() {
		_exLogging = true;
		_logger = getLogger();
		_logger.setLevel(Level.ALL);
		_logger.log(Level.CONFIG, "Plugin initializing...");
		
		Config = new MainConfig(this);
		Config.load();
	}

	public void log(Level level, String text) {
		_logger.log(Level.INFO, text);
	}

    @Override
    public void onEnable() {
        new com.steamcraftmc.HACxtras.Commands.CmdFeed(this);
        new com.steamcraftmc.HACxtras.Commands.CmdHeal(this);
        new com.steamcraftmc.HACxtras.Commands.CmdFixLight(this);
        new com.steamcraftmc.HACxtras.Commands.CmdFly(this);
        new com.steamcraftmc.HACxtras.Commands.CmdGameMode(this);
        new com.steamcraftmc.HACxtras.Commands.CmdBurn(this);
        new com.steamcraftmc.HACxtras.Commands.CmdLightning(this);
        fb = new com.steamcraftmc.HACxtras.Commands.CmdFireball(this);
        new com.steamcraftmc.HACxtras.Commands.CmdGC(this);
        god = new com.steamcraftmc.HACxtras.Commands.CmdGod(this);
        new com.steamcraftmc.HACxtras.Commands.CmdRepair(this);
        new com.steamcraftmc.HACxtras.Commands.CmdSpeed(this);

    	_listener = new WorldEvents(this);
        getServer().getPluginManager().registerEvents(_listener, this);
        fb.start();
        god.start();
        log(Level.INFO, "Plugin listening for events.");
    }

    @Override
    public void onDisable() {
        fb.stop();
        god.stop();
    	HandlerList.unregisterAll(_listener);
    }

}
