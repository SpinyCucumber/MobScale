package com.spiny.mobscaler.command;

import org.bukkit.command.CommandSender;

public abstract class CommandData<T extends CommandSender> {

	public abstract boolean execute(T sender, String label, Object[] newArgs);
	
	private String permission;
	private boolean[] args;
	private String name;
	
	private String usage;
	
	private Class<T> t;
	
	public boolean isValidSender(CommandSender sender) {
		return t.isAssignableFrom(sender.getClass());
	}
	
	public CommandData(Class<T> t, String name, String permission, boolean[] args, String usage) {
		this.t = t;
		this.setPermission(permission);
		this.setArgs(args);
		this.setName(name);
		this.setUsage(usage);
	}
	
	public CommandData(Class<T> t, String name, String permission, int args, String usage) {
		this.t = t;
		this.setPermission(permission);
		this.setArgs(new boolean[args]);
		this.setName(name);
		this.setUsage(usage);
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public boolean[] getArgs() {
		return args;
	}
	public void setArgs(boolean[] args) {
		this.args = args;
	}
	public String getPermission() {
		return permission;
	}
	public void setPermission(String permission) {
		this.permission = permission;
	}
	public String getUsage() {
		return usage;
	}
	public void setUsage(String usage) {
		this.usage = usage;
	}
}
