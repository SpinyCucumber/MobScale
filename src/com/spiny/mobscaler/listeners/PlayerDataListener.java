package com.spiny.mobscaler.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerGameModeChangeEvent;

import com.spiny.mobscaler.main.MobScaler;

public class PlayerDataListener extends BasicListener<MobScaler> {
	
	@EventHandler
	public void onPlayerGameModeChange(PlayerGameModeChangeEvent event) {
		plugin.updateFrozen(event.getPlayer(), event.getNewGameMode());
	}
	
}
