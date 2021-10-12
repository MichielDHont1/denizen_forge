package com.denizenscript.denizen.events.player;

import com.denizenscript.denizen.objects.EntityTag;
import com.denizenscript.denizen.objects.ItemTag;
import com.denizenscript.denizen.objects.LocationTag;
import com.denizenscript.denizen.objects.PlayerTag;
import com.denizenscript.denizen.utilities.implementation.BukkitScriptEntryData;
import com.denizenscript.denizen.events.ForgeScriptEvent;
import com.denizenscript.denizencore.events.ScriptEvent;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.scripts.ScriptEntryData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTakeLecternBookEvent;

public class PlayerTakesFromLecternScriptEvent extends ForgeScriptEvent {

    // <--[event]
    // @Events
    // player takes item from lectern
    // player takes <item> from lectern
    //
    // @Regex ^on player takes [^\s]+ from lectern$
    //
    // @Group Player
    //
    // @Location true
    //
    // @Cancellable true
    //
    // @Triggers when a player takes a book from a lectern.
    //
    // @Context
    // <context.location> returns the LocationTag of the lectern.
    // <context.item> returns the book ItemTag taken out of the lectern.
    //
    // @Player Always.
    //
    // -->

    public PlayerTakesFromLecternScriptEvent() {
        instance = this;
    }

    public static PlayerTakesFromLecternScriptEvent instance;
    public LocationTag location;
    public ItemTag item;
    public PlayerTakeLecternBookEvent event;

    @Override
    public boolean couldMatch(ScriptPath path) {
        if (!path.eventLower.startsWith("player takes") || !path.eventArgLowerAt(3).equals("from") || !path.eventArgLowerAt(4).equals("lectern")) {
            return false;
        }
        if (!couldMatchItem(path.eventArgLowerAt(2))) {
            return false;
        }
        return true;
    }

    @Override
    public boolean matches(ScriptEvent.ScriptPath path) {
        String itemTest = path.eventArgLowerAt(2);
        if (!tryItem(item, itemTest)) {
            return false;
        }
        if (!runInCheck(path, location)) {
            return false;
        }
        return super.matches(path);
    }

    @Override
    public String getName() {
        return "PlayerTakesFromLectern";
    }

    @Override
    public ScriptEntryData getScriptEntryData() {
        return new BukkitScriptEntryData(PlayerTag.mirrorBukkitPlayer(event.getPlayer()), null);
    }

    @Override
    public ObjectTag getContext(String name) {
        if (name.equals("location")) {
            return location;
        }
        else if (name.equals("item")) {
            return item;
        }
        return super.getContext(name);
    }

    @SubscribeEvent
    public void onPlayerTakesFromLectern(PlayerTakeLecternBookEvent event) {
        if (EntityTag.isNPC(event.getPlayer())) {
            return;
        }
        item = new ItemTag(event.getBook());
        location = new LocationTag(event.getLectern().getLocation());
        this.event = event;
        fire(event);
    }
}
