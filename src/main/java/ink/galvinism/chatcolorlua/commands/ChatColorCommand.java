package ink.galvinism.chatcolorlua.commands;

import ink.galvinism.chatcolorlua.ChatColorLua;
import ink.galvinism.chatcolorlua.ChatScript;
import ink.galvinism.chatcolorlua.models.ChatColorScript;
import ink.galvinism.chatcolorlua.utils.ColorUtils;
import ink.galvinism.chatcolorlua.utils.FilenameComparator;
import net.kyori.adventure.text.Component;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaString;

import dev.triumphteam.gui.builder.gui.PaginatedBuilder;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.PaginatedGui;

import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ChatColorCommand implements CommandExecutor {

    private ChatColorLua plugin;

    public ChatColorCommand(ChatColorLua plugin) {
        this.plugin = plugin;
    }

    private PaginatedGui buildMenu(Player p) {
        Stream<ChatScript> scriptStream = plugin.getLoadedScripts().values().stream().filter(chatScript -> p.hasPermission("chatcolorlua.use." + chatScript.getPermission().tojstring()));
        FilenameComparator comp = new FilenameComparator();
        List<ChatScript> authorized = scriptStream.sorted((o1, o2) -> comp.compare(o1.getFileName(), o2.getFileName())).collect(Collectors.toList());
        int count = authorized.size();
        if(count == 0) return null;

        PaginatedGui gui = new PaginatedBuilder()
            .title(Component.text(ColorUtils.translateHexColorCodes(plugin.rainbowify("ChatColor"))))
            .pageSize(45)
            .rows(6)
            .create();

        gui.setItem(6, 3, ItemBuilder.from(Material.PAPER).name(Component.text(ColorUtils.translateHexColorCodes("&cPrevious page"))).asGuiItem(event -> {
            gui.previous();
            event.setCancelled(true);
        }));

        gui.setItem(6, 7, ItemBuilder.from(Material.PAPER).name(Component.text(ColorUtils.translateHexColorCodes("&aNext page"))).asGuiItem(event -> {
            gui.next();
            event.setCancelled(true);
        }));

        for (ChatScript script : authorized) {
            ItemBuilder item;
            LuaString colorResult;
            
            try {
                colorResult = script.getFunction()
                    .call(LuaString.valueOf("SkyCraftia is awesome!")).checkstring();
            } catch (LuaError e) {
                plugin.getLogger().log(Level.SEVERE, "Error while executing script " + script.getFileName(), e);
                continue;
            }

            if(script.getDescription() != null && !script.getDescription().tojstring().trim().isEmpty()) {
                item = ItemBuilder
                    .from(Material.valueOf(script.getIcon().tojstring()))
                    .name(Component.text(ColorUtils.translateHexColorCodes(script.getDisplayName().tojstring())))
                    .lore(
                        Component.text(ColorUtils.translateHexColorCodes("&fExample: " + colorResult.tojstring())),
                        Component.text(""),
                        Component.text(ColorUtils.translateHexColorCodes(script.getDescription().tojstring()))
                    );
            }else {
                item = ItemBuilder
                .from(Material.valueOf(script.getIcon().tojstring()))
                .name(Component.text(ColorUtils.translateHexColorCodes(script.getDisplayName().tojstring())))
                .lore(
                    Component.text(ColorUtils.translateHexColorCodes("&fExample: " + colorResult.tojstring()))
                );
            }

            gui.addItem(item.asGuiItem(event -> {
                Player clickedPlayer = (Player) event.getWhoClicked();

                event.setCancelled(true);
                ChatColorScript ccs = new ChatColorScript(clickedPlayer, script.getName());
                try {
                    plugin.getScriptDao().createOrUpdate(ccs);
                    plugin.success(clickedPlayer, "Successfully updated your chat color to " + script.getDisplayName().tojstring());
                } catch (SQLException e) {
                    plugin.getLogger().log(Level.SEVERE, "Couldn't set chat color " + ccs.getScript() + " for " + clickedPlayer.getName(), e);
                    plugin.error(clickedPlayer, "An error occurred while changing your chat color!");
                }
                gui.close(clickedPlayer);
            }));
        }

        return gui;
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

            PaginatedGui menu = buildMenu(p);
            if(menu == null) {
                plugin.error(p, "You don't have any chat colors to choose from!");
                return true;
            }
            menu.open(p);
        }

        return true;
    }
}
