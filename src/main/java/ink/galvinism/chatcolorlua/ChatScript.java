package ink.galvinism.chatcolorlua;

import org.bukkit.Material;
import org.luaj.vm2.*;

public class ChatScript {

    private LuaFunction function;
    private String name;
    private LuaString displayName;
    private LuaString permission;
    private LuaString icon;
    private String fileName;
    private LuaString description;

    public ChatScript(LuaFunction function, String name, LuaString displayName, LuaString permission, String fileName) {
        this.function = function;
        this.name = name;
        this.displayName = displayName;
        this.permission = permission;
        this.icon = LuaString.valueOf(Material.NAME_TAG.toString());
        this.fileName = fileName;
    }

    public ChatScript(LuaFunction function, String name, LuaString displayName, LuaString permission, LuaString icon, String fileName) {
        this.function = function;
        this.name = name;
        this.displayName = displayName;
        this.permission = permission;
        this.icon = icon;
        this.fileName = fileName;
    }

    public static ChatScript fromLuaTable(LuaTable table, String name, String fileName) throws LuaError {
        LuaFunction function = table.get("func").checkfunction();
        LuaString displayName = table.get("displayName").checkstring();
        LuaString permission = table.get("permission").checkstring();
        ChatScript cs = new ChatScript(function, name, displayName, permission, fileName);
        if(!table.get("icon").eq_b(LuaValue.NIL)) {
            cs.setIcon(table.get("icon").checkstring());
        }
        if(!table.get("description").eq_b(LuaValue.NIL)) {
            cs.setDescription(table.get("description").checkstring());
        }
        return cs;
    }

    public LuaString getDescription() {
        return description;
    }

    public void setDescription(LuaString description) {
        this.description = description;
    }

    public String getFileName() {
        return fileName;
    }

    public LuaString getIcon() {
        return icon;
    }

    public void setIcon(LuaString icon) {
        this.icon = icon;
    }

    public LuaFunction getFunction() {
        return function;
    }

    public void setFunction(LuaFunction function) {
        this.function = function;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LuaString getDisplayName() {
        return displayName;
    }

    public void setDisplayName(LuaString displayName) {
        this.displayName = displayName;
    }

    public LuaString getPermission() {
        return permission;
    }

    public void setPermission(LuaString permission) {
        this.permission = permission;
    }
}
