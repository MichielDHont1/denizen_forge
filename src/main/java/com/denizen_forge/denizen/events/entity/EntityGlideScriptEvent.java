package com.denizenscript.denizen.events.entity;

import com.denizenscript.denizen.objects.EntityTag;
import com.denizenscript.denizen.utilities.implementation.BukkitScriptEntryData;
import com.denizenscript.denizen.events.ForgeScriptEvent;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.scripts.ScriptEntryData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityToggleGlideEvent;

public class EntityGlideScriptEvent extends ForgeScriptEvent {

    // <--[event]
    // @Events
    // entity toggles gliding
    // entity starts gliding
    // entity stops gliding
    // <entity> (starts/stops/toggles) gliding
    //
    // @Regex ^on [^\s]+ (toggles|starts|stops) gliding$
    //
    // @Group Entity
    //
    // @Location true
    //
    // @Cancellable true
    //
    // @Triggers when an entity starts or stops gliding.
    //
    // @Context
    // <context.entity> returns the EntityTag of this event.
    // <context.state> returns an ElementTag(Boolean) with a value of "true" if the entity is now gliding and "false" otherwise.
    //
    // @Player when the entity is a player.
    //
    // @NPC when the entity is an NPC.
    //
    // -->

    public EntityGlideScriptEvent() {
        instance = this;
    }

    public static EntityGlideScriptEvent instance;
    public EntityTag entity;
    public boolean state;

    @Override
    public boolean couldMatch(ScriptPath path) {
        if (!path.eventArgLowerAt(2).equals("gliding")) {
            return false;
        }
        String cmd = path.eventArgLowerAt(1);
        if (!cmd.equals("starts") && !cmd.equals("stops") && !cmd.equals("toggles")) {
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

        String cmd = path.eventArgLowerAt(1);
        if (cmd.equals("starts") && !state) {
            return false;
        }
        if (cmd.equals("stops") && state) {
            return false;
        }

        if (!runInCheck(path, entity.getLocation())) {
            return false;
        }

        return super.matches(path);
    }

    @Override
    public String getName() {
        return "EntityGlide";
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
        else if (name.equals("state")) {
            return new ElementTag(state);
        }
        return super.getContext(name);
    }

    @SubscribeEvent
    public void onEntityToggleGlide(EntityToggleGlideEvent event) {
        entity = new EntityTag(event.getEntity());
        state = event.isGliding();
        fire(event);
    }
}
