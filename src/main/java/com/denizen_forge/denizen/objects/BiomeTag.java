package com.denizenscript.denizen.objects;

import com.denizenscript.denizen.utilities.debugging.Debug;
import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.flags.AbstractFlagTracker;
import com.denizenscript.denizencore.flags.FlaggableObject;
import com.denizenscript.denizencore.flags.RedirectionFlagTracker;
import com.denizenscript.denizencore.objects.*;
import com.denizenscript.denizen.nms.NMSHandler;
import com.denizenscript.denizen.nms.abstracts.BiomeNMS;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizencore.tags.Attribute;
import com.denizenscript.denizencore.tags.ObjectTagProcessor;
import com.denizenscript.denizencore.tags.TagContext;
import com.denizenscript.denizencore.tags.TagRunnable;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.Deprecations;
import org.bukkit.block.Biome;
import org.bukkit.entity.EntityType;

import java.util.List;

public class BiomeTag implements ObjectTag, Adjustable, FlaggableObject {

    // <--[language]
    // @name BiomeTag Objects
    // @group Object System
    // @description
    // A BiomeTag represents a world biome type.
    //
    // A list of all valid Bukkit biomes can found be at
    // <@link url https://hub.spigotmc.org/javadocs/spigot/org/bukkit/block/Biome.html>
    //
    // These use the object notation "b@".
    // The identity format for biomes is simply the biome name, as registered in Bukkit, for example: 'desert'.
    //
    // This object type is flaggable.
    // Flags on this object type will be stored in the server saves file, under special sub-key "__biomes"
    //
    // -->

    //////////////////
    //    OBJECT FETCHER
    ////////////////

    @Deprecated
    public static BiomeTag valueOf(String string) {
        return valueOf(string, null);
    }

    /**
     * Gets a Biome Object from a string form.
     *
     * @param string the string
     */
    @Fetchable("b")
    public static BiomeTag valueOf(String string, TagContext context) {

        if (string.startsWith("b@")) {
            string = string.substring(2);
        }

        for (Biome biome : Biome.values()) {
            if (biome.name().equalsIgnoreCase(string)) {
                return new BiomeTag(biome);
            }
        }

        return null;
    }

    /**
     * Determines whether a string is a valid biome.
     *
     * @param arg the string
     * @return true if matched, otherwise false
     */
    public static boolean matches(String arg) {

        if (arg.startsWith("b@")) {
            arg = arg.substring(2);
        }

        for (Biome b : Biome.values()) {
            if (b.name().equalsIgnoreCase(arg)) {
                return true;
            }
        }

        return false;
    }

    ///////////////
    //   Constructors
    /////////////

    public BiomeTag(Biome biome) {
        this.bukkitBiome = biome;
        this.biome = NMSHandler.getInstance().getBiomeNMS(biome);
    }

    /////////////////////
    //   INSTANCE FIELDS/METHODS
    /////////////////

    private BiomeNMS biome;

    public Biome bukkitBiome;

    public BiomeNMS getBiome() {
        return biome;
    }

    String prefix = "biome";

    @Override
    public String getPrefix() {
        return prefix;
    }

    @Override
    public boolean isUnique() {
        return false;
    }

    @Override
    public String getObjectType() {
        return "Biome";
    }

    @Override
    public String identify() {
        return "b@" + CoreUtilities.toLowerCase(biome.getName());
    }

    @Override
    public String identifySimple() {
        return identify();
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
        return new RedirectionFlagTracker(DenizenCore.getImplementation().getServerFlags(), "__biomes." + biome.getName().replace(".", "&dot"));
    }

    @Override
    public void reapplyTracker(AbstractFlagTracker tracker) {
        // Nothing to do.
    }

