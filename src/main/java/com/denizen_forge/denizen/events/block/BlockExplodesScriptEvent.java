package com.denizenscript.denizen.events.block;

import com.denizenscript.denizen.events.ForgeScriptEvent;
import com.denizenscript.denizen.objects.LocationTag;
import com.denizenscript.denizen.objects.MaterialTag;
import com.denizenscript.denizen.utilities.debugging.Debug;
import com.denizenscript.denizencore.objects.ArgumentHelper;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.ListTag;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.world.ExplosionEvent;

import java.util.List;

public class ExplosionScriptEvent extends ForgeScriptEvent {

    // <--[event]
    // @Events
    // block explodes
    // <block> explodes
    //
    // @Regex ^on [^\s]+ explodes$
    //
    // @Group Block
    //
    // @Location true
    //
    // @Cancellable true
    //
    // @Triggers when a block explodes.
    //
    // @Context
    // <context.blocks> returns a ListTag of blocks that the entity blew up.
    // <context.strength> returns an ElementTag(Decimal) of the strength of the explosion.
    //
    // @Determine
    // ListTag(LocationTag) to set a new lists of blocks that are to be affected by the explosion.
    // ElementTag(Decimal) to change the strength of the explosion.
    //
    // -->

    public ExplosionScriptEvent() {
        instance = this;
    }

    public static ExplosionScriptEvent instance;
    public ExplosionEvent event;
    public List<Block> blocks;

    @Override
    public boolean couldMatch(ScriptPath path) {
        if (!path.eventArgLowerAt(1).equals("explodes")) {
            return false;
        }
        if (!couldMatchBlock(path.eventArgLowerAt(0))) {
            return false;
        }
        return true;
    }

    @Override
    public boolean matches(ScriptPath path) {
        String target = path.eventArgLowerAt(0);
        if (!target.equals("block")) {
            boolean matched = false;
            for (Block block : blocks) {
                if (tryMaterial(new MaterialTag(block.getType()), target)) {
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                return false;
            }
        }
        if (blocks.size() > 0 && !runInCheck(path, blocks.get(0).getLocation())) {
            return false;
        }
        return super.matches(path);
    }

    @Override
    public String getName() {
        return "BlockExplodes";
    }

    @Override
    public boolean applyDetermination(ScriptPath path, ObjectTag determinationObj) {
        String determination = determinationObj.toString();
        if (ArgumentHelper.matchesDouble(determination)) {
            event.setYield(Float.parseFloat(determination));
            return true;
        }
        if (ListTag.matches(determination)) {
            event.blockList().clear();
            for (String loc : ListTag.valueOf(determination, getTagContext(path))) {
                LocationTag location = LocationTag.valueOf(loc, getTagContext(path));
                if (location == null) {
                    Debug.echoError("Invalid location '" + loc + "' check [" + getName() + "]: '  for " + path.container.getName());
                }
                else {
                    event.blockList().add(location.getWorld().getBlockAt(location));
                }
            }
            return true;
        }
        return super.applyDetermination(path, determinationObj);
    }

    @Override
    public ObjectTag getContext(String name) {
        if (name.equals("blocks")) {
            ListTag blocks = new ListTag();
            for (Block block : this.blocks) {
                blocks.addObject(new LocationTag(block.getLocation()));
            }
            return blocks;
        }
        else if (name.equals("strength")) {
            return new ElementTag(event.getYield());
        }
        return super.getContext(name);
    }

    @SubscribeEvent
    public void ExplosionEvent(ExplosionEvent event) {
        this.blocks = event.blockList();
        this.event = event;
        fire(event);
    }
}
