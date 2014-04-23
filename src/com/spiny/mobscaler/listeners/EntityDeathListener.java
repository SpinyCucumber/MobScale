package com.spiny.mobscaler.listeners;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;

import com.spiny.mobscaler.main.MobScaler;
import com.spiny.mobscaler.main.RuntimePlayerData;
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
		
		event.setDroppedExp(event.getDroppedExp() * plugin.serializablePlayerData.get(((Player) killer).getUniqueId()).multiplier.intValue());
		RuntimePlayerData counter = plugin.runtimePlayerData.get((Player) killer);
		counter.count++;
		counter.timer = 5;
		if(counter.count > 1 && !plugin.serializablePlayerData.get(((Player) killer).getUniqueId()).isFrozen()) ((Player) killer).sendMessage(plugin.getMessage("MultiplierAlert", (Player) killer));
	}
}
