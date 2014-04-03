package com.spiny.mobscaler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import com.spiny.mobscaler.command.CommandData;
import com.spiny.mobscaler.command.CommandManager;

public class MobScaler extends JavaPlugin implements Listener {
	
	private Map<Player, RuntimePlayerData> runtimePlayerData = new HashMap<Player, RuntimePlayerData>();
	private Map<String, SerializablePlayerData> serializablePlayerData = new HashMap<String, SerializablePlayerData>();
	
	private File multiplierFile;
	
	private ConfigManager configManager;
	private CommandManager commandManager;
	
	EnumSet<EntityType> nonScalableMobs = EnumSet.of(EntityType.BAT, EntityType.CHICKEN, EntityType.COW, EntityType.PIG, EntityType.SHEEP, EntityType.HORSE, EntityType.OCELOT, EntityType.SQUID, EntityType.PLAYER);
	
	@SuppressWarnings("static-access")
	@Override
	public void onEnable() {
		getLogger().log(Level.INFO, commandManager.getTag() + "Initializing external command database and executors...");
		
		this.getServer().getPluginManager().registerEvents(this, this);
		
		if(!this.getDataFolder().exists()) this.getDataFolder().mkdir();
		
		multiplierFile = new File(getDataFolder(), "multipliers.bin");
		configManager = new ConfigManager(this, new String[]{"messages.yml"});
		initCommands();
		
		loadMultipliers();
		
		new BukkitRunnable() {
			public void run() {
				for(Entry<Player, RuntimePlayerData> entry : runtimePlayerData.entrySet()) {
					if(entry.getValue().timer > 0) entry.getValue().timer--;
					if(entry.getValue().timer == 0) entry.getValue().count = 0;
					for(Entry<LivingEntity, BigDecimal> mobEntry : entry.getValue().scaledMobs.entrySet()) {
						if(!isValid(mobEntry.getKey(), entry.getKey())) {
							entry.getValue().remove(mobEntry.getKey());
						}
					}
					entry.getValue().updateSets();
				}
			}
		}.runTaskTimer(this, 0, 20);
		
		logDebug("Initializing player data...");
		for(Player player : getServer().getOnlinePlayers()) {
			initPlayerData(player);
			updateFrozen(player, player.getGameMode());
		}
		
		logDebug("Setting executing instance to interactable plugin in MobScalerAPI...");
		MobScalerAPI.setPlugin(this);
	}
	
