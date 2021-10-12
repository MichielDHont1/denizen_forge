package com.denizenscript.denizen.events.block;

import com.denizenscript.denizen.events.ForgeScriptEvent;
import com.denizenscript.denizen.objects.LocationTag;
import com.denizenscript.denizen.objects.MaterialTag;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.world.BlockEvent.EntityPlaceEvent;
public class BlockBuiltScriptEvent extends ForgeScriptEvent {
    // <--[event]
    // @Events
    // block being built (on <material>)
    // <material> being built (on <material>)
    //
    // @Regex ^on [^\s]+ being built$
    //
    // @Group Block
    //
    // @Location true
    //
    // @Cancellable true
    //
    // @Triggers when an attempt is made to build a block on another block. Not necessarily caused by players.
    //
    // @Context
    // <context.location> returns the LocationTag of the block the player is trying to build on.
    // <context.old_material> returns the MaterialTag of the block the player is trying to build on.
    // <context.new_material> returns the MaterialTag of the block the player is trying to build.
    //
    // @Determine
    // "BUILDABLE" to allow the building.
    //
    // -->

    public BlockBuiltScriptEvent() {
        instance = this;
    }

    public static BlockBuiltScriptEvent instance;
    public LocationTag location;
    public MaterialTag old_material;
    public MaterialTag new_material;
    public EntityPlaceEvent event;

    @Override
    public boolean couldMatch(ScriptPath path) {
        if (!path.eventArgLowerAt(1).equals("being") ||!path.eventArgLowerAt(2).equals("built")) {
            return false;
        }
        if (!couldMatchBlock(path.eventArgLowerAt(0))) {
            return false;
        }
        return true;
    }

    @Override
    public boolean matches(ScriptPath path) {

        if (!runInCheck(path, location)) {
            return false;
        }

        String mat2 = path.eventArgLowerAt(4);
        if (mat2.length() > 0 && !tryMaterial(old_material, mat2)) {
            return false;
        }
        if (!tryMaterial(new_material, path.eventArgLowerAt(0))) {
            return false;
        }
        return super.matches(path);
    }

    @Override
    public String getName() {
        return "BlockBuilt";
    }

    @Override
    public boolean applyDetermination(ScriptPath path, ObjectTag determinationObj) {
        if (determinationObj instanceof ElementTag) {
            String determination = determinationObj.toString();
            String lower = CoreUtilities.toLowerCase(determination);
            if (lower.equals("buildable")) {
                cancelled = false;
            }
            return true;
        }
        return super.applyDetermination(path, determinationObj);
    }

    @Override
    public void cancellationChanged() {
        event.setBuildable(!cancelled);
        super.cancellationChanged();
    }

    @Override
    public ObjectTag getContext(String name) {
        switch (name) {
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
    public void onBlockBuilt(EntityPlaceEvent event) {
        // available data world, pos, state, entity, blocksnapshot, placedblock, placedagainst
        location = new LocationTag(event.getPos(), event.getWorld());
        old_material = new MaterialTag(event.getBlockSnapshot());
        new_material = new MaterialTag(event.placedBlock());
        this.event = event;
        fire(event);
    }
}
