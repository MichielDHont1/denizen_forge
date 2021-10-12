package com.denizenscript.denizen.objects.properties.entity;

import com.denizenscript.denizen.objects.EntityTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.Mechanism;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.properties.Property;
import com.denizenscript.denizencore.tags.Attribute;

public class EntitySilent implements Property {

    public static boolean describes(ObjectTag entity) {
        return entity instanceof EntityTag;
    }

    public static EntitySilent getFrom(ObjectTag entity) {
        if (!describes(entity)) {
            return null;
        }
        else {
            return new EntitySilent((EntityTag) entity);
        }
    }

    public static final String[] handledTags = new String[] {
            "silent"
    };

    public static final String[] handledMechs = new String[] {
            "silent"
    };

    private EntitySilent(EntityTag ent) {
        entity = ent;
    }

    EntityTag entity;

    @Override
    public String getPropertyString() {
        return entity.getBukkitEntity().isSilent() ? "true" : null;
    }

    @Override
    public String getPropertyId() {
        return "silent";
    }

    @Override
    public ObjectTag getObjectAttribute(Attribute attribute) {

        if (attribute == null) {
            return null;
        }

        // <--[tag]
        // @attribute <EntityTag.silent>
        // @returns ElementTag(Boolean)
        // @mechanism EntityTag.silent
        // @group attributes
        // @description
        // Returns whether the entity is silent. (Plays no sounds)
        // -->
        if (attribute.startsWith("silent")) {
            return new ElementTag(entity.getBukkitEntity().isSilent())
                    .getObjectAttribute(attribute.fulfill(1));
        }

        return null;
    }

    @Override
    public void adjust(Mechanism mechanism) {

        // <--[mechanism]
        // @object EntityTag
        // @name silent
        // @input ElementTag(Boolean)
        // @description
        // Sets whether this entity is silent. (Plays no sounds)
        // @tags
        // <EntityTag.silent>
        // -->
        if (mechanism.matches("silent") && mechanism.requireBoolean()) {
            entity.getBukkitEntity().setSilent(mechanism.getValue().asBoolean());
        }
    }
}
