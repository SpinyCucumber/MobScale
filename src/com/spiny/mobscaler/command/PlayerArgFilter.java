package com.spiny.mobscaler.command;

import org.bukkit.OfflinePlayer;

import com.spiny.util.PlayerUtil;
import com.spiny.util.command.ArgFilter;
import com.spiny.util.command.ArgNotConvertableException;

public class PlayerArgFilter implements ArgFilter {

	@Override
	public Object convert(String arg) throws ArgNotConvertableException {
		OfflinePlayer p = PlayerUtil.getPlayerFromName(arg);
		if(p == null) throw new ArgNotConvertableException();
		return p;
	}

	@Override
	public String cannotConvert(String arg) {
		return "The player " + arg + " does not exist.";
	}
	
}
