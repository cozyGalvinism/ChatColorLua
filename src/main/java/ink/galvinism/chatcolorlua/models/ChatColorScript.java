package ink.galvinism.chatcolorlua.models;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import org.bukkit.entity.Player;

import java.util.UUID;

@DatabaseTable
public class ChatColorScript {

    @DatabaseField(id = true)
    private String uuid;
    @DatabaseField
    private String script;

    public ChatColorScript() {

    }

    public ChatColorScript(String uuid, String script) {
        this.uuid = uuid;
        this.script = script;
    }

    public ChatColorScript(UUID uuid, String script) {
        this.uuid = uuid.toString();
        this.script = script;
    }

    public ChatColorScript(Player p, String script) {
        this.uuid = p.getUniqueId().toString();
        this.script = script;
    }

    public String getScript() {
        return script;
    }

    public void setScript(String script) {
        this.script = script;
    }

    public String getUuid() {
        return uuid;
    }
}
