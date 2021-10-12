package com.denizenscript.denizen.objects;

import com.denizenscript.denizen.events.ForgeScriptEvent;
import com.denizenscript.denizen.objects.notable.NotableManager;
import com.denizenscript.denizen.utilities.debugging.Debug;
import com.denizenscript.denizen.utilities.flags.LocationFlagSearchHelper;
import com.denizenscript.denizencore.flags.AbstractFlagTracker;
import com.denizenscript.denizencore.flags.FlaggableObject;
import com.denizenscript.denizencore.flags.SavableMapFlagTracker;
import com.denizenscript.denizencore.objects.*;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizencore.tags.ObjectTagProcessor;
import com.denizenscript.denizencore.objects.notable.Notable;
import com.denizenscript.denizencore.objects.notable.Note;
import com.denizenscript.denizencore.tags.Attribute;
import com.denizenscript.denizencore.tags.TagContext;
import com.denizenscript.denizencore.tags.TagRunnable;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class EllipsoidTag implements ObjectTag, Notable, Cloneable, AreaContainmentObject, FlaggableObject {

    // <--[language]
    // @name EllipsoidTag Objects
    // @group Object System
    // @description
    // An EllipsoidTag represents an ellipsoidal region in the world.
    //
    // The word 'ellipsoid' means a less strict sphere.
    // Basically: an "ellipsoid" is to a 3D "sphere" what an "ellipse" (or "oval") is to a 2D "circle".
    //
    // These use the object notation "ellipsoid@".
    // The identity format for ellipsoids is <x>,<y>,<z>,<world>,<x-radius>,<y-radius>,<z-radius>
    // For example, 'ellipsoid@1,2,3,space,7,7,7'.
    //
    // This object type can be noted.
    //
    // This object type is flaggable when it is noted.
    // Flags on this object type will be stored in the notables.yml file.
    //
    // -->

    public static List<EllipsoidTag> getNotableEllipsoidsContaining(Location location) {
        List<EllipsoidTag> ellipsoids = new ArrayList<>();
        for (EllipsoidTag ellipsoid : NotableManager.getAllType(EllipsoidTag.class)) {
            if (ellipsoid.contains(location)) {
                ellipsoids.add(ellipsoid);
            }
        }

        return ellipsoids;
    }

    //////////////////
    //    OBJECT FETCHER
    ////////////////

    @Deprecated
    public static EllipsoidTag valueOf(String string) {
        return valueOf(string, null);
    }

    /**
     * Gets an Ellipsoid Object from a string form.
     *
     * @param string the string
     */
    @Fetchable("ellipsoid")
    public static EllipsoidTag valueOf(String string, TagContext context) {
        if (string.startsWith("ellipsoid@")) {
            string = string.substring(10);
        }
        if (string.contains("@")) {
            return null;
        }
        Notable noted = NotableManager.getSavedObject(string);
        if (noted instanceof EllipsoidTag) {
            return (EllipsoidTag) noted;
        }
        List<String> split = CoreUtilities.split(string, ',');
        if (split.size() != 7) {
            return null;
        }
        WorldTag world = WorldTag.valueOf(split.get(3), false);
        if (world == null) {
            return null;
        }
        for (int i = 0; i < 7; i++) {
            if (i != 3 && !ArgumentHelper.matchesDouble(split.get(i))) {
                if (context == null || context.showErrors()) {
                    Debug.echoError("EllipsoidTag input is not a valid decimal number: " + split.get(i));
                    return null;
                }
            }
        }
        LocationTag location = new LocationTag(world.getWorld(), Double.parseDouble(split.get(0)), Double.parseDouble(split.get(1)), Double.parseDouble(split.get(2)));
        LocationTag size = new LocationTag(null, Double.parseDouble(split.get(4)), Double.parseDouble(split.get(5)), Double.parseDouble(split.get(6)));
        return new EllipsoidTag(location, size);
    }

    /**
     * Determines whether a string is a valid ellipsoid.
     *
     * @param arg the string
     * @return true if matched, otherwise false
     */
    public static boolean matches(String arg) {
        try {
            return EllipsoidTag.valueOf(arg, CoreUtilities.noDebugContext) != null;
        }
        catch (Exception e) {
            return false;
        }
    }
    @Override
    public EllipsoidTag clone() {
        return new EllipsoidTag(center.clone(), size.clone());
    }

    @Override
    public ObjectTag duplicate() {
        if (noteName != null) {
            return this;
        }
        return clone();
    }

    ///////////////
    //   Constructors
    /////////////

    public EllipsoidTag(LocationTag center, LocationTag size) {
        this.center = center;
        this.size = size;
    }

    /////////////////////
    //   INSTANCE FIELDS/METHODS
    /////////////////

    public LocationTag center;

    public LocationTag size;

    public String noteName = null;

    public AbstractFlagTracker flagTracker = null;

    public ListTag getBlocks(Attribute attribute) {
        return getBlocks(null, attribute);
    }

    public ListTag getBlocks(String matcher, Attribute attribute) {
        List<LocationTag> initial = new CuboidTag(new Location(center.getWorld(),
                center.getX() - size.getX(), center.getY() - size.getY(), center.getZ() - size.getZ()),
                new Location(center.getWorld(),
                        center.getX() + size.getX(), center.getY() + size.getY(), center.getZ() + size.getZ()))
                .getBlocks_internal(matcher, attribute);
        ListTag list = new ListTag();
        for (LocationTag loc : initial) {
            if (contains(loc)) {
                list.addObject(loc);
            }
        }
        return list;
    }

    public List<LocationTag> getBlockLocationsUnfiltered(boolean doMax) {
        List<LocationTag> initial = new CuboidTag(new Location(center.getWorld(),
                center.getX() - size.getX(), center.getY() - size.getY(), center.getZ() - size.getZ()),
                new Location(center.getWorld(),
                        center.getX() + size.getX(), center.getY() + size.getY(), center.getZ() + size.getZ()))
                .getBlockLocationsUnfiltered(doMax);
        List<LocationTag> locations = new ArrayList<>();
        for (LocationTag loc : initial) {
            if (contains(loc)) {
                locations.add(loc);
            }
        }
        return locations;
    }

    public ListTag getShell() {
        ListTag output = new ListTag();
        double yScale = size.getY();
        int maxY = (int) Math.floor(yScale);
        output.addObject(new LocationTag(center.getBlockX(), center.getBlockY() - maxY, center.getBlockZ(), center.getWorldName()));
        if (maxY != 0) {
            output.addObject(new LocationTag(center.getBlockX(), center.getBlockY() + maxY, center.getBlockZ(), center.getWorldName()));
        }
        for (int y = -maxY; y <= maxY; y++) {
            double yProgMin = Math.min(1.0, (Math.abs(y) + 1) / yScale);
            double yProgMax = Math.abs(y) / yScale;
            double minSubWidth = Math.sqrt(1.0 - yProgMin * yProgMin);
            double maxSubWidth = Math.sqrt(1.0 - yProgMax * yProgMax);
            double minX = size.getX() * minSubWidth - 1;
            double minZ = size.getZ() * minSubWidth - 1;
            double maxX = size.getX() * maxSubWidth;
            double maxZ = size.getZ() * maxSubWidth;
            for (int x = 0; x < maxX; x++) {
                for (int z = 0; z < maxZ; z++) {
                    double scaleTestMin = (x * x) / (minX * minX) + (z * z) / (minZ * minZ);
                    double scaleTestMax = (x * x) / (maxX * maxX) + (z * z) / (maxZ * maxZ);
                    if (scaleTestMin >= 1.0 && scaleTestMax <= 1.0) {
                        output.addObject(new LocationTag(center.getBlockX() + x, center.getBlockY() + y, center.getBlockZ() + z, center.getWorldName()));
                        if (x != 0) {
                            output.addObject(new LocationTag(center.getBlockX() - x, center.getBlockY() + y, center.getBlockZ() + z, center.getWorldName()));
                        }
                        if (z != 0) {
                            output.addObject(new LocationTag(center.getBlockX() + x, center.getBlockY() + y, center.getBlockZ() - z, center.getWorldName()));
                        }
                        if (x != 0 && z != 0) {
                            output.addObject(new LocationTag(center.getBlockX() - x, center.getBlockY() + y, center.getBlockZ() - z, center.getWorldName()));
                        }
                    }
                }
            }
        }
        return output;
    }

    public boolean contains(Location test) {
        double xbase = test.getX() - center.getX();
        double ybase = test.getY() - center.getY();
        double zbase = test.getZ() - center.getZ();
        return ((xbase * xbase) / (size.getX() * size.getX())
                + (ybase * ybase) / (size.getY() * size.getY())
                + (zbase * zbase) / (size.getZ() * size.getZ()) <= 1);
    }

    public boolean intersects(ChunkTag chunk) {
        int xMin = chunk.getX() * 16;
        int zMin = chunk.getZ() * 16;
        LocationTag locTest = chunk.getCenter();
        // This mess gets a position within the chunk that is as closes as possible to the ellipsoid's center
        locTest.setY(center.getY());
        if (center.getX() > xMin) {
            if (center.getX() < xMin + 16) {
                locTest.setX(center.getX());
            }
            else {
                locTest.setX(center.getX());
            }
        }
        if (center.getZ() > zMin) {
            if (center.getZ() < zMin + 16) {
                locTest.setZ(center.getZ());
            }
            else {
                locTest.setZ(center.getZ());
            }
        }
        return contains(locTest);
    }

    String prefix = "ellipsoid";

    @Override
    public String getPrefix() {
        return prefix;
    }

    @Override
    public String debuggable() {
        if (isUnique()) {
            return "ellipsoid@" + noteName + "<GR> (" + identifyFull() + ")";
        }
        else {
            return identifyFull();
        }
    }

    @Override
    public boolean isUnique() {
        return noteName != null;
    }

    @Override
    @Note("Ellipsoids")
    public Object getSaveObject() {
        ConfigurationSection section = new YamlConfiguration();
        section.set("object", identifyFull());
        section.set("flags", flagTracker.toString());
        return section;
    }

    @Override
    public void makeUnique(String id) {
        EllipsoidTag toNote = clone();
        toNote.noteName = id;
        toNote.flagTracker = new SavableMapFlagTracker();
        NotableManager.saveAs(toNote, id);
    }

    @Override
    public void forget() {
        NotableManager.remove(this);
        noteName = null;
        flagTracker = null;
    }
    @Override
    public int hashCode() {
        if (noteName != null) {
            return noteName.hashCode();
        }
        return center.hashCode() + size.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof EllipsoidTag)) {
            return false;
        }
        EllipsoidTag ellipsoid2 = (EllipsoidTag) other;
        if ((noteName == null) != (ellipsoid2.noteName == null)) {
            return false;
        }
        if (noteName != null && !noteName.equals(ellipsoid2.noteName)) {
            return false;
        }
        if (!center.getWorldName().equals(ellipsoid2.center.getWorldName())) {
            return false;
        }
        if (center.distanceSquaredNoWorld(ellipsoid2.center) >= 0.25) {
            return false;
        }
        if (size.distanceSquaredNoWorld(ellipsoid2.size) >= 0.25) {
            return false;
        }
        return true;
    }

    @Override
    public String getObjectType() {
        return "Ellipsoid";
    }

    @Override
    public String identify() {
        if (isUnique()) {
            return "ellipsoid@" + noteName;
        }
        else {
            return identifyFull();
        }
    }

    @Override
    public String identifySimple() {
        return identify();
    }

    public String identifyFull() {
        return "ellipsoid@" + center.getX() + "," + center.getY() + "," + center.getZ() + "," + center.getWorldName()
                + "," + size.getX() + "," + size.getY() + "," + size.getZ();
    }

    @Override
    public String toString() {
        return identify();
    }

    @Override
    public ObjectTag setPrefix(String prefix) {
        if (prefix != null) {
            this.prefix = prefix;
        }
        return this;
    }

    @Override
    public AbstractFlagTracker getFlagTracker() {
        return flagTracker;
    }

    @Override
    public void reapplyTracker(AbstractFlagTracker tracker) {
        if (noteName != null) {
            this.flagTracker = tracker;
        }
    }

    @Override
    public String getReasonNotFlaggable() {
        if (noteName == null) {
            return "the area is not noted - only noted areas can hold flags";
        }
        return "unknown reason - something went wrong";
    }

    public static void registerTags() {

        AbstractFlagTracker.registerFlagHandlers(tagProcessor);

        // <--[tag]
        // @attribute <EllipsoidTag.random>
        // @returns LocationTag
        // @description
        // Returns a random decimal location within the ellipsoid.
        // Note that distribution of results will not be completely even.
        // -->
        registerTag("random", (attribute, object) -> {
            // This is an awkward hack to try to weight towards the center a bit (to counteract the weight-away-from-center that would otherwise happen).
            double y = (Math.sqrt(CoreUtilities.getRandom().nextDouble()) * 2 - 1) * object.size.getY();
            Vector result = new Vector();
            result.setY(y);
            double yProg = Math.abs(y) / object.size.getY();
            double subWidth = Math.sqrt(1.0 - yProg * yProg);
            double maxX = object.size.getX() * subWidth;
            double maxZ = object.size.getZ() * subWidth;
            result.setX(maxX * (CoreUtilities.getRandom().nextDouble() * 2 - 1));
            result.setZ(maxZ * (CoreUtilities.getRandom().nextDouble() * 2 - 1));
            LocationTag out = object.center.clone();
            out.add(result);
            return out;
        });

        // <--[tag]
        // @attribute <EllipsoidTag.blocks[(<matcher>)]>
        // @returns ListTag(LocationTag)
        // @description
        // Returns each block location within the EllipsoidTag.
        // Optionally, specify a material matcher to only return locations with that block type.
        // -->
        registerTag("blocks", (attribute, object) -> {
            if (attribute.hasContext(1)) {
                return new ListTag(object.getBlocks(attribute.getContext(1), attribute));
            }
            else {
                return new ListTag(object.getBlocks(attribute));
            }
        }, "get_blocks");

        // <--[tag]
        // @attribute <EllipsoidTag.blocks_flagged[<flag_name>]>
        // @returns ListTag(LocationTag)
        // @description
        // Gets a list of all block locations with a specified flag within the ellipsoid.
        // Searches the internal flag lists, rather than through all possible blocks.
        // -->
        registerTag("blocks_flagged", (attribute, object) -> {
            if (!attribute.hasContext(1)) {
                attribute.echoError("EllipsoidTag.blocks_flagged[...] must have an input value.");
                return null;
            }
            String flagName = CoreUtilities.toLowerCase(attribute.getContext(1));
            ListTag blocks = new ListTag();
            double minPossibleX = object.center.getX() - object.size.getX();
            double minPossibleZ = object.center.getZ() - object.size.getZ();
            double maxPossibleX = object.center.getX() + object.size.getX();
            double maxPossibleZ = object.center.getZ() + object.size.getZ();
            int minChunkX = (int) Math.floor(minPossibleX / 16);
            int minChunkZ = (int) Math.floor(minPossibleZ / 16);
            int maxChunkX = (int) Math.ceil(maxPossibleX / 16);
            int maxChunkZ = (int) Math.ceil(maxPossibleZ / 16);
            ChunkTag testChunk = new ChunkTag(object.center);
            for (int x = minChunkX; x <= maxChunkX; x++) {
                testChunk.chunkX = x;
                for (int z = minChunkZ; z <= maxChunkZ; z++) {
                    testChunk.chunkZ = z;
                    testChunk.cachedChunk = null;
                    if (object.intersects(testChunk) && testChunk.isLoadedSafe()) {
                        LocationFlagSearchHelper.getFlaggedLocations(testChunk.getChunkForTag(attribute), flagName, (loc) -> {
                            if (object.doesContainLocation(loc)) {
                                blocks.addObject(new LocationTag(loc));
                            }
                        });
                    }
                }
            }
            return blocks;
        });

        // <--[tag]
        // @attribute <EllipsoidTag.shell>
        // @returns ListTag(LocationTag)
        // @description
        // Returns a 3D outline (shell) of this ellipsoid, as a list of block locations.
        // -->
        registerTag("shell", (attribute, object) -> {
            return object.getShell();
        });

        // <--[tag]
        // @attribute <EllipsoidTag.location>
        // @returns LocationTag
        // @description
        // Returns the location of the ellipsoid.
        // -->
        registerTag("location", (attribute, object) -> {
            return object.center;
        });

        // <--[tag]
        // @attribute <EllipsoidTag.size>
        // @returns LocationTag
        // @description
        // Returns the size of the ellipsoid.
        // -->
        registerTag("size", (attribute, object) -> {
            return object.size;
        });

        // <--[tag]
        // @attribute <EllipsoidTag.add[<location>]>
        // @returns EllipsoidTag
        // @description
        // Returns a copy of this ellipsoid, shifted by the input location.
        // -->
        registerTag("add", (attribute, object) -> {
            if (!attribute.hasContext(1)) {
                attribute.echoError("ellipsoid.add[...] tag must have an input.");
                return null;
            }
            return new EllipsoidTag(object.center.clone().add(attribute.contextAsType(1, LocationTag.class)), object.size.clone());
        });

        // <--[tag]
        // @attribute <EllipsoidTag.contains[<location>]>
        // @returns ElementTag(Boolean)
        // @description
        // Returns a boolean indicating whether the specified location is inside this ellipsoid.
        // -->
        registerTag("contains", (attribute, object) -> {
            if (!attribute.hasContext(1)) {
                attribute.echoError("ellipsoid.contains[...] tag must have an input.");
                return null;
            }
            return new ElementTag(object.contains(attribute.contextAsType(1, LocationTag.class)));
        });

        // <--[tag]
        // @attribute <EllipsoidTag.include[<location>]>
        // @returns EllipsoidTag
        // @description
        // Returns a copy of this ellipsoid, with the size value adapted to include the specified world location.
        // -->
        registerTag("include", (attribute, object) -> {
            if (!attribute.hasContext(1)) {
                attribute.echoError("ellipsoid.include[...] tag must have an input.");
                return null;
            }
            LocationTag target = attribute.contextAsType(1, LocationTag.class);
            if (object.contains(target)) {
                return object;
            }
            LocationTag size = object.size.clone();
            Vector relative = target.toVector().subtract(object.center.toVector());
            // Cuboid minimum expansion
            size.setX(Math.max(size.getX(), Math.abs(relative.getX())));
            size.setY(Math.max(size.getY(), Math.abs(relative.getY())));
            size.setZ(Math.max(size.getZ(), Math.abs(relative.getZ())));
            EllipsoidTag result = new EllipsoidTag(object.center.clone(), new LocationTag(size));
            if (result.contains(target)) {
                return result;
            }
            double sizeLen = size.length();
            // Ellipsoid additional expand
            while (!result.contains(target)) {
                // I gave up on figuring out the math for this, so here's an awful loop-hack
                double projX = (relative.getX() * relative.getX()) / (size.getX() * size.getX());
                double projY = (relative.getY() * relative.getY()) / (size.getY() * size.getY());
                double projZ = (relative.getZ() * relative.getZ()) / (size.getZ() * size.getZ());
                double scale = Math.max(projX + projY + projZ, sizeLen * 0.01);
                if (projX >= projY && projX >= projZ) {
                    size.setX(size.getX() + scale);
                }
                else if (projY >= projX && projY >= projZ) {
                    size.setY(size.getY() + scale);
                }
                else if (projZ >= projX && projZ >= projY) {
                    size.setZ(size.getZ() + scale);
                }
                else {
                    size = size.add(scale, scale, scale);
                }
                result.size = size;
            }
            return result;
        });

        // <--[tag]
        // @attribute <EllipsoidTag.with_location[<location>]>
        // @returns EllipsoidTag
        // @description
        // Returns a copy of this ellipsoid, set to the specified location.
        // -->
        registerTag("with_location", (attribute, object) -> {
            if (!attribute.hasContext(1)) {
                attribute.echoError("ellipsoid.with_location[...] tag must have an input.");
                return null;
            }
            return new EllipsoidTag(attribute.contextAsType(1, LocationTag.class), object.size.clone());
        });

        // <--[tag]
        // @attribute <EllipsoidTag.with_size[<location>]>
        // @returns EllipsoidTag
        // @description
        // Returns a copy of this ellipsoid, set to the specified size.
        // -->
        registerTag("with_size", (attribute, object) -> {
            if (!attribute.hasContext(1)) {
                attribute.echoError("ellipsoid.with_size[...] tag must have an input.");
                return null;
            }
            return new EllipsoidTag(object.center.clone(), attribute.contextAsType(1, LocationTag.class));
        });

        // <--[tag]
        // @attribute <EllipsoidTag.with_world[<world>]>
        // @returns EllipsoidTag
        // @description
        // Returns a copy of this ellipsoid, set to the specified world.
        // -->
        registerTag("with_world", (attribute, object) -> {
            if (!attribute.hasContext(1)) {
                attribute.echoError("ellipsoid.with_world[...] tag must have an input.");
                return null;
            }
            LocationTag loc = object.center.clone();
            loc.setWorld(attribute.contextAsType(1, WorldTag.class).getWorld());
            return new EllipsoidTag(loc, object.size.clone());
        });

        // <--[tag]
        // @attribute <EllipsoidTag.players>
        // @returns ListTag(PlayerTag)
        // @description
        // Gets a list of all players currently within the EllipsoidTag.
        // -->
        registerTag("players", (attribute, object) -> {
            ArrayList<PlayerTag> players = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (object.contains(player.getLocation())) {
                    players.add(PlayerTag.mirrorBukkitPlayer(player));
                }
            }
            return new ListTag(players);
        });


        // <--[tag]
        // @attribute <EllipsoidTag.entities[(<matcher>)]>
        // @returns ListTag(EntityTag)
        // @description
        // Gets a list of all entities currently within the EllipsoidTag, with an optional search parameter for the entity.
        // -->
        registerTag("entities", (attribute, object) -> {
            String matcher = attribute.hasContext(1) ? attribute.getContext(1) : null;
            ListTag entities = new ListTag();
            for (Entity ent : new WorldTag(object.center.getWorld()).getEntitiesForTag()) {
                EntityTag current = new EntityTag(ent);
                if (object.contains(ent.getLocation())) {
                    if (matcher == null || ForgeScriptEvent.tryEntity(current, matcher)) {
                        entities.addObject(new EntityTag(ent).getDenizenObject());
                    }
                }
            }
            return entities;
        });

        // <--[tag]
        // @attribute <EllipsoidTag.chunks>
        // @returns ListTag(ChunkTag)
        // @description
        // Returns a list of all chunks that this ellipsoid touches at all (note that no valid ellipsoid tag can ever totally contain a chunk, due to vertical limits and roundness).
        // -->
        registerTag("chunks", (attribute, object) -> {
            ListTag chunks = new ListTag();
            double minPossibleX = object.center.getX() - object.size.getX();
            double minPossibleZ = object.center.getZ() - object.size.getZ();
            double maxPossibleX = object.center.getX() + object.size.getX();
            double maxPossibleZ = object.center.getZ() + object.size.getZ();
            int minChunkX = (int) Math.floor(minPossibleX / 16);
            int minChunkZ = (int) Math.floor(minPossibleZ / 16);
            int maxChunkX = (int) Math.ceil(maxPossibleX / 16);
            int maxChunkZ = (int) Math.ceil(maxPossibleZ / 16);
            ChunkTag testChunk = new ChunkTag(object.center);
            for (int x = minChunkX; x <= maxChunkX; x++) {
                testChunk.chunkX = x;
                for (int z = minChunkZ; z <= maxChunkZ; z++) {
                    testChunk.chunkZ = z;
                    if (object.intersects(testChunk)) {
                        chunks.addObject(new ChunkTag(testChunk.world, testChunk.chunkX, testChunk.chunkZ));
                    }
                }
            }
            return chunks;
        });

        // <--[tag]
        // @attribute <EllipsoidTag.note_name>
        // @returns ElementTag
        // @description
        // Gets the name of a noted EllipsoidTag. If the ellipsoid isn't noted, this is null.
        // -->
        registerTag("note_name", (attribute, ellipsoid) -> {
            String noteName = NotableManager.getSavedId(ellipsoid);
            if (noteName == null) {
                return null;
            }
            return new ElementTag(noteName);
        });
    }

    public static ObjectTagProcessor<EllipsoidTag> tagProcessor = new ObjectTagProcessor<>();

    public static void registerTag(String name, TagRunnable.ObjectInterface<EllipsoidTag> runnable, String... variants) {
        tagProcessor.registerTag(name, runnable, variants);
    }

    @Override
    public ObjectTag getObjectAttribute(Attribute attribute) {
        return tagProcessor.getObjectAttribute(this, attribute);
    }

    @Override
    public String getNoteName() {
        return noteName;
    }

    @Override
    public boolean doesContainLocation(Location loc) {
        return contains(loc);
    }
}
