package com.denizenscript.denizen.events.entity;

import com.denizenscript.denizen.objects.EntityTag;
import com.denizenscript.denizen.objects.LocationTag;
import com.denizenscript.denizen.objects.MaterialTag;
import com.denizenscript.denizen.utilities.debugging.Debug;
import com.denizenscript.denizen.utilities.implementation.BukkitScriptEntryData;
import com.denizenscript.denizen.events.ForgeScriptEvent;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.scripts.ScriptEntryData;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.world.BlockEvent.FarmlandTrampleEvent;

public class FarmlandTrampleScriptEvent extends ForgeScriptEvent {

    // <--[event]
    // @Events
    // entity changes block
    // entity changes block (into <material>)
    // entity changes <material> (into <material>)
    // <entity> changes block (into <material>)
    // <entity> changes <material> (into <material>)
    //
    // @Regex ^on [^\s]+ changes [^\s]+( into [^\s]+)?$
    //
    // @Group Entity
    //
    // @Location true
    //
    // @Cancellable true
    //
    // @Triggers when an entity changes the material of a block.
    //
    // @Context
    // <context.entity> returns the EntityTag that changed the block.
    // <context.location> returns the LocationTag of the changed block.
    // <context.old_material> returns the old material of the block.
    // <context.new_material> returns the new material of the block.
    //
    // @Player when the entity that changed the block is a player.
    //
    // -->

    public FarmlandTrampleScriptEvent() {
        instance = this;
    }

    public static FarmlandTrampleScriptEvent instance;
    public EntityTag entity;
    public LocationTag location;
    public MaterialTag old_material;
    public MaterialTag new_material;
    public FarmlandTrampleEvent event;

    @Override
    public boolean couldMatch(ScriptPath path) {
        if (!path.eventArgLowerAt(1).equals("changes")) {
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
        String entName = path.eventArgLowerAt(0);
        if (!tryEntity(entity, entName)) {
            return false;
        }
        if (!tryMaterial(old_material, path.eventArgLowerAt(2))) {
            return false;
        }
        if (path.eventArgLowerAt(3).equals("into")) {
            String mat2 = path.eventArgLowerAt(4);
            if (mat2.isEmpty()) {
                Debug.echoError("Invalid event material [" + getName() + "]: '" + path.event + "' for " + path.container.getName());
                return false;
            }
            else if (!tryMaterial(new_material, mat2)) {
                return false;
            }
        }
        if (!runInCheck(path, location)) {
            return false;
        }
        return super.matches(path);
    }

    @Override
    public String getName() {
        return "EntityChangesBlock";
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
            case "location":
                return location;
            case "new_material":
                return new_material;
            case "old_material":
                return old_material;
        }
        return super.getContext(name);
    }

    @SubscribeEvent
    public void onFarmlandTrample(FarmlandTrampleEvent event) {
        entity = new EntityTag(event.getEntity());
        location = new LocationTag(event.getBlock().getLocation());
        old_material = new MaterialTag(location.getBlock());
        new_material = new MaterialTag(event.getTo());
        this.event = event;
        fire(event);
    }
}
