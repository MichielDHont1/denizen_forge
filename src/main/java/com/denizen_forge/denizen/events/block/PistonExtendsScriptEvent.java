package com.denizenscript.denizen.events.block;

import com.denizenscript.denizen.objects.LocationTag;
import com.denizenscript.denizen.objects.MaterialTag;
import com.denizenscript.denizen.events.ForgeScriptEvent;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizencore.objects.ObjectTag;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.world.PistonEvent;

public class PistonScriptEvent extends ForgeScriptEvent {

    // <--[event]
    // @Events
    // piston extends
    // <block> extends
    //
    // @Regex ^on [^\s]+ extends$
    //
    // @Group Block
    //
    // @Location true
    //
    // @Cancellable true
    //
    // @Triggers when a piston extends.
    //
    // @Context
    // <context.location> returns the LocationTag of the piston.
    // <context.material> returns the MaterialTag of the piston.
    // <context.length> returns an ElementTag of the number of blocks that will be moved by the piston.
    // <context.blocks> returns a ListTag of all block locations about to be moved.
    // <context.sticky> returns an ElementTag of whether the piston is sticky.
    // <context.relative> returns a LocationTag of the block in front of the piston.
    //
    // -->

    public PistonScriptEvent() {
        instance = this;
    }

    public static PistonScriptEvent instance;
    public LocationTag location;
    public MaterialTag material;
    public PistonScriptEvent event;

    @Override
    public boolean couldMatch(ScriptPath path) {
        if (!path.eventArgLowerAt(1).equals("extends")) {
            return false;
        }
        if (!path.eventArgLowerAt(0).equals("piston") && !couldMatchBlock(path.eventArgLowerAt(0))) {
            return false;
        }
        return true;
    }

    @Override
    public boolean matches(ScriptPath path) {
        String mat = path.eventArgLowerAt(0);
        if (!runInCheck(path, location)) {
            return false;
        }
        if (!mat.equals("piston") && !tryMaterial(material, mat)) {
            return false;
        }
        return super.matches(path);
    }

    @Override
    public String getName() {
        return "PistonExtends";
    }

    @Override
    public ObjectTag getContext(String name) {
        switch (name) {
            case "location":
                return location;
            case "material":
                return material;
            case "sticky":
                return new ElementTag(event.isSticky());
            case "relative":
                return new LocationTag(event.getBlock().getRelative(event.getDirection()).getLocation());
            case "blocks":
                ListTag blocks = new ListTag();
                for (Block block : event.getBlocks()) {
                    blocks.addObject(new LocationTag(block.getLocation()));
                }
                return blocks;
            case "length":
                return new ElementTag(event.getBlocks().size());
        }
        return super.getContext(name);
    }

    @SubscribeEvent
    public void onPistonEvent(PistonEvent event) {
        location = new LocationTag(event.getBlock().getLocation());
        material = new MaterialTag(event.getBlock());
        this.event = event;
        fire(event);
    }
}
