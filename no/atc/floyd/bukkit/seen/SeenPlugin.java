package no.atc.floyd.bukkit.seen;


//import java.io.*;

//import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Server;
import org.bukkit.entity.Player;
//import org.bukkit.Server;
//import org.bukkit.event.Event.Priority;
//import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
//import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
//import org.bukkit.plugin.PluginLoader;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.PluginManager;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;
import org.bukkit.command.*;


/**
* SeenPlugin plugin for Bukkit
*
* @author FloydATC
*/
public class SeenPlugin extends JavaPlugin implements Listener {
	public static final Logger logger = Logger.getLogger("Minecraft.SeenPlugin");
	private File dir = new File("plugins/SeenPlugin");
    
    public void onDisable() {
        // TODO: Place any custom disable code here

        // NOTE: All registered events are automatically unregistered when a plugin is disabled
    	
        // EXAMPLE: Custom code, here we just output some info so we can check all is well
    	PluginDescriptionFile pdfFile = this.getDescription();
		logger.info( pdfFile.getName() + " version " + pdfFile.getVersion() + " is disabled!" );
    }

    public void onEnable() {
        // TODO: Place any custom enable code here including the registration of any events

    	// Set up directory for request, denial and permission tokens. 
    	// Clear out any stale files (i.e. older than cooldown)
    	if (dir.exists() == false) { dir.mkdirs(); }
    	    	
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(this, this);

        // EXAMPLE: Custom code, here we just output some info so we can check all is well
        PluginDescriptionFile pdfFile = this.getDescription();
		logger.info( pdfFile.getName() + " version " + pdfFile.getVersion() + " is enabled!" );
	
    }

    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args ) {
    	String cmdname = cmd.getName().toLowerCase();
        Player player = null;
        if (sender instanceof Player) {
        	player = (Player)sender;
        }

    	if (cmdname.equalsIgnoreCase("seen") && (player == null || player.hasPermission("seenplugin.seen"))) {
    		if (args.length == 1) {
    			String name = args[0].toLowerCase();
    			Player p = sender.getServer().getPlayerExact(name);
    			if (p != null) {
    				// Player is logged in. Duh.
    				respond(player, "§7[§6Seen§7]§b §6"+name+"§b is logged in right now.");
    				showFirst(player, name);
    				return true;
    			}
    			Long login = loginTime(name);
    			Long logout = logoutTime(name);
    			if (login == null && logout == null) {
    				// Unknown player
    				respond(player, "§7[§6Seen§7]§c Sorry, I have no record of anyone named "+name+" (Please type the full name)");
    				return true;
    			}
    			if (login == null && logout != null) {
    				// This means we missed a login, possibly due to a reload
    				respond(player, "§7[§6Seen§7]§b §6"+name+"§b logged out §f"+clearTime(logout)+"§b but I have no record of the login, sorry");
    				showFirst(player, name);
    				return true;
    			}
    			if (logout == null) {
    				// This SHOULD mean the player is still online but apparently he isn't?
    				respond(player, "§7[§6Seen§7]§b §6"+name+"§b logged in §f"+clearTime(login)+"§b but I have no record of the logout, sorry");
    				showFirst(player, name);
    				return true;
    			}
    			// Player is offline and we have all we need
				respond(player, "§7[§6Seen§7]§b §6"+name+"§b logged out §f"+clearTime(logout)+"§b after playing for §f"+clearDelta(logout-login));
				showFirst(player, name);
    			return true;
    		}
    	}
    	return false;
    }

    @EventHandler
    public void onLogin( PlayerLoginEvent event ) {
    	long now = System.currentTimeMillis();
    	String pname = event.getPlayer().getName().toLowerCase();
    	// Delete logout token, no longer needed
    	File fout = new File(dir+"/"+pname+".logout");
    	fout.delete();
    	// Create first token if none exists
    	File first = new File(dir+"/"+pname+".first");
    	if (first.exists() == false) {
	    	try {
				first.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}
    	// Create login token
    	File fin = new File(dir+"/"+pname+".login");
    	if (fin.exists()) {
    		fin.setLastModified(now);
    	} else {
	    	try {
				fin.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}
    }

    @EventHandler
    public void onQuit( PlayerQuitEvent event ) {
    	long now = System.currentTimeMillis();
    	String pname = event.getPlayer().getName().toLowerCase();
    	// Create logout token
    	File fout = new File(dir+"/"+pname+".logout");
    	if (fout.exists()) {
    		fout.setLastModified(now);
    	} else {
	    	try {
				fout.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}
    }

    private void showFirst(Player player, String name) {
    	Long first = firstTime(name);
    	if (first != null) {
    		respond(player, "§7[§6Seen§7]§b Played on this server since §f"+clearTime(first));
    	}
    }
    
    private Long firstTime(String pname) {
    	Long timestamp = null;
    	File first = new File(dir+"/"+pname+".first");
    	if (first.exists() == false) {
    		return null;
    	}
    	try {
    		timestamp = first.lastModified();
    	} catch (Exception e) {
			//e.printStackTrace();
		}
    	return timestamp;
    }
    
    private Long loginTime(String pname) {
    	Long timestamp = null;
    	File fin = new File(dir+"/"+pname+".login");
    	if (fin.exists() == false) {
    		return null;
    	}
    	try {
    		timestamp = fin.lastModified();
    	} catch (Exception e) {
			//e.printStackTrace();
		}
    	return timestamp;
    }
    
    private Long logoutTime(String pname) {
    	Long timestamp = null;
    	File fout = new File(dir+"/"+pname+".logout");
    	if (fout.exists() == false) {
    		return null;
    	}
    	try {
    		timestamp = fout.lastModified();
    	} catch (Exception e) {
			//e.printStackTrace();
		}
    	return timestamp;
    }
    
    private String clearTime(Long timestamp) {
    	Date time = new Date(timestamp);
    	SimpleDateFormat sdf = new SimpleDateFormat("EEE dd.MMM yyyy HH:mm");
    	return sdf.format(time);
    }
    
    private String clearDelta(Long delta) {
    	Long min = delta / (60*1000);
    	Integer minutes = min.intValue();
    	Integer hours = 0;
    	if (minutes > 60) {
    		hours = minutes / 60;
    		minutes = minutes % 60;
    		return hours+" hour"+(hours==1?"":"s")+" and "+minutes+" minute"+(minutes==1?"":"s");
    	} else {
    		return minutes+" minute"+(minutes==1?"":"s");
    	}
    }
    
    private void respond(Player player, String message) {
    	if (player == null) {
        	Server server = getServer();
        	ConsoleCommandSender console = server.getConsoleSender();
        	console.sendMessage(message);
    	} else {
    		player.sendMessage(message);
    	}
    }
}

