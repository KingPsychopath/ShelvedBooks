package me.conarnar.shelvedbooks;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class ShelvedBooks extends JavaPlugin implements Listener {
	private Map<Location, Inventory> shelves = new HashMap<Location, Inventory>();
	
	public void onEnable() {
		getServer().getPluginManager().registerEvents(this, this);
	}
	
	public void onDisable() {
		for (Location loc: shelves.keySet()) {
			File file = new File(getDataFolder(), "shelves" + File.separatorChar + loc.getWorld().getName() + ".yml");
			Inventory inv = shelves.get(loc);
			FileConfiguration config = YamlConfiguration.loadConfiguration(file);
			
			for (int slot = 0; slot < 9; slot++) {
				ItemStack stack = inv.getItem(slot);
				
				if (stack != null && stack.getType() != Material.AIR) {
					config.set(loc.getBlockX() + "." +  loc.getBlockY() + "." + loc.getBlockZ() + "." + slot, stack);
				}
			}
			
			config.set(loc.getBlockX() + "." +  loc.getBlockY() + "." + loc.getBlockZ() + ".filled", true);
			
			try {
				config.save(file);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerInteract(PlayerInteractEvent evt) {
		if (evt.getAction() == Action.RIGHT_CLICK_BLOCK && evt.getClickedBlock().getType() == Material.BOOKSHELF) {
			evt.getPlayer().openInventory(loadInventory(evt.getClickedBlock().getLocation()));
		}
	}
	
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onBlockBreak(BlockBreakEvent evt) {
		if (evt.getBlock().getType() == Material.BOOKSHELF) {
			for (ItemStack stack: loadInventory(evt.getBlock().getLocation())) {
				if (stack != null) {
					evt.getBlock().getWorld().dropItemNaturally(evt.getBlock().getLocation(), stack);
				}
			}
		}
	}
	
	private Inventory loadInventory(Location loc) {
		Inventory inv;
		if ((inv = shelves.get(loc)) != null) {
			return inv;
		}
		
		FileConfiguration config = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "shelves" + File.separatorChar + loc.getWorld().getName() + ".yml"));
		
		if (!config.getBoolean(loc.getBlockX() + "." +  loc.getBlockY() + "." + loc.getBlockZ() + ".filled")) return getDefaultInventory();
		
		inv = getServer().createInventory(null, 9, ChatColor.BLUE + "Bookshelf");
		
		for (int slot = 0; slot < 9; slot++) {
			ItemStack stack = config.getItemStack(loc.getBlockX() + "." +  loc.getBlockY() + "." + loc.getBlockZ() + "." + slot);
			
			if (stack != null) inv.setItem(slot, stack);
		}
		
		return null;
	}
	
	private Inventory getDefaultInventory() {
		Inventory inv = getServer().createInventory(null, 9, ChatColor.BLUE + "Bookshelf");
		
		for (int slot = 0; slot < 3; slot++) {
			inv.addItem(new ItemStack(Material.BOOK));
		}
		
		return inv;
	}
}
