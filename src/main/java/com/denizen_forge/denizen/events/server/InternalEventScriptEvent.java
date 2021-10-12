package com.denizenscript.denizen.events.server;

import com.denizenscript.denizen.Denizen;
import com.denizenscript.denizen.events.ForgeScriptEvent;
import com.denizenscript.denizen.utilities.debugging.Debug;
import net.minecraftforge.eventbus.api.*;

import java.util.*;

public class InternalEventScriptEvent extends ForgeScriptEvent {

    // <--[event]
    // @Events
    // internal bukkit event
    //
    // @Regex ^on internal bukkit event$
    //
    // @Switch event:<path> (required) to specify the Bukkit event path to use (like "event:org.bukkit.event.block.BlockBreakEvent")
    //
    // @Warning This exists primarily for testing/debugging, and is almost never a good idea to include in a real script.
    //
    // @Group Server
    //
    // @Cancellable true
    //
    // @Triggers when the specified internal Bukkit event fires. Useful for testing/debugging, or for interoperation with external plugins that have their own Bukkit events.
    //
    // -->

    public InternalEventScriptEvent() {
        instance = this;
    }

    public static InternalEventScriptEvent instance;

    @Override
    public boolean couldMatch(ScriptPath path) {
        if (!path.eventLower.startsWith("internal bukkit event")) {
            return false;
        }
        if (!path.switches.containsKey("event")) {
            return false;
        }
        return true;
    }

    @Override
    public boolean matches(ScriptPath path) {
        if (!currentEvent.getClass().getCanonicalName().equals(path.switches.get("event"))) {
            return false;
        }
        return true;
    }

    @Override
    public String getName() {
        return "InternalBukkitEvent";
    }

    @Override
    public void destroy() {
        if (registeredHandlers != null) {
            for (Map.Entry<RegisteredListener, HandlerList> handler : registeredHandlers) {
                handler.getValue().unregister(handler.getKey());
            }
            registeredHandlers = null;
        }
    }

    @Override
    public void init() {
        registeredHandlers = new ArrayList<>();
        HashSet<String> eventsGrabbed = new HashSet<>();
        for (ScriptPath path : new ArrayList<>(eventPaths)) {
            String eventName = path.switches.get("event");
            if (!eventsGrabbed.add(eventName)) {
                continue;
            }
            try {
                Class<?> clazz = Class.forName(eventName);
                if (!Event.class.isAssignableFrom(clazz)) {
                    Debug.echoError("Cannot initialize Internal Bukkit Event for event '" + eventName + "': that class is not an event class.");
                    return;
                }
                EventPriority priority = EventPriority.NORMAL;
                String bukkitPriority = path.switches.get("bukkit_priority");
                if (bukkitPriority != null) {
                    try {
                        priority = EventPriority.valueOf(bukkitPriority.toUpperCase());
                    }
                    catch (IllegalArgumentException ex) {
                        Debug.echoError("Invalid 'bukkit_priority' switch for event '" + path.event + "' in script '" + path.container.getName() + "'.");
                        Debug.echoError(ex);
                    }
                }
                InternalEventScriptEvent handler = (InternalEventScriptEvent) clone();
                handler.eventPaths = new ArrayList<>();
                handler.eventPaths.add(path);
                handler.registeredHandlers = null;
                handler.initForPriority(priority, this, (Class<? extends Event>) clazz);
                eventPaths.remove(path);
            }
            catch (ClassNotFoundException ex) {
                Debug.echoError("Cannot initialize Internal Bukkit Event for event '" + eventName + "': that event class does not exist.");
            }
            catch (Throwable ex) {
                Debug.echoError(ex);
            }
        }
    }

    public void initForPriority(EventPriority priority, InternalEventScriptEvent baseEvent, Class<? extends Event> clazz) {
        Plugin plugin = Denizen.getInstance();
        for (EventListenerHelper.ListenerList(<Class<? extends Event> clazz), Set<RegisteredListener>> entry : plugin.getPluginLoader().createRegisteredListeners(this).entrySet()) {
            for (RegisteredListener registeredListener : entry.getValue()) {
                RegisteredListener newListener = new RegisteredListener(this, getExecutor(registeredListener), priority, plugin, false);
                HandlerList handlers = getEventListeners(clazz);
                handlers.register(newListener);
                baseEvent.registeredHandlers.add(new HashMap.SimpleEntry<>(newListener, handlers));
            }
        }
    }

    @SubscribeEvent
    public void onEventHappens(Event event) {
        fire(event);
    }
}
