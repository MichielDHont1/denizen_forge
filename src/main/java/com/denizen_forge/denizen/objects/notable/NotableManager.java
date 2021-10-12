package com.denizenscript.denizen.objects.notable;

import com.denizenscript.denizen.Denizen;
import com.denizenscript.denizen.objects.*;
import com.denizenscript.denizen.utilities.debugging.Debug;
import com.denizenscript.denizencore.flags.FlaggableObject;
import com.denizenscript.denizencore.flags.SavableMapFlagTracker;
import com.denizenscript.denizencore.objects.ObjectFetcher;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.notable.Notable;
import com.denizenscript.denizencore.objects.notable.Note;
import com.denizenscript.denizencore.tags.core.EscapeTagBase;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NotableManager {

    public NotableManager() {
        registerWithNotableManager(CuboidTag.class);
        registerWithNotableManager(EllipsoidTag.class);
        registerWithNotableManager(InventoryTag.class);
        registerWithNotableManager(ItemTag.class);
        registerWithNotableManager(LocationTag.class);
        registerWithNotableManager(PolygonTag.class);
    }

    public static HashMap<String, Notable> notableObjects = new HashMap<>();
    public static HashMap<Notable, String> reverseObjects = new HashMap<>();
    public static HashMap<Class, HashSet<Notable>> notesByType = new HashMap<>();

    public static boolean isSaved(Notable object) {
        return reverseObjects.containsKey(object);
    }

    public static boolean isExactSavedObject(Notable object) {
        String id = reverseObjects.get(object);
        if (id == null) {
            return false;
        }
        return notableObjects.get(id) == object;
    }

    public static Notable getSavedObject(String id) {
        return notableObjects.get(CoreUtilities.toLowerCase(id));
    }

    public static String getSavedId(Notable object) {
        return reverseObjects.get(object);
    }

    public static void saveAs(Notable object, String id) {
        if (object == null) {
            return;
        }
        id = CoreUtilities.toLowerCase(id);
        Notable noted = notableObjects.get(id);
        if (noted != null) {
            noted.forget();
        }
        notableObjects.put(id, object);
        reverseObjects.put(object, id);
        notesByType.get(object.getClass()).add(object);
    }

    public static Notable remove(String id) {
        id = CoreUtilities.toLowerCase(id);
        Notable obj = notableObjects.get(id);
        if (obj == null) {
            return null;
        }
        notableObjects.remove(id);
        reverseObjects.remove(obj);
        notesByType.get(obj.getClass()).remove(obj);
        return obj;
    }

    public static void remove(Notable obj) {
        String id = reverseObjects.get(obj);
        notableObjects.remove(id);
        reverseObjects.remove(obj);
        notesByType.get(obj.getClass()).remove(obj);
    }

    public static <T extends Notable> Set<T> getAllType(Class<T> type) {
        return (Set<T>) notesByType.get(type);
    }

    /**
     * Called on '/denizen reload notables'.
     */
    private static void _recallNotables() {
        notableObjects.clear();
        for (Set set : notesByType.values()) {
            set.clear();
        }
        reverseObjects.clear();
        // Find each type of notable
        for (String key : Denizen.getInstance().notableManager.getNotables().getKeys(false)) {
            Class<? extends ObjectTag> clazz = reverse_objects.get(key);
            ConfigurationSection section = Denizen.getInstance().notableManager.getNotables().getConfigurationSection(key);
            if (section == null) {
                continue;
            }
            for (String notableRaw : section.getKeys(false)) {
                String notable = EscapeTagBase.unEscape(notableRaw.replace("DOT", "."));
                String objText;
                String flagText = null;
                if (section.isConfigurationSection(notableRaw)) {
                    objText = section.getConfigurationSection(notableRaw).getString("object");
                    flagText = section.getConfigurationSection(notableRaw).getString("flags");
                }
                else {
                    objText = section.getString(notableRaw);
                }
                Notable obj = (Notable) ObjectFetcher.getObjectFrom(clazz, objText, CoreUtilities.errorButNoDebugContext);
                if (obj != null) {
                    obj.makeUnique(notable);
                    if (flagText != null && obj instanceof FlaggableObject) {
                        ((FlaggableObject) getSavedObject(notable)).reapplyTracker(new SavableMapFlagTracker(flagText));
                    }
                }
                else {
                    Debug.echoError("Notable '" + notable + "' failed to load!");
                }
            }
        }
    }

    /**
     * Called on by '/denizen save'.
     */
    private static void _saveNotables() {
        FileConfiguration notables = Denizen.getInstance().notableManager.getNotables();
        for (String key : notables.getKeys(false)) {
            notables.set(key, null);
        }
        for (Map.Entry<String, Notable> notable : notableObjects.entrySet()) {
            try {
                notables.set(getClassId(getClass(notable.getValue())) + "." + EscapeTagBase.escape(CoreUtilities.toLowerCase(notable.getKey())), notable.getValue().getSaveObject());
            }
            catch (Exception e) {
                Debug.echoError("Notable '" + notable.getKey() + "' failed to save!");
                Debug.echoError(e);
            }
        }
    }

    private static <T extends Notable> Class<T> getClass(Notable notable) {
        for (Class clazz : objects.keySet()) {
            if (clazz.isInstance(notable)) {
                return clazz;
            }
        }
        return null;
    }

    private FileConfiguration notablesSave = null;
    private File notablesFile = null;

    /**
     * Reloads, retrieves and saves notable information from/to 'notables.yml'.
     */
    public void reloadNotables() {
        if (notablesFile == null) {
            notablesFile = new File(Denizen.getInstance().getDataFolder(), "notables.yml");
        }
        notablesSave = YamlConfiguration.loadConfiguration(notablesFile);
        // Reload notables from notables.yml
        _recallNotables();
    }

    public FileConfiguration getNotables() {
        if (notablesSave == null) {
            reloadNotables();
        }
        return notablesSave;
    }

    public void saveNotables() {
        if (notablesSave == null || notablesFile == null) {
            return;
        }
        try {
            // Save notables to notables.yml
            _saveNotables();
            notablesSave.save(notablesFile);
        }
        catch (IOException ex) {
            Logger.getLogger(JavaPlugin.class.getName()).log(Level.SEVERE, "Could not save to " + notablesFile, ex);
        }
    }

    ///////////////////
    // Note Annotation Handler
    ///////////////////

    private static Map<Class, String> objects = new HashMap<>();
    private static Map<String, Class> reverse_objects = new HashMap<>();

    public static void registerWithNotableManager(Class notable) {
        for (Method method : notable.getMethods()) {
            if (method.isAnnotationPresent(Note.class)) {
                String note = method.getAnnotation(Note.class).value();
                objects.put(notable, note);
                reverse_objects.put(note, notable);
                notesByType.put(notable, new HashSet<>());
            }
        }
    }

    public static String getClassId(Class notable) {
        return objects.get(notable);
    }

    public static Map<String, Class> getReverseClassIdMap() {
        return reverse_objects;
    }
}
