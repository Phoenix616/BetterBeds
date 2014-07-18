package de.themoep.BetterBeds;

import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class BetterBeds extends JavaPlugin implements Listener {
	
	private int minPlayers = 2;
	private double sleepPercentage = 0.5;
	private HashMap<UUID,HashSet<UUID>> asleepPlayers = new HashMap<UUID,HashSet<UUID>>();
	private String leaveMessage = "Player {player} is no longer sleeping. {sleeping}/{online} ({percentage}%)";
	private String sleepMessage = "Player {player} is now sleeping. {sleeping}/{online} ({percentage}%)";
	private String wakeMessage = "Wakey, wakey, rise and shine...Good Morning everyone!";
	
	public void onEnable() {
		this.saveDefaultConfig();
		this.loadConfig();	
		
	}

	/**
	 * Reload command's method
	 */
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) throws NumberFormatException {
		if(cmd.getName().equalsIgnoreCase("betterbedsreload") || cmd.getName().equalsIgnoreCase("bbreload")) {
			sender.sendMessage("[BetterBeds] Reloading Config");
			this.loadConfig();
		}
		return false;		
	}
	
	/**
	 * Loads the options from the config file into the plugins variables
	 */
	private void loadConfig() {
		this.reloadConfig();
		this.minPlayers = this.getConfig().getInt("minPlayers");
		this.getLogger().log(Level.INFO, "Min. Players: " + Integer.toString(this.minPlayers));
		try {
			this.sleepPercentage = Double.parseDouble(this.getConfig().getString("sleepPercentage").replaceAll(" ", "").replace("%", ""));
		} catch (NumberFormatException e) {
			this.getLogger().log(Level.WARNING,"You have an Error in your config at the sleepPercentage-node! Using the default now: " + this.sleepPercentage);
		} 

		if (this.sleepPercentage > 1) {
			this.sleepPercentage = this.sleepPercentage / 100;
		}
		this.getLogger().log(Level.INFO, "Sleep Percentage: " + Double.toString(this.sleepPercentage));
		
		this.sleepMessage = this.getConfig().getString("msg.sleep");
		this.getLogger().log(Level.INFO, "Sleep Message: " + this.sleepMessage);
		
		this.leaveMessage = this.getConfig().getString("msg.leave");
		this.getLogger().log(Level.INFO, "Bedlaeve Message: " + this.leaveMessage);
		
		this.wakeMessage = this.getConfig().getString("msg.wake");	
		this.getLogger().log(Level.INFO, "Wake Message: " + this.wakeMessage);	
	}
	
	/**
	 * Calculate if number of sleeping players is enough to fast forward the night. 
	 * TODO: The calculation should really be its own method because we don't yet check if there are enough players sleeping when some players quit the game/change the world.
	 * @param event PlayerBedEnterEvent
	 */
	@EventHandler
	public void onPlayerBedEnter(PlayerBedEnterEvent event) {
		if(event.isCancelled() || event.getPlayer().hasPermission("betterbeds.ignore")) return;

		World world = event.getBed().getWorld();
		
		int calculatedPlayers = 0;
		for(Player p: world.getPlayers()) {
			if(!p.hasPermission("betterbeds.sleep") && !p.isSleeping() && event.getPlayer().hasPermission("betterbeds.sleep")) {
				event.setCancelled(true);
				event.getPlayer().sendMessage("You may not rest now, there may be ghosts nearby");
				return;
			}
			if(!p.hasPermission("betterbeds.ignore"))
				calculatedPlayers++;
			
		}
		
		HashSet<UUID> playerList = new HashSet<UUID>();
		
		if(this.asleepPlayers.containsKey(world.getUID())) {
			playerList = this.asleepPlayers.get(world.getUID());
		}
		
		playerList.add(event.getPlayer().getUniqueId());
		
		this.asleepPlayers.put(world.getUID(), playerList);
		String msg = this.buildMsg(this.sleepMessage,event.getPlayer().getName(),playerList.size(),calculatedPlayers);
		
		for(UUID playerid : playerList) {
			if(this.getServer().getPlayer(playerid) != null && this.getServer().getPlayer(playerid).isOnline()) 
				this.getServer().getPlayer(playerid).sendMessage(ChatColor.GOLD + msg);
		}

		this.getLogger().log(Level.INFO, event.getPlayer().getName() + " sleeps now. " + playerList.size() + "/" + calculatedPlayers + " players are asleep in world " + world.getName());
		
		this.checkPlayers(world);
		
		
	}
	
	
	/**
	 * Check if enough players are asleep and fast forward if so.
	 * @param world The world to calculate with
	 */
	private boolean checkPlayers(World world) {		
		
		int calculatedPlayers = 0;
		for(Player p: world.getPlayers()) {
			if(!p.hasPermission("betterbeds.sleep") && !p.isSleeping()) {
				return false;
			}
			if(!p.hasPermission("betterbeds.ignore"))
				calculatedPlayers++;
			
		}
		
		HashSet<UUID> playerList = this.asleepPlayers.get(world.getUID());
		if((playerList.size() >= minPlayers && playerList.size() >= calculatedPlayers * this.sleepPercentage) || (playerList.size() < minPlayers && playerList.size() >= calculatedPlayers)) {
			this.getLogger().log(Level.INFO, "Set time to dawn in world " + world.getName());
			for(UUID playerid : playerList) {
				if(this.getServer().getPlayer(playerid) != null && this.getServer().getPlayer(playerid).isOnline()) {
					this.getServer().getPlayer(playerid).sendMessage(ChatColor.GOLD + this.wakeMessage);
				}
			}
			
			playerList.clear();

			this.asleepPlayers.put(world.getUID(), playerList);
			
			world.setTime(23450);
			if(world.hasStorm())
				world.setStorm(false);
			
			if(world.isThundering())
				world.setThundering(false);
			return true;
		}
		return false;
		
	}

	/**
	 * Recalculates the number of sleeping players if a player leaves his bed
	 * @param event PlayerBedLeaveEvent
	 */
	@EventHandler
	public void onPlayerBedLeave(PlayerBedLeaveEvent event) {
		World world = event.getBed().getWorld();

		this.calculateBedleave(event.getPlayer(),world);
	}	

	/**
	 * Recalculates the number of sleeping players if a player quits the game between 12500 and 100 time ticks
	 * @param event PlayerQuitEvent
	 */
	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		World world = event.getPlayer().getWorld();
		if(world.getEnvironment() == Environment.NORMAL && world.getTime() >= 12500 && world.getTime() <= 100) {
			this.calculateBedleave(event.getPlayer(),world);
			this.checkPlayers(world);
		}
	}
	
	/**
	 * Recalculates the number of sleeping players if a player changes from a normal world between 12500 and 100 time ticks
	 * @param event PlayerChangedWorldEvent
	 */
	@EventHandler
	public void onWorldChange(PlayerChangedWorldEvent event) {
		World world = event.getFrom();
		if(world.getEnvironment() == Environment.NORMAL && world.getTime() >= 12500 && world.getTime() <= 100) {
			this.calculateBedleave(event.getPlayer(),world);
			this.checkPlayers(world);
		}
	}
	
	/**
	 * Calculates what happens when a player leaves the bed. 
	 * 
	 * @param player The player who left the bed
	 * @param world The world the bed was in (because it's possible the player isn't there anymore when he existed it)
	 */
	private void calculateBedleave(Player player, World world) {
		
		if(!this.asleepPlayers.containsKey(world.getUID()) || player.hasPermission("betterbeds.ignore")) return;		
		
		if(this.asleepPlayers.get(world.getUID()).contains(player.getUniqueId())) {			
			int calculatedPlayers = 0;
			for(Player p: world.getPlayers()) {
				if(!p.hasPermission("betterbeds.ignore") && p.hasPermission("betterbeds.sleep"))
					calculatedPlayers++;			
			}

			this.asleepPlayers.get(world.getUID()).remove(player.getUniqueId());
	
			HashSet<UUID> playerList = this.asleepPlayers.get(world.getUID());
	
			this.getLogger().log(Level.INFO, "[BetterBeds] " + player.getName() + " is not sleeping anymore. " + playerList.size() + "/" + calculatedPlayers + " players are asleep in world " + world.getName());
			
			String msg =  this.buildMsg(this.leaveMessage,player.getName(),playerList.size(),calculatedPlayers);			
			
			for(UUID playerid : this.asleepPlayers.get(world.getUID())) {
				if(this.getServer().getPlayer(playerid) != null && this.getServer().getPlayer(playerid).isOnline()) 
					this.getServer().getPlayer(playerid).sendMessage(ChatColor.GOLD + msg);
			}
		}
		
	}
	
	
	/**
	 * Converts eventual parameters in a message into its real values.
	 * TODO: Make it so that not every parameter is required!
	 * @param msg String of the message to convert 
	 * @param playername String of the playername to insert in the message
	 * @param sleeping Integer of sleeping players
	 * @param online Integer of online players in world
	 * @return String of the converted message
	 */
	private String buildMsg(String msg, String playername, int sleeping, int online) {
		if(playername != null) 
			msg = msg.replaceAll("{player}", ChatColor.RED + playername + ChatColor.GOLD);
		msg = msg.replaceAll("{sleeping}", sleeping + "");
		msg = msg.replaceAll("{online}", online + "");
		float percentage = (float) Math.round( ( (double) sleeping / online * 100 * 100 ) / 100 );
		msg = msg.replaceAll("{percentage}", String.format("%.2f", percentage));
		return msg;
	}
}