package com.spiny.util.command;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import com.spiny.util.grouping.FakeMap;

public class CommandManager implements CommandExecutor {
	
	public class CommandEvent {
		
		private CommandSender sender;
		private String totalCommand;
		private String label;
		private GateTestSet testedCommands = new GateTestSet();
		
		public CommandSender getSender() {
			return sender;
		}
		
		public String getTotalCommand() {
			return totalCommand;
		}
		
		public CommandEvent(CommandSender sender, String label, String commandName, String[] args) {
			totalCommand = (args.length > 0) ? commandName + " " + StringUtils.join(args, " ") : commandName;
			this.sender = sender;
			this.label = label;
			log("New CommandEvent. TotalCommandString: " + totalCommand + " Sender: " + sender.getName());
		}
		
		public void evaluateParse() {
			Set<CommandData<?>> commands = CommandManager.this.commands.get(totalCommand, new Comparator<String>(){
				public int compare(String o1, String o2) {
					return (o1.startsWith(o2)) ? 0 : 1;
				}
			});
			for(CommandData<?> command : commands) {
				log("Found command: " + command.getKey() + " " + command.getPermission());
				GateTestSet gts = new GateTestSet();
				for(CommandEventGate ceg : CommandEventGate.values()) {
					CommandData<?>.GateTest gt = ceg.justify(this, command);
					log("Testing: " + ceg.name() + ": " + gt.isDenied() + ", " + gt.getCorrelation());
					gts.add(gt);
				}
				testedCommands.add(gts.merge(false));
			}
		}
		
		@SuppressWarnings({ "rawtypes", "unchecked" })
		public boolean finish() {
			CommandData.GateTest fp = testedCommands.merge(true);
			if(fp == null) {
				sender.sendMessage("Unknown command.");
				return false;
			}
			log("Trying " + fp.getCommand().getKey() + " for " + fp.getCorrelation() + " and " + fp.isDenied());
			if(!fp.isDenied()) {
				return fp.getCommand().execute(sender, label, fp.getNewArgs());
			}
			sender.sendMessage(ChatColor.RED + fp.getPassReadoutDenial());
			return false;
		}
		
	}
	
	public static void main(String[] args) {
		System.out.println("mobscaler".replace("mobscaler", "").split(" ").length);
	}
	
	private static final String tag = "[CommandManager]";

	private FakeMap<String, CommandData<?>> commands = new FakeMap<String, CommandData<?>>(new HashSet<CommandData<?>>());
	
	private JavaPlugin plugin;
	
	public CommandManager(JavaPlugin plugin) {
		this.plugin = plugin;
	}
	
	public CommandManager addCommandData(CommandData<?> data) {
		commands.put(data);
		plugin.getCommand(data.getKey().split(" ")[0]).setExecutor(this);
		if(data.getUsage() != null) log("Registered command: " + data.getKey());
		return this;
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		CommandEvent e = new CommandEvent(sender, label, command.getName(), args);
		e.evaluateParse();
		return e.finish();
	}
	
	public Set<CommandData<?>> getSubCommands(String command) {
		return commands.get(command, new Comparator<String>(){
			public int compare(String o1, String o2) {
				return (o2.startsWith(o1) && !o1.equals(o2)) ? 0 : 1;
			}
		});
	}
	
	protected void log(String message) {
		plugin.getLogger().log(Level.INFO, tag + " " + message);
	}
	
}
