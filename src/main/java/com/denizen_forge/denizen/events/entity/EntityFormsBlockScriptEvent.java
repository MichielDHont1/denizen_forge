package com.denizenscript.denizen.events.entity;

import com.denizenscript.denizen.objects.EntityTag;
import com.denizenscript.denizen.objects.LocationTag;
import com.denizenscript.denizen.objects.MaterialTag;
import com.denizenscript.denizen.utilities.implementation.BukkitScriptEntryData;
import com.denizenscript.denizen.events.ForgeScriptEvent;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.scripts.ScriptEntryData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.EntityBlockFormEvent;

public class EntityFormsBlockScriptEvent extends ForgeScriptEvent {

    // <--[event]
    // @Events
    // entity forms block
    // entity forms <block>
    // <entity> forms block
    // <entity> forms <block>
    //
    // @Regex ^on [^\s]+ forms [^\s]+$
    //
    // @Group Entity
    //
    // @Location true
    //
    // @Cancellable true
    //
    // @Triggers when a block is formed by an entity.
    // For example, when a snowman forms snow.
    //
    // @Context
    // <context.location> returns the LocationTag the block.
    // <context.material> returns the MaterialTag of the block.
    // <context.entity> returns the EntityTag that formed the block.
    //
    // -->

    public EntityFormsBlockScriptEvent() {
        instance = this;
    }

    public static EntityFormsBlockScriptEvent instance;
    public MaterialTag material;
    public LocationTag location;
    public EntityTag entity;
    public EntityBlockFormEvent event;

    @Override
    public boolean couldMatch(ScriptPath path) {
        if (!path.eventArgLowerAt(1).equals("forms")) {
            return false;
        }
        if (!couldMatchEntity(path.eventArgLowerAt(0))) {
            return false;
        }
        if (!couldMatchBlock(path.eventArgLowerAt(2))) {
            return false;
        }
        return true;
    }

    @Override
    public boolean matches(ScriptPath path) {

        if (!tryEntity(entity, path.eventArgLowerAt(0))) {
            return false;
        }

        if (!tryMaterial(material, path.eventArgLowerAt(2))) {
            return false;
        }

        if (!runInCheck(path, location)) {
            return false;
        }

        return super.matches(path);
    }

    @Override
    public String getName() {
        return "EntityFormsBlock";
    }

    @Override
    public ScriptEntryData getScriptEntryData() {
        return new BukkitScriptEntryData(entity);
    }

    @Override
    public ObjectTag getContext(String name) {
        switch (name) {
            case "location":
                return location;
            case "material":
                return material;
            case "entity":
                return entity;
        }
        return super.getContext(name);
    }

    @SubscribeEvent
    public void onEntityFormsBlock(EntityBlockFormEvent event) {
        location = new LocationTag(event.getBlock().getLocation());
        material = new MaterialTag(event.getBlock());
        entity = new EntityTag(event.getEntity());
        this.event = event;
        fire(event);
    }
}
