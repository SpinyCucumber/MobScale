package com.spiny.mobscaler.main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Creature;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.spiny.nameuuidconverter.NameUUIDMapConverter;
import org.spiny.nameuuidconverter.NameUUIDMapConverter.NoConvertablesException;

import com.spiny.files.ConfigManager;
import com.spiny.mobscaler.PlayerArgFilter;
import com.spiny.mobscaler.listeners.BasicListener;
import com.spiny.mobscaler.listeners.EntityDamageByEntityListener;
import com.spiny.mobscaler.listeners.EntityDeathListener;
import com.spiny.mobscaler.listeners.EntityTargetListener;
import com.spiny.mobscaler.listeners.PlayerDataListener;
import com.spiny.util.ObjectIOStreamUtil;
import com.spiny.util.PlayerUtil;
import com.spiny.util.command.CommandData;
import com.spiny.util.command.CommandManager;
import com.spiny.util.entity.ScalableNMSAttribute;

public class MobScaler extends JavaPlugin implements Listener {
	
	public Map<Player, RuntimePlayerData> runtimePlayerData = new HashMap<Player, RuntimePlayerData>();
	public Map<UUID, SerializablePlayerData> serializablePlayerData = new HashMap<UUID, SerializablePlayerData>();
	
	private File multiplierFile;
	private File oldFile;
	
	public ConfigManager configManager;
	private CommandManager commandManager;
	
	private NameUUIDMapConverter<SerializablePlayerData> converter;
	
	//All passive mobs and ones that should not be considered scalable.
	public EnumSet<EntityType> nonScalableMobs = EnumSet.of(EntityType.BAT, EntityType.CHICKEN, EntityType.COW, EntityType.PIG, EntityType.SHEEP, EntityType.HORSE, EntityType.OCELOT, EntityType.SQUID, EntityType.PLAYER);
	
