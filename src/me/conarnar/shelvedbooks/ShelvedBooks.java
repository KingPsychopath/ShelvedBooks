package me.conarnar.shelvedbooks;

import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public class ShelvedBooks extends JavaPlugin implements Listener {
	public void onEnable() {
		getServer().getPluginManager().registerEvents(this, this);
	}
	
	
}
