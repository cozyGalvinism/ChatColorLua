package ink.galvinism.chatcolorlua.commands;

import ink.galvinism.chatcolorlua.ChatColorLua;
import ink.galvinism.chatcolorlua.ChatScript;
import ink.galvinism.chatcolorlua.models.ChatColorScript;
import ink.galvinism.chatcolorlua.utils.FilenameComparator;
import ink.galvinism.chatcolorlua.utils.IconMenu;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.luaj.vm2.LuaString;

import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ChatColorCommand implements CommandExecutor {

    private ChatColorLua plugin;

    public ChatColorCommand(ChatColorLua plugin) {
        this.plugin = plugin;
    }

    private IconMenu buildMenu(Player p) {
        Stream<ChatScript> scriptStream = plugin.getLoadedScripts().values().stream().filter(chatScript -> p.hasPermission("chatcolor.use." + chatScript.getPermission().tojstring()));
        FilenameComparator comp = new FilenameComparator();
        List<ChatScript> authorized = scriptStream.sorted((o1, o2) -> comp.compare(o1.getFileName(), o2.getFileName())).collect(Collectors.toList());
        double count = (double)authorized.size();
        if(count == 0) return null;
        int invSize = (int)(Math.ceil(count / 9) * 9);
        IconMenu menu = new IconMenu(ChatColor.translateAlternateColorCodes('&', plugin.rainbowify("ChatColor")), invSize, oce -> {
            oce.setWillClose(true);
            oce.setWillDestroy(true);
            ChatScript cs = authorized.get(oce.getPosition());
            ChatColorScript ccs = new ChatColorScript(oce.getPlayer(), cs.getName());
            try {
                plugin.getScriptDao().createOrUpdate(ccs);
                plugin.success(oce.getPlayer(), "Successfully updated your chat color to " + cs.getDisplayName().tojstring());
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Couldn't set chat color " + ccs.getScript() + " for " + oce.getPlayer().getName(), e);
                plugin.error(oce.getPlayer(), "An error occurred while changing your chat color!");
            }
        }, plugin);

        for(int i = 0; i < count; i++) {
            ChatScript script = authorized.get(i);
            LuaString colorResult = script.getFunction()
                    .call(LuaString.valueOf("SkyCraftia is awesome!")).checkstring();
            if(script.getDescription() != null && !script.getDescription().tojstring().trim().isEmpty()) {
                menu.setOption(i, new ItemStack(Material.valueOf(script.getIcon().tojstring())),
                        ChatColor.translateAlternateColorCodes('&', script.getDisplayName().tojstring()),
                        ChatColor.translateAlternateColorCodes('&', "&fExample: " + colorResult.tojstring()),
                        "",
                        ChatColor.translateAlternateColorCodes('&', script.getDescription().tojstring()));
            }else {
                menu.setOption(i, new ItemStack(Material.valueOf(script.getIcon().tojstring())),
                        ChatColor.translateAlternateColorCodes('&', script.getDisplayName().tojstring()),
                        ChatColor.translateAlternateColorCodes('&', "&fExample: " + colorResult.tojstring()));
            }
        }

        menu.setSpecificTo(p);
        return menu;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String command, String[] args) {
        if(!(sender instanceof Player)) {
            plugin.error(sender, "Only players can use this command!");
            return true;
        }
        Player p = (Player)sender;

        if(args.length == 0) {
            if(!p.hasPermission("chatcolorlua.gui")) {
                plugin.error(p, "You are not allowed to set your chat color!");
                return true;
            }

            IconMenu menu = buildMenu(p);
            if(menu == null) {
                plugin.error(p, "You don't have any chat colors to choose from!");
                return true;
            }
            menu.open(p);
        }

        return true;
    }
}
