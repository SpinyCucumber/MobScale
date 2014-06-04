package com.spiny.util.command;

import java.util.ArrayList;
import java.util.Collections;

public class GateTestSet extends ArrayList<CommandData<?>.GateTest> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4463447567758229894L;
	
	public CommandData<?>.GateTest merge(boolean direction) {
		Collections.sort(this);
		return (direction) ? this.get(this.size() - 1) : this.get(0);
	}
	
}