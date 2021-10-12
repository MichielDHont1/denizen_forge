package com.denizenscript.denizen.events.player;

import com.denizenscript.denizen.objects.EntityTag;
import com.denizenscript.denizen.utilities.implementation.BukkitScriptEntryData;
import com.denizenscript.denizen.events.ForgeScriptEvent;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.scripts.ScriptEntryData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleSneakEvent;

public class PlayerSneakScriptEvent extends ForgeScriptEvent {

    // <--[event]
    // @Events
    // player toggles sneaking
    // player starts sneaking
    // player stops sneaking
    //
    // @Regex ^on player (toggles|starts|stops) sneaking$
    //
    // @Group Player
    //
    // @Location true
    //
    // @Cancellable true
    //
    // @Triggers when a player starts or stops sneaking.
    //
    // @Context
    // <context.state> returns an ElementTag(Boolean) with a value of "true" if the player is now sneaking and "false" otherwise.
    //
    // @Player Always.
    //
    // -->

    public PlayerSneakScriptEvent() {
        instance = this;
    }

    public static PlayerSneakScriptEvent instance;
    public boolean state;
    public PlayerToggleSneakEvent event;

    @Override
    public boolean couldMatch(ScriptPath path) {
        if (!path.eventArgLowerAt(0).equals("player") || !path.eventArgLowerAt(2).equals("sneaking")) {
            return false;
        }
        String middleWord = path.eventArgAt(1);
        if (!(middleWord.equals("starts") || middleWord.equals("stops") || middleWord.equals("toggles"))) {
            return false;
        }
        return true;
    }

    @Override
    public boolean matches(ScriptPath path) {
        String cmd = path.eventArgLowerAt(1);
        if (cmd.equals("starts") && !state) {
            return false;
        }
        if (cmd.equals("stops") && state) {
            return false;
        }
        if (!runInCheck(path, event.getPlayer().getLocation())) {
            return false;
        }
        return super.matches(path);
    }

    @Override
    public String getName() {
        return "PlayerSneak";
    }

    @Override
    public ScriptEntryData getScriptEntryData() {
        return new BukkitScriptEntryData(event.getPlayer());
    }

    @Override
    public ObjectTag getContext(String name) {
        if (name.equals("state")) {
            return new ElementTag(state);
        }
        return super.getContext(name);
    }

    @SubscribeEvent
    public void onPlayerSneak(PlayerToggleSneakEvent event) {
        if (EntityTag.isNPC(event.getPlayer())) {
            return;
        }
        state = event.isSneaking();
        this.event = event;
        fire(event);
    }
}
