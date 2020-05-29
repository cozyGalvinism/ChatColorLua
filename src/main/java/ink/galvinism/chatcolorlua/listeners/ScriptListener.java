package ink.galvinism.chatcolorlua.listeners;

import ink.galvinism.chatcolorlua.ChatColorLua;
import ink.galvinism.chatcolorlua.ChatScript;
import ink.galvinism.chatcolorlua.models.ChatColorScript;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.luaj.vm2.*;
import org.luaj.vm2.compiler.LuaC;
import org.luaj.vm2.lib.PackageLib;
import org.luaj.vm2.lib.StringLib;
import org.luaj.vm2.lib.ZeroArgFunction;
import org.luaj.vm2.lib.jse.JseBaseLib;
import org.luaj.vm2.lib.jse.JseMathLib;

import java.util.logging.Level;

public class ScriptListener implements Listener {

    private ChatColorLua plugin;

    public ScriptListener(ChatColorLua plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncPlayerChatEvent e) {
        if(e.isCancelled()) return;
        ChatColorScript script;
        try {
            script = plugin.getScriptDao().queryForId(e.getPlayer().getUniqueId().toString());
        } catch (java.sql.SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Error while getting script: ", ex);
            return;
        }
        if(script == null) return;
        ChatScript cScript = plugin.getLoadedScripts().get(script.getScript());
        if(!e.getPlayer().hasPermission("chatcolorlua.use." + cScript.getPermission().tojstring())) return;
        if(!plugin.getLoadedScripts().containsKey(script.getScript())) {
            plugin.getLogger().warning("Script " + script.getScript() + " doesn't exist anymore! Removing...");
            try {
                plugin.getScriptDao().deleteById(script.getUuid());
            } catch (java.sql.SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, "Error while trying to delete script entry", ex);
                return;
            }
            return;
        }

        try {
            LuaString colorResult = plugin.getLoadedScripts().get(script.getScript()).getFunction()
                    .call(LuaString.valueOf(e.getMessage())).checkstring();
            if(colorResult.tojstring().trim().isEmpty()) {
                e.setCancelled(true);
                return;
            }
            e.setMessage(ChatColor.translateAlternateColorCodes('&', colorResult.toString()));
        } catch (LuaError l) {
            plugin.getLogger().log(Level.SEVERE, "Error while trying to apply chat format", l);
        }
    }
}
