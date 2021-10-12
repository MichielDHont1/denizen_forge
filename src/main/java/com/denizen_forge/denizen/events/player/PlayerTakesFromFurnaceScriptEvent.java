package com.denizenscript.denizen.events.player;

import com.denizenscript.denizen.objects.EntityTag;
import com.denizenscript.denizen.objects.ItemTag;
import com.denizenscript.denizen.objects.LocationTag;
import com.denizenscript.denizen.objects.PlayerTag;
import com.denizenscript.denizen.utilities.implementation.BukkitScriptEntryData;
import com.denizenscript.denizen.events.ForgeScriptEvent;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.scripts.ScriptEntryData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.FurnaceExtractEvent;

public class PlayerTakesFromFurnaceScriptEvent extends ForgeScriptEvent {

    // <--[event]
    // @Events
    // player takes item from furnace
    // player takes <item> from furnace
    //
    // @Regex ^on player takes [^\s]+ from furnace$
    //
    // @Group Player
    //
    // @Location true
    //
    // @Triggers when a player takes an item from a furnace.
    // @Context
    // <context.location> returns the LocationTag of the furnace.
    // <context.item> returns the ItemTag taken out of the furnace.
    //
    // @Determine
    // ElementTag(Number) to set the amount of experience the player will get.
    //
    // @Player Always.
    //
    // -->

    public PlayerTakesFromFurnaceScriptEvent() {
        instance = this;
    }

    public static PlayerTakesFromFurnaceScriptEvent instance;
    public LocationTag location;
    public ItemTag item;
    public FurnaceExtractEvent event;

    @Override
    public boolean couldMatch(ScriptPath path) {
        if (!path.eventLower.startsWith("player takes") || !path.eventArgLowerAt(3).equals("from") || !path.eventArgLowerAt(4).equals("furnace")) {
            return false;
        }
        if (!couldMatchItem(path.eventArgLowerAt(2))) {
            return false;
        }
        return true;
    }

    @Override
    public boolean matches(ScriptPath path) {
        String itemTest = path.eventArgLowerAt(2);

        if (!tryItem(item, itemTest)) {
            return false;
        }
        if (!runInCheck(path, location)) {
            return false;
        }
        return super.matches(path);
    }

    @Override
    public String getName() {
        return "PlayerTakesFromFurnace";
    }

    @Override
    public boolean applyDetermination(ScriptPath path, ObjectTag determinationObj) {
        if (determinationObj instanceof ElementTag && ((ElementTag) determinationObj).isInt()) {
            int xp = ((ElementTag) determinationObj).asInt();
            event.setExpToDrop(xp);
            return true;
        }
        return super.applyDetermination(path, determinationObj);
    }

    @Override
    public ScriptEntryData getScriptEntryData() {
        return new BukkitScriptEntryData(PlayerTag.mirrorBukkitPlayer(event.getPlayer()), null);
    }

    @Override
    public ObjectTag getContext(String name) {
        if (name.equals("location")) {
            return location;
        }
        else if (name.equals("item")) {
            return item;
        }
        return super.getContext(name);
    }

    @SubscribeEvent
    public void onPlayerTakesFromFurnace(FurnaceExtractEvent event) {
        if (EntityTag.isNPC(event.getPlayer())) {
            return;
        }
        item = new ItemTag(event.getItemType(), event.getItemAmount());
        location = new LocationTag(event.getBlock().getLocation());
        this.event = event;
        fire(event);
    }

}
