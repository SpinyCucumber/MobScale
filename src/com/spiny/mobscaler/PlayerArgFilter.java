/**
 * 
 */
/**
 * @author Elijah
 *
 */

package com.spiny.mobscaler;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

import com.spiny.util.PlayerUtil;
import com.spiny.util.command.ArgFilter;
import com.spiny.util.command.ArgNotConvertableException;

public class PlayerArgFilter implements ArgFilter {

	@Override
	public Object convert(String arg, CommandSender sender) throws ArgNotConvertableException {
		OfflinePlayer p = PlayerUtil.getPlayerFromName(arg);
		if(p == null) throw new ArgNotConvertableException();
		return p;
	}
	
}