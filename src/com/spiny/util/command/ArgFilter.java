package com.spiny.util.command;

import org.bukkit.command.CommandSender;

public interface ArgFilter {
	
	Object convert(String arg, CommandSender sender) throws ArgNotConvertableException;
	
}