    public static void registerTags() {

        AbstractFlagTracker.registerFlagHandlers(tagProcessor);

        // <--[tag]
        // @attribute <BiomeTag.downfall_type>
        // @returns ElementTag
        // @description
        // Returns this biome's downfall type for when a world has weather.
        // This can be RAIN, SNOW, or NONE.
        // -->
        registerTag("downfall_type", (attribute, object) -> {
            return new ElementTag(CoreUtilities.toLowerCase(object.biome.getDownfallType().name()));
        });

        // <--[tag]
        // @attribute <BiomeTag.name>
        // @returns ElementTag
        // @description
        // Returns this biome's name.
        // -->
        registerTag("name", (attribute, object) -> {
            return new ElementTag(CoreUtilities.toLowerCase(object.biome.getName()));
        });

        // <--[tag]
        // @attribute <BiomeTag.humidity>
        // @returns ElementTag(Decimal)
        // @mechanism BiomeTag.humidity
        // @description
        // Returns the humidity of this biome.
        // -->
        registerTag("humidity", (attribute, object) -> {
            return new ElementTag(object.biome.getHumidity());
        });
        // <--[tag]
        // @attribute <BiomeTag.temperature>
        // @returns ElementTag(Decimal)
        // @mechanism BiomeTag.temperature
        // @description
        // Returns the temperature of this biome.
        // -->
        registerTag("temperature", (attribute, object) -> {
            return new ElementTag(object.biome.getTemperature());
        });
        // <--[tag]
        // @attribute <BiomeTag.spawnable_entities[(<type>)]>
        // @returns ListTag(EntityTag)
        // @description
        // Returns all entities that spawn naturally in this biome.
        // Optionally specify a type as: AMBIENT, CREATURES, MONSTERS, WATER, or ALL.
        // (By default, will be "ALL").
        //
        // -->
        registerTag("spawnable_entities", (attribute, object) -> {
            List<EntityType> entityTypes;
            if (attribute.startsWith("ambient", 2)) {
                Deprecations.biomeSpawnableTag.warn(attribute.context);
                attribute.fulfill(1);
                entityTypes = object.biome.getAmbientEntities();
            }
            else if (attribute.startsWith("creatures", 2)) {
                Deprecations.biomeSpawnableTag.warn(attribute.context);
                attribute.fulfill(1);
                entityTypes = object.biome.getCreatureEntities();
            }
            else if (attribute.startsWith("monsters", 2)) {
                Deprecations.biomeSpawnableTag.warn(attribute.context);
                attribute.fulfill(1);
                entityTypes = object.biome.getMonsterEntities();
            }
            else if (attribute.startsWith("water", 2)) {
                Deprecations.biomeSpawnableTag.warn(attribute.context);
                attribute.fulfill(1);
                entityTypes = object.biome.getWaterEntities();
            }
            else {
                String type = attribute.hasContext(1) ? CoreUtilities.toLowerCase(attribute.getContext(1)) : "all";
                switch (type) {
                    case "ambient":
                        entityTypes = object.biome.getAmbientEntities();
                        break;
                    case "creatures":
                        entityTypes = object.biome.getCreatureEntities();
                        break;
                    case "monsters":
                        entityTypes = object.biome.getMonsterEntities();
                        break;
                    case "water":
                        entityTypes = object.biome.getWaterEntities();
                        break;
                    default:
                        entityTypes = object.biome.getAllEntities();
                        break;
                }
            }

            ListTag list = new ListTag();
            for (EntityType entityType : entityTypes) {
                list.add(entityType.name());
            }
            return list;
        });
    }

    public static ObjectTagProcessor<BiomeTag> tagProcessor = new ObjectTagProcessor<>();

    public static void registerTag(String name, TagRunnable.ObjectInterface<BiomeTag> runnable, String... variants) {
        tagProcessor.registerTag(name, runnable, variants);
    }

    @Override
    public ObjectTag getObjectAttribute(Attribute attribute) {
        return tagProcessor.getObjectAttribute(this, attribute);
    }

    @Override
    public void applyProperty(Mechanism mechanism) {
        Debug.echoError("Cannot apply properties to a biome!");
    }

    @Override
    public void adjust(Mechanism mechanism) {

        // <--[mechanism]
        // @object BiomeTag
        // @name humidity
        // @input ElementTag(Decimal)
        // @description
        // Sets the humidity for this biome server-wide.
        // If this is greater than 0.85, fire has less chance
        // to spread in this biome.
        // @tags
        // <BiomeTag.humidity>
        // -->
        if (mechanism.matches("humidity") && mechanism.requireFloat()) {
            biome.setHumidity(mechanism.getValue().asFloat());
        }

        // <--[mechanism]
        // @object BiomeTag
        // @name temperature
        // @input ElementTag(Decimal)
        // @description
        // Sets the temperature for this biome server-wide.
        // If this is less than 1.5, snow will form on the ground
        // when weather occurs in the world and a layer of ice
        // will form over water.
        // @tags
        // <BiomeTag.temperature>
        // -->
        if (mechanism.matches("temperature") && mechanism.requireFloat()) {
            biome.setTemperature(mechanism.getValue().asFloat());
        }

    }
}
