package com.denizenscript.denizen.events.player;

import com.denizenscript.denizen.objects.EntityTag;
import com.denizenscript.denizen.utilities.implementation.BukkitScriptEntryData;
import com.denizenscript.denizen.events.ForgeScriptEvent;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.scripts.ScriptEntryData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;

public class PlayerAnimatesScriptEvent extends ForgeScriptEvent {

    // <--[event]
    // @Events
    // player animates (<animation>)
    //
    // @Regex ^on player animates( [^\s]+)?$
    //
    // @Group Player
    //
    // @Location true
    //
    // @Cancellable true
    //
    // @Triggers when a player performs an animation.
    //
    // @Context
    // <context.animation> returns the name of the animation, from <@link url https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/event/player/PlayerAnimationType.html>.
    //
    // @Player Always.
    //
    // -->

    public PlayerAnimatesScriptEvent() {
        instance = this;
    }

    public static PlayerAnimatesScriptEvent instance;
    public String animation;
    public PlayerAnimationEvent event;

    @Override
    public boolean couldMatch(ScriptPath path) {
        if (!path.eventLower.startsWith("player animates")) {
            return false;
        }
        if (!path.eventArgLowerAt(2).isEmpty() && !couldMatchEnum(path.eventArgLowerAt(2), PlayerAnimationType.values())) {
            return false;
        }
        return true;
    }

    @Override
    public boolean matches(ScriptPath path) {
        if (!runInCheck(path, event.getPlayer().getLocation())) {
            return false;
        }
        String ani = path.eventArgLowerAt(2);
        if (ani.length() > 0 && !ani.equals("in") && !runGenericCheck(ani, animation)) {
            return false;
        }
        return super.matches(path);
    }

    @Override
    public String getName() {
        return "PlayerAnimates";
    }

    @Override
    public ScriptEntryData getScriptEntryData() {
        return new BukkitScriptEntryData(event.getPlayer());
    }

    @Override
    public ObjectTag getContext(String name) {
        if (name.equals("animation")) {
            return new ElementTag(animation);
        }
        return super.getContext(name);
    }

    @SubscribeEvent
    public void onPlayerAnimates(PlayerAnimationEvent event) {
        if (EntityTag.isNPC(event.getPlayer())) {
            return;
        }
        animation = event.getAnimationType().name();
        this.event = event;
        fire(event);
    }
}
