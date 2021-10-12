package com.denizenscript.denizen.events.player;

import com.denizenscript.denizen.objects.EntityTag;
import com.denizenscript.denizen.objects.LocationTag;
import com.denizenscript.denizen.utilities.implementation.BukkitScriptEntryData;
import com.denizenscript.denizen.events.ForgeScriptEvent;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.scripts.ScriptEntryData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedEnterEvent;

public class PlayerEntersBedScriptEvent extends ForgeScriptEvent {

    // <--[event]
    // @Events
    // player enters bed
    //
    // @Regex ^on player enters bed$
    //
    // @Group Player
    //
    // @Location true
    //
    // @Cancellable true
    //
    // @Triggers when a player enters a bed.
    //
    // @Context
    // <context.location> returns the LocationTag of the bed.
    //
    // @Player Always.
    //
    // -->

    public PlayerEntersBedScriptEvent() {
        instance = this;
    }

    public static PlayerEntersBedScriptEvent instance;
    public LocationTag location;
    public PlayerBedEnterEvent event;

    @Override
    public boolean couldMatch(ScriptPath path) {
        return path.eventLower.startsWith("player enters bed");
    }

    @Override
    public boolean matches(ScriptPath path) {
        if (!runInCheck(path, location)) {
            return false;
        }
        return super.matches(path);
    }

    @Override
    public String getName() {
        return "PlayerEntersBed";
    }

    @Override
    public ScriptEntryData getScriptEntryData() {
        return new BukkitScriptEntryData(event.getPlayer());
    }

    @Override
    public ObjectTag getContext(String name) {
        if (name.equals("location")) {
            return location;
        }
        return super.getContext(name);
    }

    @SubscribeEvent
    public void onPlayerEntersBed(PlayerBedEnterEvent event) {
        if (EntityTag.isNPC(event.getPlayer())) {
            return;
        }
        location = new LocationTag(event.getBed().getLocation());
        this.event = event;
        fire(event);
    }
}
