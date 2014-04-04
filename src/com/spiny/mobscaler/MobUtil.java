package com.spiny.mobscaler;

import java.math.BigDecimal;

import org.bukkit.entity.LivingEntity;

public class MobUtil {
	
	public static void scaleMaxHealth(LivingEntity mob, BigDecimal baseHealth, BigDecimal multiplier, boolean b) {
		BigDecimal health = multiplier.multiply(baseHealth);
		mob.setMaxHealth(health.doubleValue());
		if(!b) mob.setHealth(mob.getMaxHealth());
		else if(health.doubleValue() <= mob.getHealth()) mob.setHealth(health.doubleValue());
	}
}
