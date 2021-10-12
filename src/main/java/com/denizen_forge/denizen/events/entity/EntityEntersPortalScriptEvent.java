package com.denizenscript.denizen.events.entity;

import com.denizenscript.denizen.objects.EntityTag;
import com.denizenscript.denizen.objects.LocationTag;
import com.denizenscript.denizen.utilities.implementation.BukkitScriptEntryData;
import com.denizenscript.denizen.events.ForgeScriptEvent;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.scripts.ScriptEntryData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPortalEnterEvent;

public class EntityEntersPortalScriptEvent extends ForgeScriptEvent {

    // <--[event]
    // @Events
    // entity enters portal
    // <entity> enters portal
    //
    // @Regex ^on [^\s]+ enters portal$
    //
    // @Group Entity
    //
    // @Location true
    //
    // @Triggers when an entity enters a portal.
    //
    // @Context
    // <context.entity> returns the EntityTag.
    // <context.location> returns the LocationTag of the portal block touched by the entity.
    //
    // @Player when the entity that entered the portal is a player
    //
    // @NPC when the entity that entered the portal is an NPC.
    //
    // -->

    public EntityEntersPortalScriptEvent() {
        instance = this;
    }

    public static EntityEntersPortalScriptEvent instance;
    public EntityTag entity;
    public LocationTag location;
    public EntityPortalEnterEvent event;

    @Override
    public boolean couldMatch(ScriptPath path) {
        if (!path.eventArgAt(1).equals("enters") || !path.eventArgAt(2).equals("portal")) {
            return false;
        }
        if (!couldMatchEntity(path.eventArgLowerAt(0))) {
            return false;
        }
        return true;
    }

    @Override
    public boolean matches(ScriptPath path) {
        String target = path.eventArgLowerAt(0);

        if (!tryEntity(entity, target)) {
            return false;
        }

        if (!runInCheck(path, entity.getLocation())) {
            return false;
        }

        return super.matches(path);
    }

    @Override
    public String getName() {
        return "EntityEntersPortal";
    }

    @Override
    public ScriptEntryData getScriptEntryData() {
        return new BukkitScriptEntryData(entity);
    }

    @Override
    public ObjectTag getContext(String name) {
        if (name.equals("entity")) {
            return entity;
        }
        else if (name.equals("location")) {
            return location;
        }
        return super.getContext(name);
    }

    @SubscribeEvent
    public void onEntityEntersPortal(EntityPortalEnterEvent event) {
        entity = new EntityTag(event.getEntity());
        location = new LocationTag(event.getLocation());
        this.event = event;
        fire(event);
    }
}
