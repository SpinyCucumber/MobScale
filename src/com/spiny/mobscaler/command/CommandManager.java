package com.spiny.mobscaler.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class CommandManager implements CommandExecutor {
	
	//Handy little class I wrote to manage commands.
	
	private static final String tag = "[CommandManager]";

	@SuppressWarnings("rawtypes")
	private List<CommandData> commands = new ArrayList<CommandData>();
	
	private JavaPlugin plugin;
	
	@SuppressWarnings("rawtypes")
	public CommandManager(JavaPlugin plugin, List<CommandData> commandData) {
		this.plugin = plugin;
		this.commands.addAll(commandData);
	}
	
	public CommandManager(JavaPlugin plugin) {
		this.plugin = plugin;
	}
	
	public CommandManager withCommandData(CommandData<?> data) {
		commands.add(data);
		plugin.getCommand(data.getName().split(" ")[0]).setExecutor(this);
		if(data.getUsage() != null) plugin.getLogger().log(Level.INFO, getTag() + "Registered command: " + data.getUsage());
		return this;
	}
	
	//Ridiculously complex command parser ;-;
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		List<String> argList = new ArrayList<String>(Arrays.asList(args));
		CommandData<CommandSender> commandData = null;
		String testName = command.getName();
		CommandSender commandSender = sender;
		for(int i = 0; i <= args.length; i++) {
			if(i > 0) testName = testName + " " + args[i - 1];
			for(CommandData key : commands) {
				boolean keyIsCommand = key.getName().equalsIgnoreCase(testName);
				if(keyIsCommand) {
					if(i > 0) argList.remove(args[i - 1]);
					boolean isPlayer = key.isValidSender(sender);
					boolean hasPermission = sender.hasPermission(key.getPermission());
					boolean hasArgs = argList.size() == key.getArgs().length;
					 
					if(isPlayer && hasPermission && hasArgs) {
						commandData = key;
						break;
					}
				}
			}
		}
		if(commandData != null){
			Object[] newArgs = new Object[argList.size()];
			for(int b = 0; b < argList.size(); b++) {
				if(commandData.getArgs()[b]) {
					Player argPlayer = getPlayer(sender, argList.get(b));
					if(argPlayer == null) return false;
					newArgs[b] = argPlayer;
				} else newArgs[b] = argList.get(b);
			}
			return commandData.execute(commandSender, label, newArgs);
		    } else sender.sendMessage("Unknown command.");
		return false;
	}
	
	public CommandData<?>[] getSubCommands(String command) {
		List<CommandData<?>> subCommands = new ArrayList<CommandData<?>>();
		for(CommandData<?> data : commands) {
			if(data.getName().startsWith(command) && data.getName() != command) subCommands.add(data);
		}
		return subCommands.toArray(new CommandData[subCommands.size()]);
	}

	public static String getTag() {
		return tag + " ";
	}
	
	public Player getPlayer(CommandSender sender, String name) {
		Player player = plugin.getServer().getPlayerExact(name);
		if(player == null) sender.sendMessage("No player has been found with the name of '" + name + "'.");
		return player;
	}
}
