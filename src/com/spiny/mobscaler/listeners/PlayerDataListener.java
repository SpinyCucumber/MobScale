package com.spiny.mobscaler.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import com.spiny.mobscaler.main.MobScaler;

public class PlayerDataListener extends BasicListener<MobScaler> {
	
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		plugin.initPlayerData(player);
		plugin.updateFrozen(player, player.getGameMode());
	}
	
	@EventHandler
	public void onPlayerGameModeChange(PlayerGameModeChangeEvent event) {
		plugin.updateFrozen(event.getPlayer(), event.getNewGameMode());
	}
}
