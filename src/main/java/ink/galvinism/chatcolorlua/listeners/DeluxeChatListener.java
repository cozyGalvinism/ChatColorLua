package ink.galvinism.chatcolorlua.listeners;

import java.util.logging.Level;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaString;

import ink.galvinism.chatcolorlua.ChatColorLua;
import ink.galvinism.chatcolorlua.ChatScript;
import ink.galvinism.chatcolorlua.models.ChatColorScript;
import ink.galvinism.chatcolorlua.utils.ColorUtils;
import me.clip.deluxechat.events.DeluxeChatEvent;

public class DeluxeChatListener implements Listener {
    
    private final ChatColorLua plugin;

    public DeluxeChatListener( ChatColorLua plugin ) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onDeluxeChat(DeluxeChatEvent event) {
        ChatColorScript script;
        try {
            script = plugin.getScriptDao().queryForId(event.getPlayer().getUniqueId().toString());
        } catch (java.sql.SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Error while getting script: ", ex);
            return;
        }
        if(script == null) return;
        ChatScript cScript = plugin.getLoadedScripts().get(script.getScript());
        if(!event.getPlayer().hasPermission("chatcolorlua.use." + cScript.getPermission().tojstring())) return;
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
                    .call(LuaString.valueOf(event.getChatMessage())).checkstring();
            if(colorResult.tojstring().trim().isEmpty()) {
                event.setCancelled(true);
                return;
            }
            event.setChatMessage(ColorUtils.translateHexColorCodes(colorResult.toString()));
        } catch (LuaError l) {
            plugin.getLogger().log(Level.SEVERE, "Error while trying to apply chat format", l);
        }
    }
}
