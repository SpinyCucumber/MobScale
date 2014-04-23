package com.spiny.mobscaler.listeners;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

public class BasicListener<T extends Plugin> implements Listener {
	
	private List<T> plugins = new ArrayList<T>();
	protected T plugin;
	
	public List<T> getPlugins() {
		return plugins;
	}
	
	public void listen(T p) {
		p.getServer().getPluginManager().registerEvents(this, p);
		plugin = p;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void newListener(Class<? extends BasicListener> clazz, Plugin... ps) {
		try {
			BasicListener l = clazz.newInstance();
			for(Plugin p : ps) l.listen(p);
		} catch (InstantiationException | IllegalAccessException e) {
			e.printStackTrace();
		}
	}
}
