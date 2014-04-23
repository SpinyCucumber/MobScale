package com.spiny.util.command;

import org.bukkit.command.CommandSender;

public abstract class CommandData<T extends CommandSender> {

	public abstract boolean execute(T sender, String label, Object[] newArgs);
	
	private String permission;
	private ArgFilter[] args;
	private String name;
	
	private String usage;
	
	public CommandData(String name, String permission, String usage, ArgFilter... args) {
		this.setPermission(permission);
		this.setArgs(args);
		this.setName(name);
		this.setUsage(usage);
	}
	
	public CommandData(String name, String permission, int args, String usage) {
		this.setPermission(permission);
		this.setArgs(new ArgFilter[args]);
		this.setName(name);
		this.setUsage(usage);
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public ArgFilter[] getArgs() {
		return args;
	}
	public void setArgs(ArgFilter[] args) {
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
