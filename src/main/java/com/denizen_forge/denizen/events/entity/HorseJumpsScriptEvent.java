package com.denizenscript.denizen.events.entity;

import com.denizenscript.denizen.objects.EntityTag;
import com.denizenscript.denizen.events.ForgeScriptEvent;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import org.bukkit.entity.Horse;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.HorseJumpEvent;

public class HorseJumpsScriptEvent extends ForgeScriptEvent {

    // <--[event]
    // @Events
    // horse jumps
    // (<color>) (<type>) jumps
    //
    // @Regex ^on [^\s]+( [^\s]+)? jumps$
    //
    // @Group Entity
    //
    // @Location true
    //
    // @Cancellable true
    //
    // @Triggers when a horse jumps.
    //
    // @Context
    // <context.entity> returns the EntityTag of the horse.
    // <context.color> returns an ElementTag of the horse's color.
    // <context.power> returns an ElementTag(Decimal) of the jump's power.
    //
    // @Determine
    // ElementTag(Decimal) to set the power of the jump.
    //
    // -->

    public HorseJumpsScriptEvent() {
        instance = this;
    }

    public static HorseJumpsScriptEvent instance;
    public EntityTag entity;
    public ElementTag color;
    public HorseJumpEvent event;

    @Override
    public boolean couldMatch(ScriptPath path) {
        if (!path.eventLower.equals("horse jumps") && !path.eventLower.endsWith("jumps")) {
            return false;
        }
        if (path.eventLower.startsWith("player")) {
            return false;
        }
        return true;
    }

    @Override
    public boolean matches(ScriptPath path) {
        String arg1 = path.eventArgLowerAt(0);
        String arg2 = path.eventArgLowerAt(1);
        String tamed = arg2.equals("jumps") ? arg1 : arg2;

        if (!tryEntity(entity, tamed)) {
            return false;
        }

        if (path.eventArgLowerAt(2).equals("jumps") && !arg1.equals(CoreUtilities.toLowerCase(color.toString()))) {
            return false;
        }

        if (!runInCheck(path, entity.getLocation())) {
            return false;
        }

        return super.matches(path);
    }

    @Override
    public String getName() {
        return "HorseJumps";
    }

    @Override
    public boolean applyDetermination(ScriptPath path, ObjectTag determinationObj) {
        if (determinationObj instanceof ElementTag && ((ElementTag) determinationObj).isFloat()) {
            event.setPower(((ElementTag) determinationObj).asFloat());
            return true;
        }
        return super.applyDetermination(path, determinationObj);
    }

    @Override
    public ObjectTag getContext(String name) {
        switch (name) {
            case "entity":
                return entity;
            case "color":
                return color;
            case "power":
                return new ElementTag(event.getPower());
        }
        return super.getContext(name);
    }

    @SubscribeEvent
    public void onHorseJumps(HorseJumpEvent event) {
        if (event.getEntity() instanceof Horse) {
            entity = new EntityTag(event.getEntity());
            color = new ElementTag(((Horse) event.getEntity()).getColor().name());
            this.event = event;
            fire(event);
        }
    }

}