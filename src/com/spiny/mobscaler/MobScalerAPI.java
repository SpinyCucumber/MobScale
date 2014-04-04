package com.spiny.mobscaler;

import java.math.BigDecimal;

import org.bukkit.entity.Player;

public class MobScalerAPI {
	
	private static MobScaler plugin;
	
	public static void setPlugin(MobScaler newPlugin) {
		plugin = newPlugin;
	}
	
	//Add/subtract a certain amount from a player's multiplier, like when they hit a mob.
	public static void modMultiplierNatural(Player player, double amount) {
		plugin.modMultiplier(player, BigDecimal.valueOf(amount));
	}
	
	//Restrictive: Freeze if the given multiplier does not meet the criteria (Like when you invoke set command).
	public static void setMultiplier(Player player, double d, boolean restrictive) {
		if(restrictive) plugin.setMultiplier(player, d);
		else plugin.data(player).multiplier = BigDecimal.valueOf(d);
	}
	
	//Toggle a player's frozen value similar to the command.
	public static void toggleFrozenNatural(Player player) {
		plugin.toggleFrozen(player);
	}
	
	public static void setFrozen(Player player, boolean frozen) {
		plugin.data(player).setFrozen(frozen);
	}
	
	public static double getMultiplier(Player player) {
		return plugin.data(player).multiplier.doubleValue();
	}
	
	public static boolean getFrozen(Player player) {
		return plugin.data(player).isFrozen();
	}
}
