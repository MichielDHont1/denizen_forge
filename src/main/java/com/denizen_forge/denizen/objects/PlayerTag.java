package com.denizenscript.denizen.objects;

import com.denizenscript.denizen.events.ForgeScriptEvent;
import com.denizenscript.denizen.nms.interfaces.AdvancementHelper;
import com.denizenscript.denizen.objects.properties.entity.EntityHealth;
import com.denizenscript.denizen.scripts.commands.player.DisguiseCommand;
import com.denizenscript.denizen.scripts.commands.player.SidebarCommand;
import com.denizenscript.denizen.utilities.AdvancedTextImpl;
import com.denizenscript.denizen.utilities.FormattedTextHelper;
import com.denizenscript.denizen.utilities.Utilities;
import com.denizenscript.denizen.utilities.blocks.FakeBlock;
import com.denizenscript.denizen.utilities.debugging.Debug;
import com.denizenscript.denizen.utilities.entity.BossBarHelper;
import com.denizenscript.denizen.utilities.entity.FakeEntity;
import com.denizenscript.denizen.utilities.entity.HideEntitiesHelper;
import com.denizenscript.denizen.utilities.flags.PlayerFlagHandler;
import com.denizenscript.denizen.utilities.packets.DenizenPacketHandler;
import com.denizenscript.denizen.utilities.packets.HideParticles;
import com.denizenscript.denizen.utilities.packets.ItemChangeMessage;
import com.denizenscript.denizencore.flags.AbstractFlagTracker;
import com.denizenscript.denizencore.flags.FlaggableObject;
import com.denizenscript.denizencore.objects.*;
import com.denizenscript.denizen.nms.NMSHandler;
import com.denizenscript.denizen.nms.abstracts.ImprovedOfflinePlayer;
import com.denizenscript.denizen.nms.abstracts.Sidebar;
import com.denizenscript.denizen.nms.interfaces.PlayerHelper;
import com.denizenscript.denizen.tags.core.PlayerTagBase;
import com.denizenscript.denizencore.objects.core.*;
import com.denizenscript.denizencore.tags.Attribute;
import com.denizenscript.denizencore.tags.ObjectTagProcessor;
import com.denizenscript.denizencore.tags.TagContext;
import com.denizenscript.denizencore.tags.TagRunnable;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.Deprecations;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.npc.NPCSelector;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import org.bukkit.*;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.block.banner.PatternType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.map.MapView;
import org.bukkit.util.RayTraceResult;

import java.util.*;

public class PlayerTag implements ObjectTag, Adjustable, EntityFormObject, FlaggableObject {

    // <--[language]
    // @name PlayerTag Objects
    // @group Object System
    // @description
    // A PlayerTag represents a player in the game.
    //
    // These use the object notation "p@".
    // The identity format for players is the UUID of the relevant player.
    //
    // This object type is flaggable.
    // Flags on this object type will be stored in the file "plugins/Denizen/player_flags/(UUID).dat",
    // with automatic loading only when the player is online and caching for interacting with offline player flags.
    //
    // -->

    /////////////////////
    //   STATIC METHODS
    /////////////////

    public static PlayerTag mirrorBukkitPlayer(OfflinePlayer player) {
        if (player == null) {
            return null;
        }
        else {
            return new PlayerTag(player);
        }
    }

    static Map<String, UUID> playerNames = new HashMap<>();

    /**
     * Notes that the player exists, for easy PlayerTag valueOf handling.
     */
    public static void notePlayer(OfflinePlayer player) {
        if (player.getName() == null) {
            Debug.echoError("Null player " + player.toString());
            return;
        }
        if (!playerNames.containsKey(CoreUtilities.toLowerCase(player.getName()))) {
            playerNames.put(CoreUtilities.toLowerCase(player.getName()), player.getUniqueId());
        }
    }

    public static boolean isNoted(OfflinePlayer player) {
        return playerNames.containsValue(player.getUniqueId());
    }

    public static Map<String, UUID> getAllPlayers() {
        return playerNames;
    }

    /////////////////////
    //   OBJECT FETCHER
    /////////////////

    @Deprecated
    public static PlayerTag valueOf(String string) {
        return valueOf(string, null);
    }

    @Fetchable("p")
    public static PlayerTag valueOf(String string, TagContext context) {
        return valueOfInternal(string, context, true);
    }

    public static PlayerTag valueOfInternal(String string, boolean announce) {
        return valueOfInternal(string, null, announce);
    }

    public static String playerByNameMessage = Deprecations.playerByNameWarning.message;

    public static PlayerTag valueOfInternal(String string, TagContext context, boolean defaultAnnounce) {
        if (string == null) {
            return null;
        }
        boolean announce = context == null ? defaultAnnounce : context.showErrors();
        string = string.replace("p@", "").replace("P@", "");
        if (string.indexOf('-') >= 0) {
            try {
                UUID uuid = UUID.fromString(string);
                if (uuid != null) {
                    OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
                    if (player != null) {
                        return new PlayerTag(player);
                    }
                }
            }
            catch (IllegalArgumentException e) {
                // Nothing
            }
        }
        // Match as a player name
        if (playerNames.containsKey(CoreUtilities.toLowerCase(string))) {
            OfflinePlayer player = Bukkit.getOfflinePlayer(playerNames.get(CoreUtilities.toLowerCase(string)));
            if (announce) {
                Deprecations.playerByNameWarning.message = playerByNameMessage + " Player named '" + player.getName() + "' has UUID: " + player.getUniqueId();
                Deprecations.playerByNameWarning.warn(context);
            }
            return new PlayerTag(player);
        }

        if (announce) {
            Debug.log("Minor: Invalid Player! '" + string + "' could not be found.");
        }

        return null;
    }

    public static boolean matches(String arg) {
        // If passed null, of course it doesn't match!
        if (arg == null) {
            return false;
        }

        // If passed a identified object that starts with 'p@', return true
        // even if the player doesn't technically exist.
        if (CoreUtilities.toLowerCase(arg).startsWith("p@")) {
            return true;
        }

        arg = arg.replace("p@", "").replace("P@", "");
        if (arg.indexOf('-') >= 0) {
            try {
                UUID uuid = UUID.fromString(arg);
                if (uuid != null) {
                    OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
                    if (player != null && player.hasPlayedBefore()) {
                        return true;
                    }
                }
            }
            catch (IllegalArgumentException e) {
                // Nothing
            }
        }
        return false;
    }

    public static boolean playerNameIsValid(String name) {
        return playerNames.containsKey(CoreUtilities.toLowerCase(name));
    }

    /////////////////////
    //   CONSTRUCTORS
    /////////////////

    public PlayerTag(OfflinePlayer player) {
        offlinePlayer = player;
    }

    public PlayerTag(UUID uuid) {
        offlinePlayer = Bukkit.getOfflinePlayer(uuid);
    }

    public PlayerTag(Player player) {
        this((OfflinePlayer) player);
        if (EntityTag.isNPC(player)) {
            throw new IllegalStateException("NPCs are not allowed as PlayerTag objects!");
        }
    }

    /////////////////////
    //   INSTANCE FIELDS/METHODS
    /////////////////

    @Override
    public AbstractFlagTracker getFlagTracker() {
        return PlayerFlagHandler.getTrackerFor(getUUID());
    }

    @Override
    public void reapplyTracker(AbstractFlagTracker tracker) {
        // Nothing to do.
    }

    OfflinePlayer offlinePlayer;

    public boolean isValid() {
        return getPlayerEntity() != null || getOfflinePlayer() != null;
    }

    public Player getPlayerEntity() {
        if (offlinePlayer == null) {
            return null;
        }
        return Bukkit.getPlayer(offlinePlayer.getUniqueId());
    }

    public UUID getUUID() {
        if (offlinePlayer == null) {
            return null;
        }
        return offlinePlayer.getUniqueId();
    }

    public OfflinePlayer getOfflinePlayer() {
        return offlinePlayer;
    }

    public ImprovedOfflinePlayer getNBTEditor() {
        return NMSHandler.getPlayerHelper().getOfflineData(getOfflinePlayer());
    }

    @Override
    public EntityTag getDenizenEntity() {
        return new EntityTag(getPlayerEntity());
    }

    public String getName() {
        if (offlinePlayer == null) {
            return null;
        }
        return offlinePlayer.getName();
    }

    @Override
    public LocationTag getLocation() {
        if (isOnline()) {
            return new LocationTag(getPlayerEntity().getLocation());
        }
        else {
            return new LocationTag(getNBTEditor().getLocation());
        }
    }

    public int getRemainingAir() {
        if (isOnline()) {
            return getPlayerEntity().getRemainingAir();
        }
        else {
            return getNBTEditor().getRemainingAir();
        }
    }

    public int getMaximumAir() {
        if (isOnline()) {
            return getPlayerEntity().getMaximumAir();
        }
        else {
            return 300;
        }
    }

    public double getHealth() {
        if (isOnline()) {
            return getPlayerEntity().getHealth();
        }
        else {
            return getNBTEditor().getHealthFloat();
        }
    }

    public double getMaxHealth() {
        if (isOnline()) {
            return getPlayerEntity().getMaxHealth();
        }
        else {
            return getNBTEditor().getMaxHealth();
        }
    }

    public int getFoodLevel() {
        if (isOnline()) {
            return getPlayerEntity().getFoodLevel();
        }
        else {
            return getNBTEditor().getFoodLevel();
        }
    }

    public LocationTag getEyeLocation() {
        if (isOnline()) {
            return new LocationTag(getPlayerEntity().getEyeLocation());
        }
        else {
            return null;
        }
    }

    public PlayerInventory getBukkitInventory() {
        if (isOnline()) {
            return getPlayerEntity().getInventory();
        }
        else {
            return getNBTEditor().getInventory();
        }
    }

    public InventoryTag getInventory() {
        if (isOnline()) {
            return InventoryTag.mirrorBukkitInventory(getPlayerEntity().getInventory());
        }
        else {
            return new InventoryTag(getNBTEditor());
        }
    }

    public CraftingInventory getBukkitWorkbench() {
        if (isOnline()) {
            if (getPlayerEntity().getOpenInventory().getType() == InventoryType.WORKBENCH
                    || getPlayerEntity().getOpenInventory().getType() == InventoryType.CRAFTING) {
                return (CraftingInventory) getPlayerEntity().getOpenInventory().getTopInventory();
            }
        }
        return null;
    }

    public InventoryTag getWorkbench() {
        if (isOnline()) {
            CraftingInventory workbench = getBukkitWorkbench();
            if (workbench != null) {
                return new InventoryTag(workbench, getPlayerEntity());
            }
        }
        return null;
    }

    public Inventory getBukkitEnderChest() {
        if (isOnline()) {
            return getPlayerEntity().getEnderChest();
        }
        else {
            return getNBTEditor().getEnderChest();
        }
    }

    public InventoryTag getEnderChest() {
        if (isOnline()) {
            return new InventoryTag(getPlayerEntity().getEnderChest(), getPlayerEntity());
        }
        else {
            return new InventoryTag(getNBTEditor(), true);
        }
    }

    public World getWorld() {
        if (isOnline()) {
            return getPlayerEntity().getWorld();
        }
        else {
            return getLocation().getWorld();
        }
    }

    public ItemTag getHeldItem() {
        return new ItemTag(getPlayerEntity().getEquipment().getItemInMainHand());
    }

    public void decrementStatistic(Statistic statistic, int amount) {
        getOfflinePlayer().decrementStatistic(statistic, amount);
    }

    public void decrementStatistic(Statistic statistic, EntityType entity, int amount) {
        if (statistic.getType() == Statistic.Type.ENTITY) {
            getOfflinePlayer().decrementStatistic(statistic, entity, amount);
        }
    }

    public void decrementStatistic(Statistic statistic, Material material, int amount) {
        if (statistic.getType() == Statistic.Type.BLOCK || statistic.getType() == Statistic.Type.ITEM) {
            getOfflinePlayer().decrementStatistic(statistic, material, amount);
        }
    }

    public void incrementStatistic(Statistic statistic, int amount) {
        getOfflinePlayer().incrementStatistic(statistic, amount);
    }

    public void incrementStatistic(Statistic statistic, EntityType entity, int amount) {
        if (statistic.getType() == Statistic.Type.ENTITY) {
            getOfflinePlayer().incrementStatistic(statistic, entity, amount);
        }
    }

    public void incrementStatistic(Statistic statistic, Material material, int amount) {
        if (statistic.getType() == Statistic.Type.BLOCK  || statistic.getType() == Statistic.Type.ITEM) {
            getOfflinePlayer().incrementStatistic(statistic, material, amount);
        }
    }

    public void setStatistic(Statistic statistic, int amount) {
        getOfflinePlayer().setStatistic(statistic, amount);
    }

    public void setStatistic(Statistic statistic, EntityType entity, int amount) {
        if (statistic.getType() == Statistic.Type.ENTITY) {
            getOfflinePlayer().setStatistic(statistic, entity, amount);
        }
    }

    public void setStatistic(Statistic statistic, Material material, int amount) {
        if (statistic.getType() == Statistic.Type.BLOCK || statistic.getType() == Statistic.Type.ITEM) {
            getOfflinePlayer().setStatistic(statistic, material, amount);
        }
    }

    public boolean isOnline() {
        return getPlayerEntity() != null;
    }

    public void setBedSpawnLocation(Location location) {
        if (isOnline()) {
            getPlayerEntity().setBedSpawnLocation(location);
        }
        else {
            getNBTEditor().setBedSpawnLocation(location, getNBTEditor().isSpawnForced());
        }
    }

    public void setLocation(Location location) {
        if (isOnline()) {
            getPlayerEntity().teleport(location);
        }
        else {
            getNBTEditor().setLocation(location);
        }
    }

    public void setMaximumAir(int air) {
        if (isOnline()) {
            getPlayerEntity().setMaximumAir(air);
        }
        else {
            Debug.echoError("Cannot set the maximum air of an offline player!");
        }
    }

    public void setRemainingAir(int air) {
        if (isOnline()) {
            getPlayerEntity().setRemainingAir(air);
        }
        else {
            getNBTEditor().setRemainingAir(air);
        }
    }

    public void setHealth(double health) {
        if (isOnline()) {
            getPlayerEntity().setHealth(health);
        }
        else {
            getNBTEditor().setHealthFloat((float) health);
        }
    }

    public void setMaxHealth(double maxHealth) {
        if (isOnline()) {
            getPlayerEntity().setMaxHealth(maxHealth);
        }
        else {
            getNBTEditor().setMaxHealth(maxHealth);
        }
    }

    public void setFoodLevel(int foodLevel) {
        if (isOnline()) {
            getPlayerEntity().setFoodLevel(foodLevel);
        }
        else {
            getNBTEditor().setFoodLevel(foodLevel);
        }
    }

