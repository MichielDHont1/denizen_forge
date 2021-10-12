package com.denizenscript.denizen.events.item;

import com.denizenscript.denizen.objects.EntityTag;
import com.denizenscript.denizen.objects.InventoryTag;
import com.denizenscript.denizen.objects.ItemTag;
import com.denizenscript.denizen.events.ForgeScriptEvent;
import com.denizenscript.denizencore.objects.ObjectTag;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryPickupItemEvent;

public class InventoryPicksUpItemScriptEvent extends ForgeScriptEvent {

    // <--[event]
    // @Events
    // inventory picks up item
    // inventory picks up <item>
    // <inventory type> picks up item
    // <inventory type> picks up <item>
    //
    // @Regex ^on [^\s]+ picks up [^\s]+$
    //
    // @Group Item
    //
    // @Location true
    //
    // @Cancellable true
    //
    // @Triggers when a hopper or hopper minecart picks up an item.
    //
    // @Context
    // <context.inventory> returns the InventoryTag that picked up the item.
    // <context.item> returns the ItemTag.
    // <context.entity> returns a EntityTag of the item entity.
    //
    // -->

    public InventoryPicksUpItemScriptEvent() {
        instance = this;
    }

    public static InventoryPicksUpItemScriptEvent instance;
    public InventoryTag inventory;
    public ItemTag item;
    public InventoryPickupItemEvent event;

    @Override
    public boolean couldMatch(ScriptPath path) {
        if (!path.eventArgLowerAt(1).equals("picks") || !path.eventArgLowerAt(2).equals("up")) {
            return false;
        }
        if (couldMatchEntity(path.eventArgLowerAt(0))) {
            return false;
        }
        if (!couldMatchInventory(path.eventArgLowerAt(0))) {
            return false;
        }
        if (!couldMatchItem(path.eventArgLowerAt(3))) {
            return false;
        }
        return true;
    }

    @Override
    public boolean matches(ScriptPath path) {
        if (!tryInventory(inventory, path.eventArgLowerAt(0))) {
            return false;
        }
        if (!tryItem(item, path.eventArgLowerAt(3))) {
            return false;
        }
        if (!runInCheck(path, event.getItem().getLocation())) {
            return false;
        }
        return super.matches(path);
    }

    @Override
    public String getName() {
        return "InventoryPicksUpItem";
    }

    @Override
    public ObjectTag getContext(String name) {
        switch (name) {
            case "item":
                return item;
            case "inventory":
                return inventory;
            case "entity":
                return new EntityTag(event.getItem());
        }
        return super.getContext(name);
    }

    @SubscribeEvent
    public void onInvPicksUpItem(InventoryPickupItemEvent event) {
        this.event = event;
        inventory = InventoryTag.mirrorBukkitInventory(event.getInventory());
        item = new ItemTag(event.getItem().getItemStack());
        fire(event);
    }
}
