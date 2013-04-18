package fi.sweetmoon.moonifier;

//import java.util.logging.Logger;

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
	//private Logger log = Logger.getLogger("Moonifier");
	public Moonifier plugin;
	public FileConfiguration config;
	private static PotionEffect potef = new PotionEffect(PotionEffectType.JUMP, Integer.MAX_VALUE, 2);
	private static String world;
	private static String dropworld1;
	
	@Override
	public void onEnable() {
		this.plugin = this;
		this.saveDefaultConfig();
		this.config = this.getConfig();
		getServer().getPluginManager().registerEvents(this, this);
		Moonifier.world = config.getString("moonworld");
		Moonifier.dropworld1 = config.getString("dropworld");
		
		plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
			public void run() {
		      for (Player player : Moonifier.this.getServer().getOnlinePlayers())
		    	  if (player.getWorld().getName().equals(world)) {
		          	((CraftLivingEntity) player).getHandle().getDataWatcher().watch(8, Integer.valueOf(0));
		    	  }
			}
		}, 4L, 4L);
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onPlayerJoin(PlayerJoinEvent e) {
		//log.info("World is: " + e.getPlayer().getWorld().getName() + " and config world is: " + config.getString("world") + " and player is: " + e.getPlayer().getName());
		Player p = e.getPlayer();
		
		if (p.getWorld().getName().equals(world)) {
			//log.info("Passed the loginevent if loop.");
			p.addPotionEffect(potef, true);
		}
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onPlayerWorldChange(PlayerChangedWorldEvent e) {
		//log.info("World is: " + e.getPlayer().getWorld().getName() + " worldFrom is: " + e.getFrom().getName() + " and config world is: " + config.getString("world") + " and player is: " + e.getPlayer().getName());
		if (e.getPlayer().getWorld().getName().equals(world)) {
			//log.info("Passed the playerchangedworldevent if loop.");
			e.getPlayer().addPotionEffect(potef, true);
		}
		
		if (e.getFrom().getName().equals(world)) {
			//log.info("Passed the playerchangedworldevent if loop for removing potion.");
			e.getPlayer().removePotionEffect(PotionEffectType.JUMP);
		}
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onDeath(PlayerRespawnEvent e) {
		//log.info("PlayerRespawnEvent.");
		final Player pl = e.getPlayer();
		
		if (e.getPlayer().getWorld().getName().equals(world)) {
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
		} else if (!(e.getEntity().getWorld().getName().equals(world))) {
			return;
		}
		
		Player pl = (Player) e.getEntity();
		//log.info("Entity: " + ((Player) e.getEntity()).getName() + " Damage cause: " + e.getCause().toString());
		if (e.getCause() == DamageCause.VOID) {
			pl.teleport(new Location(getServer().getWorld(dropworld1), 
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