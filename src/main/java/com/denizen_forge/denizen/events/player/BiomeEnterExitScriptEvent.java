package com.denizenscript.denizen.events.player;

import com.denizenscript.denizen.objects.BiomeTag;
import com.denizenscript.denizen.objects.EntityTag;
import com.denizenscript.denizen.utilities.implementation.BukkitScriptEntryData;
import com.denizenscript.denizen.events.ForgeScriptEvent;
import com.denizenscript.denizen.objects.LocationTag;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.scripts.ScriptEntryData;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import org.bukkit.block.Biome;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class BiomeEnterExitScriptEvent extends ForgeScriptEvent {

    // <--[event]
    // @Events
    // player enters <biome>
    // player exits <biome>
    // player enters biome
    // player exits biome
    //
    // @Group Player
    //
    // @Regex ^on player (enters|exits) [^\s]+$
    //
    // @Location true
    //
    // @Warning Cancelling this event will fire a similar event immediately after.
    //
    // @Cancellable true
    //
    // @Triggers when a player enters or exits a biome.
    //
    // @Context
    // <context.from> returns the block location moved from.
    // <context.to> returns the block location moved to.
    // <context.old_biome> returns the biome being left.
    // <context.new_biome> returns the biome being entered.
    //
    // @Player Always.
    //
    // -->

    public BiomeEnterExitScriptEvent() {
        instance = this;
    }

    public static BiomeEnterExitScriptEvent instance;

    public LocationTag from;
    public LocationTag to;
    public BiomeTag old_biome;
    public BiomeTag new_biome;
    public PlayerMoveEvent event;

    @Override
    public boolean couldMatch(ScriptPath path) {
        if (!path.eventLower.startsWith("player enters") && !path.eventLower.startsWith("player exits")) {
            return false;
        }
        if (!path.eventArgLowerAt(2).equals("biome") && !couldMatchEnum(path.eventArgLowerAt(2), Biome.values())) {
            return false;
        }
        return true;
    }

    @Override
    public boolean matches(ScriptPath path) {
        String biome_test = path.eventArgAt(2);
        String direction = path.eventArgAt(1);

        if (!runInCheck(path, from) && !runInCheck(path, to)) {
            return false;
        }

        BiomeTag biome = direction.equals("enters") ? new_biome : (direction.equals("exits") ? old_biome : null);
        if (biome == null) {
            return false;
        }

        if (!biome_test.equals("biome") && !biome_test.equals(CoreUtilities.toLowerCase(biome.getBiome().getName()))) {
            return false;
        }
        return super.matches(path);
    }

    @Override
    public String getName() {
        return "BiomeEnterExit";
    }

    @Override
    public ScriptEntryData getScriptEntryData() {
        return new BukkitScriptEntryData(EntityTag.getPlayerFrom(event.getPlayer()), null);
    }

    @Override
    public ObjectTag getContext(String name) {
        switch (name) {
            case "to":
                return to;
            case "from":
                return from;
            case "old_biome":
                return old_biome;
            case "new_biome":
                return new_biome;
        }
        return super.getContext(name);
    }

    @SubscribeEvent
    public void onPlayerEntersExitsBiome(PlayerMoveEvent event) {
        if (LocationTag.isSameBlock(event.getFrom(), event.getTo())) {
            return;
        }
        if (event.getFrom().getBlockY() < 0 || event.getFrom().getBlockY() > 255
            || event.getTo().getBlockY() < 0 || event.getTo().getBlockY() > 255) {
            return;
        }
        from = new LocationTag(event.getFrom());
        to = new LocationTag(event.getTo());
        old_biome = new BiomeTag(from.getBlock().getBiome());
        new_biome = new BiomeTag(to.getBlock().getBiome());
        if (old_biome.identify().equals(new_biome.identify())) {
            return;
        }
        this.event = event;
        fire(event);
    }
}
