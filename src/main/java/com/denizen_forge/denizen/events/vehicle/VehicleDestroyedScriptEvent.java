package com.denizenscript.denizen.events.vehicle;

import com.denizenscript.denizen.objects.EntityTag;
import com.denizenscript.denizen.utilities.implementation.BukkitScriptEntryData;
import com.denizenscript.denizen.events.ForgeScriptEvent;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.scripts.ScriptEntryData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.vehicle.VehicleDestroyEvent;

public class VehicleDestroyedScriptEvent extends ForgeScriptEvent {

    // <--[event]
    // @Events
    // vehicle destroyed
    // <vehicle> destroyed
    // entity destroys vehicle
    // <entity> destroys vehicle
    // entity destroys <vehicle>
    // <entity> destroys <vehicle>
    //
    // @Group Vehicle
    //
    // @Regex ^on [^\s]+ (destroyed|destroys [^\s]+)$
    //
    // @Location true
    //
    // @Cancellable true
    //
    // @Triggers when a vehicle is destroyed.
    //
    // @Context
    // <context.vehicle> returns the EntityTag of the vehicle.
    // <context.entity> returns the EntityTag of the attacking entity.
    //
    // @NPC when the entity that destroyed the vehicle is a player..
    //
    // @NPC when the entity that destroyed the vehicle is an NPC.
    //
    // -->

    public VehicleDestroyedScriptEvent() {
        instance = this;
    }

    public static VehicleDestroyedScriptEvent instance;
    public EntityTag vehicle;
    public EntityTag entity;
    public VehicleDestroyEvent event;

    @Override
    public boolean couldMatch(ScriptPath path) {
        String cmd = path.eventArgLowerAt(1);
        if (cmd.equals("destroys")) {
            if (!couldMatchEntity(path.eventArgLowerAt(0))) {
                return false;
            }
            if (!couldMatchVehicle(path.eventArgLowerAt(2))) {
                return false;
            }
        }
        else if (cmd.equals("destroyed")) {
            if (!couldMatchVehicle(path.eventArgLowerAt(0))) {
                return false;
            }
        }
        else {
            return false;
        }
        return true;
    }

    @Override
    public boolean matches(ScriptPath path) {
        String cmd = path.eventArgLowerAt(1);
        String veh = cmd.equals("destroyed") ? path.eventArgLowerAt(0) : path.eventArgLowerAt(2);
        String ent = cmd.equals("destroys") ? path.eventArgLowerAt(0) : "";
        if (!tryEntity(vehicle, veh)) {
            return false;
        }
        if (ent.length() > 0 && (entity == null || !tryEntity(entity, ent))) {
            return false;
        }
        if (!runInCheck(path, vehicle.getLocation())) {
            return false;
        }
        return super.matches(path);
    }

    @Override
    public String getName() {
        return "VehicleDestroyed";
    }

    @Override
    public ScriptEntryData getScriptEntryData() {
        return new BukkitScriptEntryData(entity);
    }

    @Override
    public ObjectTag getContext(String name) {
        if (name.equals("vehicle")) {
            return vehicle;
        }
        else if (name.equals("entity") && entity != null) {
            return entity;
        }
        return super.getContext(name);
    }

    @SubscribeEvent
    public void onVehicleDestroyed(VehicleDestroyEvent event) {
        vehicle = new EntityTag(event.getVehicle());
        entity = event.getAttacker() != null ? new EntityTag(event.getAttacker()) : null;
        this.event = event;
        fire(event);
    }
}
