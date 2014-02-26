package me.conarnar.shelvedbooks;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class ShelvedBooks extends JavaPlugin implements Listener {
	private Map<Location, Inventory> shelves = new HashMap<Location, Inventory>();
	private List<Material> validItems;
	public void onEnable() {
		getServer().getPluginManager().registerEvents(this, this);
		
		if (getConfig().isSet("valid-items")) {
			validItems = new ArrayList<Material>();
			
			for (String string: getConfig().getStringList("valid-items")) {
				Material material = Material.getMaterial(string);
				
				if (material != null) {
					validItems.add(material);
				}
			}
		} else {
			validItems = Arrays.asList(Material.BOOK, Material.WRITTEN_BOOK, Material.BOOK_AND_QUILL, Material.PAPER, Material.MAP, Material.EMPTY_MAP);
		}
		
		List<String> list = new ArrayList<String>();
		
		for (Material material: validItems) {
			list.add(material.name());
		}
		
		getConfig().set("valid-items", list);
		saveConfig();
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
	
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onPlayerInteract(PlayerInteractEvent evt) {
		if (evt.getAction() == Action.RIGHT_CLICK_BLOCK && evt.getClickedBlock().getType() == Material.BOOKSHELF) {
			evt.getPlayer().openInventory(loadInventory(evt.getClickedBlock().getLocation()));
			evt.setCancelled(true);
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onBlockBreak(BlockBreakEvent evt) {
		if (evt.getBlock().getType() == Material.BOOKSHELF) {
			for (ItemStack stack: loadInventory(evt.getBlock().getLocation())) {
				if (stack != null) {
					evt.getBlock().getWorld().dropItemNaturally(evt.getBlock().getLocation(), stack);
				}
			}
			
			evt.setCancelled(true);
			evt.getBlock().setType(Material.AIR);
			
			for (Player player: evt.getBlock().getWorld().getPlayers()) {
				if (player != evt.getPlayer() && player.getLocation().distanceSquared(evt.getBlock().getLocation()) <= 4096) {
					player.playEffect(evt.getBlock().getLocation(), Effect.STEP_SOUND, Material.BOOKSHELF);
				}
			}
			
			unloadInventory(evt.getBlock().getLocation());
		}
	}
	
	@EventHandler
	public void onInventoryClick(InventoryClickEvent evt) {
		if (evt.getInventory().getTitle().equals(ChatColor.BLUE + "Bookshelf")) {
			switch (evt.getAction()) {
			case HOTBAR_SWAP:
				if (evt.getRawSlot() < 9 && !validItems.contains(evt.getWhoClicked().getInventory().getItem(evt.getHotbarButton()).getType())) {
					evt.setCancelled(true);
				}
				break;
			case MOVE_TO_OTHER_INVENTORY:
				if (evt.getRawSlot() > 8 && !validItems.contains(evt.getCurrentItem().getType())) {
					evt.setCancelled(true);
				}
				break;
			case PLACE_ALL:
			case PLACE_ONE:
			case PLACE_SOME:
			case SWAP_WITH_CURSOR:
				if (evt.getRawSlot() < 9 && !validItems.contains(evt.getCursor().getType())) {
					evt.setCancelled(true);
				}
				break;
			default:
				break;
			}
		}
	}
	
	@EventHandler
	public void onInventoryDrag(final InventoryDragEvent evt) {
		for (Integer i: evt.getRawSlots()) {
			if (i < 9) {
				if (!validItems.contains(evt.getNewItems().get(i).getType())) {
					evt.setCancelled(true);
					return;
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
		
		if (!config.getBoolean(loc.getBlockX() + "." +  loc.getBlockY() + "." + loc.getBlockZ() + ".filled")) {
			inv = getDefaultInventory();
			shelves.put(loc, inv);
			return inv;
		}
		
		inv = getServer().createInventory(null, 9, ChatColor.BLUE + "Bookshelf");
		
		for (int slot = 0; slot < 9; slot++) {
			ItemStack stack = config.getItemStack(loc.getBlockX() + "." +  loc.getBlockY() + "." + loc.getBlockZ() + "." + slot);
			
			if (stack != null) inv.setItem(slot, stack);
		}
		
		shelves.put(loc, inv);
		
		return inv;
	}
	
	private void unloadInventory(Location loc) {
		shelves.remove(loc);
		File file = new File(getDataFolder(), "shelves" + File.separatorChar + loc.getWorld().getName() + ".yml");
		FileConfiguration config = YamlConfiguration.loadConfiguration(file);
		config.set(loc.getBlockX() + "." +  loc.getBlockY() + "." + loc.getBlockZ(), null);
		
		try {
			config.save(file);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private Inventory getDefaultInventory() {
		Inventory inv = getServer().createInventory(null, 9, ChatColor.BLUE + "Bookshelf");
		
		for (int slot = 0; slot < 3; slot++) {
			inv.addItem(new ItemStack(Material.BOOK));
		}
		
		return inv;
	}
}
