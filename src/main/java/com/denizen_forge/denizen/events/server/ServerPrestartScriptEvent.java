package com.denizenscript.denizen.events.server;

import com.denizenscript.denizen.events.ForgeScriptEvent;
import com.denizenscript.denizencore.events.ScriptEvent;
import com.denizenscript.denizencore.scripts.containers.core.WorldScriptContainer;

public class ServerPrestartScriptEvent extends ForgeScriptEvent {

    // <--[event]
    // @Events
    // server prestart
    //
    // @Regex ^on server prestart$
    //
    // @Group Server
    //
    // @Triggers before the server finishes starting... fired after some saves are loaded, but before other data is loaded. Use with extreme caution.
    //
    // @Warning This event uses special pre-loading tricks to fire before everything else. Use extreme caution.
    //
    // -->

    public ServerPrestartScriptEvent() {
        instance = this;
    }

    public static ServerPrestartScriptEvent instance;

    @Override
    public boolean couldMatch(ScriptPath path) {
        if (!path.eventLower.startsWith("server prestart")) {
            return false;
        }
        return true;
    }

    @Override
    public String getName() {
        return "ServerPrestart";
    }

    public void specialHackRunEvent() {
        for (WorldScriptContainer container : ScriptEvent.worldContainers) {
            if (container.contains("events.on server prestart")) {
                ScriptPath path = new ScriptPath(container, "server prestart", "on server prestart");
                clone().run(path);
            }
        }
    }
}
