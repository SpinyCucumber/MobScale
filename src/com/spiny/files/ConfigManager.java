package com.spiny.files;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

// Handy little class I wrote for managing custom configs.

public class ConfigManager {
	
	private JavaPlugin plugin;
	
	private Map<String, YamlConfiguration> configs = new TreeMap<String, YamlConfiguration>(new Comparator<String>()
	{
		public int compare(String o1, String o2) {
			if(o2.startsWith(o1)) return 0;
			return 1;
		}
	});
	
	public ConfigManager(JavaPlugin plugin, String[] configFiles) {
		this.plugin = plugin;
		plugin.getLogger().log(Level.INFO, "Initializing custom config database...");
		plugin.saveDefaultConfig();
		configs.put("config.yml", (YamlConfiguration) plugin.getConfig());
		for(String path : configFiles) {
			if(!getFile(path).exists()) copyDefaults(path);
			configs.put(path, YamlConfiguration.loadConfiguration(getFile(path)));
		}
	}
	
	private File getFile(String path) {
		return new File(plugin.getDataFolder() + File.separator + path);
	}
	
	public void copyDefaults(String path) {
		InputStream configStream = plugin.getResource(path);
		YamlConfiguration config = YamlConfiguration.loadConfiguration(configStream);
		try {
			config.save(getFile(path));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public YamlConfiguration getConfig(String path) {
		return configs.get(path);
	}
	
	public void saveConfigFromDatabase(String path) {
		try {
			configs.get(path).save(getFile(path));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
