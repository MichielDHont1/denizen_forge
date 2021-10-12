package com.denizenscript.denizen.scripts.triggers.core;

import com.denizenscript.denizen.Denizen;
import com.denizenscript.denizen.scripts.containers.core.InteractScriptContainer;
import com.denizenscript.denizen.scripts.containers.core.InteractScriptHelper;
import com.denizenscript.denizen.npc.traits.TriggerTrait;
import com.denizenscript.denizen.objects.EntityTag;
import com.denizenscript.denizen.objects.ItemTag;
import com.denizenscript.denizen.objects.NPCTag;
import com.denizenscript.denizen.objects.PlayerTag;
import com.denizenscript.denizen.scripts.triggers.AbstractTrigger;
import com.denizenscript.denizen.tags.BukkitTagContext;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.ScriptTag;
import com.denizenscript.denizencore.tags.TagManager;
import net.citizensnpcs.api.CitizensAPI;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.HashMap;
import java.util.Map;

public class DamageTrigger extends AbstractTrigger implements Listener {

    // <--[language]
    // @name Damage Triggers
    // @group NPC Interact Scripts
    // @description
    // Damage Triggers are triggered when when a player left clicks the NPC.
    // Despite the name, these do not actually require the NPC take any damage, only that the player left clicks the NPC.
    //
    // In scripts, use <context.damage> to measure how much damage was done to the NPC
    // (though note that invincible NPCs don't necessarily take any damage even when this is non-zero).
    //
    // These are very basic with no extraneous complexity.
    //
    // -->

    // <--[action]
    // @Actions
    // no damage trigger
    //
    // @Triggers when the NPC is damaged by a player but no damage trigger fires.
    //
    // @Context
    // None
    //
    // -->

    // Technically defined in TriggerTrait, but placing here instead.
    // <--[action]
    // @Actions
    // damage
    //
    // @Triggers when the NPC is damaged by a player.
    //
    // @Context
    // <context.damage> returns how much damage was done.
    //
    // @Determine
    // "cancelled" to cancel the damage event.
    //
    // -->

    // <--[action]
    // @Actions
    // damaged
    //
    // @Triggers when the NPC is damaged by an entity.
    //
    // @Context
    // <context.damage> returns how much damage was done.
    // <context.damager> returns the entity that did the damage.
    //
    // @Determine
    // "cancelled" to cancel the damage event.
    //
    // -->
    @SubscribeEvent
    public void damageTrigger(EntityDamageByEntityEvent event) {
        Map<String, ObjectTag> context = new HashMap<>();
        context.put("damage", new ElementTag(event.getDamage()));
        if (CitizensAPI.getNPCRegistry().isNPC(event.getEntity())) {
            NPCTag npc = new NPCTag(CitizensAPI.getNPCRegistry().getNPC(event.getEntity()));
            if (npc == null) {
                return;
            }
            if (npc.getCitizen() == null) {
                return;
            }
            EntityTag damager = new EntityTag(event.getDamager());
            if (damager.isProjectile() && damager.hasShooter()) {
                damager = damager.getShooter();
            }
            context.put("damager", damager.getDenizenObject());
            String determ = npc.action("damaged", null, context);
            if (determ != null && determ.equalsIgnoreCase("CANCELLED")) {
                event.setCancelled(true);
                return;
            }
            if (!damager.isPlayer()) {
                return;
            }
            PlayerTag dplayer = damager.getDenizenPlayer();
            if (!npc.getCitizen().hasTrait(TriggerTrait.class)) {
                return;
            }
            if (!npc.getTriggerTrait().isEnabled(name)) {
                return;
            }
            TriggerTrait.TriggerContext trigger = npc.getTriggerTrait().trigger(this, dplayer);
            if (!trigger.wasTriggered()) {
                return;
            }
            if (trigger.hasDetermination() && trigger.getDetermination().equalsIgnoreCase("cancelled")) {
                event.setCancelled(true);
                return;
            }
            InteractScriptContainer script = InteractScriptHelper.getInteractScript(npc, dplayer, true, getClass());
            String id = null;
            if (script != null) {
                Map<String, String> idMap = script.getIdMapFor(this.getClass(), dplayer);
                if (!idMap.isEmpty()) {
                    for (Map.Entry<String, String> entry : idMap.entrySet()) {
                        String entry_value = TagManager.tag(entry.getValue(), new BukkitTagContext(dplayer, npc, null, false, new ScriptTag(script)));
                        if (ItemTag.valueOf(entry_value, script).comparesTo(dplayer.getPlayerEntity().getEquipment().getItemInMainHand()) >= 0) {
                            id = entry.getKey();
                        }
                    }
                }
            }
            if (!parse(npc, dplayer, script, id, context)) {
                npc.action("no damage trigger", dplayer);
            }
        }
    }

    @Override
    public void onEnable() {
        Bukkit.getServer().getPluginManager().registerEvents(this, Denizen.getInstance());
    }
}
