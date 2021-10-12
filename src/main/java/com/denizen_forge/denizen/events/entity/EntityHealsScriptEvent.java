package com.denizenscript.denizen.events.entity;

import com.denizenscript.denizen.objects.EntityTag;
import com.denizenscript.denizen.utilities.implementation.BukkitScriptEntryData;
import com.denizenscript.denizen.events.ForgeScriptEvent;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.scripts.ScriptEntryData;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityRegainHealthEvent;

public class EntityHealsScriptEvent extends ForgeScriptEvent {

    // <--[event]
    // @Events
    // entity heals (because <cause>)
    // <entity> heals (because <cause>)
    //
    // @Regex ^on [^\s]+ heals( because [^\s]+)?$
    //
    // @Group Entity
    //
    // @Location true
    //
    // @Cancellable true
    //
    // @Triggers when an entity heals.
    //
    // @Context
    // <context.amount> returns the amount the entity healed.
    // <context.entity> returns the EntityTag that healed.
    // <context.reason> returns the cause of the entity healing. Can be: <@link url https://hub.spigotmc.org/javadocs/spigot/org/bukkit/event/entity/EntityRegainHealthEvent.RegainReason.html>
    //
    // @Determine
    // ElementTag(Decimal) to set the amount of health the entity receives.
    //
    // @Player when the entity that was healed is a player.
    //
    // @NPC when the entity that was healed was an NPC.
    //
    // -->

    public EntityHealsScriptEvent() {
        instance = this;
    }

    public static EntityHealsScriptEvent instance;
    public EntityTag entity;
    public ElementTag reason;
    public EntityRegainHealthEvent event;

    @Override
    public boolean couldMatch(ScriptPath path) {
        if (!path.eventArgLowerAt(1).equals("heals")) {
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

        if (path.eventArgLowerAt(2).equals("because") &&
                !path.eventArgLowerAt(3).equals(CoreUtilities.toLowerCase(reason.toString()))) {
            return false;
        }

        if (!runInCheck(path, entity.getLocation())) {
            return false;
        }

        return super.matches(path);
    }

    @Override
    public String getName() {
        return "EntityHeals";
    }

    @Override
    public boolean applyDetermination(ScriptPath path, ObjectTag determinationObj) {
        if (determinationObj instanceof ElementTag && ((ElementTag) determinationObj).isDouble()) {
            event.setAmount(((ElementTag) determinationObj).asDouble());
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
            case "reason":
                return reason;
            case "amount":
                return new ElementTag(event.getAmount());
        }
        return super.getContext(name);
    }

    @SubscribeEvent
    public void onEntityHeals(EntityRegainHealthEvent event) {
        entity = new EntityTag(event.getEntity());
        reason = new ElementTag(event.getRegainReason().toString());
        this.event = event;
        fire(event);
    }
}
