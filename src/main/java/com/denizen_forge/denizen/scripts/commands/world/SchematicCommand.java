package com.denizenscript.denizen.scripts.commands.world;

import com.denizenscript.denizen.Denizen;
import com.denizenscript.denizen.objects.PlayerTag;
import com.denizenscript.denizen.utilities.Utilities;
import com.denizenscript.denizen.utilities.blocks.*;
import com.denizenscript.denizen.utilities.debugging.Debug;
import com.denizenscript.denizen.objects.CuboidTag;
import com.denizenscript.denizen.objects.LocationTag;
import com.denizenscript.denizen.objects.MaterialTag;
import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.objects.*;
import com.denizenscript.denizencore.objects.core.DurationTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.denizenscript.denizencore.scripts.commands.Holdable;
import com.denizenscript.denizencore.tags.Attribute;
import com.denizenscript.denizencore.tags.ReplaceableTagEvent;
import com.denizenscript.denizencore.tags.TagManager;
import com.denizenscript.denizencore.tags.TagRunnable;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPhysicsEvent;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class SchematicCommand extends AbstractCommand implements Holdable, Listener {

    public SchematicCommand() {
        setName("schematic");
        setSyntax("schematic [create/load/unload/rotate (angle:<#>)/paste (fake_to:<player>|... fake_duration:<duration>)/save/flip_x/flip_y/flip_z) (noair) (mask:<material>|...)] [name:<name>] (filename:<name>) (<location>) (<cuboid>) (delayed) (max_delay_ms:<#>) (entities) (flags)");
        setRequiredArguments(2, 13);
        TagManager.registerTagHandler(new TagRunnable.RootForm() {
            @Override
            public void run(ReplaceableTagEvent event) {
                schematicTags(event);
            }
        }, "schematic");
        schematics = new HashMap<>();
        noPhys = false;
        Bukkit.getPluginManager().registerEvents(this, Denizen.getInstance());
        isProcedural = false;
    }

    // <--[command]
    // @Name Schematic
    // @Syntax schematic [create/load/unload/rotate (angle:<#>)/paste (fake_to:<player>|... fake_duration:<duration>)/save/flip_x/flip_y/flip_z) (noair) (mask:<material>|...)] [name:<name>] (filename:<name>) (<location>) (<cuboid>) (delayed) (max_delay_ms:<#>) (entities) (flags)
    // @Group world
    // @Required 2
    // @Maximum 13
    // @Short Creates, loads, pastes, and saves schematics (Sets of blocks).
    //
    // @Description
    // Creates, loads, pastes, and saves schematics. Schematics are files containing info about blocks and the order of those blocks.
    //
    // Denizen offers a number of tools to manipulate and work with schematics.
    // Schematics can be rotated, flipped, pasted with no air, or pasted with a delay.
    //
    // All schematic command usages must specify the "name" argument, which is a unique global identifier of the schematic in memory.
    // This will be created by "create" or "load" options, and persist in memory until "unload" is used (or the server is restarted).
    //
    // The 'create' option requires a cuboid region and a center location as input. This will create a new schematic in memory based on world data.
    //
    // The "rotate" and "flip_x/y/z" options will apply the change to the copy of the schematic in memory, to later be pasted or saved.
    // This will rotate the set of blocks itself, the relative origin, and any directional blocks inside the schematic.
    //
    // The "delayed" option makes the command non-instant. This is recommended for large schematics.
    // For 'save', 'load', and 'rotate', this processes async to prevent server lockup.
    // For 'paste' and 'create', this delays how many blocks can be processed at once, spread over many ticks.
    // Optionally, specify 'max_delay_ms' to control how many milliseconds the 'delayed' set can run for in any given tick (defaults to 50) (for create/paste only).
    //
    // The "load" option by default will load '.schem' files. If no '.schem' file is available, will attempt to load a legacy '.schematic' file instead.
    //
    // For load and save, the "filename" option is available to specify the name of the file to look for.
    // If unspecified, the filename will default to the same as the "name" input.
    //
    // The "noair" option skips air blocks in the pasted schematics- this means those air blocks will not replace any blocks in the target location.
    //
    // The "mask" option can be specified to limit what block types the schematic will be pasted over.
    //
    // The "fake_to" option can be specified to cause the schematic paste to be a fake (packet-based, see <@link command showfake>)
    // block set, instead of actually modifying the blocks in the world.
    // This takes an optional duration as "fake_duration" for how long the fake blocks should remain.
    //
    // The "create" and "paste" options allow the "entities" argument to be specified - when used, entities will be copied or pasted.
    // At current time, entity types included will be: Paintings, ItemFrames, ArmorStands.
    //
    // The "create" option allows the "flags" argument to be specified - when used, block location flags will be copied.
    //
    // The schematic command is ~waitable as an alternative to 'delayed' argument. Refer to <@link language ~waitable>.
    //
    // @Tags
    // <schematic[<name>].height>
    // <schematic[<name>].length>
    // <schematic[<name>].width>
    // <schematic[<name>].block[<location>]>
    // <schematic[<name>].origin>
    // <schematic[<name>].blocks>
    // <schematic[<name>].exists>
    // <schematic[<name>].cuboid[<origin location>]>
    // <schematic.list>
    //
    // @Usage
    // Use to create a new schematic from a cuboid and an origin location.
    // - schematic create name:MySchematic <[my_cuboid]> <player.location>
    //
    // @Usage
    // Use to load a schematic.
    // - ~schematic load name:MySchematic
    //
    // @Usage
    // Use to unload a schematic.
    // - schematic unload name:MySchematic
    //
    // @Usage
    // Use to paste a loaded schematic with no air blocks.
    // - schematic paste name:MySchematic <player.location> noair
    //
    // @Usage
    // Use to save a created schematic.
    // - ~schematic save name:MySchematic
    // -->

    public static boolean noPhys = false;

    @SubscribeEvent
    public void onBlockPhysics(BlockPhysicsEvent event) {
        if (noPhys) {
            event.setCancelled(true);
        }
    }

    @Override
    public void addCustomTabCompletions(String arg, Consumer<String> addOne) {
        if (arg.startsWith("name:")) {
            for (String schem : schematics.keySet()) {
                addOne.accept("name:" + schem);
            }
        }
    }

    private enum Type {CREATE, LOAD, UNLOAD, ROTATE, PASTE, SAVE, FLIP_X, FLIP_Y, FLIP_Z}

    public static Map<String, CuboidBlockSet> schematics;

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {
        for (Argument arg : scriptEntry.getProcessedArgs()) {
            if (!scriptEntry.hasObject("type")
                    && arg.matchesEnum(Type.values())) {
                scriptEntry.addObject("type", new ElementTag(arg.getRawValue().toUpperCase()));
            }
            else if (!scriptEntry.hasObject("name")
                    && arg.matchesPrefix("name")) {
                scriptEntry.addObject("name", arg.asElement());
            }
            else if (!scriptEntry.hasObject("filename")
                    && arg.matchesPrefix("filename")) {
                scriptEntry.addObject("filename", arg.asElement());
            }
            else if (!scriptEntry.hasObject("angle")
                    && arg.matchesPrefix("angle")
                    && arg.matchesInteger()) {
                scriptEntry.addObject("angle", arg.asElement());
            }
            else if (!scriptEntry.hasObject("max_delay_ms")
                    && arg.matchesPrefix("max_delay_ms")
                    && arg.matchesInteger()) {
                scriptEntry.addObject("max_delay_ms", arg.asElement());
            }
            else if (!scriptEntry.hasObject("delayed")
                    && arg.matches("delayed")) {
                scriptEntry.addObject("delayed", new ElementTag("true"));
            }
            else if (!scriptEntry.hasObject("noair")
                    && arg.matches("noair")) {
                scriptEntry.addObject("noair", new ElementTag("true"));
            }
            else if (!scriptEntry.hasObject("entities")
                    && arg.matches("entities")) {
                scriptEntry.addObject("entities", new ElementTag("true"));
            }
            else if (!scriptEntry.hasObject("flags")
                    && arg.matches("flags")) {
                scriptEntry.addObject("flags", new ElementTag("true"));
            }
            else if (!scriptEntry.hasObject("mask")
                    && arg.matchesPrefix("mask")
                    && arg.matchesArgumentList(MaterialTag.class)) {
                scriptEntry.addObject("mask", arg.asType(ListTag.class).filter(MaterialTag.class, scriptEntry));
            }
            else if (!scriptEntry.hasObject("fake_to")
                    && arg.matchesPrefix("fake_to")
                    && arg.matchesArgumentList(PlayerTag.class)) {
                scriptEntry.addObject("fake_to", arg.asType(ListTag.class).filter(PlayerTag.class, scriptEntry));
            }
            else if (!scriptEntry.hasObject("fake_duration")
                    && arg.matchesPrefix("fake_duration")
                    && arg.matchesArgumentType(DurationTag.class)) {
                scriptEntry.addObject("fake_duration", arg.asType(DurationTag.class));
            }
            else if (!scriptEntry.hasObject("location")
                    && arg.matchesArgumentType(LocationTag.class)) {
                scriptEntry.addObject("location", arg.asType(LocationTag.class).getBlockLocation());
            }
            else if (!scriptEntry.hasObject("cuboid")
                    && arg.matchesArgumentType(CuboidTag.class)) {
                scriptEntry.addObject("cuboid", arg.asType(CuboidTag.class));
            }
            else {
                arg.reportUnhandled();
            }
        }
        if (scriptEntry.shouldWaitFor()) {
            scriptEntry.addObject("delayed", new ElementTag("true"));
        }
        scriptEntry.defaultObject("max_delay_ms", new ElementTag(50));
        if (!scriptEntry.hasObject("type")) {
            throw new InvalidArgumentsException("Missing type argument!");
        }
        if (!scriptEntry.hasObject("name")) {
            throw new InvalidArgumentsException("Missing name argument!");
        }
    }

    @Override
    public void execute(final ScriptEntry scriptEntry) {
        ElementTag angle = scriptEntry.getElement("angle");
        ElementTag type = scriptEntry.getElement("type");
        ElementTag name = scriptEntry.getElement("name");
        ElementTag filename = scriptEntry.getElement("filename");
        ElementTag noair = scriptEntry.getElement("noair");
        ElementTag delayed = scriptEntry.getElement("delayed");
        ElementTag maxDelayMs = scriptEntry.getElement("max_delay_ms");
        ElementTag copyEntities = scriptEntry.getElement("entities");
        ElementTag flags = scriptEntry.getElement("flags");
        LocationTag location = scriptEntry.getObjectTag("location");
        List<MaterialTag> mask = (List<MaterialTag>) scriptEntry.getObject("mask");
        List<PlayerTag> fakeTo = (List<PlayerTag>) scriptEntry.getObject("fake_to");
        DurationTag fakeDuration = scriptEntry.getObjectTag("fake_duration");
        CuboidTag cuboid = scriptEntry.getObjectTag("cuboid");
        if (scriptEntry.dbCallShouldDebug()) {
            Debug.report(scriptEntry, getName(), type, name, location, filename, cuboid, angle, noair, delayed, maxDelayMs, fakeDuration, flags,
                    (mask != null ? ArgumentHelper.debugList("mask", mask) : ""), (fakeTo != null ? ArgumentHelper.debugList("fake_to", fakeTo) : ""));
        }
        CuboidBlockSet set;
        Type ttype = Type.valueOf(type.asString());
        String fname = filename != null ? filename.asString() : name.asString();
        switch (ttype) {
            case CREATE: {
                if (schematics.containsKey(name.asString().toUpperCase())) {
                    Debug.echoError(scriptEntry.getResidingQueue(), "Schematic file " + name.asString() + " is already loaded.");
                    scriptEntry.setFinished(true);
                    return;
                }
                if (cuboid == null) {
                    Debug.echoError(scriptEntry.getResidingQueue(), "Missing cuboid argument!");
                    scriptEntry.setFinished(true);
                    return;
                }
                if (location == null) {
                    Debug.echoError(scriptEntry.getResidingQueue(), "Missing origin location argument!");
                    scriptEntry.setFinished(true);
                    return;
                }
                try {
                    if (delayed != null && delayed.asBoolean()) {
                        set = new CuboidBlockSet();
                        set.buildDelayed(cuboid, location, () -> {
                            if (copyEntities != null && copyEntities.asBoolean()) {
                                set.buildEntities(cuboid, location);
                            }
                            schematics.put(name.asString().toUpperCase(), set);
                            scriptEntry.setFinished(true);
                        }, maxDelayMs.asLong(), flags != null && flags.asBoolean());
                    }
                    else {
                        scriptEntry.setFinished(true);
                        set = new CuboidBlockSet(cuboid, location, flags != null && flags.asBoolean());
                        if (copyEntities != null && copyEntities.asBoolean()) {
                            set.buildEntities(cuboid, location);
                        }
                        schematics.put(name.asString().toUpperCase(), set);
                    }
                }
                catch (Exception ex) {
                    Debug.echoError(scriptEntry.getResidingQueue(), "Error creating schematic object " + name.asString() + ".");
                    Debug.echoError(scriptEntry.getResidingQueue(), ex);
                    scriptEntry.setFinished(true);
                    return;
                }
                break;
            }
            case LOAD: {
                if (schematics.containsKey(name.asString().toUpperCase())) {
                    Debug.echoError(scriptEntry.getResidingQueue(), "Schematic file " + name.asString() + " is already loaded.");
                    scriptEntry.setFinished(true);
                    return;
                }
                String directory = URLDecoder.decode(System.getProperty("user.dir"));
                File f = new File(directory + "/plugins/Denizen/schematics/" + fname + ".schem");
                if (!Utilities.canReadFile(f)) {
                    Debug.echoError("Cannot read from that file path due to security settings in Denizen/config.yml.");
                    scriptEntry.setFinished(true);
                    return;
                }
                if (!f.exists()) {
                    f = new File(directory + "/plugins/Denizen/schematics/" + fname + ".schematic");
                    if (!f.exists()) {
                        Debug.echoError("Schematic file " + fname + " does not exist. Are you sure it's in " + directory + "/plugins/Denizen/schematics/?");
                        scriptEntry.setFinished(true);
                        return;
                    }
                }
                File schemFile = f;
                Runnable loadRunnable = () -> {
                    try {
                        InputStream fs = new FileInputStream(schemFile);
                        CuboidBlockSet newSet;
                        newSet = SpongeSchematicHelper.fromSpongeStream(fs);
                        fs.close();
                        Runnable storeSchem = () -> {
                            schematics.put(name.asString().toUpperCase(), newSet);
                            scriptEntry.setFinished(true);
                        };
                        if (delayed != null && delayed.asBoolean()) {
                            Bukkit.getScheduler().runTask(Denizen.getInstance(), storeSchem);
                        }
                        else {
                            storeSchem.run();
                        }
                    }
                    catch (Exception ex) {
                        Runnable showError = () -> {
                            Debug.echoError(scriptEntry.getResidingQueue(), "Error loading schematic file " + name.asString() + ".");
                            Debug.echoError(scriptEntry.getResidingQueue(), ex);
                        };
                        if (delayed != null && delayed.asBoolean()) {
                            Bukkit.getScheduler().runTask(Denizen.getInstance(), showError);
                        }
                        else {
                            showError.run();
                        }
                        scriptEntry.setFinished(true);
                        return;
                    }
                };
                if (delayed != null && delayed.asBoolean()) {
                    Bukkit.getScheduler().runTaskAsynchronously(Denizen.getInstance(), loadRunnable);
                }
                else {
                    loadRunnable.run();
                    scriptEntry.setFinished(true);
                }
                break;
            }
            case UNLOAD: {
                if (!schematics.containsKey(name.asString().toUpperCase())) {
                    Debug.echoError(scriptEntry.getResidingQueue(), "Schematic file " + name.asString() + " is not loaded.");
                    scriptEntry.setFinished(true);
                    return;
                }
                schematics.remove(name.asString().toUpperCase());
                scriptEntry.setFinished(true);
                break;
            }
            case ROTATE: {
                if (!schematics.containsKey(name.asString().toUpperCase())) {
                    Debug.echoError(scriptEntry.getResidingQueue(), "Schematic file " + name.asString() + " is not loaded.");
                    scriptEntry.setFinished(true);
                    return;
                }
                if (angle == null) {
                    Debug.echoError(scriptEntry.getResidingQueue(), "Missing angle argument!");
                    scriptEntry.setFinished(true);
                    return;
                }
                Runnable rotateRunnable = () -> {
                    int ang = angle.asInt();
                    while (ang < 0) {
                        ang = 360 + ang;
                    }
                    while (ang >= 360) {
                        ang -= 360;
                    }
                    ang = 360 - ang;
                    while (ang > 0) {
                        ang -= 90;
                        schematics.get(name.asString().toUpperCase()).rotateOne();
                    }
                    Bukkit.getScheduler().runTask(Denizen.getInstance(), () -> scriptEntry.setFinished(true));
                };
                if (delayed != null && delayed.asBoolean()) {
                    Bukkit.getScheduler().runTaskAsynchronously(Denizen.getInstance(), rotateRunnable);
                }
                else {
                    scriptEntry.setFinished(true);
                    rotateRunnable.run();
                }
                break;
            }
            case FLIP_X: {
                if (!schematics.containsKey(name.asString().toUpperCase())) {
                    Debug.echoError(scriptEntry.getResidingQueue(), "Schematic file " + name.asString() + " is not loaded.");
                    scriptEntry.setFinished(true);
                    return;
                }
                schematics.get(name.asString().toUpperCase()).flipX();
                scriptEntry.setFinished(true);
                break;
            }
            case FLIP_Y: {
                if (!schematics.containsKey(name.asString().toUpperCase())) {
                    Debug.echoError(scriptEntry.getResidingQueue(), "Schematic file " + name.asString() + " is not loaded.");
                    scriptEntry.setFinished(true);
                    return;
                }
                schematics.get(name.asString().toUpperCase()).flipY();
                scriptEntry.setFinished(true);
                break;
            }
            case FLIP_Z: {
                if (!schematics.containsKey(name.asString().toUpperCase())) {
                    Debug.echoError(scriptEntry.getResidingQueue(), "Schematic file " + name.asString() + " is not loaded.");
                    scriptEntry.setFinished(true);
                    return;
                }
                schematics.get(name.asString().toUpperCase()).flipZ();
                scriptEntry.setFinished(true);
                break;
            }
            case PASTE: {
                if (!schematics.containsKey(name.asString().toUpperCase())) {
                    Debug.echoError(scriptEntry.getResidingQueue(), "Schematic file " + name.asString() + " is not loaded.");
                    scriptEntry.setFinished(true);
                    return;
                }
                if (location == null) {
                    Debug.echoError(scriptEntry.getResidingQueue(), "Missing location argument!");
                    scriptEntry.setFinished(true);
                    return;
                }
                try {
                    BlockSet.InputParams input = new BlockSet.InputParams();
                    input.centerLocation = location;
                    input.noAir = noair != null && noair.asBoolean();
                    input.fakeTo = fakeTo;
                    if (fakeTo != null && copyEntities != null && copyEntities.asBoolean()) {
                        Debug.echoError(scriptEntry.getResidingQueue(), "Cannot fake paste entities currently.");
                        scriptEntry.setFinished(true);
                        return;
                    }
                    if (fakeDuration == null) {
                        fakeDuration = new DurationTag(0);
                    }
                    input.fakeDuration = fakeDuration;
                    if (mask != null) {
                        input.mask = new HashSet<>();
                        for (MaterialTag material : mask) {
                            input.mask.add(material.getMaterial());
                        }
                    }
                    set = schematics.get(name.asString().toUpperCase());
                    if (delayed != null && delayed.asBoolean()) {
                        set.setBlocksDelayed(() -> {
                            if (copyEntities != null && copyEntities.asBoolean()) {
                                set.pasteEntities(location);
                            }
                            scriptEntry.setFinished(true);
                        }, input, maxDelayMs.asLong());
                    }
                    else {
                        set.setBlocks(input);
                        if (copyEntities != null && copyEntities.asBoolean()) {
                            set.pasteEntities(location);
                        }
                        scriptEntry.setFinished(true);
                    }
                }
                catch (Exception ex) {
                    Debug.echoError(scriptEntry.getResidingQueue(), "Exception pasting schematic file " + name.asString() + ".");
                    Debug.echoError(scriptEntry.getResidingQueue(), ex);
                    scriptEntry.setFinished(true);
                    return;
                }
                break;
            }
            case SAVE: {
                if (!schematics.containsKey(name.asString().toUpperCase())) {
                    Debug.echoError(scriptEntry.getResidingQueue(), "Schematic file " + name.asString() + " is not loaded.");
                    return;
                }
                set = schematics.get(name.asString().toUpperCase());
                String directory = URLDecoder.decode(System.getProperty("user.dir"));
                String extension = ".schem";
                File f = new File(directory + "/plugins/Denizen/schematics/" + fname + extension);
                if (!Utilities.canWriteToFile(f)) {
                    Debug.echoError("Cannot write to that file path due to security settings in Denizen/config.yml.");
                    scriptEntry.setFinished(true);
                    return;
                }
                Runnable saveRunnable = () -> {
                    try {
                        f.getParentFile().mkdirs();
                        FileOutputStream fs = new FileOutputStream(f);
                        SpongeSchematicHelper.saveToSpongeStream(set, fs);
                        fs.flush();
                        fs.close();
                        Bukkit.getScheduler().runTask(Denizen.getInstance(), () -> scriptEntry.setFinished(true));
                    }
                    catch (Exception ex) {
                        Bukkit.getScheduler().runTask(Denizen.getInstance(), () -> {
                            Debug.echoError(scriptEntry.getResidingQueue(), "Error saving schematic file " + fname + ".");
                            Debug.echoError(scriptEntry.getResidingQueue(), ex);
                        });
                        scriptEntry.setFinished(true);
                        return;
                    }
                };
                if (delayed != null && delayed.asBoolean()) {
                    Bukkit.getScheduler().runTaskAsynchronously(Denizen.getInstance(), saveRunnable);
                }
                else {
                    scriptEntry.setFinished(true);
                    saveRunnable.run();
                }
                break;
            }
        }
    }

    public void schematicTags(ReplaceableTagEvent event) {
        if (!event.matches("schematic")) {
            return;
        }
        Attribute attribute = event.getAttributes();
        String id = attribute.hasContext(1) ? attribute.getContext(1).toUpperCase() : null;
        attribute = attribute.fulfill(1);

        // <--[tag]
        // @attribute <schematic.list>
        // @returns ListTag
        // @description
        // Returns a list of all loaded schematics.
        // -->
        if (attribute.startsWith("list")) {
            event.setReplaced(new ListTag(schematics.keySet()).getAttribute(attribute.fulfill(1)));
        }
        if (id == null) {
            return;
        }
        if (!schematics.containsKey(id)) {
            // Meta below
            if (attribute.startsWith("exists")) {
                event.setReplaced(new ElementTag(false)
                        .getAttribute(attribute.fulfill(1)));
                return;
            }
            Debug.echoError(attribute.getScriptEntry() != null ? attribute.getScriptEntry().getResidingQueue() : null, "Schematic file " + id + " is not loaded.");
            return;
        }
        CuboidBlockSet set = schematics.get(id);

        // <--[tag]
        // @attribute <schematic[<name>].exists>
        // @returns ElementTag(Boolean)
        // @description
        // Returns whether the schematic exists.
        // -->
        if (attribute.startsWith("exists")) {
            event.setReplaced(new ElementTag(true)
                    .getAttribute(attribute.fulfill(1)));
            return;
        }

        // <--[tag]
        // @attribute <schematic[<name>].height>
        // @returns ElementTag(Number)
        // @description
        // Returns the height (Y) of the schematic.
        // -->
        if (attribute.startsWith("height")) {
            event.setReplaced(new ElementTag(set.y_length)
                    .getAttribute(attribute.fulfill(1)));
            return;
        }

        // <--[tag]
        // @attribute <schematic[<name>].length>
        // @returns ElementTag(Number)
        // @description
        // Returns the length (Z) of the schematic.
        // -->
        if (attribute.startsWith("length")) {
            event.setReplaced(new ElementTag(set.z_height)
                    .getAttribute(attribute.fulfill(1)));
            return;
        }

        // <--[tag]
        // @attribute <schematic[<name>].width>
        // @returns ElementTag(Number)
        // @description
        // Returns the width (X) of the schematic.
        // -->
        if (attribute.startsWith("width")) {
            event.setReplaced(new ElementTag(set.x_width)
                    .getAttribute(attribute.fulfill(1)));
            return;
        }

        // <--[tag]
        // @attribute <schematic[<name>].block[<location>]>
        // @returns MaterialTag
        // @description
        // Returns the material for the block at the location in the schematic.
        // An input location of 0,0,0 corresponds to the minimum corner of the schematic.
        // -->
        if (attribute.startsWith("block")) {
            if (attribute.hasContext(1) && LocationTag.matches(attribute.getContext(1))) {
                LocationTag location = attribute.contextAsType(1, LocationTag.class);
                FullBlockData block = set.blockAt(location.getX(), location.getY(), location.getZ());
                event.setReplaced(new MaterialTag(block.data)
                        .getAttribute(attribute.fulfill(1)));
                return;
            }
        }

        // <--[tag]
        // @attribute <schematic[<name>].origin>
        // @returns LocationTag
        // @description
        // Returns the origin location of the schematic.
        // -->
        if (attribute.startsWith("origin")) {
            event.setReplaced(new LocationTag(null, set.center_x, set.center_y, set.center_z)
                    .getAttribute(attribute.fulfill(1)));
            return;
        }

        // <--[tag]
        // @attribute <schematic[<name>].blocks>
        // @returns ElementTag(Number)
        // @description
        // Returns the number of blocks in the schematic.
        // -->
        if (attribute.startsWith("blocks")) {
            event.setReplaced(new ElementTag(set.blocks.length)
                    .getAttribute(attribute.fulfill(1)));
            return;
        }

        // <--[tag]
        // @attribute <schematic[<name>].cuboid[<origin_location>]>
        // @returns CuboidTag
        // @description
        // Returns a cuboid of where the schematic would be if it was pasted at an origin.
        // -->
        if (attribute.startsWith("cuboid") && attribute.hasContext(1)) {
            LocationTag origin = attribute.contextAsType(1, LocationTag.class);
            event.setReplaced(set.getCuboid(origin)
                    .getAttribute(attribute.fulfill(1)));
            return;
        }
    }
}