    public void setLevel(int level) {
        if (isOnline()) {
            getPlayerEntity().setLevel(level);
        }
        else {
            getNBTEditor().setLevel(level);
        }
    }

    public void setFlySpeed(float speed) {
        if (isOnline()) {
            getPlayerEntity().setFlySpeed(speed);
        }
        else {
            getNBTEditor().setFlySpeed(speed);
        }
    }

    public void setGameMode(GameMode mode) {
        if (isOnline()) {
            getPlayerEntity().setGameMode(mode);
        }
        else {
            getNBTEditor().setGameMode(mode);
        }
    }

    public boolean hasChunkLoaded(Chunk chunk) {
        return NMSHandler.getPlayerHelper().hasChunkLoaded(getPlayerEntity(), chunk);
    }

    /////////////////////
    //   ObjectTag Methods
    /////////////////

    private String prefix = "Player";

    @Override
    public String getPrefix() {
        return prefix;
    }

    @Override
    public PlayerTag setPrefix(String prefix) {
        this.prefix = prefix;
        return this;
    }

    @Override
    public String debuggable() {
        return "p@" + offlinePlayer.getUniqueId().toString() + "<GR> (" + offlinePlayer.getName() + ")";
    }

    @Override
    public boolean isUnique() {
        return true;
    }

    @Override
    public String getObjectType() {
        return "Player";
    }

    @Override
    public String identify() {
        return "p@" + offlinePlayer.getUniqueId().toString();
    }

    @Override
    public String identifySimple() {
        return "p@" + offlinePlayer.getName();
    }

    @Override
    public String toString() {
        return identify();
    }

