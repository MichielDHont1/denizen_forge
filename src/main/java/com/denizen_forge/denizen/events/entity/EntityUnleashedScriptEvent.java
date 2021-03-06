package com.denizenscript.denizen.events.entity;

import com.denizenscript.denizen.objects.EntityTag;
import com.denizenscript.denizen.events.ForgeScriptEvent;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityUnleashEvent;

public class EntityUnleashedScriptEvent extends ForgeScriptEvent {

    // <--[event]
    // @Events
    // entity unleashed (because <reason>)
    // <entity> unleashed (because <reason>)
    //
    // @Regex ^on [^\s]+ unleashed( because [^\s]+)?$
    //
    // @Group Entity
    //
    // @Location true
    //
    // @Triggers when an entity is unleashed.
    //
    // @Context
    // <context.entity> returns the EntityTag.
    // <context.reason> returns an ElementTag of the reason for the unleashing.
    // Reasons include DISTANCE, HOLDER_GONE, PLAYER_UNLEASH, and UNKNOWN
    //
    // @NPC when the entity being unleashed is an NPC.
    //
    // -->

    public EntityUnleashedScriptEvent() {
        instance = this;
    }

    public static EntityUnleashedScriptEvent instance;
    public EntityTag entity;
    public ElementTag reason;
    public EntityUnleashEvent event;

    @Override
    public boolean couldMatch(ScriptPath path) {
        if (!path.eventArgLowerAt(1).equals("unleashed")) {
            return false;
        }
        if (!couldMatchEntity(path.eventArgLowerAt(0))) {
            return false;
        }
        return true;
    }

    @Override
    public boolean matches(ScriptPath path) {

        if (!tryEntity(entity, path.eventArgLowerAt(0))) {
            return false;
        }

        if (path.eventArgAt(2).equals("because") && !path.eventArgLowerAt(3).equals(CoreUtilities.toLowerCase(reason.asString()))) {
            return false;
        }

        if (!runInCheck(path, entity.getLocation())) {
            return false;
        }

        return super.matches(path);
    }

    @Override
    public String getName() {
        return "EntityUnleashed";
    }

    @Override
    public ObjectTag getContext(String name) {
        if (name.equals("entity")) {
            return entity;
        }
        else if (name.equals("reason")) {
            return reason;
        }
        return super.getContext(name);
    }

    @SubscribeEvent
    public void onEntityUnleashed(EntityUnleashEvent event) {
        entity = new EntityTag(event.getEntity());
        reason = new ElementTag(event.getReason().toString());
        this.event = event;
        fire(event);
    }

}
