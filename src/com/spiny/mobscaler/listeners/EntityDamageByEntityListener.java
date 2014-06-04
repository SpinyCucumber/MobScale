package com.spiny.mobscaler.listeners;

import java.math.BigDecimal;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import com.spiny.mobscaler.main.MobScaler;
import com.spiny.util.entity.BasicEntityUtil.ShooterNotEntityException;
import com.spiny.util.entity.BasicEntityUtil;

public class EntityDamageByEntityListener extends BasicListener<MobScaler> {

	@EventHandler
	public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
		Entity entity = event.getEntity();
		Entity damager = event.getDamager();
		try {
			damager = BasicEntityUtil.projectileCheck(damager);
		} catch (ShooterNotEntityException e) {
			e.printStackTrace();
		}
		if(!(entity instanceof Player) && !(damager instanceof Player)) return;
		double damage = Math.round(event.getDamage());
		if(damager instanceof Player && !plugin.nonScalableMobs.contains(entity.getType())) {
			plugin.modMultiplier((Player) damager, BigDecimal.valueOf(plugin.configManager.getConfig("config").getDouble("Dealt")).multiply(BigDecimal.valueOf(damage)));
			plugin.updateMobAttributes((Player) damager, true);
		}
		else if(entity instanceof Player && !plugin.nonScalableMobs.contains(damager.getType())) {
			plugin.modMultiplier((Player) entity, BigDecimal.valueOf(plugin.configManager.getConfig("config").getDouble("Dealt")).multiply(BigDecimal.valueOf(damage)).multiply(BigDecimal.valueOf(-1)));
			event.setDamage(damage * plugin.sData((Player) entity).multiplier.doubleValue());
			plugin.updateMobAttributes((Player) entity, true);
		}
	}
}
