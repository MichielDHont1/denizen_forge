package com.denizenscript.denizen.events.entity;

import com.denizenscript.denizen.events.ForgeScriptEvent;
import com.denizenscript.denizen.objects.EntityTag;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.ListTag;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTransformEvent;

public class EntityTransformScriptEvent extends ForgeScriptEvent {

    // <--[event]
    // @Events
    // entity transforms
    // <entity> transforms (into <entity>)
    //
    // @Regex ^on [^\s]+ transforms( into [^\s]+)?$
    //
    // @Group Entity
    //
    // @Location true
    // @Switch because:<reason> to only process the event if a specific reason caused the transformation.
    //
    // @Cancellable true
    //
    // @Triggers when an entity transforms into different entities (including villager infections, slime splitting, etc).
    //
    // @Context
    // <context.entity> returns the old entity that was transformed from.
    // <context.new_entities> returns a list of new entities that were transformed into.
    // <context.cause> returns the reason for transformation, from <@link url https://hub.spigotmc.org/javadocs/spigot/org/bukkit/event/entity/EntityTransformEvent.TransformReason.html>.
    //
    // -->

    public EntityTransformScriptEvent() {
        instance = this;
    }

    public static EntityTransformScriptEvent instance;
    public EntityTransformEvent event;
    public EntityTag originalEntity;

    @Override
    public boolean couldMatch(ScriptPath path) {
        if (!path.eventArgLowerAt(1).equals("transforms")) {
            return false;
        }
        if (!couldMatchEntity(path.eventArgLowerAt(0))) {
            return false;
        }
        return true;
    }

    @Override
    public boolean matches(ScriptPath path) {
        if (!runInCheck(path, originalEntity.getLocation())) {
            return false;
        }
        if (!runGenericSwitchCheck(path, "because", event.getTransformReason().name())) {
            return false;
        }
        if (!tryEntity(originalEntity, path.eventArgLowerAt(0))) {
            return false;
        }
        if (path.eventArgLowerAt(2).equals("into") && !tryEntity(new EntityTag(event.getTransformedEntity()), path.eventArgLowerAt(3))) {
            return false;
        }
        return super.matches(path);
    }

    @Override
    public String getName() {
        return "EntityTransforms";
    }

    @Override
    public ObjectTag getContext(String name) {
        switch (name) {
            case "entity":
                return originalEntity.getDenizenObject();
            case "new_entities":
                ListTag output = new ListTag();
                for (Entity ent : event.getTransformedEntities()) {
                    output.addObject(new EntityTag(ent).getDenizenObject());
                }
                return output;
            case "cause":
                return new ElementTag(event.getTransformReason().name());
        }
        return super.getContext(name);
    }

    @SubscribeEvent
    public void onEntityTransform(EntityTransformEvent event) {
        this.event = event;
        originalEntity = new EntityTag(event.getEntity());
        fire(event);
    }
}
