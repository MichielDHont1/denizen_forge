package com.denizenscript.denizen.scripts.commands.entity;

import com.denizenscript.denizen.nms.NMSHandler;
import com.denizenscript.denizen.objects.notable.NotableManager;
import com.denizenscript.denizen.utilities.Utilities;
import com.denizenscript.denizen.utilities.debugging.Debug;
import com.denizenscript.denizen.objects.EntityTag;
import com.denizenscript.denizen.objects.LocationTag;
import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.objects.Argument;
import com.denizenscript.denizencore.objects.ArgumentHelper;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizencore.objects.notable.Notable;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class TeleportCommand extends AbstractCommand {

    public TeleportCommand() {
        setName("teleport");
        setSyntax("teleport (<entity>|...) [<location>]");
        setRequiredArguments(1, 2);
        isProcedural = false;
    }

    // <--[command]
    // @Name Teleport
    // @Syntax teleport (<entity>|...) [<location>]
    // @Required 1
    // @Maximum 2
    // @Short Teleports the entity(s) to a new location.
    // @Synonyms tp
    // @Group entity
    //
    // @Description
    // Teleports the entity or entities to the new location.
    // Entities can be teleported between worlds using this command.
    //
    // @Tags
    // <EntityTag.location>
    //
    // @Usage
    // Use to teleport a player to the location their cursor is pointing at.
    // - teleport <player> <player.cursor_on>
    //
    // @Usage
    // Use to teleport a player high above.
    // - teleport <player> <player.location.add[0,200,0]>
    //
    // @Usage
    // Use to teleport to a random online player.
    // - teleport <player> <server.online_players.random.location>
    //
    // @Usage
    // Use to teleport all players to your location.
    // - teleport <server.online_players> <player.location>
    //
    // @Usage
    // Use to teleport the NPC to a location that was noted wih the <@link command note> command.
    // - teleport <npc> my_prenoted_location
    // -->

    @Override
    public void addCustomTabCompletions(String arg, Consumer<String> addOne) {
        for (Notable note : NotableManager.notesByType.get(LocationTag.class)) {
            addOne.accept(NotableManager.getSavedId(note));
        }
    }

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {
        for (Argument arg : scriptEntry.getProcessedArgs()) {
            if (!scriptEntry.hasObject("location")
                    && arg.matchesArgumentType(LocationTag.class)) {
                scriptEntry.addObject("location", arg.asType(LocationTag.class));
            }
            else if (!scriptEntry.hasObject("entities")
                    && arg.matchesArgumentList(EntityTag.class)) {
                scriptEntry.addObject("entities", arg.asType(ListTag.class).filter(EntityTag.class, scriptEntry));
            }
            // NPC arg for compatibility with old scripts
            else if (arg.matches("npc") && Utilities.entryHasNPC(scriptEntry)) {
                scriptEntry.addObject("entities", Collections.singletonList(Utilities.getEntryNPC(scriptEntry).getDenizenEntity()));
            }
            else {
                arg.reportUnhandled();
            }
        }
        if (!scriptEntry.hasObject("location")) {
            throw new InvalidArgumentsException("Must specify a location!");
        }
        if (!scriptEntry.hasObject("entities")) {
            scriptEntry.defaultObject("entities", Utilities.entryDefaultEntityList(scriptEntry, true));
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void execute(final ScriptEntry scriptEntry) {
        LocationTag location = scriptEntry.getObjectTag("location");
        List<EntityTag> entities = (List<EntityTag>) scriptEntry.getObject("entities");
        if (scriptEntry.dbCallShouldDebug()) {
            Debug.report(scriptEntry, getName(), ArgumentHelper.debugObj("location", location) +
                    ArgumentHelper.debugObj("entities", entities.toString()));
        }
        for (EntityTag entity : entities) {
            if (entity.isFake && entity.getWorld().equals(location.getWorld())) {
                NMSHandler.getEntityHelper().snapPositionTo(entity.getBukkitEntity(), location.toVector());
                NMSHandler.getEntityHelper().look(entity.getBukkitEntity(), location.getYaw(), location.getPitch());
                return;
            }
            entity.spawnAt(location);
        }
    }
}
