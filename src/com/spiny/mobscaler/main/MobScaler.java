package com.spiny.mobscaler.main;

import java.io.File;
import java.io.FileNotFoundException;
import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.OfflinePlayer;
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
import com.spiny.mobscaler.command.PlayerArgFilter;
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
	
	public Map<OfflinePlayer, RuntimePlayerData> runtimePlayerData = new HashMap<OfflinePlayer, RuntimePlayerData>();
	public Map<UUID, SerializablePlayerData> serializablePlayerData = new HashMap<UUID, SerializablePlayerData>();
	
	private File multiplierFile;
	private File oldFile;
	
	public ConfigManager configManager;
	private CommandManager commandManager;
	
	private NameUUIDMapConverter<SerializablePlayerData> converter;
	
	//All passive mobs and ones that should not be considered scalable.
	public EnumSet<EntityType> nonScalableMobs = EnumSet.of(EntityType.BAT, EntityType.CHICKEN, EntityType.COW, EntityType.PIG, EntityType.SHEEP, EntityType.HORSE, EntityType.OCELOT, EntityType.SQUID, EntityType.PLAYER);
	
	@SuppressWarnings({ "unchecked" })
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
				for(Entry<OfflinePlayer, RuntimePlayerData> entry : runtimePlayerData.entrySet()) {
					for(Entry<LivingEntity, BigDecimal> mobEntry : entry.getValue().scaledMobs.entrySet()) {
						if(!isValid(mobEntry.getKey(), entry.getKey())) {
							entry.getValue().remove(mobEntry.getKey());
						}
					}
					entry.getValue().updateSets();
				}
			}
		}.runTaskTimer(this, 0, 20);
		
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
	private void initCommands() {
		commandManager = new CommandManager(this);
		commandManager.addCommandData(
			new CommandData<CommandSender>(CommandSender.class, "mobscaler", "mobscaler", "/mobscaler"){
				public boolean execute(CommandSender sender, String label, Object[] args) {
					sender.sendMessage(ChatColor.GRAY + "Below is a list of all mobscaler subcommands:");
					for(CommandData<?> subCommand : commandManager.getSubCommands("mobscaler")) {
						if(subCommand.getUsage() != null) sender.sendMessage(ChatColor.GOLD + subCommand.getUsage());
					}
					return true;
				}
			}
		);
		commandManager.addCommandData(
			new CommandData<Player>(Player.class, "mobscaler info", "mobscaler.info", "/mobscaler info [player]"){
				public boolean execute(Player sender, String label, Object[] args) {
					((Player) sender).sendMessage(getMessage("TellMultiplier", (OfflinePlayer) sender));
					return true;
				}
			}
		);
		commandManager.addCommandData(
			new CommandData<CommandSender>(CommandSender.class, "mobscaler info", "mobscaler.info.other", null, new PlayerArgFilter()){
				public boolean execute(CommandSender sender, String label, Object[] args) {
					sender.sendMessage(getMessage("TellMultiplierOther", (OfflinePlayer) args[0]));
					return true;
				}
			}
		);
		commandManager.addCommandData(
			new CommandData<Player>(Player.class, "mobscaler set", "mobscaler.set", "/mobscaler set [player] <multiplier>", 1){
				public boolean execute(Player sender, String label, Object[] args) {
					try {
						setMultiplier(sender, Double.parseDouble((String) args[0]));
						sender.sendMessage(getMessage("TellUpdatedMultiplier", (OfflinePlayer) sender));
						updateMobAttributes(sender, false);
						return true;
					} catch(NumberFormatException e) {
						sender.sendMessage(this.getUsage());
						return false;
					}
				}
			}
		);
		commandManager.addCommandData(
			new CommandData<CommandSender>(CommandSender.class, "mobscaler set", "mobscaler.set.other", null, new PlayerArgFilter(), null){
				public boolean execute(CommandSender sender, String label, Object[] args) {
					try {
						setMultiplier((OfflinePlayer) args[0], Double.parseDouble((String) args[1]));
						sender.sendMessage(getMessage("TellUpdatedMultiplierOther", (OfflinePlayer) args[0]));
						if(configManager.getConfig("config").getBoolean("AlwaysNotify") && args[0] instanceof Player) ((Player) args[0]).sendMessage(getMessage("TellUpdatedMultiplier", (Player) args[0]));
						updateMobAttributes((Player) args[0], false);
						return true;
					} catch(NumberFormatException e) {
						sender.sendMessage(this.getUsage());
						return false;
					}
				}
			}
		);
		commandManager.addCommandData(
			new CommandData<Player>(Player.class, "mobscaler freeze", "mobscaler.freeze", "/mobscaler freeze [player]"){
				public boolean execute(Player sender, String label, Object[] args) {
					toggleFrozen(sender);
					sender.sendMessage(getMessage("TellUpdatedMultiplier", (OfflinePlayer) sender));
					return true;
				}
			}
		);
		commandManager.addCommandData(
			new CommandData<CommandSender>(CommandSender.class, "mobscaler freeze", "mobscaler.freeze.other", null, new PlayerArgFilter()){
				public boolean execute(CommandSender sender, String label, Object[] args) {
					toggleFrozen((Player) args[0]);
					sender.sendMessage(getMessage("TellUpdatedMultiplierOther", (OfflinePlayer) args[0]));
					if(configManager.getConfig("config").getBoolean("AlwaysNotify") && args[0] instanceof Player) ((Player) args[0]).sendMessage(getMessage("TellUpdatedMultiplier", (Player) args[0]));
					return true;
				}
			}
		);
		commandManager.addCommandData(
			new CommandData<CommandSender>(CommandSender.class, "mobscaler reload", "mobscaler.reload", "/mobscaler reload"){
				public boolean execute(CommandSender sender, String label, Object[] args) {
					ObjectIOStreamUtil.save(multiplierFile, serializablePlayerData);
					configManager = new ConfigManager(MobScaler.this, new String[]{"messages.yml"});
					sender.sendMessage(getMessage("SuccessfulReload", null));
					return true;
				}
			}
		);
		commandManager.addCommandData(
			new CommandData<CommandSender>(CommandSender.class, "mobscaler save", "mobscaler.save", "/mobscaler save"){
				public boolean execute(CommandSender sender, String label, Object[] args) {
					ObjectIOStreamUtil.save(multiplierFile, serializablePlayerData);
					sender.sendMessage(getMessage("SuccessfulSave", null));
					return true;
				}
			}
		);
	}
	
	//Generic initialization of player data; called when data is null
	public SerializablePlayerData newSerializablePlayerData() {
	
		return new SerializablePlayerData(new BigDecimal(getConfig().getDouble("Default")), getConfig().getBoolean("Freeze"));

	}
	
	public RuntimePlayerData newRuntimePlayerData() {
		
		return new RuntimePlayerData(false);
		
	}
	
	public void updateFrozen(Player player, GameMode gm) {
		SerializablePlayerData data = sData(player);
		boolean oldFrozen = data.isFrozen();
		if(gm == GameMode.CREATIVE) data.setFrozen(true);
		else data.setFrozen(rData(player).getWasFrozen());
		if(oldFrozen != data.isFrozen() && configManager.getConfig("config").getBoolean("TellMultiplierIfChangedWithGamemode")) player.sendMessage(getMessage("TellUpdatedMultiplier", player));
	}
	
	public String getMessage(String path, OfflinePlayer args) {
		
		String message = configManager.getConfig("messages").getString(path);
		if(args != null) {
			SerializablePlayerData data = sData(args);
			message = message.replace(configManager.getConfig("messages").getString("MultiplierCode"), String.valueOf(data.multiplier));
			message = message.replace(configManager.getConfig("messages").getString("FrozenCode"), String.valueOf(data.isFrozen()));
			message = message.replace(configManager.getConfig("messages").getString("NameCode"), args.getName());
		}
		message = ChatColor.translateAlternateColorCodes(configManager.getConfig("messages").getString("ColorCode").charAt(0), message);
		
		return message;
	}
	
	public void updateMobAttributes(OfflinePlayer player, boolean b) {
		logDebug("Updating attributes for " + player.getName());
		RuntimePlayerData data = rData(player);
		for(Entry<LivingEntity, BigDecimal> entry : data.scaledMobs.entrySet()) {
			updateAttributes(entry.getKey(), entry.getValue(), sData(player).multiplier, b);
		}
	}
	
	public static void updateAttributes(LivingEntity mob, BigDecimal baseHealth, BigDecimal multiplier, boolean b) {
		ScalableNMSAttribute.MobScalerMovementSpeed.apply(mob, multiplier.doubleValue());
		ScalableNMSAttribute.MobScalerFollowRange.apply(mob, multiplier.doubleValue());
		MobUtil.scaleMaxHealth(mob, baseHealth, multiplier, b);
	}

	public void setMultiplier(OfflinePlayer player, double d) {
		SerializablePlayerData data = sData(player);
		data.multiplier = BigDecimal.valueOf(d);
		if(data.multiplier.doubleValue() < configManager.getConfig("config").getDouble("Minimum")
			|| data.multiplier.doubleValue() > configManager.getConfig("config").getDouble("Maximum")) {
			data.setFrozen(true);
		}
		setWasFrozen(player, data.isFrozen());
	}
	
	public void modMultiplier(OfflinePlayer player, BigDecimal d) {
		double minimum = configManager.getConfig("config").getDouble("Minimum");
		double maximum = configManager.getConfig("config").getDouble("Maximum");
		SerializablePlayerData data = sData(player);
		if(!data.isFrozen()) {
			if(data.multiplier.add(d).doubleValue() > maximum) data.multiplier = BigDecimal.valueOf(maximum);
			else if(data.multiplier.add(d).doubleValue() < minimum) data.multiplier = BigDecimal.valueOf(minimum);
			else data.multiplier = data.multiplier.add(d);
		}
	}
	
	public void toggleFrozen(OfflinePlayer player) {
		SerializablePlayerData data = sData(player);
		data.setFrozen(!data.isFrozen());
		if(!data.isFrozen()) {
			if(data.multiplier.doubleValue() < configManager.getConfig("config.yml").getDouble("Minimum")
				|| data.multiplier.doubleValue() > configManager.getConfig("config.yml").getDouble("Maximum")) {
				data.multiplier = new BigDecimal(configManager.getConfig("config.yml").getDouble("Default"));
			}
		}
		setWasFrozen(player, data.isFrozen());
	}
	
	private void setWasFrozen(OfflinePlayer player, boolean f) {
		RuntimePlayerData data = rData(player);
		data.setWasFrozen(f);
	}
	
	//Checks if a mob is fit for artificial augmentation by the system.
	public boolean isValid(LivingEntity mob, OfflinePlayer offlinePlayer) {
		boolean c = mob != null;
		if(c) {
			if(mob instanceof Creature) {
				if(((Creature) mob).getTarget() == null) c = false; 
				else c = ((Creature) mob).getTarget().equals(offlinePlayer);
			}
			if(mob.isDead()) c = false;
		}
		return c;
	}
	
	public SerializablePlayerData sData(OfflinePlayer p) {
		SerializablePlayerData d = serializablePlayerData.get(p.getUniqueId());
		if(d == null) {
			d = this.newSerializablePlayerData();
			serializablePlayerData.put(p.getUniqueId(), d);
		}
		return d;
	}
	
	public RuntimePlayerData rData(OfflinePlayer p) {
		RuntimePlayerData d = runtimePlayerData.get(p);
		if(d == null) {
			d = this.newRuntimePlayerData();
			runtimePlayerData.put(p, d);
		}
		return d;
	}
	
	public void logDebug(String log) {
		if(!configManager.getConfig("config").getBoolean("Debug")) return;
		getLogger().log(Level.INFO, log);
	}
	
}
