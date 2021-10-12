package com.denizenscript.denizen.scripts.commands.player;

import com.denizenscript.denizen.utilities.Utilities;
import com.denizenscript.denizen.utilities.debugging.Debug;
import com.denizenscript.denizen.objects.MaterialTag;
import com.denizenscript.denizen.objects.PlayerTag;
import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.objects.Argument;
import com.denizenscript.denizencore.objects.core.DurationTag;
import com.denizenscript.denizencore.objects.ArgumentHelper;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.function.Consumer;

public class ItemCooldownCommand extends AbstractCommand {

    public ItemCooldownCommand() {
        setName("itemcooldown");
        setSyntax("itemcooldown [<material>|...] (duration:<duration>)");
        setRequiredArguments(1, 2);
        isProcedural = false;
    }

    // <--[command]
    // @Name ItemCooldown
    // @Syntax itemcooldown [<material>|...] (duration:<duration>)
    // @Required 1
    // @Maximum 2
    // @Short Places a cooldown on a material in a player's inventory.
    // @Group player
    //
    // @Description
    // Places a cooldown on a material in a player's inventory.
    //
    // @Tags
    // <PlayerTag.item_cooldown[<material>]>
    //
    // @Usage
    // Places a 1 second cooldown on using an ender pearl.
    // - itemcooldown ender_pearl
    //
    // @Usage
    // Places a 10 minute cooldown on using golden apples.
    // - itemcooldown golden_apple d:10m
    //
    // -->

    @Override
    public void addCustomTabCompletions(String arg, Consumer<String> addOne) {
        for (Material material : Material.values()) {
            if (material.isItem()) {
                addOne.accept(material.name());
            }
        }
    }

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {

        for (Argument arg : scriptEntry.getProcessedArgs()) {

            if (!scriptEntry.hasObject("materials")
                    && (arg.matchesArgumentType(MaterialTag.class)
                    || arg.matchesArgumentType(ListTag.class))) {
                scriptEntry.addObject("materials", arg.asType(ListTag.class).filter(MaterialTag.class, scriptEntry));
            }
            else if (!scriptEntry.hasObject("duration")
                    && arg.matchesPrefix("d", "duration")
                    && arg.matchesArgumentType(DurationTag.class)) {
                scriptEntry.addObject("duration", arg.asType(DurationTag.class));
            }
            else {
                arg.reportUnhandled();
            }
        }

        if (!scriptEntry.hasObject("materials")) {
            throw new InvalidArgumentsException("Missing materials argument!");
        }

        scriptEntry.defaultObject("duration", new DurationTag(1));
    }

    @Override
    public void execute(ScriptEntry scriptEntry) {

        ArrayList<MaterialTag> materials = (ArrayList<MaterialTag>) scriptEntry.getObject("materials");
        DurationTag duration = scriptEntry.getObjectTag("duration");
        PlayerTag player = Utilities.getEntryPlayer(scriptEntry);

        if (player == null) {
            Debug.echoError("Invalid linked player.");
            return;
        }

        if (scriptEntry.dbCallShouldDebug()) {

            Debug.report(scriptEntry, getName(), ArgumentHelper.debugList("materials", materials) + duration.debug());

        }

        for (MaterialTag mat : materials) {
            player.getPlayerEntity().setCooldown(mat.getMaterial(), duration.getTicksAsInt());
        }
    }
}
