package com.spiny.mobscaler;

import java.io.Serializable;
import java.math.BigDecimal;

public class SerializablePlayerData implements Serializable {
	
	private static final long serialVersionUID = 1L;
	public BigDecimal multiplier;
	private boolean frozen;
	
	public SerializablePlayerData(BigDecimal multiplier, boolean frozen) {
		this.multiplier = multiplier;
		this.setFrozen(frozen);
	}

	public boolean isFrozen() {
		return frozen;
	}

	public void setFrozen(boolean frozen) {
		this.frozen = frozen;
	}
}
