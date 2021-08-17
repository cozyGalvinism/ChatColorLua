package ink.galvinism.chatcolorlua;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import ink.galvinism.chatcolorlua.commands.ChatColorCommand;
import ink.galvinism.chatcolorlua.listeners.ScriptListener;
import ink.galvinism.chatcolorlua.models.ChatColorScript;
import ink.galvinism.chatcolorlua.utils.FileComparator;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.luaj.vm2.*;
import org.luaj.vm2.compiler.LuaC;
import org.luaj.vm2.lib.PackageLib;
import org.luaj.vm2.lib.StringLib;
import org.luaj.vm2.lib.jse.JseBaseLib;
import org.luaj.vm2.lib.jse.JseMathLib;

import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ChatColorLua extends JavaPlugin {

    private Dao<ChatColorScript, String> scriptDao;
    private ConnectionSource connectionSource;
    private static Globals serverGlobals;
    private HashMap<String, ChatScript> loadedScripts;

    public ChatColorLua() {
        loadedScripts = new HashMap<>();
    }

    @Override
    public void onEnable() {
        // Plugin startup logic
        if(!new File(getDataFolder(), "config.yml").exists()) saveDefaultConfig();
        reloadConfig();

        ChatColorLua.serverGlobals = new Globals();
        ChatColorLua.serverGlobals.load(new JseBaseLib());
        ChatColorLua.serverGlobals.load(new PackageLib());
        ChatColorLua.serverGlobals.load(new JseMathLib());
        ChatColorLua.serverGlobals.load(new StringLib());
        LoadState.install(ChatColorLua.serverGlobals);
        LuaC.install(ChatColorLua.serverGlobals);

        File colorDir = new File(getDataFolder() + File.separator + "colors");
        if(!colorDir.exists()) {
            colorDir.mkdir();
            ResourceExtractor extractor = new ResourceExtractor(this, colorDir, "colors", ".*\\.(lua)");
            try {
                extractor.extract();
            }catch (IOException io) {
                getLogger().log(Level.SEVERE, "Unable to extract default formats", io);
            }
        }

        Pattern namePattern = Pattern.compile("((?<order>\\d*)-)?(?<name>.+)");
        File[] files = Objects.requireNonNull(colorDir.listFiles((file, name) -> name.endsWith(".lua") && !getConfig().getList("disabled").contains(name)));
        Arrays.sort(files, new FileComparator());
        for(File luaFile : files) {
            getLogger().info("Loading script functions from " + luaFile.getName());
            try {
                FileReader fr = new FileReader(luaFile);
                LuaValue chunk = ChatColorLua.serverGlobals.load(fr, "main").call();
                fr.close();
                LuaTable functionTable = chunk.checktable();
                Matcher m = namePattern.matcher(luaFile.getName());
                ChatScript script;
                if(m.find()) {
                    script = ChatScript.fromLuaTable(functionTable, m.group("name"), luaFile.getName());
                }else {
                    script = ChatScript.fromLuaTable(functionTable, luaFile.getName(), luaFile.getName());
                }
                loadedScripts.put(script.getName(), script);
                getLogger().fine("Loaded script function " + script.getName());
            } catch (FileNotFoundException e) {
                getLogger().log(Level.SEVERE, "Couldn't open chat script " + luaFile.getName(), e);
            } catch (LuaError l) {
                getLogger().log(Level.SEVERE, "Error while initializing script", l);
            } catch (IOException e) {
                getLogger().log(Level.SEVERE, "Couldn't close chat script " + luaFile.getName(), e);
            }
        }

        try {
            connectionSource = new JdbcConnectionSource(getConfig().getString("database"));
            scriptDao = DaoManager.createDao(connectionSource, ChatColorScript.class);
            TableUtils.createTableIfNotExists(connectionSource, ChatColorScript.class);
        } catch (java.sql.SQLException e) {
            getLogger().log(Level.SEVERE, "Error while loading database: ", e);
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        Bukkit.getPluginManager().registerEvents(new ScriptListener(this), this);
        getCommand("chatcolor").setExecutor(new ChatColorCommand(this));
    }

    public HashMap<String, ChatScript> getLoadedScripts() {
        return loadedScripts;
    }

    /*
    private Map<LuaValue, LuaValue> convertFromLuaTable(LuaTable table) {
        HashMap<LuaValue, LuaValue> tableContents = new HashMap<>();
        LuaValue k = LuaValue.NIL;
        while(true) {
            Varargs n = table.next(k);
            if((k = n.arg(1)).isnil()) break;
            LuaValue v = n.arg(2);
            tableContents.put(k, v);
        }
        return tableContents;
    }
    */

    public String rainbowify(String original) {
        String[] chars = new String[]{"c&l", "6&l", "e&l", "a&l", "b&l", "3&l", "d&l"};
        int index = 0;
        String returnValue = "";
        for (char c : original.toCharArray()){
            returnValue += "&" + chars[index] + c;
            index++;
            if (index == chars.length){
                index = 0;
            }
        }
        return returnValue;
    }

    public void success(CommandSender sender, String message) {
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7[" + rainbowify("ChatColor") + "&7] &a" + message));
    }

    public void error(CommandSender sender, String message) {
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7[" + rainbowify("ChatColor") + "&7] &c" + message));
    }

    public static Globals getServerGlobals() {
        return serverGlobals;
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        if(connectionSource != null) {
            connectionSource.closeQuietly();
        }
    }

    public Dao<ChatColorScript, String> getScriptDao() {
        return scriptDao;
    }
}