	@SuppressWarnings({ "static-access", "unchecked" })
	@Override
	public void onEnable() {
		
		this.getServer().getPluginManager().registerEvents(this, this);
		
		if(!this.getDataFolder().exists()) this.getDataFolder().mkdir();
		
		configManager = new ConfigManager(this, new String[]{"messages.yml"});
		
		logDebug("Loading files...");
		multiplierFile = new File(getDataFolder(), configManager.getConfig("config").getString("NewMultiplierFile"));
		oldFile = new File(getDataFolder(), configManager.getConfig("config").getString("OldMultiplierFile"));
		
		//The initialization of the listener classes and converter as well as the APIs follows:
		
		logDebug("Creating listeners...");
		BasicListener.newListener(EntityDamageByEntityListener.class, this);
		BasicListener.newListener(EntityDeathListener.class, this);
		BasicListener.newListener(EntityTargetListener.class, this);
		BasicListener.newListener(PlayerDataListener.class, this);
		
		logDebug("Starting converter...");
		try {
			converter = new NameUUIDMapConverter<SerializablePlayerData>(serializablePlayerData, oldFile, multiplierFile, HashMap.class);
		} catch (NoConvertablesException e) {
			logDebug("There is nothing to convert, converter is null.");
		}
		
		PlayerUtil.setServer(this.getServer());
		
		logDebug("Setting executing instance to interactable plugin in MobScalerAPI...");
		MobScalerAPI.setPlugin(this);
		
		
		logDebug("Loading multipliers...");
		try {
			serializablePlayerData = (Map<UUID, SerializablePlayerData>) ObjectIOStreamUtil.load(multiplierFile);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		//Internal timer for removing scaled mobs from player data classes (does not actually update attributes) and managing the alarm timer.
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
		
		logDebug(commandManager.getTag() + "Initializing external command database and executors...");
		
		initCommands();
	}
	
	@Override
	public void onDisable() {
		logDebug("Saving multipliers...");
		ObjectIOStreamUtil.save(multiplierFile, serializablePlayerData);
		try {
			converter.save();
		} catch (NullPointerException e) {
			logDebug("Converter has nothing to convert...");
		}
	}
	
	//The initialization of the commands correlated with the 'CommandManager' class follows: (called in onEnable)
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void initCommands() {
		commandManager = new CommandManager(this).withCommandData(
			new CommandData("mobscaler", "op", 0, "/mobscaler"){
				public boolean execute(CommandSender sender, String label, Object[] args) {
					sender.sendMessage(ChatColor.GRAY + "Below is a list of all mobscaler subcommands:");
					for(CommandData<?> subCommand : commandManager.getSubCommands("mobscaler")) {
						if(subCommand.getUsage() != null) sender.sendMessage(ChatColor.GOLD + subCommand.getUsage());
					}
					return true;
				}
			}
		).withCommandData(
			new CommandData<Player>("mobscaler info", "mobscaler.info", 0, "/mobscaler info [player]"){
				public boolean execute(Player sender, String label, Object[] args) {
					((Player) sender).sendMessage(getMessage("TellMultiplier", (Player) sender));
					return true;
				}
			}
		).withCommandData(
			new CommandData("mobscaler info", "mobscaler.info.other", null, new PlayerArgFilter()){
				public boolean execute(CommandSender sender, String label, Object[] args) {
					sender.sendMessage(getMessage("TellMultiplierOther", (Player) args[0]));
					return true;
				}
			}
		).withCommandData(
			new CommandData<Player>("mobscaler set", "mobscaler.set", 1, "/mobscaler set [player] <multiplier>"){
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
			new CommandData("mobscaler set", "mobscaler.set.other", null, new PlayerArgFilter(), null){
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
			new CommandData<Player>("mobscaler freeze", "mobscaler.freeze", 0, "/mobscaler freeze [player]"){
				public boolean execute(Player sender, String label, Object[] args) {
					toggleFrozen(sender);
					sender.sendMessage(getMessage("TellUpdatedMultiplier", (Player) sender));
					return true;
				}
			}
		).withCommandData(
			new CommandData("mobscaler freeze", "mobscaler.freeze.other", null, new PlayerArgFilter()){
				public boolean execute(CommandSender sender, String label, Object[] args) {
					toggleFrozen((Player) args[0]);
					sender.sendMessage(getMessage("TellUpdatedMultiplierOther", (Player) args[0]));
					if(configManager.getConfig("config").getBoolean("AlwaysNotify")) ((Player) args[0]).sendMessage(getMessage("TellUpdatedMultiplier", (Player) args[0]));
					return true;
				}
			}
		).withCommandData(
			new CommandData("mobscaler reload", "mobscaler.reload", 0, "/mobscaler reload"){
				public boolean execute(CommandSender sender, String label, Object[] args) {
					try {
						serializablePlayerData = (Map<UUID, SerializablePlayerData>) ObjectIOStreamUtil.load(multiplierFile);
					} catch (FileNotFoundException e) {
						try {
							multiplierFile.createNewFile();
						} catch (IOException e1) {
							e1.printStackTrace();
						}
					}
					sender.sendMessage(getMessage("SuccessfulReload", null));
					return true;
				}
			}
		).withCommandData(
			new CommandData("mobscaler save", "mobscaler.save", 0, "/mobscaler save"){
				public boolean execute(CommandSender sender, String label, Object[] args) {
					ObjectIOStreamUtil.save(multiplierFile, serializablePlayerData);
					sender.sendMessage(getMessage("SuccessfulSave", null));
					return true;
				}
			}
		);
	}
	
	//Generic initialization of player data; called on joins and enables.
	public void initPlayerData(Player player) {
		try {
			converter.update(player);
		} catch (NullPointerException e) {
			logDebug("Converter has nothing to convert...");
		}
		if(!serializablePlayerData.containsKey(player.getUniqueId())) serializablePlayerData.put(player.getUniqueId(), new SerializablePlayerData(new BigDecimal(getConfig().getDouble("Default")), getConfig().getBoolean("Freeze")));
		runtimePlayerData.put(player, new RuntimePlayerData(false));
	}
	
	public void updateFrozen(Player player, GameMode gm) {
		SerializablePlayerData data = serializablePlayerData.get(player.getUniqueId());
		boolean oldFrozen = data.isFrozen();
		if(gm == GameMode.CREATIVE) data.setFrozen(true);
		else data.setFrozen(runtimePlayerData.get(player).getWasFrozen());
		if(oldFrozen != data.isFrozen() && configManager.getConfig("config").getBoolean("TellMultiplierIfChangedWithGamemode")) player.sendMessage(getMessage("TellUpdatedMultiplier", player));
		serializablePlayerData.put(player.getUniqueId(), data);
	}
	
	public String getMessage(String path, Player player) {
		
		String message = configManager.getConfig("messages").getString(path);
		if(player != null) {
			SerializablePlayerData data = serializablePlayerData.get(player.getUniqueId());
			message = message.replace(configManager.getConfig("messages").getString("MultiplierCode"), String.valueOf(data.multiplier));
			message = message.replace(configManager.getConfig("messages").getString("FrozenCode"), String.valueOf(data.isFrozen()));
			message = message.replace(configManager.getConfig("messages").getString("NameCode"), player.getName());
		}
		message = ChatColor.translateAlternateColorCodes(configManager.getConfig("messages").getString("ColorCode").charAt(0), message);
		
		return message;
	}
	
	public void updateMobAttributes(Player player, boolean b) {
		logDebug("Updating attributes for " + player.getName());
		RuntimePlayerData data = runtimePlayerData.get(player);
		for(Entry<LivingEntity, BigDecimal> entry : data.scaledMobs.entrySet()) {
			updateAttributes(entry.getKey(), entry.getValue(), serializablePlayerData.get(player.getUniqueId()).multiplier, b);
		}
	}
	
	public static void updateAttributes(LivingEntity mob, BigDecimal baseHealth, BigDecimal multiplier, boolean b) {
		ScalableNMSAttribute.MobScalerMovementSpeed.apply(mob, multiplier.doubleValue());
		ScalableNMSAttribute.MobScalerFollowRange.apply(mob, multiplier.doubleValue());
		MobUtil.scaleMaxHealth(mob, baseHealth, multiplier, b);
	}

	public void setMultiplier(Player player, double d) {
		SerializablePlayerData data = serializablePlayerData.get(player.getUniqueId());
		data.multiplier = BigDecimal.valueOf(d);
		if(data.multiplier.doubleValue() < configManager.getConfig("config").getDouble("Minimum")
			|| data.multiplier.doubleValue() > configManager.getConfig("config").getDouble("Maximum")) {
			data.setFrozen(true);
		}
		setWasFrozen(player, data.isFrozen());
	}
	
	public void modMultiplier(Player player, BigDecimal d) {
		double minimum = configManager.getConfig("config").getDouble("Minimum");
		double maximum = configManager.getConfig("config").getDouble("Maximum");
		SerializablePlayerData data = serializablePlayerData.get(player.getUniqueId());
		if(!data.isFrozen()) {
			if(data.multiplier.add(d).doubleValue() > maximum) data.multiplier = BigDecimal.valueOf(maximum);
			else if(data.multiplier.add(d).doubleValue() < minimum) data.multiplier = BigDecimal.valueOf(minimum);
			else data.multiplier = data.multiplier.add(d);
		}
	}
	
	public void toggleFrozen(Player player) {
		SerializablePlayerData data = serializablePlayerData.get(player.getUniqueId());
		data.setFrozen(!data.isFrozen());
		if(!data.isFrozen()) {
			if(data.multiplier.doubleValue() < configManager.getConfig("config.yml").getDouble("Minimum")
				|| data.multiplier.doubleValue() > configManager.getConfig("config.yml").getDouble("Maximum")) {
				data.multiplier = new BigDecimal(configManager.getConfig("config.yml").getDouble("Default"));
			}
		}
		setWasFrozen(player, data.isFrozen());
	}
	
	private void setWasFrozen(Player player, boolean f) {
		RuntimePlayerData data = runtimePlayerData.get(player);
		data.setWasFrozen(f);
		runtimePlayerData.put(player, data);
	}
	
	//Checks if a mob is fit for artificial augmentation by the system.
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
