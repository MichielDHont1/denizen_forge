package com.denizenscript.denizen.events.player;

import com.denizenscript.denizen.objects.EntityTag;
import com.denizenscript.denizen.utilities.implementation.BukkitScriptEntryData;
import com.denizenscript.denizen.events.ForgeScriptEvent;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.scripts.ScriptEntryData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerEggThrowEvent;

public class PlayerThrowsEggScriptEvent extends ForgeScriptEvent {

    // <--[event]
    // @Events
    // player throws (hatching/non-hatching) egg
    //
    // @Regex ^on player throws( (hatching|non-hatching))? egg$
    //
    // @Group Player
    //
    // @Location true
    //
    // @Cancellable true
    //
    // @Triggers when a player throws an egg.
    //
    // @Context
    // <context.egg> returns the EntityTag of the egg.
    // <context.is_hatching> returns an ElementTag with a value of "true" if the egg will hatch and "false" otherwise.
    //
    // @Determine
    // EntityTag to set the type of the hatching entity.
    //
    // @Player Always.
    //
    // -->

    public PlayerThrowsEggScriptEvent() {
        instance = this;
    }

    public static PlayerThrowsEggScriptEvent instance;
    public EntityTag egg;
    public PlayerEggThrowEvent event;

    @Override
    public boolean couldMatch(ScriptPath path) {
        if (!path.eventLower.startsWith("player throws") || !path.eventLower.contains("egg")) {
            return false;
        }
        String type = path.eventArgLowerAt(2);
        if (!type.equals("hatching") && !type.equals("non-hatching") && !type.equals("egg")) {
            return false;
        }
        return true;
    }

    @Override
    public boolean matches(ScriptPath path) {
        if (path.eventArgLowerAt(2).equals("hatching") && !event.isHatching()) {
            return false;
        }
        if (path.eventArgLowerAt(2).equals("non-hatching") && event.isHatching()) {
            return false;
        }

        if (!runInCheck(path, egg.getLocation())) {
            return false;
        }
        return super.matches(path);
    }

    @Override
    public String getName() {
        return "PlayerThrowsEgg";
    }

    @Override
    public boolean applyDetermination(ScriptPath path, ObjectTag determinationObj) {
        String determination = determinationObj.toString();
        if (EntityTag.matches(determination)) {
            event.setHatching(true);
            EntityType type = EntityTag.valueOf(determination, getTagContext(path)).getBukkitEntityType();
            event.setHatchingType(type);
            return true;
        }
        return super.applyDetermination(path, determinationObj);
    }

    @Override
    public ScriptEntryData getScriptEntryData() {
        return new BukkitScriptEntryData(event.getPlayer());
    }

    @Override
    public ObjectTag getContext(String name) {
        if (name.equals("is_hatching")) {
            return new ElementTag(event.isHatching());
        }
        else if (name.equals("egg")) {
            return egg;
        }
        return super.getContext(name);
    }

    @Override
    public void cancellationChanged() {
        event.setHatching(!cancelled);
    }

    @SubscribeEvent
    public void onPlayerThrowsEgg(PlayerEggThrowEvent event) {
        if (EntityTag.isNPC(event.getPlayer())) {
            return;
        }
        Entity eggEntity = event.getEgg();
        EntityTag.rememberEntity(eggEntity);
        egg = new EntityTag(event.getEgg());
        this.event = event;
        cancelled = false;
        fire(event);
        EntityTag.forgetEntity(eggEntity);
    }
}
