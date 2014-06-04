package com.spiny.mobscaler.listeners;

import java.math.BigDecimal;

import org.bukkit.GameMode;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityTargetEvent;

import com.spiny.mobscaler.main.MobScaler;
import com.spiny.mobscaler.main.RuntimePlayerData;

public class EntityTargetListener extends BasicListener<MobScaler> {
	
	@EventHandler
	public void onEntityTarget(EntityTargetEvent event) {
		if(!(event.getEntity() instanceof LivingEntity) || !(event.getTarget() instanceof Player)) return;
		LivingEntity mob = (LivingEntity) (event.getEntity());
		Player player = (Player) (event.getTarget());
		
		RuntimePlayerData data = plugin.rData(player);
		if(!data.scaledMobs.containsKey(mob) && player.getGameMode() != GameMode.CREATIVE) {
			BigDecimal h = BigDecimal.valueOf(Math.round(mob.getMaxHealth()));
			plugin.logDebug("Added " + mob.getType().name() + " to " + player.getName() + " with " + h + " base health.");
			plugin.updateMobAttributes(player, false);
		}
	}
}
