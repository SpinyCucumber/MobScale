package com.spiny.mobscaler.listeners;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;

import com.spiny.mobscaler.main.MobScaler;
import com.spiny.util.entity.BasicEntityUtil;
import com.spiny.util.entity.BasicEntityUtil.ShooterNotEntityException;

public class EntityDeathListener extends BasicListener<MobScaler> {

	@EventHandler
	public void onEntityDeath(EntityDeathEvent event) {
		Entity entity = event.getEntity();
		EntityDamageEvent damageEvent = entity.getLastDamageCause();
		if(!(damageEvent instanceof EntityDamageByEntityEvent)) return;
		Entity killer = ((EntityDamageByEntityEvent) damageEvent).getDamager();
		try {
			killer = BasicEntityUtil.projectileCheck(killer);
		} catch (ShooterNotEntityException e) {
			e.printStackTrace();
		}
		if(!(killer instanceof Player) || plugin.nonScalableMobs.contains(entity.getType())) return;
		
		event.setDroppedExp(event.getDroppedExp() * plugin.sData((OfflinePlayer) killer).multiplier.intValue());
	}
}
