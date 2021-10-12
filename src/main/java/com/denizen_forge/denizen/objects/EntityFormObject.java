package com.denizenscript.denizen.objects;

import com.denizenscript.denizencore.objects.ObjectTag;

public interface EntityFormObject extends ObjectTag {

    EntityTag getDenizenEntity();

    default LocationTag getLocation() {
        return getDenizenEntity().getLocation();
    }
}