    @Override
    public int hashCode() {
        return getUUID().hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof PlayerTag)) {
            return false;
        }
        return getUUID().equals(((PlayerTag) other).getUUID());
    }

    public static void registerTags() {

        AbstractFlagTracker.registerFlagHandlers(tagProcessor);

        /////////////////////
        //   OFFLINE ATTRIBUTES
        /////////////////

        // Defined in EntityTag
        registerTag("is_player", (attribute, object) -> {
            return new ElementTag(true);
        });

        /////////////////////
        //   DENIZEN ATTRIBUTES
        /////////////////

        // <--[tag]
        // @attribute <PlayerTag.chat_history_list>
        // @returns ListTag
        // @description
        // Returns a list of the last 10 things the player has said, less if the player hasn't said all that much.
        // Works with offline players.
        // -->
        registerTag("chat_history_list", (attribute, object) -> {
            return new ListTag(PlayerTagBase.playerChatHistory.get(object.getUUID()));
        });

        // <--[tag]
        // @attribute <PlayerTag.chat_history[<#>]>
        // @returns ElementTag
        // @description
        // Returns the last thing the player said.
        // If a number is specified, returns an earlier thing the player said.
        // Works with offline players.
        // -->
        registerTag("chat_history", (attribute, object) -> {
            int x = 1;
            if (attribute.hasContext(1) && ArgumentHelper.matchesInteger(attribute.getContext(1))) {
                x = attribute.getIntContext(1);
            }
            // No playerchathistory? Return null.
            if (!PlayerTagBase.playerChatHistory.containsKey(object.getUUID())) {
                return null;
            }
            List<String> messages = PlayerTagBase.playerChatHistory.get(object.getUUID());
            if (messages.size() < x || x < 1) {
                return null;
            }
            return new ElementTag(messages.get(x - 1));
        });


        /////////////////////
        //   ENTITY LIST ATTRIBUTES
        /////////////////

        // <--[tag]
        // @attribute <PlayerTag.target[(<entity>|...)]>
        // @returns EntityTag
        // @description
        // Returns the entity that the player is looking at, within a maximum range of 50 blocks,
        // or null if the player is not looking at an entity.
        // Optionally, specify a list of entities, entity types, or 'npc' to only count those targets.
        // -->
        registerOnlineOnlyTag("target", (attribute, object) -> {
            double range = 50;
            ListTag filterList = attribute.hasContext(1) ? attribute.contextAsType(1, ListTag.class) : null;

            // <--[tag]
            // @attribute <PlayerTag.target[(<entity>|...)].within[(<#.#>)]>
            // @returns EntityTag
            // @description
            // Returns the living entity that the player is looking at within the specified range limit,
            // or null if the player is not looking at an entity.
            // Optionally, specify a list of entities, entity types, or 'npc' to only count those targets.
            // -->
            if (attribute.startsWith("within", 2) && attribute.hasContext(2)) {
                range = attribute.getDoubleContext(2);
                attribute.fulfill(1);
            }
            Location eyeLoc = object.getEyeLocation();
            RayTraceResult result = eyeLoc.getWorld().rayTrace(eyeLoc, eyeLoc.getDirection(), range, FluidCollisionMode.NEVER, true, 0.01, (e) -> {
                if (e.getUniqueId().equals(object.getUUID())) {
                    return false;
                }
                if (filterList != null) {
                    EntityTag ent = new EntityTag(e);
                    for (String val : filterList) {
                        if (CoreUtilities.equalsIgnoreCase(val, "npc") && ent.isCitizensNPC()) {
                            return true;
                        }
                        if (CoreUtilities.equalsIgnoreCase(val, e.getType().name())) {
                            return true;
                        }
                        String script = ent.getEntityScript();
                        if (script != null && CoreUtilities.equalsIgnoreCase(val, script)) {
                            return true;
                        }
                        EntityTag match = EntityTag.valueOf(val, CoreUtilities.noDebugContext);
                        if (match.isUnique() && e.getUniqueId().equals(match.getUUID())) {
                            return true;
                        }
                    }
                    return false;
                }
                return true;
            });
            if (result == null || result.getHitEntity() == null) {
                return null;
            }
            return new EntityTag(result.getHitEntity()).getDenizenObject();
        });

        /////////////////////
        //   LOCATION ATTRIBUTES
        /////////////////

        // <--[tag]
        // @attribute <PlayerTag.bed_spawn>
        // @returns LocationTag
        // @mechanism PlayerTag.bed_spawn_location
        // @description
        // Returns the location of the player's bed spawn location, null if
        // it doesn't exist.
        // Works with offline players.
        // -->
        registerTag("bed_spawn", (attribute, object) -> {
            try {
                NMSHandler.getChunkHelper().changeChunkServerThread(object.getWorld());
                if (object.getOfflinePlayer().getBedSpawnLocation() == null) {
                    return null;
                }
                return new LocationTag(object.getOfflinePlayer().getBedSpawnLocation());
            }
            finally {
                NMSHandler.getChunkHelper().restoreServerThread(object.getWorld());
            }
        });

        registerTag("location", (attribute, object) -> {
            if (object.isOnline() && !object.getPlayerEntity().isDead()) {
                return new EntityTag(object.getPlayerEntity()).getObjectAttribute(attribute);
            }
            return object.getLocation();
        });

        registerTag("world", (attribute, object) -> {
            return new WorldTag(object.getWorld());
        });

        /////////////////////
        //   STATE ATTRIBUTES
        /////////////////

        // <--[tag]
        // @attribute <PlayerTag.item_cooldown[<material>]>
        // @returns DurationTag
        // @description
        // Returns the cooldown duration remaining on player's material.
        // -->
        registerTag("item_cooldown", (attribute, object) -> {
            MaterialTag mat = new ElementTag(attribute.getContext(1)).asType(MaterialTag.class, attribute.context);
            if (mat != null) {
                return new DurationTag((long) object.getPlayerEntity().getCooldown(mat.getMaterial()));
            }
            return null;
        });

        // <--[tag]
        // @attribute <PlayerTag.first_played_time>
        // @returns TimeTag
        // @description
        // Returns the time of when the player first logged on to this server.
        // Works with offline players.
        // -->
        registerTag("first_played_time", (attribute, object) -> {
            return new TimeTag(object.getOfflinePlayer().getFirstPlayed());
        });
        registerTag("first_played", (attribute, object) -> {
            Deprecations.playerTimePlayedTags.warn(attribute.context);
            return new DurationTag(object.getOfflinePlayer().getFirstPlayed() / 50);
        });

        // <--[tag]
        // @attribute <PlayerTag.has_played_before>
        // @returns ElementTag(Boolean)
        // @description
        // Returns whether the player has played before.
        // Works with offline players.
        // Note: This will just always return true.
        // -->
        registerTag("has_played_before", (attribute, object) -> {
            return new ElementTag(true);
        });

        // <--[tag]
        // @attribute <PlayerTag.exhaustion>
        // @returns ElementTag(Decimal)
        // @mechanism PlayerTag.exhaustion
        // @description
        // Returns how fast the food level drops (exhaustion).
        // -->
        registerTag("exhaustion", (attribute, object) -> {
            return new ElementTag(object.getPlayerEntity().getExhaustion());
        });

        // Handle EntityTag oxygen tags here to allow getting them when the player is offline
        registerTag("max_oxygen", (attribute, object) -> {
            return new DurationTag((long) object.getMaximumAir());
        });

        registerTag("oxygen", (attribute, object) -> {
            if (attribute.startsWith("max", 2)) {
                Deprecations.entityMaxOxygenTag.warn(attribute.context);
                attribute.fulfill(1);
                return new DurationTag((long) object.getMaximumAir());
            }
            return new DurationTag((long) object.getRemainingAir());
        });

        // <--[tag]
        // @attribute <PlayerTag.health_is_scaled>
        // @returns ElementTag(Boolean)
        // @description
        // Returns whether the player's health bar is currently being scaled.
        // -->
        registerTag("health_is_scaled", (attribute, object) -> {
            return new ElementTag(object.getPlayerEntity().isHealthScaled());
        });

        // <--[tag]
        // @attribute <PlayerTag.health_scale>
        // @returns ElementTag(Decimal)
        // @mechanism PlayerTag.health_scale
        // @description
        // Returns the current scale for the player's health bar
        // -->
        registerTag("health_scale", (attribute, object) -> {
            return new ElementTag(object.getPlayerEntity().getHealthScale());
        });

        // Handle EntityTag health tags here to allow getting them when the player is offline
        registerTag("formatted_health", (attribute, object) -> {
            Double maxHealth = attribute.hasContext(1) ? attribute.getDoubleContext(1) : null;
            return EntityHealth.getHealthFormatted(new EntityTag(object.getPlayerEntity()), maxHealth);
        });

        registerTag("health_percentage", (attribute, object) -> {
            double maxHealth = object.getPlayerEntity().getMaxHealth();
            if (attribute.hasContext(1)) {
                maxHealth = attribute.getIntContext(1);
            }
            return new ElementTag((object.getPlayerEntity().getHealth() / maxHealth) * 100);
        });

        registerTag("health_max", (attribute, object) -> {
            return new ElementTag(object.getMaxHealth());
        });

        registerTag("health", (attribute, object) -> {
            if (attribute.startsWith("is_scaled", 2)) {
                attribute.fulfill(1);
                Deprecations.entityHealthTags.warn(attribute.context);
                return new ElementTag(object.getPlayerEntity().isHealthScaled());
            }

            if (attribute.startsWith("scale", 2)) {
                attribute.fulfill(1);
                Deprecations.entityHealthTags.warn(attribute.context);
                return new ElementTag(object.getPlayerEntity().getHealthScale());
            }
            if (attribute.startsWith("formatted", 2)) {
                Deprecations.entityHealthTags.warn(attribute.context);
                Double maxHealth = attribute.hasContext(2) ? attribute.getDoubleContext(2) : null;
                attribute.fulfill(1);
                return EntityHealth.getHealthFormatted(new EntityTag(object.getPlayerEntity()), maxHealth);
            }
            if (attribute.startsWith("percentage", 2)) {
                Deprecations.entityHealthTags.warn(attribute.context);
                attribute.fulfill(1);
                double maxHealth = object.getPlayerEntity().getMaxHealth();
                if (attribute.hasContext(1)) {
                    maxHealth = attribute.getIntContext(1);
                }
                return new ElementTag((object.getPlayerEntity().getHealth() / maxHealth) * 100);
            }
            if (attribute.startsWith("max", 2)) {
                Deprecations.entityHealthTags.warn(attribute.context);
                attribute.fulfill(1);
                return new ElementTag(object.getMaxHealth());
            }
            return new ElementTag(object.getHealth());
        });

        // <--[tag]
        // @attribute <PlayerTag.is_banned>
        // @returns ElementTag(Boolean)
        // @description
        // Returns whether the player is banned.
        // -->
        registerTag("is_banned", (attribute, object) -> {
            BanEntry ban = Bukkit.getBanList(BanList.Type.NAME).getBanEntry(object.getName());
            if (ban == null) {
                return new ElementTag(false);
            }
            else if (ban.getExpiration() == null) {
                return new ElementTag(true);
            }
            return new ElementTag(ban.getExpiration().after(new Date()));
        });

        // <--[tag]
        // @attribute <PlayerTag.is_online>
        // @returns ElementTag(Boolean)
        // @description
        // Returns whether the player is currently online.
        // Works with offline players (returns false in that case).
        // -->
        registerTag("is_online", (attribute, object) -> {
            return new ElementTag(object.isOnline());
        });

        // <--[tag]
        // @attribute <PlayerTag.is_op>
        // @returns ElementTag(Boolean)
        // @mechanism PlayerTag.is_op
        // @description
        // Returns whether the player is a full server operator.
        // Works with offline players.
        // -->
        registerTag("is_op", (attribute, object) -> {
            return new ElementTag(object.getOfflinePlayer().isOp());
        });

        // <--[tag]
        // @attribute <PlayerTag.is_whitelisted>
        // @returns ElementTag(Boolean)
        // @mechanism PlayerTag.is_whitelisted
        // @description
        // Returns whether the player is whitelisted.
        // Works with offline players.
        // -->
        registerTag("is_whitelisted", (attribute, object) -> {
            return new ElementTag(object.getOfflinePlayer().isWhitelisted());
        });

        // <--[tag]
        // @attribute <PlayerTag.last_played_time>
        // @returns TimeTag
        // @description
        // Returns the time of when the player was last seen.
        // Works with offline players.
        // Not very useful for online players.
        // -->
        registerTag("last_played_time", (attribute, object) -> {
            if (object.isOnline()) {
                return TimeTag.now();
            }
            return new TimeTag(object.getOfflinePlayer().getLastPlayed());
        });
        registerTag("last_played", (attribute, object) -> {
            Deprecations.playerTimePlayedTags.warn(attribute.context);
            if (object.isOnline()) {
                return new DurationTag(System.currentTimeMillis() / 50);
            }
            return new DurationTag(object.getOfflinePlayer().getLastPlayed() / 50);
        });

        // <--[tag]
        // @attribute <PlayerTag.ban_expiration_time>
        // @returns TimeTag
        // @description
        // Returns the expiration of the player's ban, if they are banned.
        // Potentially can be null.
        // -->
        registerTag("ban_expiration_time", (attribute, object) -> {
            BanEntry ban = Bukkit.getBanList(BanList.Type.NAME).getBanEntry(object.getName());
            if (ban == null || ban.getExpiration() == null || (ban.getExpiration() != null && ban.getExpiration().before(new Date()))) {
                return null;
            }
            return new TimeTag(ban.getExpiration().getTime());
        });
        registerTag("ban_expiration", (attribute, object) -> {
            Deprecations.playerTimePlayedTags.warn(attribute.context);
            BanEntry ban = Bukkit.getBanList(BanList.Type.NAME).getBanEntry(object.getName());
            if (ban == null || ban.getExpiration() == null || (ban.getExpiration() != null && ban.getExpiration().before(new Date()))) {
                return null;
            }
            return new DurationTag(ban.getExpiration().getTime() / 50);
        });

        // <--[tag]
        // @attribute <PlayerTag.ban_reason>
        // @returns ElementTag
        // @description
        // Returns the reason for the player's ban, if they are banned.
        // -->
        registerTag("ban_reason", (attribute, object) -> {
            BanEntry ban = Bukkit.getBanList(BanList.Type.NAME).getBanEntry(object.getName());
            if (ban == null || (ban.getExpiration() != null && ban.getExpiration().before(new Date()))) {
                return null;
            }
            return new ElementTag(ban.getReason());
        });

        // <--[tag]
        // @attribute <PlayerTag.ban_created_time>
        // @returns TimeTag
        // @description
        // Returns when the player's ban was created, if they are banned.
        // -->
        registerTag("ban_created_time", (attribute, object) -> {
            BanEntry ban = Bukkit.getBanList(BanList.Type.NAME).getBanEntry(object.getName());
            if (ban == null || (ban.getExpiration() != null && ban.getExpiration().before(new Date()))) {
                return null;
            }
            return new TimeTag(ban.getCreated().getTime());
        });
        registerTag("ban_created", (attribute, object) -> {
            Deprecations.timeTagRewrite.warn(attribute.context);
            BanEntry ban = Bukkit.getBanList(BanList.Type.NAME).getBanEntry(object.getName());
            if (ban == null || (ban.getExpiration() != null && ban.getExpiration().before(new Date()))) {
                return null;
            }
            return new DurationTag(ban.getCreated().getTime() / 50);
        });

        // <--[tag]
        // @attribute <PlayerTag.ban_source>
        // @returns ElementTag
        // @description
        // Returns the source of the player's ban, if they are banned.
        // -->
        registerTag("ban_source", (attribute, object) -> {
            BanEntry ban = Bukkit.getBanList(BanList.Type.NAME).getBanEntry(object.getName());
            if (ban == null || (ban.getExpiration() != null && ban.getExpiration().before(new Date()))) {
                return null;
            }
            return new ElementTag(ban.getSource());
        });

        registerTag("ban_info", (attribute, object) -> {
            Deprecations.playerBanInfoTags.warn(attribute.context);
            BanEntry ban = Bukkit.getBanList(BanList.Type.NAME).getBanEntry(object.getName());
            if (ban == null || (ban.getExpiration() != null && ban.getExpiration().before(new Date()))) {
                return null;
            }

            if (attribute.startsWith("expiration", 2) && ban.getExpiration() != null) {
                attribute.fulfill(1);
                return new DurationTag(ban.getExpiration().getTime() / 50);
            }

            else if (attribute.startsWith("reason", 2)) {
                attribute.fulfill(1);
                return new ElementTag(ban.getReason());
            }

            else if (attribute.startsWith("created", 2)) {
                attribute.fulfill(1);
                return new DurationTag(ban.getCreated().getTime() / 50);
            }

            else if (attribute.startsWith("source", 2)) {
                attribute.fulfill(1);
                return new ElementTag(ban.getSource());
            }

            return null;
        });


        // <--[tag]
        // @attribute <PlayerTag.statistic[<statistic>]>
        // @returns ElementTag(Number)
        // @description
        // Returns the player's current value for the specified statistic.
        // Valid statistics: <@link url https://hub.spigotmc.org/javadocs/spigot/org/bukkit/Statistic.html>
        // Works with offline players.
        // -->
        registerTag("statistic", (attribute, object) -> {
            if (!attribute.hasContext(1)) {
                return null;
            }
            Statistic statistic;
            try {
                statistic = Statistic.valueOf(attribute.getContext(1).toUpperCase());
            }
            catch (IllegalArgumentException ex) {
                attribute.echoError("Statistic '" + attribute.getContext(1) + "' does not exist: " + ex.getMessage());
                return null;
            }

            // <--[tag]
            // @attribute <PlayerTag.statistic[<statistic>].qualifier[<material>/<entity>]>
            // @returns ElementTag(Number)
            // @description
            // Returns the player's current value for the specified statistic, with the
            // specified qualifier, which can be either an entity or material.
            // Valid statistics: <@link url https://hub.spigotmc.org/javadocs/spigot/org/bukkit/Statistic.html>
            // To check a statistic type dynamically, refer to <@link tag server.statistic_type>.
            // -->
            if (attribute.startsWith("qualifier", 2)) {
                ObjectTag obj = ObjectFetcher.pickObjectFor(attribute.getContext(2), attribute.context);
                attribute.fulfill(1);
                try {
                    if (obj instanceof MaterialTag) {
                        return new ElementTag(object.getOfflinePlayer().getStatistic(statistic, ((MaterialTag) obj).getMaterial()));
                    }
                    else if (obj instanceof EntityTag) {
                        return new ElementTag(object.getOfflinePlayer().getStatistic(statistic, ((EntityTag) obj).getBukkitEntityType()));
                    }
                    else {
                        return null;
                    }
                }
                catch (Exception e) {
                    Debug.echoError("Invalid statistic: " + statistic + " for this player!");
                    return null;
                }
            }
            try {
                return new ElementTag(object.getOfflinePlayer().getStatistic(statistic));
            }
            catch (Exception e) {
                Debug.echoError("Invalid statistic: " + statistic + " for this player!");
                return null;
            }
        });

        // <--[tag]
        // @attribute <PlayerTag.uuid>
        // @returns ElementTag
        // @description
        // Returns the UUID of the player.
        // Works with offline players.
        // -->
        registerTag("uuid", (attribute, object) -> {
            return new ElementTag(object.getUUID().toString());
        });

        // <--[tag]
        // @attribute <PlayerTag.list_name>
        // @returns ElementTag
        // @mechanism PlayerTag.player_list_name
        // @description
        // Returns the name of the player as shown in the player list.
        // -->
        registerOnlineOnlyTag("list_name", (attribute, object) -> {
            return new ElementTag(AdvancedTextImpl.instance.getPlayerListName(object.getPlayerEntity()));
        });

        // <--[tag]
        // @attribute <PlayerTag.display_name>
        // @returns ElementTag
        // @mechanism PlayerTag.display_name
        // @description
        // Returns the display name of the player, which may contain prefixes and suffixes, colors, etc.
        // -->
        registerOnlineOnlyTag("display_name", (attribute, object) -> {
            return new ElementTag(object.getPlayerEntity().getDisplayName());
        });

        // Documented in EntityTag
        registerTag("name", (attribute, object) -> {
            if (attribute.startsWith("list", 2) && object.isOnline()) {
                Deprecations.playerNameTags.warn(attribute.context);
                attribute.fulfill(1);
                return new ElementTag(object.getPlayerEntity().getPlayerListName());
            }
            if (attribute.startsWith("display", 2) && object.isOnline()) {
                Deprecations.playerNameTags.warn(attribute.context);
                attribute.fulfill(1);
                return new ElementTag(object.getPlayerEntity().getDisplayName());
            }
            return new ElementTag(object.getName());
        });

        // <--[tag]
        // @attribute <PlayerTag.client_brand>
        // @returns ElementTag
        // @description
        // Returns the brand of the client, as sent via the "minecraft:brand" packet.
        // On normal clients, will say "vanilla". On broken clients, will say "unknown". Modded clients will identify themselves (though not guaranteed!).
        // -->
        registerOnlineOnlyTag("client_brand", (attribute, object) -> {
            return new ElementTag(NMSHandler.getPlayerHelper().getPlayerBrand(object.getPlayerEntity()));
        });

        // <--[tag]
        // @attribute <PlayerTag.locale>
        // @returns ElementTag
        // @description
        // Returns the current locale of the player.
        // -->
        registerOnlineOnlyTag("locale", (attribute, object) -> {
            return new ElementTag(object.getPlayerEntity().getLocale());
        });

        /////////////////////
        //   INVENTORY ATTRIBUTES
        /////////////////

        // <--[tag]
        // @attribute <PlayerTag.inventory>
        // @returns InventoryTag
        // @description
        // Returns a InventoryTag of the player's current inventory.
        // Works with offline players.
        // -->
        registerTag("inventory", (attribute, object) -> {
            return object.getInventory();
        });

        // <--[tag]
        // @attribute <PlayerTag.enderchest>
        // @returns InventoryTag
        // @description
        // Gets the player's enderchest inventory.
        // Works with offline players.
        // -->
        registerTag("enderchest", (attribute, object) -> {
            return object.getEnderChest();
        });

        /////////////////////
        //   ONLINE ATTRIBUTES
        /////////////////

        // <--[tag]
        // @attribute <PlayerTag.open_inventory>
        // @returns InventoryTag
        // @description
        // Gets the inventory the player currently has open. If the player has no open
        // inventory, this returns the player's inventory.
        // -->
        registerOnlineOnlyTag("open_inventory", (attribute, object) -> {
            return InventoryTag.mirrorBukkitInventory(object.getPlayerEntity().getOpenInventory().getTopInventory());
        });

        // <--[tag]
        // @attribute <PlayerTag.discovered_recipes>
        // @returns ListTag
        // @description
        // Returns a list of the recipes the player has discovered, in the Namespace:Key format, for example "minecraft:gold_nugget".
        // -->
        registerOnlineOnlyTag("discovered_recipes", (attribute, object) -> {
            return new ListTag(NMSHandler.getEntityHelper().getDiscoveredRecipes(object.getPlayerEntity()));
        });

        // <--[tag]
        // @attribute <PlayerTag.selected_trade_index>
        // @returns ElementTag(Number)
        // @description
        // Returns the index of the trade the player is currently viewing, if any.
        // -->
        registerOnlineOnlyTag("selected_trade_index", (attribute, object) -> {
            if (object.getPlayerEntity().getOpenInventory().getTopInventory() instanceof MerchantInventory) {
                return new ElementTag(((MerchantInventory) object.getPlayerEntity().getOpenInventory().getTopInventory())
                        .getSelectedRecipeIndex() + 1);
            }
            return null;
        });

        // [tag]
        // @attribute <PlayerTag.selected_trade>
        // @returns TradeTag
        // @description
        // Returns the trade the player is currently viewing, if any.
        // This is almost completely broke and only works if the player has placed items in the trade slots.
        //
        registerOnlineOnlyTag("selected_trade", (attribute, object) -> {
            Inventory playerInventory = object.getPlayerEntity().getOpenInventory().getTopInventory();
            if (playerInventory instanceof MerchantInventory && ((MerchantInventory) playerInventory).getSelectedRecipe() != null) {
                return new TradeTag(((MerchantInventory) playerInventory).getSelectedRecipe());
            }
            return null;
        });

        // <--[tag]
        // @attribute <PlayerTag.item_on_cursor>
        // @returns ItemTag
        // @mechanism PlayerTag.item_on_cursor
        // @description
        // Returns the item on the player's cursor, if any. This includes
        // chest interfaces, inventories, and hotbars, etc.
        // -->
        registerOnlineOnlyTag("item_on_cursor", (attribute, object) -> {
            return new ItemTag(object.getPlayerEntity().getItemOnCursor());
        });

        // <--[tag]
        // @attribute <PlayerTag.held_item_slot>
        // @returns ElementTag(Number)
        // @description
        // Returns the slot location of the player's selected item.
        // -->
        registerOnlineOnlyTag("held_item_slot", (attribute, object) -> {
            return new ElementTag(object.getPlayerEntity().getInventory().getHeldItemSlot() + 1);
        });

        registerOnlineOnlyTag("item_in_hand", (attribute, object) -> {
            if (attribute.startsWith("slot", 2)) {
                Deprecations.playerItemInHandSlotTag.warn(attribute.context);
                attribute.fulfill(1);
                return new ElementTag(object.getPlayerEntity().getInventory().getHeldItemSlot() + 1);
            }
            return object.getHeldItem();
        });

        // <--[tag]
        // @attribute <PlayerTag.sidebar_lines>
        // @returns ListTag
        // @description
        // Returns the current lines set on the player's Sidebar via <@link command sidebar>.
        // -->
        registerOnlineOnlyTag("sidebar_lines", (attribute, object) -> {
            Sidebar sidebar = SidebarCommand.getSidebar(object);
            if (sidebar == null) {
                return null;
            }
            return new ListTag(sidebar.getLinesText());
        });

        // <--[tag]
        // @attribute <PlayerTag.sidebar_title>
        // @returns ElementTag
        // @description
        // Returns the current title set on the player's Sidebar via <@link command sidebar>.
        // -->
        registerOnlineOnlyTag("sidebar_title", (attribute, object) -> {
            Sidebar sidebar = SidebarCommand.getSidebar(object);
            if (sidebar == null) {
                return null;
            }
            return new ElementTag(sidebar.getTitle());
        });

        // <--[tag]
        // @attribute <PlayerTag.sidebar_scores>
        // @returns ListTag
        // @description
        // Returns the current scores set on the player's Sidebar via <@link command sidebar>,
        // in the same order as <@link tag PlayerTag.sidebar_lines>.
        // -->
        registerOnlineOnlyTag("sidebar_scores", (attribute, object) -> {
            Sidebar sidebar = SidebarCommand.getSidebar(object);
            if (sidebar == null) {
                return null;
            }
            ListTag scores = new ListTag();
            for (int score : sidebar.getScores()) {
                scores.add(String.valueOf(score));
            }
            return scores;
        });

        registerOnlineOnlyTag("sidebar", (attribute, object) -> {
            Deprecations.playerSidebarTags.warn(attribute.context);
            if (attribute.startsWith("lines", 2)) {
                attribute.fulfill(1);
                Sidebar sidebar = SidebarCommand.getSidebar(object);
                if (sidebar == null) {
                    return null;
                }
                return new ListTag(sidebar.getLinesText());
            }
            if (attribute.startsWith("title", 2)) {
                attribute.fulfill(1);
                Sidebar sidebar = SidebarCommand.getSidebar(object);
                if (sidebar == null) {
                    return null;
                }
                return new ElementTag(sidebar.getTitle());
            }
            if (attribute.startsWith("scores", 2)) {
                attribute.fulfill(1);
                Sidebar sidebar = SidebarCommand.getSidebar(object);
                if (sidebar == null) {
                    return null;
                }
                ListTag scores = new ListTag();
                for (int score : sidebar.getScores()) {
                    scores.add(String.valueOf(score));
                }
                return scores;
            }
            return null;
        });

        // <--[tag]
        // @attribute <PlayerTag.skin_blob>
        // @returns ElementTag
        // @mechanism PlayerTag.skin_blob
        // @description
        // Returns the player's current skin blob.
        // In the format: "texture;signature" (two values separated by a semicolon).
        // See also <@link language Player Entity Skins (Skin Blobs)>.
        // -->
        registerOnlineOnlyTag("skin_blob", (attribute, object) -> {
            return new ElementTag(NMSHandler.getInstance().getProfileEditor().getPlayerSkinBlob(object.getPlayerEntity()));
        });

        // <--[tag]
        // @attribute <PlayerTag.skull_skin>
        // @returns ElementTag
        // @description
        // Returns the player's current skin blob, formatted for input to a Player Skull item.
        // In the format: "UUID|Texture|Name" (three values separated by pipes).
        // See also <@link language Player Entity Skins (Skin Blobs)>.
        // -->
        registerOnlineOnlyTag("skull_skin", (attribute, object) -> {
            String skin = NMSHandler.getInstance().getProfileEditor().getPlayerSkinBlob(object.getPlayerEntity());
            if (skin == null) {
                return null;
            }
            int semicolon = skin.indexOf(';');
            return new ElementTag(object.getPlayerEntity().getUniqueId() + "|" + skin.substring(0, semicolon) + "|" + object.getName());
        });

        // <--[tag]
        // @attribute <PlayerTag.skull_item>
        // @returns ItemTag
        // @description
        // Returns a Player_Head item with the skin of the player.
        // See also <@link language Player Entity Skins (Skin Blobs)>.
        // -->
        registerOnlineOnlyTag("skull_item", (attribute, object) -> {
            ItemStack item = new ItemStack(Material.PLAYER_HEAD);
            item = NMSHandler.getItemHelper().setSkullSkin(item, NMSHandler.getInstance().getPlayerProfile(object.getPlayerEntity()));
            return new ItemTag(item);
        });

        // <--[tag]
        // @attribute <PlayerTag.attack_cooldown_duration>
        // @returns DurationTag
        // @mechanism PlayerTag.attack_cooldown
        // @description
        // Returns the amount of time that passed since the start of the attack cooldown.
        // -->
        registerOnlineOnlyTag("attack_cooldown_duration", (attribute, object) -> {
            return new DurationTag((long) NMSHandler.getPlayerHelper().ticksPassedDuringCooldown(object.getPlayerEntity()));
        });

        // <--[tag]
        // @attribute <PlayerTag.attack_cooldown_max_duration>
        // @returns DurationTag
        // @mechanism PlayerTag.attack_cooldown
        // @description
        // Returns the maximum amount of time that can pass before the player's main hand has returned
        // to its original place after the cooldown has ended.
        // NOTE: This is slightly inaccurate and may not necessarily match with the actual attack
        // cooldown progress.
        // -->
        registerOnlineOnlyTag("attack_cooldown_max_duration", (attribute, object) -> {
            return new DurationTag((long) NMSHandler.getPlayerHelper().getMaxAttackCooldownTicks(object.getPlayerEntity()));
        });

        // <--[tag]
        // @attribute <PlayerTag.attack_cooldown_percent>
        // @returns ElementTag(Decimal)
        // @mechanism PlayerTag.attack_cooldown_percent
        // @description
        // Returns the progress of the attack cooldown. 0 means that the attack cooldown has just
        // started, while 100 means that the attack cooldown has finished.
        // NOTE: This may not match exactly with the clientside attack cooldown indicator.
        // -->
        registerOnlineOnlyTag("attack_cooldown_percent", (attribute, object) -> {
            return new ElementTag(NMSHandler.getPlayerHelper().getAttackCooldownPercent(object.getPlayerEntity()) * 100);
        });

        registerOnlineOnlyTag("attack_cooldown", (attribute, object) -> {
            Deprecations.playerAttackCooldownTags.warn(attribute.context);
            if (attribute.startsWith("duration", 2)) {
                attribute.fulfill(1);
                return new DurationTag((long) NMSHandler.getPlayerHelper()
                        .ticksPassedDuringCooldown(object.getPlayerEntity()));
            }
            else if (attribute.startsWith("max_duration", 2)) {
                attribute.fulfill(1);
                return new DurationTag((long) NMSHandler.getPlayerHelper()
                        .getMaxAttackCooldownTicks(object.getPlayerEntity()));
            }

            else if (attribute.startsWith("percent", 2)) {
                attribute.fulfill(1);
                return new ElementTag(NMSHandler.getPlayerHelper()
                        .getAttackCooldownPercent(object.getPlayerEntity()) * 100);
            }

            Debug.echoError("The tag 'player.attack_cooldown...' must be followed by a sub-tag.");

            return null;
        });

        /////////////////////
        //   CITIZENS ATTRIBUTES
        /////////////////

        // <--[tag]
        // @attribute <PlayerTag.selected_npc>
        // @returns NPCTag
        // @mechanism PlayerTag.selected_npc
        // @description
        // Returns the NPCTag that the player currently has selected with '/npc select', null if no NPC selected.
        // -->
        registerOnlineOnlyTag("selected_npc", (attribute, object) -> {
            if (object.getPlayerEntity().hasMetadata("selected")) {
                return object.getSelectedNPC();
            }
            return null;
        });

        /////////////////////
        //   CONVERSION ATTRIBUTES
        /////////////////

        // <--[tag]
        // @attribute <PlayerTag.entity>
        // @returns EntityTag
        // @description
        // Returns the EntityTag object of the player.
        // (Note: This should never actually be needed. PlayerTags are considered valid EntityTags.)
        // -->
        registerOnlineOnlyTag("entity", (attribute, object) -> {
            return new EntityTag(object.getPlayerEntity());
        });

        /////////////////////
        //   IDENTIFICATION ATTRIBUTES
        /////////////////

        // <--[tag]
        // @attribute <PlayerTag.ip_address>
        // @returns ElementTag
        // @description
        // Returns the player's IP address, without port or hostname.
        // -->
        registerOnlineOnlyTag("ip_address", (attribute, object) -> {
            return new ElementTag(object.getPlayerEntity().getAddress().getAddress().toString());
        });

        // <--[tag]
        // @attribute <PlayerTag.ip>
        // @returns ElementTag
        // @description
        // Returns the player's IP address host name.
        // -->
        registerOnlineOnlyTag("ip", (attribute, object) -> {
            // <--[tag]
            // @attribute <PlayerTag.ip.address_only>
            // @returns ElementTag
            // @description
            // Returns the player's IP address with port (without triggering an rdns lookup).
            // -->
            if (attribute.startsWith("address_only", 2)) {
                attribute.fulfill(1);
                return new ElementTag(object.getPlayerEntity().getAddress().toString());
            }
            String host = object.getPlayerEntity().getAddress().getHostName();

            // <--[tag]
            // @attribute <PlayerTag.ip.address>
            // @returns ElementTag
            // @description
            // Returns the player's IP address with port (usually including an rdns host path).
            // -->
            if (attribute.startsWith("address", 2)) {
                attribute.fulfill(1);
                return new ElementTag(object.getPlayerEntity().getAddress().toString());
            }
            return new ElementTag(host);
        }, "host_name");

        // <--[tag]
        // @attribute <PlayerTag.nameplate>
        // @returns ElementTag
        // @description
        // Returns the displayed text in the nameplate of the player.
        // -->
        registerOnlineOnlyTag("nameplate", (attribute, object) -> {
            return new ElementTag(NMSHandler.getInstance().getProfileEditor().getPlayerName(object.getPlayerEntity()));
        });

        /////////////////////
        //   LOCATION ATTRIBUTES
        /////////////////

        // <--[tag]
        // @attribute <PlayerTag.compass_target>
        // @returns LocationTag
        // @description
        // Returns the location of the player's compass target.
        // -->
        registerOnlineOnlyTag("compass_target", (attribute, object) -> {
            Location target = object.getPlayerEntity().getCompassTarget();
            if (target != null) {
                return new LocationTag(target);
            }
            return null;
        });

        // <--[tag]
        // @attribute <PlayerTag.chunk_loaded[<chunk>]>
        // @returns ElementTag(Boolean)
        // @description
        // Returns whether the player has the chunk loaded on their client.
        // -->
        registerOnlineOnlyTag("chunk_loaded", (attribute, object) -> {
            if (!attribute.hasContext(1)) {
                return null;
            }
            ChunkTag chunk = attribute.contextAsType(1, ChunkTag.class);
            if (chunk == null) {
                return null;
            }
            return new ElementTag(chunk.isLoadedSafe() && object.hasChunkLoaded(chunk.getChunkForTag(attribute)));
        });

        /////////////////////
        //   STATE ATTRIBUTES
        /////////////////

        // <--[tag]
        // @attribute <PlayerTag.can_fly>
        // @returns ElementTag(Boolean)
        // @mechanism PlayerTag.can_fly
        // @description
        // Returns whether the player is allowed to fly.
        // -->
        registerOnlineOnlyTag("can_fly", (attribute, object) -> {
            return new ElementTag(object.getPlayerEntity().getAllowFlight());
        }, "allowed_flight");

        // <--[tag]
        // @attribute <PlayerTag.fly_speed>
        // @returns ElementTag(Decimal)
        // @mechanism PlayerTag.fly_speed
        // @description
        // Returns the speed the player can fly at.
        // Default value is '0.2'.
        // -->
        registerOnlineOnlyTag("fly_speed", (attribute, object) -> {
            return new ElementTag(object.getPlayerEntity().getFlySpeed());
        });

        // <--[tag]
        // @attribute <PlayerTag.walk_speed>
        // @returns ElementTag(Decimal)
        // @mechanism PlayerTag.walk_speed
        // @description
        // Returns the speed the player can walk at.
        // -->
        registerOnlineOnlyTag("walk_speed", (attribute, object) -> {
            return new ElementTag(object.getPlayerEntity().getWalkSpeed());
        });

        // <--[tag]
        // @attribute <PlayerTag.saturation>
        // @returns ElementTag(Decimal)
        // @mechanism PlayerTag.saturation
        // @description
        // Returns the current food saturation of the player.
        // -->
        registerOnlineOnlyTag("saturation", (attribute, object) -> {
            return new ElementTag(object.getPlayerEntity().getSaturation());
        });

        // <--[tag]
        // @attribute <PlayerTag.formatted_food_level[(<max>)]>
        // @returns ElementTag
        // @mechanism PlayerTag.food_level
        // @description
        // Returns a 'formatted' value of the player's current food level.
        // May be 'starving', 'famished', 'parched, 'hungry', or 'healthy'.
        // -->
        registerOnlineOnlyTag("formatted_food_level", (attribute, object) -> {
            double maxHunger = object.getPlayerEntity().getMaxHealth();
            if (attribute.hasContext(1)) {
                maxHunger = attribute.getIntContext(1);
            }
            attribute.fulfill(1);
            int foodLevel = object.getFoodLevel();
            if (foodLevel / maxHunger < .10) {
                return new ElementTag("starving");
            }
            else if (foodLevel / maxHunger < .40) {
                return new ElementTag("famished");
            }
            else if (foodLevel / maxHunger < .75) {
                return new ElementTag("parched");
            }
            else if (foodLevel / maxHunger < 1) {
                return new ElementTag("hungry");
            }
            else {
                return new ElementTag("healthy");
            }
        });

        // <--[tag]
        // @attribute <PlayerTag.food_level>
        // @returns ElementTag(Number)
        // @mechanism PlayerTag.food_level
        // @description
        // Returns the current food level (aka hunger) of the player.
        // -->
        registerOnlineOnlyTag("food_level", (attribute, object) -> {
            if (attribute.startsWith("formatted", 2)) {
                Deprecations.playerFoodLevelFormatTag.warn(attribute.context);
                double maxHunger = object.getPlayerEntity().getMaxHealth();
                if (attribute.hasContext(2)) {
                    maxHunger = attribute.getIntContext(2);
                }
                attribute.fulfill(1);
                int foodLevel = object.getFoodLevel();
                if (foodLevel / maxHunger < .10) {
                    return new ElementTag("starving");
                }
                else if (foodLevel / maxHunger < .40) {
                    return new ElementTag("famished");
                }
                else if (foodLevel / maxHunger < .75) {
                    return new ElementTag("parched");
                }
                else if (foodLevel / maxHunger < 1) {
                    return new ElementTag("hungry");
                }
                else {
                    return new ElementTag("healthy");
                }
            }
            return new ElementTag(object.getFoodLevel());
        });

        // <--[tag]
        // @attribute <PlayerTag.gamemode>
        // @returns ElementTag
        // @mechanism PlayerTag.gamemode
        // @description
        // Returns the name of the gamemode the player is currently set to.
        // Works with offline players.
        // -->
        registerTag("gamemode", (attribute, object) -> {
            if (object.isOnline()) {
                return new ElementTag(object.getPlayerEntity().getGameMode().name());
            }
            return new ElementTag(object.getNBTEditor().getGameMode().name());
        });

        // <--[tag]
        // @attribute <PlayerTag.is_blocking>
        // @returns ElementTag(Boolean)
        // @description
        // Returns whether the player is currently blocking.
        // -->
        registerOnlineOnlyTag("is_blocking", (attribute, object) -> {
            return new ElementTag(object.getPlayerEntity().isBlocking());
        });

        // <--[tag]
        // @attribute <PlayerTag.ping>
        // @returns ElementTag(Number)
        // @description
        // Returns the player's current ping.
        // -->
        registerOnlineOnlyTag("ping", (attribute, object) -> {
            return new ElementTag(NMSHandler.getPlayerHelper().getPing(object.getPlayerEntity()));
        });

        // <--[tag]
        // @attribute <PlayerTag.is_flying>
        // @returns ElementTag(Boolean)
        // @description
        // Returns whether the player is currently flying.
        // -->
        registerOnlineOnlyTag("is_flying", (attribute, object) -> {
            return new ElementTag(object.getPlayerEntity().isFlying());
        });

        // <--[tag]
        // @attribute <PlayerTag.is_sneaking>
        // @returns ElementTag(Boolean)
        // @description
        // Returns whether the player is currently sneaking.
        // -->
        registerOnlineOnlyTag("is_sneaking", (attribute, object) -> {
            return new ElementTag(object.getPlayerEntity().isSneaking());
        });

        // <--[tag]
        // @attribute <PlayerTag.is_sprinting>
        // @returns ElementTag(Boolean)
        // @mechanism PlayerTag.sprinting
        // @description
        // Returns whether the player is currently sprinting.
        // -->
        registerOnlineOnlyTag("is_sprinting", (attribute, object) -> {
            return new ElementTag(object.getPlayerEntity().isSprinting());
        });

        // <--[tag]
        // @attribute <PlayerTag.has_advancement[<advancement>]>
        // @returns ElementTag(Boolean)
        // @description
        // Returns whether the player has completed the specified advancement.
        // -->
        registerOnlineOnlyTag("has_advancement", (attribute, object) -> {
            if (!attribute.hasContext(1)) {
                return null;
            }
            Advancement adv = AdvancementHelper.getAdvancement(attribute.getContext(1));
            if (adv == null) {
                if (!attribute.hasAlternative()) {
                    Debug.echoError("Advancement '" + attribute.getContext(1) + "' does not exist.");
                }
                return null;
            }
            AdvancementProgress progress = object.getPlayerEntity().getAdvancementProgress(adv);
            return new ElementTag(progress.isDone());
        });

        // <--[tag]
        // @attribute <PlayerTag.advancements>
        // @returns ListTag
        // @description
        // Returns a list of the names of all advancements the player has completed.
        // -->
        registerOnlineOnlyTag("advancements", (attribute, object) -> {
            ListTag list = new ListTag();
            Bukkit.advancementIterator().forEachRemaining((adv) -> {
                if (object.getPlayerEntity().getAdvancementProgress(adv).isDone()) {
                    list.add(adv.getKey().toString());
                }
            });
            return list;
        }, "list_advancements");

        // <--[tag]
        // @attribute <PlayerTag.time_asleep>
        // @returns DurationTag
        // @description
        // Returns the time the player has been asleep.
        // -->
        registerOnlineOnlyTag("time_asleep", (attribute, object) -> {
            return new DurationTag((long) object.getPlayerEntity().getSleepTicks());
        });

        // <--[tag]
        // @attribute <PlayerTag.time>
        // @returns ElementTag(Number)
        // @description
        // Returns the time the player is currently experiencing. This time could differ from
        // the time that the rest of the world is currently experiencing if a 'time' or 'freeze_time'
        // mechanism is being used on the player.
        // -->
        registerOnlineOnlyTag("time", (attribute, object) -> {
            return new ElementTag(object.getPlayerEntity().getPlayerTime());
        });

        // <--[tag]
        // @attribute <PlayerTag.weather>
        // @returns ElementTag
        // @description
        // Returns the type of weather the player is experiencing. This will be different
        // from the weather currently in the world that the player is residing in if
        // the weather is currently being forced onto the player.
        // Returns null if the player does not currently have any forced weather.
        // -->
        registerOnlineOnlyTag("weather", (attribute, object) -> {
            if (object.getPlayerEntity().getPlayerWeather() != null) {
                return new ElementTag(object.getPlayerEntity().getPlayerWeather().name());
            }
            else {
                return null;
            }
        });

        // <--[tag]
        // @attribute <PlayerTag.xp_level>
        // @returns ElementTag(Number)
        // @description
        // Returns the number of XP levels the player has.
        // -->
        registerOnlineOnlyTag("xp_level", (attribute, object) -> {
            return new ElementTag(object.getPlayerEntity().getLevel());
        });
        // <--[tag]
        // @attribute <PlayerTag.xp_to_next_level>
        // @returns ElementTag(Number)
        // @description
        // Returns the amount of XP the player needs to get to the next level.
        // -->
        registerOnlineOnlyTag("xp_to_next_level", (attribute, object) -> {
            return new ElementTag(object.getPlayerEntity().getExpToLevel());
        });

        // <--[tag]
        // @attribute <PlayerTag.xp_total>
        // @returns ElementTag(Number)
        // @description
        // Returns the total amount of experience points the player has.
        // -->
        registerOnlineOnlyTag("xp_total", (attribute, object) -> {
            return new ElementTag(object.getPlayerEntity().getTotalExperience());
        });

        // <--[tag]
        // @attribute <PlayerTag.xp>
        // @returns ElementTag(Decimal)
        // @description
        // Returns the percentage of experience points to the next level.
        // -->
        registerOnlineOnlyTag("xp", (attribute, object) -> {
            if (attribute.startsWith("level", 2)) {
                Deprecations.playerXpTags.warn(attribute.context);
                attribute.fulfill(1);
                return new ElementTag(object.getPlayerEntity().getLevel());
            }
            if (attribute.startsWith("to_next_level", 2)) {
                Deprecations.playerXpTags.warn(attribute.context);
                attribute.fulfill(1);
                return new ElementTag(object.getPlayerEntity().getExpToLevel());
            }
            if (attribute.startsWith("total", 2)) {
                Deprecations.playerXpTags.warn(attribute.context);
                attribute.fulfill(1);
                return new ElementTag(object.getPlayerEntity().getTotalExperience());
            }
            return new ElementTag(object.getPlayerEntity().getExp() * 100);
        });

        // <--[tag]
        // @attribute <PlayerTag.fake_block_locations>
        // @returns ListTag(LocationTag)
        // @description
        // Returns a list of locations that the player will see a fake block at, as set by <@link command showfake> or connected commands.
        // -->
        registerTag("fake_block_locations", (attribute, object) -> {
            ListTag list = new ListTag();
            FakeBlock.FakeBlockMap map = FakeBlock.blocks.get(object.getUUID());
            if (map != null) {
                for (LocationTag loc : map.byLocation.keySet()) {
                    list.addObject(loc.clone());
                }
            }
            return list;
        });

        // <--[tag]
        // @attribute <PlayerTag.fake_block[<location>]>
        // @returns MaterialTag
        // @description
        // Returns the fake material that the player will see at the input location, as set by <@link command showfake> or connected commands.
        // Works best alongside <@link tag PlayerTag.fake_block_locations>.
        // Returns null if the player doesn't have a fake block at the location.
        // -->
        registerTag("fake_block", (attribute, object) -> {
            if (!attribute.hasContext(1)) {
                return null;
            }
            LocationTag input = attribute.contextAsType(1, LocationTag.class);
            FakeBlock.FakeBlockMap map = FakeBlock.blocks.get(object.getUUID());
            if (map != null) {
                FakeBlock block = map.byLocation.get(input);
                if (block != null) {
                    return block.material;
                }
            }
            return null;
        });

        // <--[tag]
        // @attribute <PlayerTag.fake_entities>
        // @returns ListTag(EntityTag)
        // @description
        // Returns a list of fake entities the player can see, as set by <@link command fakespawn>.
        // Note that these entities are not being tracked by the server, so many operations may not be possible on them.
        // -->
        registerTag("fake_entities", (attribute, object) -> {
            ListTag list = new ListTag();
            FakeEntity.FakeEntityMap map = FakeEntity.playersToEntities.get(object.getUUID());
            if (map != null) {
                for (Map.Entry<Integer, FakeEntity> entry : map.byId.entrySet()) {
                    list.addObject(entry.getValue().entity);
                }
            }
            return list;
        });

        // <--[tag]
        // @attribute <PlayerTag.disguise_to_self[(<player>)]>
        // @returns EntityTag
        // @group properties
        // @description
        // Returns the fake entity used to disguise the entity in the player's self-view (only relevant to players), either globally (if no context input given), or to the specified player.
        // Relates to <@link command disguise>.
        // -->
        registerTag("disguise_to_self", (attribute, object) -> {
            HashMap<UUID, DisguiseCommand.TrackedDisguise> map = DisguiseCommand.disguises.get(object.getUUID());
            if (map == null) {
                return null;
            }
            DisguiseCommand.TrackedDisguise disguise;
            if (attribute.hasContext(1)) {
                PlayerTag player = attribute.contextAsType(1, PlayerTag.class);
                if (player == null) {
                    attribute.echoError("Invalid player for is_disguised tag.");
                    return null;
                }
                disguise = map.get(player.getUUID());
                if (disguise == null) {
                    disguise = map.get(null);
                }
            }
            else {
                disguise = map.get(null);
            }
            if (disguise == null) {
                return null;
            }
            if (disguise.fakeToSelf == null) {
                return null;
            }
            return disguise.fakeToSelf.entity;
        });

        // <--[tag]
        // @attribute <PlayerTag.spectator_target>
        // @returns EntityTag
        // @mechanism PlayerTag.spectator_target
        // @description
        // Returns the entity that a spectator-mode player is currently spectating, if any.
        // -->
        registerOnlineOnlyTag("spectator_target", (attribute, object) -> {
            if (object.getPlayerEntity().getGameMode() != GameMode.SPECTATOR) {
                return null;
            }
            Entity target = object.getPlayerEntity().getSpectatorTarget();
            if (target == null) {
                return null;
            }
            return new EntityTag(target).getDenizenObject();
        });

        // <--[tag]
        // @attribute <PlayerTag.packets_sent>
        // @returns ElementTag(Number)
        // @description
        // Returns a total count of how many network packets have been sent to this player while they have been online.
        // -->
        registerOnlineOnlyTag("packets_sent", (attribute, object) -> {
            return new ElementTag(NMSHandler.getPacketHelper().getPacketStats(object.getPlayerEntity(), true));
        });

        // <--[tag]
        // @attribute <PlayerTag.packets_received>
        // @returns ElementTag(Number)
        // @description
        // Returns a total count of how many network packets have been received from this player while they have been online.
        // -->
        registerOnlineOnlyTag("packets_received", (attribute, object) -> {
            return new ElementTag(NMSHandler.getPacketHelper().getPacketStats(object.getPlayerEntity(), false));
        });
    }

    public static ObjectTagProcessor<PlayerTag> tagProcessor = new ObjectTagProcessor<>();

    public static void registerOnlineOnlyTag(String name, TagRunnable.ObjectInterface<PlayerTag> runnable, String... variants) {
        TagRunnable.ObjectInterface<PlayerTag> newRunnable = (attribute, object) -> {
            if (!object.isOnline()) {
                if (!attribute.hasAlternative()) {
                    Debug.echoError("Player is not online, but tag '" + attribute.getAttributeWithoutContext(1) + "' requires the player be online, for player: " + object.debuggable());
                }
                return null;
            }
            return runnable.run(attribute, object);
        };
        registerTag(name, newRunnable, variants);
    }

    public static void registerTag(String name, TagRunnable.ObjectInterface<PlayerTag> runnable, String... variants) {
        tagProcessor.registerTag(name, runnable, variants);
    }

    @Override
    public ObjectTag getObjectAttribute(Attribute attribute) {
        return tagProcessor.getObjectAttribute(this, attribute);
    }

    @Override
    public ObjectTag getNextObjectTypeDown() {
        if (isOnline()) {
            return new EntityTag(getPlayerEntity());
        }
        return new ElementTag(identify());
    }

    public void applyProperty(Mechanism mechanism) {
        Debug.echoError("Cannot apply properties to a player!");
    }

    @Override
    public void adjust(Mechanism mechanism) {

        // <--[mechanism]
        // @object PlayerTag
        // @name noclip
        // @input ElementTag(Boolean)
        // @description
        // When true, causes the server to allow the player to noclip (ie, walk through blocks without being prevented).
        // This is purely serverside. The client will still not walk through blocks.
        // This is useful alongside <@link command showfake>.
        // Note that this may sometimes be imperfect / sometimes momentarily continue to clip block.
        // Note that this may also prevent other collisions (eg projectile impact) but is not guaranteed to.
        // -->
        if (mechanism.matches("noclip") && mechanism.hasValue()) {
            if (mechanism.getValue().asBoolean()) {
                DenizenPacketHandler.forceNoclip.add(getUUID());
            }
            else {
                DenizenPacketHandler.forceNoclip.remove(getUUID());
            }
        }

        // <--[mechanism]
        // @object PlayerTag
        // @name respawn
        // @input None
        // @description
        // Forces the player to respawn if they are on the death screen.
        // -->
        if (mechanism.matches("respawn")) {
            NMSHandler.getPacketHelper().respawn(getPlayerEntity());
        }

        // <--[mechanism]
        // @object PlayerTag
        // @name vision
        // @input ElementTag
        // @description
        // Changes the player's vision to the provided entity type. Valid types:
        // ENDERMAN, CAVE_SPIDER, SPIDER, CREEPER
        // Provide no value to reset the player's vision.
        // -->
        if (mechanism.matches("vision")) {
            if (mechanism.hasValue() && mechanism.requireEnum(false, EntityType.values())) {
                NMSHandler.getPacketHelper().setVision(getPlayerEntity(), EntityType.valueOf(mechanism.getValue().asString().toUpperCase()));
            }
            else {
                NMSHandler.getPacketHelper().forceSpectate(getPlayerEntity(), getPlayerEntity());
            }
        }

        // <--[mechanism]
        // @object PlayerTag
        // @name level
        // @input ElementTag(Number)
        // @description
        // Sets the level on the player. Does not affect the current progression of experience towards next level.
        // @tags
        // <PlayerTag.xp_level>
        // -->
        if (mechanism.matches("level") && mechanism.requireInteger()) {
            setLevel(mechanism.getValue().asInt());
        }

        // <--[mechanism]
        // @object PlayerTag
        // @name item_slot
        // @input ElementTag(Number)
        // @description
        // Sets the inventory slot that the player has selected.
        // Works with offline players.
        // @tags
        // <PlayerTag.held_item_slot>
        // -->
        if (mechanism.matches("item_slot") && mechanism.requireInteger()) {
            if (isOnline()) {
                getPlayerEntity().getInventory().setHeldItemSlot(mechanism.getValue().asInt() - 1);
            }
            else {
                getNBTEditor().setItemInHand(mechanism.getValue().asInt() - 1);
            }
        }

        // <--[mechanism]
        // @object PlayerTag
        // @name window_property
        // @input ElementTag
        // @description
        // Sets various properties of a window the player has open, such as the open page in a lectern.
        // Input is of the form PROPERTY,VALUE where the value is a number.
        // Note that any adjusted window properties are entirely clientside.
        // Valid properties: <@link url https://hub.spigotmc.org/javadocs/spigot/org/bukkit/inventory/InventoryView.Property.html>
        // -->
        if (mechanism.matches("window_property")) {
            String[] split = mechanism.getValue().asString().split(",", 2);
            if (split.length != 2) {
                Debug.echoError("Invalid input! Must be in the form PROPERTY,VALUE");
            }
            else {
                try {
                    getPlayerEntity().setWindowProperty(InventoryView.Property.valueOf(split[0].toUpperCase()), Integer.parseInt(split[1]));
                }
                catch (NumberFormatException e) {
                    Debug.echoError("Input value must be a number!");
                }
                catch (IllegalArgumentException e) {
                    Debug.echoError("Must specify a valid window property!");
                }
            }
        }

        // <--[mechanism]
        // @object PlayerTag
        // @name item_on_cursor
        // @input ItemTag
        // @description
        // Sets the item on the player's cursor.
        // This includes chest interfaces, inventories, and hotbars, etc.
        // @tags
        // <PlayerTag.item_on_cursor>
        // -->
        if (mechanism.matches("item_on_cursor") && mechanism.requireObject(ItemTag.class)) {
            getPlayerEntity().setItemOnCursor(mechanism.valueAsType(ItemTag.class).getItemStack());
        }

        // <--[mechanism]
        // @object PlayerTag
        // @name award_advancement
        // @input ElementTag
        // @description
        // Awards an advancement to the player.
        // @tags
        // <PlayerTag.has_advancement[<name>]>
        // -->
        if (mechanism.matches("award_advancement")) {
            Advancement adv = AdvancementHelper.getAdvancement(mechanism.getValue().asString());
            if (adv == null) {
                if (mechanism.shouldDebug()) {
                    Debug.echoError("Advancement '" + mechanism.getValue().asString() + "' does not exist.");
                }
                return;
            }
            AdvancementProgress prog = getPlayerEntity().getAdvancementProgress(adv);
            for (String criteria : prog.getRemainingCriteria()) {
                prog.awardCriteria(criteria);
            }
        }

        // <--[mechanism]
        // @object PlayerTag
        // @name revoke_advancement
        // @input ElementTag
        // @description
        // Un-awards an advancement from the player.
        // @tags
        // <PlayerTag.has_advancement[<name>]>
        // -->
        if (mechanism.matches("revoke_advancement")) {
            Advancement adv = AdvancementHelper.getAdvancement(mechanism.getValue().asString());
            if (adv == null) {
                if (mechanism.shouldDebug()) {
                    Debug.echoError("Advancement '" + mechanism.getValue().asString() + "' does not exist.");
                }
                return;
            }
            AdvancementProgress prog = getPlayerEntity().getAdvancementProgress(adv);
            for (String criteria : prog.getAwardedCriteria()) {
                prog.revokeCriteria(criteria);
            }
        }

        // <--[mechanism]
        // @object PlayerTag
        // @name fake_absorption_health
        // @input ElementTag(Decimal)
        // @description
        // Shows the player fake absorption health that persists on damage.
        // -->
        if (mechanism.matches("fake_absorption_health") && mechanism.requireFloat()) {
            NMSHandler.getPacketHelper().setFakeAbsorption(getPlayerEntity(), mechanism.getValue().asFloat());
        }

        // <--[mechanism]
        // @object PlayerTag
        // @name health_scale
        // @input ElementTag(Decimal)
        // @description
        // Sets the 'health scale' on the Player. Each heart equals '2'. The standard health scale is
        // 20, so for example, indicating a value of 40 will display double the amount of hearts
        // standard.
        // Player relogging will reset this mechanism.
        // @tags
        // <PlayerTag.health_scale>
        // -->
        if (mechanism.matches("health_scale") && mechanism.requireDouble()) {
            getPlayerEntity().setHealthScale(mechanism.getValue().asDouble());
        }

        // <--[mechanism]
        // @object PlayerTag
        // @name scale_health
        // @input ElementTag(Boolean)
        // @description
        // Enables or disables the health scale value. Disabling will result in the standard
        // amount of hearts being shown.
        // @tags
        // <PlayerTag.health_is_scaled>
        // -->
        if (mechanism.matches("scale_health") && mechanism.requireBoolean()) {
            getPlayerEntity().setHealthScaled(mechanism.getValue().asBoolean());
        }

        // Allow offline editing of health values
        if (mechanism.matches("max_health") && mechanism.requireDouble()) {
            setMaxHealth(mechanism.getValue().asDouble());
        }

        if (mechanism.matches("health") && mechanism.requireDouble()) {
            setHealth(mechanism.getValue().asDouble());
        }

        // <--[mechanism]
        // @object PlayerTag
        // @name redo_attack_cooldown
        // @input None
        // @description
        // Forces the player to wait for the full attack cooldown duration for the item in their hand.
        // NOTE: The clientside attack cooldown indicator will not reflect this change!
        // @tags
        // <PlayerTag.attack_cooldown_duration>
        // <PlayerTag.attack_cooldown_max_duration>
        // <PlayerTag.attack_cooldown_percent>
        // -->
        if (mechanism.matches("redo_attack_cooldown")) {
            NMSHandler.getPlayerHelper().setAttackCooldown(getPlayerEntity(), 0);
        }

        // <--[mechanism]
        // @object PlayerTag
        // @name reset_attack_cooldown
        // @input None
        // @description
        // Ends the player's attack cooldown.
        // NOTE: This will do nothing if the player's attack speed attribute is set to 0.
        // NOTE: The clientside attack cooldown indicator will not reflect this change!
        // @tags
        // <PlayerTag.attack_cooldown_duration>
        // <PlayerTag.attack_cooldown_max_duration>
        // <PlayerTag.attack_cooldown_percent>
        // -->
        if (mechanism.matches("reset_attack_cooldown")) {
            PlayerHelper playerHelper = NMSHandler.getPlayerHelper();
            playerHelper.setAttackCooldown(getPlayerEntity(), Math.round(playerHelper.getMaxAttackCooldownTicks(getPlayerEntity())));
        }

        // <--[mechanism]
        // @object PlayerTag
        // @name attack_cooldown_percent
        // @input ElementTag(Decimal)
        // @description
        // Sets the progress of the player's attack cooldown. Takes a decimal from 0 to 1.
        // 0 means the cooldown has just begun, while 1 means the cooldown has been completed.
        // NOTE: The clientside attack cooldown indicator will not reflect this change!
        // @tags
        // <PlayerTag.attack_cooldown_duration>
        // <PlayerTag.attack_cooldown_max_duration>
        // <PlayerTag.attack_cooldown_percent>
        // -->
        if (mechanism.matches("attack_cooldown_percent") && mechanism.requireFloat()) {
            float percent = mechanism.getValue().asFloat();
            System.out.println(percent + " >> " + (percent >= 0 && percent <= 1));
            if (percent >= 0 && percent <= 1) {
                PlayerHelper playerHelper = NMSHandler.getPlayerHelper();
                playerHelper.setAttackCooldown(getPlayerEntity(),
                        Math.round(playerHelper.getMaxAttackCooldownTicks(getPlayerEntity()) * mechanism.getValue().asFloat()));
            }
            else {
                Debug.echoError("Invalid percentage! \"" + percent + "\" is not between 0 and 1!");
            }
        }

        // <--[mechanism]
        // @object PlayerTag
        // @name attack_cooldown
        // @input DurationTag
        // @description
        // Sets the player's time since their last attack. If the time is greater than the max duration of their
        // attack cooldown, then the cooldown is considered finished.
        // NOTE: The clientside attack cooldown indicator will not reflect this change!
        // @tags
        // <PlayerTag.attack_cooldown_duration>
        // <PlayerTag.attack_cooldown_max_duration>
        // <PlayerTag.attack_cooldown_percent>
        // -->
        if (mechanism.matches("attack_cooldown") && mechanism.requireObject(DurationTag.class)) {
            NMSHandler.getPlayerHelper().setAttackCooldown(getPlayerEntity(),
                    mechanism.getValue().asType(DurationTag.class, mechanism.context).getTicksAsInt());
        }

        // <--[mechanism]
        // @object PlayerTag
        // @name resource_pack
        // @input ElementTag(|ElementTag)
        // @description
        // Sets the current resource pack by specifying a valid URL to a resource pack.
        // Optionally, specify a 40-character (20 byte) hexadecimal SHA-1 hash value (without '0x') for the resource pack to prevent redownloading cached data.
        // With a hash, the input is formatted like "https://example.com/pack.zip|0102030405060708090a0b0c0d0e0f1112131415"
        // -->
        if (mechanism.matches("resource_pack") || mechanism.matches("texture_pack")) {
            String pack = mechanism.getValue().asString();
            int pipe = pack.indexOf('|');
            if (pipe > 0) {
                String hash = pack.substring(pipe + 1);
                pack = pack.substring(0, pipe);
                if (hash.length() != 40) {
                    Debug.echoError("Invalid resource_pack hash. Should be 40 characters of hexadecimal data.");
                    return;
                }
                byte[] hashData = new byte[20];
                for (int i = 0; i < 20; i++) {
                    hashData[i] = (byte) Integer.parseInt(hash.substring(i * 2, i * 2 + 2), 16);
                }
                getPlayerEntity().setResourcePack(pack, hashData);
            }
            else {
                getPlayerEntity().setResourcePack(pack);
            }
        }

        // <--[mechanism]
        // @object PlayerTag
        // @name saturation
        // @input ElementTag(Decimal)
        // @description
        // Sets the current food saturation level of a player.
        // @tags
        // <PlayerTag.saturation>
        // -->
        if (mechanism.matches("saturation") && mechanism.requireFloat()) {
            getPlayerEntity().setSaturation(mechanism.getValue().asFloat());
        }

        // <--[mechanism]
        // @object PlayerTag
        // @name send_map
        // @input ElementTag(Number)
        // @description
        // Forces a player to receive the entirety of the specified map ID instantly.
        // -->
        if (mechanism.matches("send_map") && mechanism.requireInteger()) {
            MapView map = Bukkit.getServer().getMap((short) mechanism.getValue().asInt());
            if (map != null) {
                getPlayerEntity().sendMap(map);
            }
            else {
                Debug.echoError("No map found for ID " + mechanism.getValue().asInt() + "!");
            }
        }

        // <--[mechanism]
        // @object PlayerTag
        // @name food_level
        // @input ElementTag(Number)
        // @description
        // Sets the current food level of a player. Typically, '20' is full.
        // @tags
        // <PlayerTag.food_level>
        // -->
        if (mechanism.matches("food_level") && mechanism.requireInteger()) {
            setFoodLevel(mechanism.getValue().asInt());
        }

        // <--[mechanism]
        // @object PlayerTag
        // @name bed_spawn_location
        // @input LocationTag
        // @description
        // Sets the bed location that the player respawns at.
        // @tags
        // <PlayerTag.bed_spawn>
        // -->
        if (mechanism.matches("bed_spawn_location") && mechanism.requireObject(LocationTag.class)) {
            setBedSpawnLocation(mechanism.valueAsType(LocationTag.class));
        }

        // <--[mechanism]
        // @object PlayerTag
        // @name can_fly
        // @input ElementTag(Boolean)
        // @description
        // Sets whether the player is allowed to fly.
        // @tags
        // <PlayerTag.can_fly>
        // -->
        if (mechanism.matches("can_fly") && mechanism.requireBoolean()) {
            getPlayerEntity().setAllowFlight(mechanism.getValue().asBoolean());
        }

        // <--[mechanism]
        // @object PlayerTag
        // @name fly_speed
        // @input ElementTag(Decimal)
        // @description
        // Sets the fly speed of the player. Valid range is 0.0 to 1.0
        // @tags
        // <PlayerTag.fly_speed>
        // -->
        if (mechanism.matches("fly_speed") && mechanism.requireFloat()) {
            setFlySpeed(mechanism.getValue().asFloat());
        }

        // <--[mechanism]
        // @object PlayerTag
        // @name flying
        // @input ElementTag(Boolean)
        // @description
        // Sets whether the player is flying.
        // @tags
        // <PlayerTag.is_flying>
        // -->
        if (mechanism.matches("flying") && mechanism.requireBoolean()) {
            getPlayerEntity().setFlying(mechanism.getValue().asBoolean());
        }

        // <--[mechanism]
        // @object PlayerTag
        // @name sprinting
        // @input ElementTag(Boolean)
        // @description
        // Sets whether the player is sprinting.
        // @tags
        // <PlayerTag.is_sprinting>
        // -->
        if (mechanism.matches("sprinting") && mechanism.requireBoolean()) {
            getPlayerEntity().setSprinting(mechanism.getValue().asBoolean());
        }

        // <--[mechanism]
        // @object PlayerTag
        // @name gamemode
        // @input ElementTag
        // @description
        // Sets the game mode of the player.
        // Valid gamemodes are survival, creative, adventure, and spectator.
        // Works with offline players.
        // @tags
        // <PlayerTag.gamemode>
        // -->
        if (mechanism.matches("gamemode") && mechanism.requireEnum(false, GameMode.values())) {
            setGameMode(GameMode.valueOf(mechanism.getValue().asString().toUpperCase()));
        }

        if (mechanism.matches("kick")) {
            Deprecations.oldKickMech.warn(mechanism.context);
            getPlayerEntity().kickPlayer(mechanism.getValue().asString());
        }

        if (mechanism.matches("weather") && mechanism.requireEnum(false, WeatherType.values())) {
            Deprecations.oldWeatherMech.warn(mechanism.context);
            getPlayerEntity().setPlayerWeather(WeatherType.valueOf(mechanism.getValue().asString().toUpperCase()));
        }

        if (mechanism.matches("reset_weather")) {
            Deprecations.oldWeatherMech.warn(mechanism.context);
            getPlayerEntity().resetPlayerWeather();
        }

        // <--[mechanism]
        // @object PlayerTag
        // @name player_list_name
        // @input ElementTag
        // @description
        // Sets the entry that is shown in the 'player list' that is shown when pressing tab.
        // @tags
        // <PlayerTag.list_name>
        // -->
        if (mechanism.matches("player_list_name")) {
            AdvancedTextImpl.instance.setPlayerListName(getPlayerEntity(), mechanism.getValue().asString());
        }

        // <--[mechanism]
        // @object PlayerTag
        // @name display_name
        // @input ElementTag
        // @description
        // Sets the name displayed for the player when chatting.
        // @tags
        // <PlayerTag.display_name>
        // -->
        if (mechanism.matches("display_name")) {
            getPlayerEntity().setDisplayName(mechanism.getValue().asString());
            return;
        }

        // <--[mechanism]
        // @object PlayerTag
        // @name show_workbench
        // @input LocationTag
        // @description
        // Shows the player a workbench GUI corresponding to a given location.
        // -->
        if (mechanism.matches("show_workbench") && mechanism.requireObject(LocationTag.class)) {
            getPlayerEntity().openWorkbench(mechanism.valueAsType(LocationTag.class), true);
            return;
        }

        // <--[mechanism]
        // @object PlayerTag
        // @name location
        // @input LocationTag
        // @description
        // If the player is online, teleports the player to a given location.
        // Otherwise, sets the player's next spawn location.
        // @tags
        // <PlayerTag.location>
        // -->
        if (mechanism.matches("location") && mechanism.requireObject(LocationTag.class)) {
            setLocation(mechanism.valueAsType(LocationTag.class));
        }

        if (mechanism.matches("time") && mechanism.requireInteger()) {
            Deprecations.oldTimeMech.warn(mechanism.context);
            getPlayerEntity().setPlayerTime(mechanism.getValue().asInt(), true);
        }

        if (mechanism.matches("freeze_time")) {
            Deprecations.oldTimeMech.warn(mechanism.context);
            if (mechanism.requireInteger("Invalid integer specified. Assuming current world time.")) {
                getPlayerEntity().setPlayerTime(mechanism.getValue().asInt(), false);
            }
            else {
                getPlayerEntity().setPlayerTime(getPlayerEntity().getWorld().getTime(), false);
            }
        }

        if (mechanism.matches("reset_time")) {
            Deprecations.oldTimeMech.warn(mechanism.context);
            getPlayerEntity().resetPlayerTime();
        }

        // <--[mechanism]
        // @object PlayerTag
        // @name walk_speed
        // @input ElementTag(Decimal)
        // @description
        // Sets the walk speed of the player. The standard value is '0.2'. Valid range is 0.0 to 1.0
        // @tags
        // <PlayerTag.walk_speed>
        // -->
        if (mechanism.matches("walk_speed") && mechanism.requireFloat()) {
            getPlayerEntity().setWalkSpeed(mechanism.getValue().asFloat());
        }

        // <--[mechanism]
        // @object PlayerTag
        // @name exhaustion
        // @input ElementTag(Decimal)
        // @description
        // Sets the exhaustion level of a player.
        // @tags
        // <PlayerTag.exhaustion>
        // -->
        if (mechanism.matches("exhaustion") && mechanism.requireFloat()) {
            getPlayerEntity().setExhaustion(mechanism.getValue().asFloat());
        }

        // <--[mechanism]
        // @object PlayerTag
        // @name show_entity
        // @input EntityTag
        // @description
        // Shows the player a previously hidden entity.
        // To show for everyone, use <@link mechanism EntityTag.show_to_players>.
        // See also <@link mechanism PlayerTag.hide_entity>.
        // -->
        if (mechanism.matches("show_entity") && mechanism.requireObject(EntityTag.class)) {
            HideEntitiesHelper.unhideEntity(getPlayerEntity(), mechanism.valueAsType(EntityTag.class).getBukkitEntity());
        }

        // <--[mechanism]
        // @object PlayerTag
        // @name hide_entity
        // @input EntityTag
        // @description
        // Hides an entity from the player.
        // To hide from everyone, use <@link mechanism EntityTag.hide_from_players>.
        // See also <@link mechanism PlayerTag.show_entity>.
        // -->
        if (mechanism.matches("hide_entity")) {
            if (!mechanism.getValue().asString().isEmpty()) {
                ListTag split = mechanism.valueAsType(ListTag.class);
                if (split.size() > 0 && new ElementTag(split.get(0)).matchesType(EntityTag.class)) {
                    EntityTag entity = EntityTag.valueOf(split.get(0), mechanism.context);
                    if (!entity.isSpawnedOrValidForTag()) {
                        Debug.echoError("Can't hide the unspawned entity '" + split.get(0) + "'!");
                    }
                    else {
                        HideEntitiesHelper.hideEntity(getPlayerEntity(), entity.getBukkitEntity());
                    }
                }
                else {
                    Debug.echoError("'" + split.get(0) + "' is not a valid entity!");
                }
            }
            else {
                Debug.echoError("Must specify an entity to hide!");
            }
        }

        // <--[mechanism]
        // @object PlayerTag
        // @name hide_entities
        // @input ElementTag
        // @description
        // Hides a matchable type of entity from the player. Can use any advanced entity matchers per <@link language Advanced Script Event Matching>.
        // To hide a specific entity from the player, use <@link mechanism PlayerTag.hide_entity>.
        // To remove hide sets, use <@link mechanism PlayerTag.unhide_entities>.
        // Note that dynamic matchables like 'entity_flagged' will behave in unexpected ways when dynamically changing.
        // -->
        if (mechanism.matches("hide_entities") && mechanism.hasValue()) {
            HideEntitiesHelper.PlayerHideMap map = HideEntitiesHelper.getPlayerMapFor(getUUID());
            String hideMe = mechanism.getValue().asString();
            map.matchersHidden.add(hideMe);
            if (isOnline()) {
                for (Entity ent : getPlayerEntity().getWorld().getEntities()) {
                    if (ForgeScriptEvent.tryEntity(new EntityTag(ent), hideMe) && map.shouldHide(ent)) {
                        NMSHandler.getEntityHelper().sendHidePacket(getPlayerEntity(), ent);
                    }
                }
            }
        }

        // <--[mechanism]
        // @object PlayerTag
        // @name unhide_entities
        // @input ElementTag
        // @description
        // Removes any entity hides added by <@link mechanism PlayerTag.hide_entities>. Input must exactly match the input given to the hide mechanism.
        // -->
        if (mechanism.matches("unhide_entities") && mechanism.hasValue()) {
            HideEntitiesHelper.PlayerHideMap map = HideEntitiesHelper.getPlayerMapFor(getUUID());
            String unhideMe = mechanism.getValue().asString();
            map.matchersHidden.remove(unhideMe);
            if (map.matchersHidden.isEmpty() && map.entitiesHidden.isEmpty() && map.overridinglyShow.isEmpty()) {
                HideEntitiesHelper.playerHides.remove(getUUID());
            }
            if (isOnline()) {
                for (Entity ent : getPlayerEntity().getWorld().getEntities()) {
                    if (ForgeScriptEvent.tryEntity(new EntityTag(ent), unhideMe) && !map.shouldHide(ent)) {
                        NMSHandler.getEntityHelper().sendShowPacket(getPlayerEntity(), ent);
                    }
                }
            }
        }

        if (mechanism.matches("show_boss_bar")) {
            Deprecations.oldBossBarMech.warn(mechanism.context);
            if (!mechanism.getValue().asString().isEmpty()) {
                String[] split = mechanism.getValue().asString().split("\\|", 2);
                if (split.length == 2 && new ElementTag(split[0]).isDouble()) {
                    BossBarHelper.showSimpleBossBar(getPlayerEntity(), split[1], new ElementTag(split[0]).asDouble() * (1.0 / 200.0));
                }
                else {
                    BossBarHelper.showSimpleBossBar(getPlayerEntity(), split[0], 1.0);
                }
            }
            else {
                BossBarHelper.removeSimpleBossBar(getPlayerEntity());
            }
        }

        // <--[mechanism]
        // @object PlayerTag
        // @name fake_experience
        // @input ElementTag(Decimal)(|ElementTag(Number))
        // @description
        // Shows the player a fake experience bar, with a number between 0.0 and 1.0 to specify how far along the bar is.
        // Use with no input value to reset to the player's normal experience.
        // Optionally, you can specify a fake experience level.
        // For example: - adjust <player> fake_experience:0.5|5
        // -->
        if (mechanism.matches("fake_experience")) {
            if (!mechanism.getValue().asString().isEmpty()) {
                String[] split = mechanism.getValue().asString().split("\\|", 2);
                if (split.length > 0 && new ElementTag(split[0]).isFloat()) {
                    if (split.length > 1 && new ElementTag(split[1]).isInt()) {
                        NMSHandler.getPacketHelper().showExperience(getPlayerEntity(),
                                new ElementTag(split[0]).asFloat(), new ElementTag(split[1]).asInt());
                    }
                    else {
                        NMSHandler.getPacketHelper().showExperience(getPlayerEntity(),
                                new ElementTag(split[0]).asFloat(), getPlayerEntity().getLevel());
                    }
                }
                else {
                    Debug.echoError("'" + split[0] + "' is not a valid decimal number!");
                }
            }
            else {
                NMSHandler.getPacketHelper().resetExperience(getPlayerEntity());
            }
        }

        // <--[mechanism]
        // @object PlayerTag
        // @name fake_health
        // @input ElementTag(Decimal)(|ElementTag(Number)(|ElementTag(Decimal)))
        // @description
        // Shows the player a fake health bar, with a number between 0 and 20, where 1 is half of a heart.
        // Use with no input value to reset to the player's normal health.
        // Optionally, you can specify a fake food level, between 0 and 20.
        // You can also optionally specify a food saturation level between 0 and 10.
        // - adjust <player> fake_health:1
        // - adjust <player> fake_health:10|15
        // - adjust <player> fake_health:<player.health>|3|0
        // -->
        if (mechanism.matches("fake_health")) {
            if (!mechanism.getValue().asString().isEmpty()) {
                String[] split = mechanism.getValue().asString().split("\\|", 3);
                if (split.length > 0 && new ElementTag(split[0]).isFloat()) {
                    if (split.length > 1 && new ElementTag(split[1]).isInt()) {
                        if (split.length > 2 && new ElementTag(split[2]).isFloat()) {
                            NMSHandler.getPacketHelper().showHealth(getPlayerEntity(), new ElementTag(split[0]).asFloat(),
                                    new ElementTag(split[1]).asInt(), new ElementTag(split[2]).asFloat());
                        }
                        else {
                            NMSHandler.getPacketHelper().showHealth(getPlayerEntity(), new ElementTag(split[0]).asFloat(),
                                    new ElementTag(split[1]).asInt(), getPlayerEntity().getSaturation());
                        }
                    }
                    else {
                        NMSHandler.getPacketHelper().showHealth(getPlayerEntity(), new ElementTag(split[0]).asFloat(),
                                getPlayerEntity().getFoodLevel(), getPlayerEntity().getSaturation());
                    }
                }
                else {
                    Debug.echoError("'" + split[0] + "' is not a valid decimal number!");
                }
            }
            else {
                NMSHandler.getPacketHelper().resetHealth(getPlayerEntity());
            }
        }

        // <--[mechanism]
        // @object PlayerTag
        // @name fake_equipment
        // @input EntityTag(|ElementTag|ItemTag)
        // @description
        // Shows the player fake equipment on the specified living entity, which has no real non-visual effects.
        // Input is in the form Entity|Slot|Item, where the slot can be one of the following: HAND, OFF_HAND, BOOTS, LEGS, CHEST, HEAD
        // Optionally, exclude the slot and item to stop showing the fake equipment, if any, on the specified entity.
        // - adjust <player> fake_equipment:<[some_entity]>|chest|diamond_chestplate
        // - adjust <player> fake_equipment:<player>|head|jack_o_lantern
        // Consider instead using <@link command fakeequip>.
        // -->
        if (mechanism.matches("fake_equipment")) {
            if (!mechanism.getValue().asString().isEmpty()) {
                String[] split = mechanism.getValue().asString().split("\\|", 3);
                if (split.length > 0 && new ElementTag(split[0]).matchesType(EntityTag.class)) {
                    String slot = split.length > 1 ? split[1].toUpperCase() : null;
                    if (split.length > 1 && (new ElementTag(slot).matchesEnum(EquipmentSlot.values())
                            || slot.equals("MAIN_HAND") || slot.equals("BOOTS"))) {
                        if (split.length > 2 && new ElementTag(split[2]).matchesType(ItemTag.class)) {
                            if (slot.equals("MAIN_HAND")) {
                                slot = "HAND";
                            }
                            else if (slot.equals("BOOTS")) {
                                slot = "FEET";
                            }
                            NMSHandler.getPacketHelper().showEquipment(getPlayerEntity(),
                                    new ElementTag(split[0]).asType(EntityTag.class, mechanism.context).getLivingEntity(),
                                    EquipmentSlot.valueOf(slot),
                                    new ElementTag(split[2]).asType(ItemTag.class, mechanism.context).getItemStack());
                        }
                        else if (split.length > 2) {
                            Debug.echoError("'" + split[2] + "' is not a valid ItemTag!");
                        }
                    }
                    else if (split.length > 1) {
                        Debug.echoError("'" + split[1] + "' is not a valid slot; must be HAND, OFF_HAND, BOOTS, LEGS, CHEST, or HEAD!");
                    }
                    else {
                        NMSHandler.getPacketHelper().resetEquipment(getPlayerEntity(),
                                new ElementTag(split[0]).asType(EntityTag.class, mechanism.context).getLivingEntity());
                    }
                }
                else {
                    Debug.echoError("'" + split[0] + "' is not a valid EntityTag!");
                }
            }
        }

        // <--[mechanism]
        // @object PlayerTag
        // @name fov_multiplier
        // @input ElementTag(Decimal)
        // @description
        // Sets the player's field of view multiplier.
        // Leave input empty to reset.
        // Note: Values outside a (-1, 1) range will have little effect on the player's fov.
        // -->
        if (mechanism.matches("fov_multiplier")) {
            if (mechanism.hasValue() && mechanism.requireFloat()) {
                NMSHandler.getPacketHelper().setFieldOfView(getPlayerEntity(), mechanism.getValue().asFloat());
            }
            else {
                NMSHandler.getPacketHelper().setFieldOfView(getPlayerEntity(), Float.NaN);
            }
        }

        // <--[mechanism]
        // @object PlayerTag
        // @name item_message
        // @input ElementTag
        // @description
        // Shows the player an item message as if the item they are carrying had changed names to the input message.
        // -->
        if (mechanism.matches("item_message")) {
            ItemChangeMessage.sendMessage(getPlayerEntity(), mechanism.getValue().asString());
        }

        // <--[mechanism]
        // @object PlayerTag
        // @name show_endcredits
        // @input None
        // @description
        // Shows the player the end credits.
        // -->
        if (mechanism.matches("show_endcredits")) {
            NMSHandler.getPlayerHelper().showEndCredits(getPlayerEntity());
        }

        // <--[mechanism]
        // @object PlayerTag
        // @name show_demo
        // @input None
        // @description
        // Shows the player the demo screen.
        // -->
        if (mechanism.matches("show_demo")) {
            NMSHandler.getPacketHelper().showDemoScreen(getPlayerEntity());
        }

        // <--[mechanism]
        // @object PlayerTag
        // @name spectator_target
        // @input EntityTag
        // @description
        // Switches the player to spectator mode and causes them to immediately start spectating an entity.
        // To instead fake this effect, use <@link mechanism PlayerTag.spectate>
        // Give no input to detach the player from any target.
        // @tags
        // <PlayerTag.spectator_target>
        // -->
        if (mechanism.matches("spectator_target")) {
            if (mechanism.hasValue()) {
                getPlayerEntity().setGameMode(GameMode.SPECTATOR);
                getPlayerEntity().setSpectatorTarget(mechanism.valueAsType(EntityTag.class).getBukkitEntity());
            }
            else if (getPlayerEntity().getGameMode() == GameMode.SPECTATOR) {
                getPlayerEntity().setSpectatorTarget(null);
            }
        }

        // <--[mechanism]
        // @object PlayerTag
        // @name spectate
        // @input EntityTag
        // @description
        // Forces the player to spectate from the entity's point of view, using a packet (meaning, the player starts spectating clientside, but not serverside).
        // The player will not move from their existing location serverside.
        // To cause real spectator mode spectating, use <@link mechanism PlayerTag.spectator_target>
        // Note that in some cases you may want to force the player into the spectate gamemode prior to using this mechanism.
        // Note: They cannot cancel the spectating without a re-log -- you must make them spectate themselves to cancel the effect.
        // (i.e. - adjust <player> spectate:<player>)
        // -->
        if (mechanism.matches("spectate") && mechanism.requireObject(EntityTag.class)) {
            NMSHandler.getPacketHelper().forceSpectate(getPlayerEntity(), mechanism.valueAsType(EntityTag.class).getBukkitEntity());
        }

        // <--[mechanism]
        // @object PlayerTag
        // @name open_book
        // @input None
        // @description
        // Forces the player to open the written book in their hand.
        // The book can safely be removed from the player's hand without the player closing the book.
        // -->
        if (mechanism.matches("open_book")) {
            NMSHandler.getPacketHelper().openBook(getPlayerEntity(), EquipmentSlot.HAND);
        }

        // <--[mechanism]
        // @object PlayerTag
        // @name open_offhand_book
        // @input None
        // @description
        // Forces the player to open the written book in their offhand.
        // The book can safely be removed from the player's offhand without the player closing the book.
        // -->
        if (mechanism.matches("open_offhand_book")) {
            NMSHandler.getPacketHelper().openBook(getPlayerEntity(), EquipmentSlot.OFF_HAND);
        }

        // <--[mechanism]
        // @object PlayerTag
        // @name show_book
        // @input ItemTag
        // @description
        // Displays a book to a player. Must be a WRITTEN_BOOK item.
        // For simple usage, consider specifying a book script name as the input.
        // -->
        if (mechanism.matches("show_book")
                && mechanism.requireObject(ItemTag.class)) {
            ItemTag book = mechanism.valueAsType(ItemTag.class);
            if (!(book.getItemMeta() instanceof BookMeta)) {
                Debug.echoError("show_book mechanism must have a book as input.");
                return;
            }
            NMSHandler.getPacketHelper().showEquipment(getPlayerEntity(), getPlayerEntity(),
                    EquipmentSlot.OFF_HAND, book.getItemStack());
            NMSHandler.getPacketHelper().openBook(getPlayerEntity(), EquipmentSlot.OFF_HAND);
            NMSHandler.getPacketHelper().showEquipment(getPlayerEntity(), getPlayerEntity(),
                    EquipmentSlot.OFF_HAND, getPlayerEntity().getEquipment().getItemInOffHand());
        }

        // <--[mechanism]
        // @object PlayerTag
        // @name resend_recipes
        // @input None
        // @description
        // Sends the player a list of the full details of all recipes on the server.
        // This is useful when reloading new item scripts with custom recipes.
        // This will automatically resend discovered recipes at the same time (otherwise the player will seemingly have no recipes unlocked).
        // -->
        if (mechanism.matches("resend_recipes")) {
            NMSHandler.getPlayerHelper().resendRecipeDetails(getPlayerEntity());
            NMSHandler.getPlayerHelper().resendDiscoveredRecipes(getPlayerEntity());
        }

        // <--[mechanism]
        // @object PlayerTag
        // @name resend_discovered_recipes
        // @input None
        // @description
        // Sends the player the full list of recipes they have discovered over again.
        // This is useful when used alongside <@link mechanism PlayerTag.quietly_discover_recipe>.
        // -->
        if (mechanism.matches("resend_discovered_recipes")) {
            NMSHandler.getPlayerHelper().resendDiscoveredRecipes(getPlayerEntity());
        }

        // <--[mechanism]
        // @object PlayerTag
        // @name quietly_discover_recipe
        // @input ListTag
        // @description
        // Causes the player to discover a recipe, or list of recipes, without being notified or updated about this happening.
        // Generally helpful to follow this with <@link mechanism PlayerTag.resend_discovered_recipes>.
        // Input is in the Namespace:Key format, for example "minecraft:gold_nugget".
        // -->
        if (mechanism.matches("quietly_discover_recipe")) {
            for (String keyText : mechanism.valueAsType(ListTag.class)) {
                NamespacedKey key = Utilities.parseNamespacedKey(keyText);
                NMSHandler.getPlayerHelper().quietlyAddRecipe(getPlayerEntity(), key);
            }
        }

        // <--[mechanism]
        // @object PlayerTag
        // @name discover_recipe
        // @input ListTag
        // @description
        // Causes the player to discover a recipe, or list of recipes. Input is in the Namespace:Key format, for example "minecraft:gold_nugget".
        // -->
        if (mechanism.matches("discover_recipe")) {
            List<NamespacedKey> keys = new ArrayList<>();
            for (String key : mechanism.valueAsType(ListTag.class)) {
                keys.add(Utilities.parseNamespacedKey(key));
            }
            getPlayerEntity().discoverRecipes(keys);
        }

        // <--[mechanism]
        // @object PlayerTag
        // @name forget_recipe
        // @input ListTag
        // @description
        // Causes the player to forget ('undiscover') a recipe, or list of recipes. Input is in the Namespace:Key format, for example "minecraft:gold_nugget".
        // -->
        if (mechanism.matches("forget_recipe")) {
            List<NamespacedKey> keys = new ArrayList<>();
            for (String key : mechanism.valueAsType(ListTag.class)) {
                keys.add(Utilities.parseNamespacedKey(key));
            }
            getPlayerEntity().undiscoverRecipes(keys);
        }

        // <--[mechanism]
        // @object PlayerTag
        // @name edit_sign
        // @input LocationTag
        // @description
        // Allows the player to edit an existing sign. To create a sign, see <@link command Sign>.
        // Give no input to make a fake edit interface.
        // -->
        if (mechanism.matches("edit_sign")) {
            if (!NMSHandler.getPacketHelper().showSignEditor(getPlayerEntity(), mechanism.hasValue() ? mechanism.valueAsType(LocationTag.class) : null)) {
                Debug.echoError("Can't edit non-sign materials!");
            }
        }

        // <--[mechanism]
        // @object PlayerTag
        // @name tab_list_info
        // @input ElementTag(|ElementTag)
        // @description
        // Show the player some text in the header and footer area in their tab list.
        // - adjust <player> tab_list_info:<header>|<footer>
        // -->
        if (mechanism.matches("tab_list_info")) {
            if (!mechanism.getValue().asString().isEmpty()) {
                String[] split = mechanism.getValue().asString().split("\\|", 2);
                if (split.length > 0) {
                    String header = split[0];
                    String footer = "";
                    if (split.length > 1) {
                        footer = split[1];
                    }
                    NMSHandler.getPacketHelper().showTabListHeaderFooter(getPlayerEntity(), header, footer);
                }
                else {
                    Debug.echoError("Must specify a header and footer to show!");
                }
            }
            else {
                NMSHandler.getPacketHelper().resetTabListHeaderFooter(getPlayerEntity());
            }
        }

        // <--[mechanism]
        // @object PlayerTag
        // @name sign_update
        // @input LocationTag|ListTag
        // @description
        // Shows the player fake lines on a sign.
        // -->
        if (mechanism.matches("sign_update")) {
            if (!mechanism.getValue().asString().isEmpty()) {
                String[] split = mechanism.getValue().asString().split("\\|", 2);
                if (LocationTag.matches(split[0]) && split.length > 1) {
                    ListTag lines = ListTag.valueOf(split[1], mechanism.context);
                    getPlayerEntity().sendSignChange(LocationTag.valueOf(split[0], mechanism.context), lines.toArray(new String[4]));
                }
                else {
                    Debug.echoError("Must specify a valid location and at least one sign line!");
                }
            }
            else {
                Debug.echoError("Must specify a valid location and at least one sign line!");
            }
        }

        // <--[mechanism]
        // @object PlayerTag
        // @name banner_update
        // @input LocationTag|ListTag
        // @description
        // Shows the player fake patterns on a banner. Input must be in the form: "LOCATION|COLOR/PATTERN|..."
        // As of Minecraft 1.13, the base color is unique material types, and so <@link command showfake> must be used for base color changes.
        // For the list of possible patterns, see <@link url https://hub.spigotmc.org/javadocs/spigot/org/bukkit/block/banner/PatternType.html>.
        // -->
        if (mechanism.matches("banner_update")) {
            if (mechanism.getValue().asString().length() > 0) {
                String[] split = mechanism.getValue().asString().split("\\|");
                List<org.bukkit.block.banner.Pattern> patterns = new ArrayList<>();
                if (LocationTag.matches(split[0]) && split.length > 1) {
                    List<String> splitList;
                    for (int i = 1; i < split.length; i++) {
                        String string = split[i];
                        if (i == 1 && !string.contains("/")) {
                            continue; // Comapt with old input format that had base_color
                        }
                        try {
                            splitList = CoreUtilities.split(string, '/', 2);
                            patterns.add(new org.bukkit.block.banner.Pattern(DyeColor.valueOf(splitList.get(0).toUpperCase()),
                                    PatternType.valueOf(splitList.get(1).toUpperCase())));
                        }
                        catch (Exception e) {
                            Debug.echoError("Could not apply pattern to banner: " + string);
                        }
                    }
                    LocationTag location = LocationTag.valueOf(split[0], mechanism.context);
                    NMSHandler.getPacketHelper().showBannerUpdate(getPlayerEntity(), location, DyeColor.WHITE, patterns);
                }
                else {
                    Debug.echoError("Must specify a valid location and pattern list!");
                }
            }
        }

        // <--[mechanism]
        // @object PlayerTag
        // @name stop_sound
        // @input ElementTag
        // @description
        // Stops all sounds of the specified type for the player.
        // Valid types are AMBIENT, BLOCKS, HOSTILE, MASTER, MUSIC, NEUTRAL, PLAYERS, RECORDS, VOICE, and WEATHER
        // Instead of a type, you can specify a full sound key, which usually has the 'minecraft:' prefix.
        // If no sound type is specified, all types will be stopped.
        // -->
        if (mechanism.matches("stop_sound")) {
            SoundCategory category = null;
            String key = null;
            if (mechanism.hasValue()) {
                try {
                    if (mechanism.getValue().matchesEnum(SoundCategory.values())) {
                        category = SoundCategory.valueOf(mechanism.getValue().asString().toUpperCase());
                    }
                    else {
                        key = mechanism.getValue().asString();
                    }
                }
                catch (Exception e) {
                }
            }
            else {
                category = SoundCategory.MASTER;
            }
            NMSHandler.getPlayerHelper().stopSound(getPlayerEntity(), key, category);
        }

        if (mechanism.matches("action_bar")) {
            Deprecations.playerActionBarMech.warn(mechanism.context);
            getPlayerEntity().spigot().sendMessage(ChatMessageType.ACTION_BAR, FormattedTextHelper.parse(mechanism.getValue().asString(), ChatColor.WHITE));
        }

        // <--[mechanism]
        // @object PlayerTag
        // @name update_advancements
        // @input None
        // @description
        // Updates the player's client-side advancements to match their server data.
        // -->
        if (mechanism.matches("update_advancements")) {
            NMSHandler.getAdvancementHelper().update(getPlayerEntity());
        }

        // <--[mechanism]
        // @object PlayerTag
        // @name name
        // @input ElementTag
        // @description
        // Changes the name on this player's nameplate.
        // @tags
        // <PlayerTag.name>
        // -->
        if (mechanism.matches("name")) {
            String name = mechanism.getValue().asString();
            if (name.length() > 16) {
                Debug.echoError("Must specify a name with no more than 16 characters.");
            }
            else {
                NMSHandler.getInstance().getProfileEditor().setPlayerName(getPlayerEntity(), mechanism.getValue().asString());
            }
        }

        // <--[mechanism]
        // @object PlayerTag
        // @name skin
        // @input ElementTag
        // @description
        // Changes the skin of the player to the skin of the given player name.
        // See also <@link language Player Entity Skins (Skin Blobs)>.
        // -->
        if (mechanism.matches("skin")) {
            String name = mechanism.getValue().asString();
            if (name.length() > 16) {
                Debug.echoError("Must specify a name with no more than 16 characters.");
            }
            else {
                NMSHandler.getInstance().getProfileEditor().setPlayerSkin(getPlayerEntity(), mechanism.getValue().asString());
            }
        }

        // <--[mechanism]
        // @object PlayerTag
        // @name skin_blob
        // @input ElementTag
        // @description
        // Changes the skin of the player to the specified blob.
        // In the format: "texture;signature" (two values separated by a semicolon).
        // See also <@link language Player Entity Skins (Skin Blobs)>.
        // @tags
        // <PlayerTag.skin_blob>
        // -->
        if (mechanism.matches("skin_blob")) {
            NMSHandler.getInstance().getProfileEditor().setPlayerSkinBlob(getPlayerEntity(), mechanism.getValue().asString());
        }

        // <--[mechanism]
        // @object PlayerTag
        // @name is_whitelisted
        // @input ElementTag(Boolean)
        // @description
        // Changes whether the player is whitelisted or not.
        // @tags
        // <PlayerTag.is_whitelisted>
        // -->
        if (mechanism.matches("is_whitelisted") && mechanism.requireBoolean()) {
            getPlayerEntity().setWhitelisted(mechanism.getValue().asBoolean());
        }

        // <--[mechanism]
        // @object PlayerTag
        // @name is_op
        // @input ElementTag(Boolean)
        // @description
        // Changes whether the player is a server operator or not.
        // @tags
        // <PlayerTag.is_op>
        // -->
        if (mechanism.matches("is_op") && mechanism.requireBoolean()) {
            getOfflinePlayer().setOp(mechanism.getValue().asBoolean());
        }



        // <--[mechanism]
        // @object PlayerTag
        // @name hide_particles
        // @input ElementTag
        // @description
        // Hides a certain type of particle from the player.
        // Input is the particle type name - refer to <@link tag server.particle_types>.
        // Give no input to remove all hides from a player.
        // Hides will persist through players reconnecting, but not through servers restarting.
        // -->
        if (mechanism.matches("hide_particles")) {
            if (!mechanism.hasValue()) {
                HideParticles.hidden.remove(getUUID());
            }
            else {
                HashSet<Particle> particles = HideParticles.hidden.computeIfAbsent(getUUID(), k -> new HashSet<>());
                Particle particle = Particle.valueOf(mechanism.getValue().asString().toUpperCase());
                particles.add(particle);
            }
        }

    }
}
