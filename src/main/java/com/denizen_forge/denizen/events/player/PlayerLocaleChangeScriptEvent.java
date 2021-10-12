package com.denizenscript.denizen.events.player;

import com.denizenscript.denizen.utilities.implementation.BukkitScriptEntryData;
import com.denizenscript.denizen.events.ForgeScriptEvent;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.scripts.ScriptEntryData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLocaleChangeEvent;

public class PlayerLocaleChangeScriptEvent extends ForgeScriptEvent {

    // <--[event]
    // @Events
    // player locale change
    //
    // @Regex ^on player locale change$
    //
    // @Group Player
    //
    // @Triggers when a player changes their locale in their client settings.
    //
    // @Context
    // <context.new_locale> returns an ElementTag of the player's new locale.
    //
    // @Player Always.
    //
    // -->

    public PlayerLocaleChangeScriptEvent() {
        instance = this;
    }

    public static PlayerLocaleChangeScriptEvent instance;
    public PlayerLocaleChangeEvent event;

    @Override
    public boolean couldMatch(ScriptPath path) {
        return path.eventLower.startsWith("player locale change");
    }

    @Override
    public String getName() {
        return "PlayerLocaleChange";
    }

    @Override
    public ScriptEntryData getScriptEntryData() {
        return new BukkitScriptEntryData(event.getPlayer());
    }

    @Override
    public ObjectTag getContext(String name) {
        if (name.equals("new_locale")) {
            return new ElementTag(event.getLocale());
        }
        return super.getContext(name);
    }

    @SubscribeEvent
    public void onPlayerLocaleChange(PlayerLocaleChangeEvent event) {
        this.event = event;
        fire(event);
    }
}
