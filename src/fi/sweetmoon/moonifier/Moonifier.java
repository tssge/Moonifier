package fi.sweetmoon.moonifier;

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
	
	@Override
	public void onEnable() {
		this.plugin = this;
		this.saveDefaultConfig();
		this.config = this.getConfig();
		getServer().getPluginManager().registerEvents(this, this);
		Moonifier.LOW_GRAVITY_WORLD = config.getString("moonworld");
		Moonifier.WORLD_BELOW = config.getString("dropworld");
		
		/*
		 * A hack to handle removal of potion bubbles without removing the actual effect
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
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onPlayerWorldChange(PlayerChangedWorldEvent e) {
		if (e.getPlayer().getWorld().getName().equals(LOW_GRAVITY_WORLD)) {
			e.getPlayer().addPotionEffect(potef, true);
		}
		
		if (e.getFrom().getName().equals(LOW_GRAVITY_WORLD)) {
			e.getPlayer().removePotionEffect(PotionEffectType.JUMP);
		}
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onDeath(PlayerRespawnEvent e) {
		final Player pl = e.getPlayer();
		
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
		if (e.getCause() == DamageCause.VOID) {
			pl.teleport(new Location(getServer().getWorld(WORLD_BELOW), 
					pl.getLocation().getX(), 
					400, 
					pl.getLocation().getZ(), 
					pl.getLocation().getYaw(), 
					pl.getLocation().getPitch()));
			e.setDamage(0);
		}
	
		if (e.getDamage() >= 4 && e.getCause() == DamageCause.FALL) {
			e.setDamage((e.getDamage() - 4));
		} else {
			e.setDamage(0);
		}
	}
	
}