package com.spiny.mobscaler.main;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bukkit.entity.LivingEntity;

import com.spiny.util.entity.ScalableNMSAttribute;

public class RuntimePlayerData {
	
	public int count;
	public int timer;
	
	private boolean wasFrozen;
	
	public Map<LivingEntity, BigDecimal> scaledMobs = new HashMap<LivingEntity, BigDecimal>();
	private Set<LivingEntity> removeMobs = new HashSet<LivingEntity>();
	
	public void updateSets() {
		scaledMobs.keySet().removeAll(removeMobs);
		removeMobs.clear();
	}
 	
	public RuntimePlayerData(boolean wasFrozen) {
		this.count = 0;
		this.timer = 0;
		this.wasFrozen = wasFrozen;
	}

	public boolean getWasFrozen() {
		return wasFrozen;
	}

	public void setWasFrozen(boolean wasFrozen) {
		this.wasFrozen = wasFrozen;
	}
	
	public void remove(LivingEntity mob) {
		removeMobs.add(mob);
		ScalableNMSAttribute.MobScalerMovementSpeed.remove(mob);
		ScalableNMSAttribute.MobScalerFollowRange.remove(mob);
		MobUtil.scaleMaxHealth(mob, scaledMobs.get(mob), BigDecimal.valueOf(1), true);
	}
}
