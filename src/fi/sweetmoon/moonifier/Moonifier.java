package fi.sweetmoon.moonifier;

import java.util.HashMap;
import java.util.List;

import org.bukkit.craftbukkit.v1_5_R3.entity.CraftLivingEntity;

import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class Moonifier extends JavaPlugin implements Listener {
	public Moonifier plugin;
	public FileConfiguration config;
	private static PotionEffect potef = new PotionEffect(PotionEffectType.JUMP, Integer.MAX_VALUE, 2);
	private static String LOW_GRAVITY_WORLD;
	HashMap<String,Integer> voidCounter = new HashMap<String,Integer>();
	private static List<String> WORLD_LIST;
	private static int DROP_HEIGHT;
	private static int VOID_DROP_HEIGHT;
	
	@Override
	public void onEnable() {
		this.plugin = this;
		
		// Config stuff
		this.saveDefaultConfig();
		this.config = this.getConfig();
		loadConfVars();
		
		getServer().getPluginManager().registerEvents(this, this);
		/*
		 * No idea how ((CraftLivingEntity) player).getHandle().getDataWatcher().watch(8, Integer.valueOf(0)); actually works.
		 * It's a hack to remove potion bubbles from players in "Moon"
		 * Using scheduler for these kind of things is kind of bad practice. Maybe there's a better way to do this?
		 */
		plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
			public void run() {
		      for (Player player : Moonifier.this.getServer().getOnlinePlayers())
		    	  if (player.getWorld().getName().equals(LOW_GRAVITY_WORLD)) {
		          	((CraftLivingEntity) player).getHandle().getDataWatcher().watch(8, Integer.valueOf(0));
		    	  }
			}
		}, 4L, 4L);
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onPlayerJoin(PlayerJoinEvent e) {
		Player p = e.getPlayer();
		
		if (p.getWorld().getName().equals(LOW_GRAVITY_WORLD)) {
			p.addPotionEffect(potef, true);
		}
		
		// We have to put player into voidCounter to avoid null stacktrace on the if clause in entityDamageEvent
		voidCounter.put(p.getName().toLowerCase(), 0);
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onPlayerQuit(PlayerQuitEvent e) {
		// Remove player so we don't cause memory leak or anything nasty
		voidCounter.remove(e.getPlayer().getName().toLowerCase());
	}
	
	/*
	 * Multiverse Invetories listens on this event at priority LOW
	 * We need to get the event first to cancel the potion effect, 
	 * so that it doesn't pass to worlds we don't want it to pass to
	 */
	@EventHandler(ignoreCancelled=true, priority=EventPriority.LOWEST)
	public void PlayerWorldChangeLowest(PlayerChangedWorldEvent e) {
		// Remove potion effect when player is not on "Moon"
		if (e.getFrom().getName().equals(LOW_GRAVITY_WORLD)) {
			e.getPlayer().removePotionEffect(PotionEffectType.JUMP);
		}
	}
	
	/*
	 * We want to apply the potion effect after Multiverse Invetories
	 * which listens on this event at priority LOW
	 */
	@EventHandler(ignoreCancelled=true, priority=EventPriority.NORMAL)
	public void PlayerWorldChangeNormal(PlayerChangedWorldEvent e) {
		// Add potion effect when player comes to "Moon"
		if (e.getPlayer().getWorld().getName().equals(LOW_GRAVITY_WORLD)) {
			e.getPlayer().addPotionEffect(potef, true);
		}
	}
	
	@EventHandler(ignoreCancelled=true)
	public void onDeath(PlayerRespawnEvent e) {
		final Player pl = e.getPlayer();
		
		/*
		 * Player doesn't actually respawn when this event is fired (Player object isn't created)
		 * This hack is a necessary workaround to apply the potion effect (1 tick wait after respawn event)
		 * Maybe there's a better way to do this?
		 */
	    plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
	        public void run() {
	        	/*
	        	 * Had to move the if here, because Bukkit's event is so bad that it won't event get the right world, so we need to add hacky delay
	        	 * Yes, me mad
	        	 */
	        	if (pl.getWorld().getName().equals(LOW_GRAVITY_WORLD)) {
	        		pl.addPotionEffect(potef, true);
	        	}
	        }
	    }, 1);	
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onEntityDamageEvent(EntityDamageEvent e) {
		if (!(e.getEntity() instanceof Player)) {
			return;
		}
		
		Player pl = (Player) e.getEntity();
		
		if(e.getCause() == DamageCause.VOID && WORLD_LIST.contains(e.getEntity().getWorld().getName())) {
			// Check so that the world is not the last one in the list (we don't want to teleport if it is)
			if(WORLD_LIST.indexOf(e.getEntity().getWorld().getName()) != (WORLD_LIST.size() - 1)) {
				// If player is 25 blocks below the void, tp them to the world below
				if(voidCounter.get(pl.getName().toLowerCase()).equals(VOID_DROP_HEIGHT)) {
					// Get the next world from the list, teleport player there (Could improve it a bit?)
					pl.teleport(new Location(getServer().getWorld(WORLD_LIST.get(WORLD_LIST.indexOf(e.getEntity().getWorld().getName())+1)), 
							pl.getLocation().getX(), 
							DROP_HEIGHT, 
							pl.getLocation().getZ(), 
							pl.getLocation().getYaw(), 
							pl.getLocation().getPitch()));
					voidCounter.put(pl.getName().toLowerCase(), 0);
					e.setCancelled(true);
				} else {
					voidCounter.put(pl.getName().toLowerCase(), 1+voidCounter.get(pl.getName().toLowerCase()));
					e.setCancelled(true);
				}
			}
		} 
			
		
		// Make fall damage scale correctly
		if (e.getEntity().getWorld().getName().equals(LOW_GRAVITY_WORLD) && e.getDamage() >= 4 && e.getCause() == DamageCause.FALL) {
			e.setDamage((e.getDamage() - 4));
		} else if(e.getEntity().getWorld().getName().equals(LOW_GRAVITY_WORLD) && e.getCause() == DamageCause.FALL) {
			e.setDamage(0);
		}
	}
	
	// Function to load config, if we implement reload command in the future
	private void loadConfVars() {
		WORLD_LIST = config.getStringList("World list");
		LOW_GRAVITY_WORLD = config.getString("moonworld");
		DROP_HEIGHT = config.getInt("Drop height");
		VOID_DROP_HEIGHT = config.getInt("Void drop height");
	}
}