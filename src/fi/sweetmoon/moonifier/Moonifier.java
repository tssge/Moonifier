package fi.sweetmoon.moonifier;

import java.util.HashMap;

import org.bukkit.craftbukkit.v1_5_R2.entity.CraftLivingEntity;

import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
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
	private static String WORLD_BELOW;
	HashMap<String,Integer> voidCounter = new HashMap<String,Integer>();
	
	@Override
	public void onEnable() {
		this.plugin = this;
		this.saveDefaultConfig();
		this.config = this.getConfig();
		getServer().getPluginManager().registerEvents(this, this);
		LOW_GRAVITY_WORLD = config.getString("moonworld");
		WORLD_BELOW = config.getString("dropworld");
		
		/*
		 * No idea how ((CraftLivingEntity) player).getHandle().getDataWatcher().watch(8, Integer.valueOf(0)); actually works.
		 * It's a hack to remove potion bubbles from players in "Moon"
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
	
	@EventHandler(ignoreCancelled = true)
	public void onPlayerWorldChange(PlayerChangedWorldEvent e) {
		
		// Add potion effect when player comes to "Moon"
		if (e.getPlayer().getWorld().getName().equals(LOW_GRAVITY_WORLD)) {
			e.getPlayer().addPotionEffect(potef, true);
		}
		
		// Remove potion effect when player is not on "Moon"
		if (e.getFrom().getName().equals(LOW_GRAVITY_WORLD)) {
			e.getPlayer().removePotionEffect(PotionEffectType.JUMP);
		}
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onDeath(PlayerRespawnEvent e) {
		final Player pl = e.getPlayer();
		
		/*
		 * Player doesn't actually respawn when this event is fired (Player object isn't created)
		 * This hack is a necessary workaround to apply the potion effect (1 tick wait after respawn event)
		 */
		if (e.getPlayer().getWorld().getName().equals(LOW_GRAVITY_WORLD)) {
		    plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
		        public void run() {             
		            pl.addPotionEffect(potef, true);
		        }
		    }, 1);
		}	
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onEntityDamageEvent(EntityDamageEvent e) {
		if (!(e.getEntity() instanceof Player)) {
			return;
		} else if (!(e.getEntity().getWorld().getName().equals(LOW_GRAVITY_WORLD))) {
			return;
		}
		
		Player pl = (Player) e.getEntity();
		
		if(e.getCause() == DamageCause.VOID) {
			if(voidCounter.get(pl.getName().toLowerCase()).equals(50)) {
				pl.teleport(new Location(getServer().getWorld(WORLD_BELOW), 
						pl.getLocation().getX(), 
						400, 
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
			
		
		// Make fall damage scale correctly
		if (e.getDamage() >= 4 && e.getCause() == DamageCause.FALL) {
			e.setDamage((e.getDamage() - 4));
		} else {
			e.setDamage(0);
		}
	}
	
}