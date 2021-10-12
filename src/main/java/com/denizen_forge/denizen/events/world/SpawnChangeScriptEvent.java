package com.denizenscript.denizen.events.world;

import com.denizenscript.denizen.objects.LocationTag;
import com.denizenscript.denizen.objects.WorldTag;
import com.denizenscript.denizen.events.ForgeScriptEvent;
import com.denizenscript.denizencore.objects.ObjectTag;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.SpawnChangeEvent;

public class SpawnChangeScriptEvent extends ForgeScriptEvent {

    // <--[event]
    // @Events
    // spawn changes
    //
    // @Regex ^on spawn changes$
    //
    // @Switch for:<world> to only process the event when a specified world's spawn changes.
    //
    // @Group World
    //
    // @Triggers when the world's spawn point changes.
    //
    // @Context
    // <context.world> returns the WorldTag that the spawn point changed in.
    // <context.old_location> returns the LocationTag of the old spawn point.
    // <context.new_location> returns the LocationTag of the new spawn point.
    //
    // -->

    public SpawnChangeScriptEvent() {
        instance = this;
    }

    public static SpawnChangeScriptEvent instance;
    public SpawnChangeEvent event;

    @Override
    public boolean couldMatch(ScriptPath path) {
        if (!path.eventLower.startsWith("spawn changes")) {
            return false;
        }
        return true;
    }

    @Override
    public boolean matches(ScriptPath path) {
        if (path.switches.containsKey("world") && !tryWorld(new WorldTag(event.getWorld()), path.switches.get("world"))) {
            return false;
        }
        return super.matches(path);
    }

    @Override
    public String getName() {
        return "SpawnChange";
    }

    @Override
    public ObjectTag getContext(String name) {
        switch (name) {
            case "world":
                return new WorldTag(event.getWorld());
            case "old_location":
                return new LocationTag(event.getPreviousLocation());
            case "new_location":
                return new LocationTag(event.getWorld().getSpawnLocation());
        }
        return super.getContext(name);
    }

    @SubscribeEvent
    public void onSpawnChange(SpawnChangeEvent event) {
        this.event = event;
        fire(event);
    }
}