	@Override
	public void onDisable() {
		saveMultipliers();
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void initCommands() {
		commandManager = new CommandManager(this).withCommandData(
			new CommandData(CommandSender.class, "mobscale", "op", 0, "/mobscale"){
				public boolean execute(CommandSender sender, String label, Object[] args) {
					sender.sendMessage(ChatColor.GRAY + "Below is a list of all mobscale subcommands:");
					for(CommandData<?> subCommand : commandManager.getSubCommands("mobscale")) {
						if(subCommand.getUsage() != null) sender.sendMessage(ChatColor.GOLD + subCommand.getUsage());
					}
					return true;
				}
			}
		).withCommandData(
			new CommandData<Player>(Player.class, "mobscale info", "mobscale.info", 0, "/mobscale info [player]"){
				public boolean execute(Player sender, String label, Object[] args) {
					((Player) sender).sendMessage(getMessage("TellMultiplier", (Player) sender));
					return true;
				}
			}
		).withCommandData(
			new CommandData(CommandSender.class, "mobscale info", "mobscale.info.other", new boolean[]{true}, null){
				public boolean execute(CommandSender sender, String label, Object[] args) {
					sender.sendMessage(getMessage("TellMultiplierOther", (Player) args[0]));
					return true;
				}
			}
		).withCommandData(
			new CommandData<Player>(Player.class, "mobscale set", "mobscale.set", 1, "/mobscale set [player] <multiplier>"){
				public boolean execute(Player sender, String label, Object[] args) {
					try {
						setMultiplier(sender, Double.parseDouble((String) args[0]));
						sender.sendMessage(getMessage("TellUpdatedMultiplier", (Player) sender));
						updateMobAttributes(sender, false);
						return true;
					} catch(NumberFormatException e) {
						sender.sendMessage(this.getUsage());
						return false;
					}
				}
			}
		).withCommandData(
			new CommandData(CommandSender.class, "mobscale set", "mobscale.set.other", new boolean[]{true, false}, null){
				public boolean execute(CommandSender sender, String label, Object[] args) {
					try {
						setMultiplier((Player) args[0], Double.parseDouble((String) args[1]));
						sender.sendMessage(getMessage("TellUpdatedMultiplierOther", (Player) args[0]));
						if(configManager.getConfig("config").getBoolean("AlwaysNotify")) ((Player) args[0]).sendMessage(getMessage("TellUpdatedMultiplier", (Player) args[0]));
						updateMobAttributes((Player) args[0], false);
						return true;
					} catch(NumberFormatException e) {
						sender.sendMessage(this.getUsage());
						return false;
					}
				}
			}
		).withCommandData(
			new CommandData<Player>(Player.class, "mobscale freeze", "mobscale.freeze", 0, "/mobscale freeze [player]"){
				public boolean execute(Player sender, String label, Object[] args) {
					toggleFrozen(sender);
					sender.sendMessage(getMessage("TellUpdatedMultiplier", (Player) sender));
					return true;
				}
			}
		).withCommandData(
			new CommandData(CommandSender.class, "mobscale freeze", "mobscale.freeze.other", new boolean[]{true}, null){
				public boolean execute(CommandSender sender, String label, Object[] args) {
					toggleFrozen((Player) args[0]);
					sender.sendMessage(getMessage("TellUpdatedMultiplierOther", (Player) args[0]));
					if(configManager.getConfig("config").getBoolean("AlwaysNotify")) ((Player) args[0]).sendMessage(getMessage("TellUpdatedMultiplier", (Player) args[0]));
					return true;
				}
			}
		).withCommandData(
			new CommandData(CommandSender.class, "mobscale reload", "mobscale.reload", 0, "/mobscale reload"){
				public boolean execute(CommandSender sender, String label, Object[] args) {
					saveMultipliers();
					loadMultipliers();
					sender.sendMessage(getMessage("SuccessfulReload", null));
					return true;
				}
			}
		).withCommandData(
			new CommandData(CommandSender.class, "mobscale save", "mobscale.save", 0, "/mobscale save"){
				public boolean execute(CommandSender sender, String label, Object[] args) {
					saveMultipliers();
					sender.sendMessage(getMessage("SuccessfulSave", null));
					return true;
				}
			}
		);
	}
	
	private void initPlayerData(Player player) {
		if(!serializablePlayerData.containsKey(player.getName())) serializablePlayerData.put(player.getName(), new SerializablePlayerData(new BigDecimal(getConfig().getDouble("Default")), getConfig().getBoolean("Freeze")));
		runtimePlayerData.put(player, new RuntimePlayerData(false));
	}
	
	private void updateFrozen(Player player, GameMode gm) {
		SerializablePlayerData data = serializablePlayerData.get(player.getName());
		boolean oldFrozen = data.isFrozen();
		if(gm == GameMode.CREATIVE) data.setFrozen(true);
		else data.setFrozen(runtimePlayerData.get(player).getWasFrozen());
		if(oldFrozen != data.isFrozen() && configManager.getConfig("config").getBoolean("TellMultiplierIfChangedWithGamemode")) player.sendMessage(getMessage("TellUpdatedMultiplier", player));
		serializablePlayerData.put(player.getName(), data);
	}
	
	public String getMessage(String path, Player player) {
		
		String message = configManager.getConfig("messages").getString(path);
		if(player != null) {
			SerializablePlayerData data = serializablePlayerData.get(player.getName());
			message = message.replace(configManager.getConfig("messages").getString("MultiplierCode"), String.valueOf(data.multiplier));
			message = message.replace(configManager.getConfig("messages").getString("FrozenCode"), String.valueOf(data.isFrozen()));
			message = message.replace(configManager.getConfig("messages").getString("NameCode"), player.getName());
		}
		message = ChatColor.translateAlternateColorCodes(configManager.getConfig("messages").getString("ColorCode").charAt(0), message);
		
		return message;
	}
	
	@SuppressWarnings("unchecked")
	public void loadMultipliers() {
		logDebug("Loading multipliers...");
		try {
			if(!multiplierFile.createNewFile()) {
				try {
					ObjectInputStream stream = new ObjectInputStream(new FileInputStream(multiplierFile));
					serializablePlayerData = (HashMap<String, SerializablePlayerData>) (stream.readObject());
					stream.close();
				} catch(IOException | ClassNotFoundException e) {
					e.printStackTrace();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void saveMultipliers() {
		logDebug("Saving multipliers...");
		try {
			ObjectOutputStream stream = new ObjectOutputStream(new FileOutputStream(multiplierFile));
			stream.writeObject(serializablePlayerData);
			stream.flush();
			stream.close();
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		initPlayerData(player);
		updateFrozen(player, player.getGameMode());
	}
	
	@EventHandler
	public void onPlayerGameModeChange(PlayerGameModeChangeEvent event) {
		updateFrozen(event.getPlayer(), event.getNewGameMode());
	}
	
	@EventHandler
	public void onEntityTarget(EntityTargetEvent event) {
		if(!(event.getEntity() instanceof LivingEntity) || !(event.getTarget() instanceof Player)) return;
		LivingEntity mob = (LivingEntity) (event.getEntity());
		Player player = (Player) (event.getTarget());
		
		RuntimePlayerData data = runtimePlayerData.get(player);
		if(!data.scaledMobs.containsKey(mob) && player.getGameMode() != GameMode.CREATIVE) {
			BigDecimal h = BigDecimal.valueOf(Math.round(mob.getMaxHealth()));
			logDebug("Added " + mob.getType().name() + " to " + player.getName() + " with " + h + " base health.");
			data.scaledMobs.put(mob, h);
			updateMobAttributes(player, false);
		}
	}
	
	@EventHandler
	public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
		Entity entity = event.getEntity();
		Entity damager = event.getDamager();
		if(damager instanceof Projectile) damager = ((Projectile) damager).getShooter();
		if(!(entity instanceof Player) && !(damager instanceof Player)) return;
		double damage = Math.round(event.getDamage());
		if(damager instanceof Player && !nonScalableMobs.contains(entity.getType())) {
			modMultiplier((Player) damager, BigDecimal.valueOf(configManager.getConfig("config").getDouble("Dealt")).multiply(BigDecimal.valueOf(damage)));
			updateMobAttributes((Player) damager, true);
		}
		else if(entity instanceof Player && !nonScalableMobs.contains(damager.getType())) {
			modMultiplier((Player) entity, BigDecimal.valueOf(configManager.getConfig("config").getDouble("Dealt")).multiply(BigDecimal.valueOf(damage)).multiply(BigDecimal.valueOf(-1)));
			event.setDamage(damage * serializablePlayerData.get(((Player) entity).getName()).multiplier.doubleValue());
			updateMobAttributes((Player) entity, true);
		}
	}
	
	@EventHandler
	public void onEntityDeath(EntityDeathEvent event) {
		Entity entity = event.getEntity();
		EntityDamageEvent damageEvent = entity.getLastDamageCause();
		if(!(damageEvent instanceof EntityDamageByEntityEvent)) return;
		Entity killer = ((EntityDamageByEntityEvent) damageEvent).getDamager();
		if(killer instanceof Projectile) killer = ((Projectile) killer).getShooter();
		if(!(killer instanceof Player) || nonScalableMobs.contains(entity.getType())) return;
		
		event.setDroppedExp(event.getDroppedExp() * serializablePlayerData.get(((Player) killer).getName()).multiplier.intValue());
		RuntimePlayerData counter = runtimePlayerData.get((Player) killer);
		counter.count++;
		counter.timer = 5;
		if(counter.count > 1 && !serializablePlayerData.get(((Player) killer).getName()).isFrozen()) ((Player) killer).sendMessage(getMessage("MultiplierAlert", (Player) killer));
	}
	
	private void updateMobAttributes(Player player, boolean b) {
		logDebug("Updating attributes for " + player.getName());
		RuntimePlayerData data = runtimePlayerData.get(player);
		for(Entry<LivingEntity, BigDecimal> entry : data.scaledMobs.entrySet()) {
			updateAttributes(entry.getKey(), entry.getValue(), serializablePlayerData.get(player.getName()).multiplier, b);
		}
	}
	
	public static void updateAttributes(LivingEntity mob, BigDecimal baseHealth, BigDecimal multiplier, boolean b) {
		ScalableNMSAttribute.MobScalerMovementSpeed.apply(mob, multiplier.doubleValue());
		ScalableNMSAttribute.MobScalerFollowRange.apply(mob, multiplier.doubleValue());
		MobUtil.scaleMaxHealth(mob, baseHealth, multiplier, b);
	}

	public void setMultiplier(Player player, double d) {
		SerializablePlayerData data = serializablePlayerData.get(player.getName());
		data.multiplier = BigDecimal.valueOf(d);
		if(data.multiplier.doubleValue() < configManager.getConfig("config").getDouble("Minimum")
			|| data.multiplier.doubleValue() > configManager.getConfig("config").getDouble("Maximum")) {
			data.setFrozen(true);
		}
		serializablePlayerData.put(player.getName(), data);
		setWasFrozen(player, data.isFrozen());
	}
	
	public void modMultiplier(Player player, BigDecimal d) {
		double minimum = configManager.getConfig("config").getDouble("Minimum");
		double maximum = configManager.getConfig("config").getDouble("Maximum");
		SerializablePlayerData data = serializablePlayerData.get(player.getName());
		if(!data.isFrozen()) {
			if(data.multiplier.add(d).doubleValue() > maximum) data.multiplier = BigDecimal.valueOf(maximum);
			else if(data.multiplier.add(d).doubleValue() < minimum) data.multiplier = BigDecimal.valueOf(minimum);
			else data.multiplier = data.multiplier.add(d);
		}
	}
	
	public void toggleFrozen(Player player) {
		SerializablePlayerData data = serializablePlayerData.get(player.getName());
		data.setFrozen(!data.isFrozen());
		if(!data.isFrozen()) {
			if(data.multiplier.doubleValue() < configManager.getConfig("config.yml").getDouble("Minimum")
				|| data.multiplier.doubleValue() > configManager.getConfig("config.yml").getDouble("Maximum")) {
				data.multiplier = new BigDecimal(configManager.getConfig("config.yml").getDouble("Default"));
			}
		}
		serializablePlayerData.put(player.getName(), data);
		setWasFrozen(player, data.isFrozen());
	}
	
	private void setWasFrozen(Player player, boolean f) {
		RuntimePlayerData data = runtimePlayerData.get(player);
		data.setWasFrozen(f);
		runtimePlayerData.put(player, data);
	}
	
	public boolean isValid(LivingEntity mob, LivingEntity target) {
		boolean c = mob != null;
		if(c) {
			if(mob instanceof Creature) {
				if(((Creature) mob).getTarget() == null) c = false; 
				else c = ((Creature) mob).getTarget().equals(target);
			}
			if(mob.isDead()) c = false;
		}
		return c;
	}
	
	public SerializablePlayerData data(Player player) {
		return serializablePlayerData.get(player.getName());
	}
	
	public void logDebug(String log) {
		if(!configManager.getConfig("config").getBoolean("Debug")) return;
		getLogger().log(Level.INFO, log);
	}
}
