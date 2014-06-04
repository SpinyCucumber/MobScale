package com.spiny.util.command;

import org.bukkit.command.CommandSender;

import com.spiny.util.grouping.Identifiable;

public abstract class CommandData<T extends CommandSender> implements Identifiable<String> {
	
	public class GateTest implements Comparable<CommandData<?>.GateTest> {
		
		private int correlation;
		private String passReadoutDenial;
		private boolean denied;
		private Object[] newArgs;
		
		public int getCorrelation() {
			return correlation;
		}

		public String getPassReadoutDenial() {
			return passReadoutDenial;
		}

		public boolean isDenied() {
			return denied;
		}
		
		public Object[] getNewArgs() {
			return newArgs;
		}
		
		public CommandData<T> getCommand() {
			return CommandData.this;
		}
		
		public int compareTo(CommandData<?>.GateTest arg0) {
			return correlation - arg0.correlation;
		}

		public GateTest(int correlation, String passReadoutDenial, boolean denied, Object...newArgs) {
			this.correlation = correlation;
			if(denied) this.correlation *= -1;
			this.passReadoutDenial = passReadoutDenial;
			this.denied = denied;
			this.newArgs = newArgs;
		}
		
	}
	
	public abstract boolean execute(T sender, String label, Object[] newArgs);
	
	public boolean isCorrectSenderType(CommandSender s) {
		return clazz.isInstance(s);
	}
	
	private String permission;
	private ArgFilter[] args;
	
	private String usage;
	private String name;
	private Class<T> clazz;
	
	public CommandData(Class<T> clazz, String name, String permission, String usage, ArgFilter... args) {
		this.name = name;
		this.clazz = clazz;
		this.permission = permission;
		this.args = args;
		this.usage = usage;
	}
	
	public CommandData(Class<T> clazz, String name, String permission, String usage, int args) {
		this.name = name;
		this.clazz = clazz;
		this.permission = permission;
		this.args = new ArgFilter[args];
		this.usage = usage;
	}
	
	@Override
	public String getKey() {
		return name;
	}
	public ArgFilter[] getArgs() {
		return args;
	}
	public int getArgNumber() {
		return args.length;
	}
	public String getPermission() {
		return permission;
	}
	public String getUsage() {
		return usage;
	}
	
}
