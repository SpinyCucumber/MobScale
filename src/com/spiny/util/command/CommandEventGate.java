package com.spiny.util.command;

import org.bukkit.ChatColor;

import com.spiny.util.command.CommandManager.CommandEvent;

public enum CommandEventGate {
	
	PERMISSION{
		public CommandData<?>.GateTest justify(CommandEvent e, CommandData<?> d) {
			return d.new GateTest(2, PERM_MESSAGE, !e.getSender().hasPermission(d.getPermission()));
		}
	},
	REQUIRED_ARGS{
		public CommandData<?>.GateTest justify(CommandEvent e, CommandData<?> d) {
			int an = d.getArgNumber();
			String args = e.getTotalCommand().replace((an > 0) ? d.getKey() + " " : d.getKey(), "");
			String[] sArgs = (args.length() == 0) ? new String[0] : args.split(" ");
			int l = sArgs.length;
			System.out.println("REQUIRED_ARGS broke " + args + " off of " + d.getKey() + " and " + e.getTotalCommand() + ", " + l + " in length");
			if(l != an) return d.new GateTest(3, "Usage: " + d.getUsage(), true);
			Object[] oArgs = new Object[l];
			System.arraycopy(sArgs, 0, oArgs, 0, l);
			ArgFilter[] filters = d.getArgs();
			for(int i = 0; i < filters.length; i++) {
				ArgFilter af = filters[i];
				if(af == null) continue;
				String arg = (String) sArgs[i];
				try {
					oArgs[i] = af.convert(arg);
				} catch(ArgNotConvertableException ance) {
					return d.new GateTest(4, af.cannotConvert(arg), true);
				}
			}
			return d.new GateTest(1, null, false, oArgs);
		}
	},
	PROPER_SENDER{
		public CommandData<?>.GateTest justify(CommandEvent e, CommandData<?> d) {
			return d.new GateTest(5, SENDER_MESSAGE, !d.isCorrectSenderType(e.getSender()));
		}
	};
	
	private static final String PERM_MESSAGE = ChatColor.RED + "You do not have permission to use this command.";
	private static final String SENDER_MESSAGE = ChatColor.RED + "You are not the correct type of command sender to be able to use this command.";
	
	public abstract CommandData<?>.GateTest justify(CommandEvent e, CommandData<?> d);
	
}
