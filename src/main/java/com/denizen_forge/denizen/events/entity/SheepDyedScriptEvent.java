package com.denizenscript.denizen.events.entity;

import com.denizenscript.denizen.objects.EntityTag;
import com.denizenscript.denizen.events.ForgeScriptEvent;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import org.bukkit.DyeColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.SheepDyeWoolEvent;

public class SheepDyedScriptEvent extends ForgeScriptEvent {

    // <--[event]
    // @Events
    // sheep dyed (<color>)
    //
    // @Regex ^on sheep dyed [^\s]+$
    //
    // @Group Entity
    //
    // @Location true
    //
    // @Cancellable true
    //
    // @Warning Determine color will not update the clientside, use - wait 1t and adjust <context.entity> color:YOUR_COLOR to force-update.
    //
    // @Triggers when a sheep is dyed.
    //
    // @Context
    // <context.entity> returns the EntityTag of the sheep.
    // <context.color> returns an ElementTag of the color the sheep is being dyed.
    //
    // @Determine
    // ElementTag that matches DyeColor to dye it a different color.
    //
    // -->

    public SheepDyedScriptEvent() {
        instance = this;
    }

    public static SheepDyedScriptEvent instance;
    public EntityTag entity;
    public DyeColor color;
    public SheepDyeWoolEvent event;

    @Override
    public boolean couldMatch(ScriptPath path) {
        if (!path.eventLower.startsWith("sheep dyed") && !path.eventLower.startsWith("player dyes sheep")) {
            return false;
        }
        return true;
    }

    @Override
    public boolean matches(ScriptPath path) {
        String cmd = path.eventArgLowerAt(1);

        String new_color = cmd.equals("dyes") ? path.eventArgLowerAt(3) : path.eventArgLowerAt(2);
        if (!new_color.isEmpty() && !new_color.equals(CoreUtilities.toLowerCase(color.toString()))) {
            return false;
        }

        if (!runInCheck(path, entity.getLocation())) {
            return false;
        }

        return super.matches(path);
    }

    @Override
    public String getName() {
        return "SheepDyed";
    }

    @Override
    public boolean applyDetermination(ScriptPath path, ObjectTag determinationObj) {
        if (!isDefaultDetermination(determinationObj)) {
            try {
                color = DyeColor.valueOf(determinationObj.toString().toUpperCase());
                event.setColor(color);
                return true;
            }
            catch (IllegalArgumentException e) {
            }
        }
        return super.applyDetermination(path, determinationObj);
    }

    @Override
    public ObjectTag getContext(String name) {
        if (name.equals("color")) {
            return new ElementTag(color.toString());
        }
        else if (name.equals("entity")) {
            return entity;
        }
        return super.getContext(name);
    }

    @SubscribeEvent
    public void onSheepDyed(SheepDyeWoolEvent event) {
        entity = new EntityTag(event.getEntity());
        color = DyeColor.valueOf(event.getColor().toString());
        this.event = event;
        fire(event);
    }
}
