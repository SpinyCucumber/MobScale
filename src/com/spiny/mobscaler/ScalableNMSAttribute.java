package com.spiny.mobscaler;

import java.util.UUID;

import org.bukkit.craftbukkit.v1_7_R1.entity.CraftLivingEntity;
import org.bukkit.entity.LivingEntity;

import net.minecraft.server.v1_7_R1.AttributeInstance;
import net.minecraft.server.v1_7_R1.AttributeModifier;
import net.minecraft.server.v1_7_R1.EntityInsentient;
import net.minecraft.server.v1_7_R1.GenericAttributes;
import net.minecraft.server.v1_7_R1.IAttribute;

public enum ScalableNMSAttribute {

	MobScalerMovementSpeed(GenericAttributes.d, UUID.fromString("206a89dc-ae78-4c4d-b42c-3b31db3f5a7c")),
	MobScalerFollowRange(GenericAttributes.b, UUID.fromString("7bbe3bb1-079d-4150-ac6f-669e71550776"));

	private IAttribute attribute;
	private UUID uid;
	
	ScalableNMSAttribute(IAttribute attribute, UUID uid) {
		this.attribute = attribute;
		this.uid = uid;
	}
	
	public void apply(LivingEntity mob, double multiplier) {
		EntityInsentient nmsEntity = (EntityInsentient) ((CraftLivingEntity) mob).getHandle();
        AttributeInstance attributes = nmsEntity.getAttributeInstance(attribute);
        
        AttributeModifier modifier = new AttributeModifier(uid, name(), multiplier - 1, 1);
        
        attributes.b(modifier);
        attributes.a(modifier);
	}
	
	public void remove(LivingEntity mob) {
		EntityInsentient nmsEntity = (EntityInsentient) ((CraftLivingEntity) mob).getHandle();
        AttributeInstance attributes = nmsEntity.getAttributeInstance(attribute);
        AttributeModifier modifier = new AttributeModifier(uid, name(), 0, 1);
        
        attributes.b(modifier);
	}
}
