package com.denizenscript.denizen.events.world;

import com.denizenscript.denizen.objects.WorldTag;
import com.denizenscript.denizen.events.ForgeScriptEvent;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.ObjectTag;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.weather.WeatherChangeEvent;

public class WeatherChangesScriptEvent extends ForgeScriptEvent {

    // <--[event]
    // @Events
    // weather changes/rains/clears (in <world>)
    //
    // @Regex ^on weather (changes|rains|clears)( in [^\s]+)?$
    //
    // @Group World
    //
    // @Cancellable true
    //
    // @Triggers when weather changes in a world.
    //
    // @Context
    // <context.world> returns the WorldTag the weather changed in.
    // <context.weather> returns an ElementTag with the name of the new weather. (rain or clear).
    //
    // -->

    public WeatherChangesScriptEvent() {
        instance = this;
    }

    public static WeatherChangesScriptEvent instance;
    public WorldTag world;
    public ElementTag weather;
    public WeatherChangeEvent event;

    @Override
    public boolean couldMatch(ScriptPath path) {
        if (!path.eventArgLowerAt(0).equals("weather")) {
            return false;
        }
        return true;
    }

    @Override
    public boolean matches(ScriptPath path) {
        String cmd = path.eventArgLowerAt(1);
        if (!cmd.equals("changes") && !cmd.equals(weather.asString())) {
            return false;
        }
        if (path.eventArgLowerAt(2).equals("in") && !tryWorld(world, path.eventArgLowerAt(3))) {
            return false;
        }
        return super.matches(path);
    }

    @Override
    public String getName() {
        return "WeatherChanges";
    }

    @Override
    public ObjectTag getContext(String name) {
        if (name.equals("world")) {
            return world;
        }
        else if (name.equals("weather")) {
            return weather;
        }
        return super.getContext(name);
    }

    @SubscribeEvent
    public void onWeatherChanges(WeatherChangeEvent event) {
        world = new WorldTag(event.getWorld());
        weather = new ElementTag(event.toWeatherState() ? "rains" : "clears");
        this.event = event;
        fire(event);
    }
}
