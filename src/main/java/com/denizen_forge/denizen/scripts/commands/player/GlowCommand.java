package com.denizenscript.denizen.scripts.commands.player;

import com.denizenscript.denizen.utilities.Utilities;
import com.denizenscript.denizen.utilities.debugging.Debug;
import com.denizenscript.denizen.objects.EntityTag;
import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.objects.Argument;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.ArgumentHelper;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.entity.LivingEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

public class GlowCommand extends AbstractCommand {

    public GlowCommand() {
        setName("glow");
        setSyntax("glow [<entity>|...] (<should glow>)");
        setRequiredArguments(1, 2);
        isProcedural = false;
    }

    // <--[command]
    // @Name Glow
    // @Syntax glow [<entity>|...] (<should glow>)
    // @Required 1
    // @Maximum 2
    // @Short Makes the linked player see the chosen entities as glowing.
    // @Group player
    //
    // @Description
    // Makes the linked player see the chosen entities as glowing.
    // BE WARNED, THIS COMMAND IS HIGHLY EXPERIMENTAL AND MAY NOT WORK AS EXPECTED.
    // This command works by globally enabling the glow effect, then whitelisting who is allowed to see it.
    // This command does it's best to disable glow effect when the entity is unloaded, but does not guarantee it.
    //
    // @Tags
    // <EntityTag.glowing>
    //
    // @Usage
    // Use to make the player's target glow.
    // - glow <player.target>
    //
    // @Usage
    // Use to make the player's target not glow.
    // - glow <player.target> false
    // -->

    public static HashMap<Integer, HashSet<UUID>> glowViewers = new HashMap<>();

    public static void unGlow(LivingEntity e) {
        if (glowViewers.containsKey(e.getEntityId())) {
            glowViewers.remove(e.getEntityId());
            e.setGlowing(false);
        }
    }

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {
        for (Argument arg : scriptEntry.getProcessedArgs()) {
            if (!scriptEntry.hasObject("entities")
                    && arg.matchesArgumentList(EntityTag.class)) {
                scriptEntry.addObject("entities", arg.asType(ListTag.class).filter(EntityTag.class, scriptEntry));
            }
            else if (!scriptEntry.hasObject("glowing")
                    && arg.matchesBoolean()) {
                scriptEntry.addObject("glowing", arg.asElement());
            }
            else {
                arg.reportUnhandled();
            }
        }
        scriptEntry.defaultObject("glowing", new ElementTag("true"));
        if (!Utilities.entryHasPlayer(scriptEntry)) {
            throw new InvalidArgumentsException("Must have a valid player link!");
        }
        if (!scriptEntry.hasObject("entities")) {
            throw new InvalidArgumentsException("Must specify entities to make glow!");
        }
    }

    @Override
    public void execute(ScriptEntry scriptEntry) {
        final ArrayList<EntityTag> entities = (ArrayList<EntityTag>) scriptEntry.getObject("entities");
        ElementTag glowing = scriptEntry.getElement("glowing");
        if (scriptEntry.dbCallShouldDebug()) {
            Debug.report(scriptEntry, getName(), ArgumentHelper.debugList("entities", entities) + glowing.debug());
        }
        boolean shouldGlow = glowing.asBoolean();
        final UUID puuid = Utilities.getEntryPlayer(scriptEntry).getUUID();
        for (EntityTag ent : entities) {
            if (shouldGlow) {
                HashSet<UUID> players = glowViewers.computeIfAbsent(ent.getLivingEntity().getEntityId(), k -> new HashSet<>());
                players.add(puuid);
            }
            else {
                HashSet<UUID> players = glowViewers.get(ent.getLivingEntity().getEntityId());
                if (players != null) {
                    players.remove(puuid);
                    shouldGlow = !players.isEmpty();
                    if (!shouldGlow) {
                        glowViewers.remove(ent.getLivingEntity().getEntityId());
                    }
                }
            }
            ent.getLivingEntity().setGlowing(shouldGlow);
        }
    }
}
