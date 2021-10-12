package com.denizenscript.denizen.events.player;

import com.denizenscript.denizen.objects.EntityTag;
import com.denizenscript.denizen.objects.LocationTag;
import com.denizenscript.denizen.utilities.implementation.BukkitScriptEntryData;
import com.denizenscript.denizen.events.ForgeScriptEvent;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.scripts.ScriptEntryData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;

public class PlayerUsesPortalScriptEvent extends ForgeScriptEvent {

    // <--[event]
    // @Events
    // player uses portal
    //
    // @Regex ^on player uses portal$
    //
    // @Group Player
    //
    // @Location true
    //
    // @Triggers when a player enters a portal.
    //
    // @Context
    // <context.from> returns the location teleported from.
    // <context.to> returns the location teleported to.
    //
    // @Determine
    // LocationTag to change the destination.
    //
    // @Player Always.
    //
    // -->

    public PlayerUsesPortalScriptEvent() {
        instance = this;
    }

    public static PlayerUsesPortalScriptEvent instance;
    public EntityTag entity;
    public LocationTag to;
    public LocationTag from;
    public PlayerPortalEvent event;

    @Override
    public boolean couldMatch(ScriptPath path) {
        return path.eventLower.startsWith("player uses portal");
    }

    @Override
    public boolean matches(ScriptPath path) {
        if (!runInCheck(path, to) && !runInCheck(path, from)) {
            return false;
        }
        return super.matches(path);
    }

    @Override
    public String getName() {
        return "PlayerUsesPortal";
    }

    @Override
    public boolean applyDetermination(ScriptPath path, ObjectTag determinationObj) {
        String determination = determinationObj.toString();
        if (LocationTag.matches(determination)) {
            to = LocationTag.valueOf(determination, getTagContext(path));
            event.setTo(to);
            return true;
        }
        return super.applyDetermination(path, determinationObj);
    }

    @Override
    public ScriptEntryData getScriptEntryData() {
        return new BukkitScriptEntryData(entity);
    }

    @Override
    public ObjectTag getContext(String name) {
        switch (name) {
            case "entity":
                return entity;
            case "to":
                return to;
            case "from":
                return from;
        }
        return super.getContext(name);
    }

    @SubscribeEvent
    public void onPlayerEntersPortal(PlayerPortalEvent event) {
        if (EntityTag.isNPC(event.getPlayer())) {
            return;
        }
        entity = new EntityTag(event.getPlayer());
        to = event.getTo() == null ? null : new LocationTag(event.getTo());
        from = new LocationTag(event.getFrom());
        this.event = event;
        fire(event);
    }
}
