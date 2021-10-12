package com.denizenscript.denizen.events.block;

import com.denizenscript.denizen.objects.InventoryTag;
import com.denizenscript.denizen.objects.LocationTag;
import com.denizenscript.denizen.events.ForgeScriptEvent;
import com.denizenscript.denizencore.objects.ObjectTag;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.brewing.PotionBrewEvent;

public class PotionBrewEventScriptEvent extends ForgeScriptEvent {

    // <--[event]
    // @Events
    // brewing stand brews
    //
    // @Regex ^on brewing stand brews$
    //
    // @Group Block
    //
    // @Location true
    //
    // @Cancellable true
    //
    // @Triggers when a brewing stand brews a potion.
    //
    // @Context
    // <context.location> returns the LocationTag of the brewing stand.
    // <context.inventory> returns the InventoryTag of the brewing stand's contents.
    //
    // -->

    public PotionBrewEventScriptEvent() {
        instance = this;
    }

    public static PotionBrewEventScriptEvent instance;
    public LocationTag location;
    public PotionBrewEvent event;

    @Override
    public boolean couldMatch(ScriptPath path) {
        if (!path.eventLower.startsWith("brewing stand brews")) {
            return false;
        }
        return true;
    }

    @Override
    public boolean matches(ScriptPath path) {
        if (!runInCheck(path, location)) {
            return false;
        }
        return super.matches(path);
    }

    @Override
    public String getName() {
        return "Brews";
    }

    @Override
    public ObjectTag getContext(String name) {
        if (name.equals("location")) {
            return location;
        }
        else if (name.equals("inventory")) {
            return InventoryTag.mirrorBukkitInventory(event.getContents());
        }
        return super.getContext(name);
    }

    @SubscribeEvent
    public void onPotionBrewEvent(PotionBrewEvent event) {
        location = new LocationTag(event.getBlock().getLocation());
        this.event = event;
        fire(event);
    }
}
