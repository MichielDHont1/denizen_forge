package com.denizenscript.denizen.scripts.triggers.core;

import com.denizenscript.denizen.Denizen;
import com.denizenscript.denizen.scripts.containers.core.InteractScriptContainer;
import com.denizenscript.denizen.scripts.containers.core.InteractScriptHelper;
import com.denizenscript.denizen.utilities.Utilities;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizen.utilities.Settings;
import com.denizenscript.denizen.nms.NMSHandler;
import com.denizenscript.denizen.npc.traits.TriggerTrait;
import com.denizenscript.denizen.objects.NPCTag;
import com.denizenscript.denizen.objects.PlayerTag;
import com.denizenscript.denizen.scripts.triggers.AbstractTrigger;
import com.denizenscript.denizen.tags.BukkitTagContext;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.ArgumentHelper;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.ScriptTag;
import com.denizenscript.denizencore.scripts.commands.queue.DetermineCommand;
import com.denizenscript.denizencore.tags.TagManager;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerChatEvent;

import java.util.*;
import java.util.concurrent.FutureTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("deprecation")
public class ChatTrigger extends AbstractTrigger implements Listener {

    final static Pattern triggerPattern = Pattern.compile("/([^/]*)/");

    // <--[language]
    // @name Chat Triggers
    // @group NPC Interact Scripts
    // @description
    // Chat Triggers are triggered when when a player chats to the NPC (usually while standing close to the NPC and facing the NPC).
    //
    // Interact scripts are allowed to define a list of possible messages a player may type and the scripts triggered in response.
    //
    // Within any given step, the format is then as follows:
    // <code>
    // # Some identifier for the trigger, this only serves to make the sub-triggers unique, and sort them (alphabetically).
    // 1:
    //   # The trigger message written by a player. The text between // must be typed by a player, the other text is filled automatically.
    //   trigger: /keyword/ othertext
    //   script:
    //   # Your code here
    //   - wait 1
    //   # use "<context.message>" for the exact text written by the player.
    //   - chat "<context.message> eh?"
    // # You can list as many as you want
    // 2:
    //   # You can have multi-option triggers, separated by pipes (the "|" symbol). This example matches if player types 'hi', 'hello', OR 'hey'.
    //   trigger: /hi|hello|hey/
    //   script:
    //   - wait 1
    //   # use "<context.keyword>" for the specific word that was said.
    //   # this example will respond to players that said 'hi' with "hi there buddy!", 'hello' with "hello there buddy!", etc.
    //   - chat "<context.keyword> there buddy!"
    // 3:
    //   # You can have regex triggers. This example matches when the player types any numbers.
    //   trigger: /regex:\d+/
    //   script:
    //   - wait 1
    //   # use "<context.keyword>" for the text matched by the regex matcher.
    //   - chat "<context.keyword> eh?"
    // 4:
    //   # Use '*' as the trigger to match anything at all.
    //   trigger: /*/
    //   # Add this line to hide the "[Player -> NPC]: hi" initial trigger message.
    //   hide trigger message: true
    //   # Add this line to show the player chat message in the normal chat.
    //   show as normal chat: true
    //   script:
    //   # If you hide the trigger message but not show as normal chat, you might want to fill that spot with something else.
    //   - narrate "[Player -> NPC]: I don't know how to type the right thing"
    //   - wait 1
    //   - chat "Well type 'keyword' or any number!"
    // </code>
    //
    // -->

    @Override
    public void onEnable() {
        Bukkit.getServer().getPluginManager().registerEvents(this, Denizen.getInstance());
    }

    // Technically defined in TriggerTrait, but placing here instead.
    // <--[action]
    // @Actions
    // chat
    //
    // @Triggers when a player chats to the NPC.
    //
    // @Context
    // <context.message> returns the triggering message
    // <context.keyword> returns the keyword matched by a RegEx trigger
    //
    // @Determine
    // "CANCELLED" to stop the player from chatting.
    // ElementTag to change the message.
    //
    // -->
    public ChatContext process(Player player, String message) {

        // Check if there is an NPC within range of a player to chat to.
        NPCTag npc = Utilities.getClosestNPC_ChatTrigger(player.getLocation(), 25);
        PlayerTag denizenPlayer = PlayerTag.mirrorBukkitPlayer(player);

        if (Debug.verbose) {
            Debug.log("Processing chat trigger: valid npc? " + (npc != null));
        }
        // No NPC? Nothing else to do here.
        if (npc == null) {
            return new ChatContext(false);
        }

        if (Debug.verbose) {
            Debug.log("Has trait?  " + npc.getCitizen().hasTrait(TriggerTrait.class));
        }
        // If the NPC doesn't have triggers, or the triggers are not enabled, then
        // just return false.
        if (!npc.getCitizen().hasTrait(TriggerTrait.class)) {
            return new ChatContext(false);
        }
        if (Debug.verbose) {
            Debug.log("enabled? " + npc.getCitizen().getOrAddTrait(TriggerTrait.class).isEnabled(name));
        }
        if (!npc.getCitizen().getOrAddTrait(TriggerTrait.class).isEnabled(name)) {
            return new ChatContext(false);
        }

        // Check range
        if (npc.getTriggerTrait().getRadius(name) < npc.getLocation().distance(player.getLocation())) {
            if (Debug.verbose) {
                Debug.log("Not in range");
            }
            return new ChatContext(false);
        }

        // The Denizen config can require some other criteria for a successful chat-with-npc.
        // Should we check 'line of sight'? Players cannot talk to NPCs through walls
        // if enabled. Should the Player chat only when looking at the NPC? This may
        // reduce accidental chats with NPCs.

        if (Settings.chatMustSeeNPC()) {
            if (!player.hasLineOfSight(npc.getEntity())) {
                if (Debug.verbose) {
                    Debug.log("no LOS");
                }
                return new ChatContext(false);
            }
        }

        if (Settings.chatMustLookAtNPC()) {
            if (!NMSHandler.getEntityHelper().isFacingEntity(player, npc.getEntity(), 45)) {
                if (Debug.verbose) {
                    Debug.log("Not facing");
                }
                return new ChatContext(false);
            }
        }

        boolean ret = false;

        // Denizen should be good to interact with. Let's get the script.
        InteractScriptContainer script = npc.getInteractScript(denizenPlayer, ChatTrigger.class);

        Map<String, ObjectTag> context = new HashMap<>();
        context.put("message", new ElementTag(message));

        //
        // Fire the Actions!
        //

        // If engaged or not cool, calls On Unavailable, if cool, calls On Chat
        // If available (not engaged, and cool) sets cool down and returns true.
        TriggerTrait.TriggerContext trigger = npc.getTriggerTrait().trigger(ChatTrigger.this, denizenPlayer, context);

        // Return false if determine cancelled
        if (trigger.hasDetermination()) {
            if (trigger.getDetermination().equalsIgnoreCase("cancelled")) {
                if (Debug.verbose) {
                    Debug.log("Cancelled");
                }
                // Mark as handled, the event will cancel.
                return new ChatContext(true);
            }
        }

        // Return false if trigger was unable to fire
        if (!trigger.wasTriggered()) {
            // If the NPC is not interact-able, Settings may allow the chat to filter
            // through. Check the Settings if this is enabled.
            if (Settings.chatGloballyIfUninteractable()) {
                Debug.echoDebug(script, ChatColor.YELLOW + "Resuming. " + ChatColor.WHITE
                        + "The NPC is currently cooling down or engaged.");
                return new ChatContext(false);

            }
            else {
                ret = true;
            }
        }

        // Change the text if it's in the determination
        if (trigger.hasDetermination()) {
            message = trigger.getDetermination();
        }

        if (script == null) {
            if (Debug.verbose) {
                Debug.log("null script");
            }
            return new ChatContext(message, false);
        }

        Debug.report(script, name, ArgumentHelper.debugObj("Player", player.getName())
                + ArgumentHelper.debugObj("NPC", npc.toString())
                + ArgumentHelper.debugObj("Radius(Max)", npc.getLocation().distance(player.getLocation())
                + "(" + npc.getTriggerTrait().getRadius(name) + ")")
                + ArgumentHelper.debugObj("Trigger text", message)
                + ArgumentHelper.debugObj("LOS", String.valueOf(player.hasLineOfSight(npc.getEntity())))
                + ArgumentHelper.debugObj("Facing", String.valueOf(NMSHandler.getEntityHelper().isFacingEntity(player, npc.getEntity(), 45))));

        // Check if the NPC has Chat Triggers for this step.
        String step = InteractScriptHelper.getCurrentStep(denizenPlayer, script.getName());
        if (!script.containsTriggerInStep(step, ChatTrigger.class)) {

            // No chat trigger for this step.. do we chat globally, or to the NPC?
            if (!Settings.chatGloballyIfNoChatTriggers()) {
                Debug.echoDebug(script, player.getName() + " says to "
                        + npc.getNicknameTrait().getNickname() + ", " + message);
                return new ChatContext(false);
            }
            else {
                if (Debug.verbose) {
                    Debug.log("No trigger in step, chatting globally");
                }
                return new ChatContext(message, ret);
            }
        }

        // Parse the script and match Triggers.. if found, cancel the text! The
        // parser will take care of everything else.
        String id = null;
        boolean matched = false;
        String replacementText = null;
        String regexId = null;
        String regexMessage = null;

        String messageLow = CoreUtilities.toLowerCase(message);

        // Use TreeMap to sort chat triggers alphabetically
        TreeMap<String, String> idMap = new TreeMap<>(script.getIdMapFor(ChatTrigger.class, denizenPlayer));

        if (!idMap.isEmpty()) {
            // Iterate through the different id entries in the step's chat trigger
            List<Map.Entry<String, String>> entries = new ArrayList<>(idMap.entrySet());
            entries.sort((o1, o2) -> {
                if (o1 == null || o2 == null) {
                    return 0;
                }
                return o1.getKey().compareToIgnoreCase(o2.getKey());
            });
            for (Map.Entry<String, String> entry : entries) {

                // Check if the chat trigger specified in the specified id's 'trigger:' key
                // matches the text the player has said
                // TODO: script arg?
                String triggerText = TagManager.tag(entry.getValue(), new BukkitTagContext(denizenPlayer, npc, null, false, null));
                Matcher matcher = triggerPattern.matcher(triggerText);
                while (matcher.find()) {
                    // TODO: script arg?
                    String keyword = TagManager.tag(matcher.group().replace("/", ""), new BukkitTagContext(denizenPlayer, npc, null, false, null));
                    String[] split = keyword.split("\\\\\\+REPLACE:", 2);
                    String replace = null;
                    if (split.length == 2) {
                        keyword = split[0];
                        replace = split[1];
                    }
                    String keywordLow = CoreUtilities.toLowerCase(keyword);
                    // Check if the trigger is REGEX, but only if we don't have a REGEX
                    // match already (thus using alphabetical priority for triggers)
                    if (regexId == null && keywordLow.startsWith("regex:")) {
                        Pattern pattern = Pattern.compile(keyword.substring(6));
                        Matcher m = pattern.matcher(message);
                        if (m.find()) {
                            // REGEX matches are left for last, so save it in case non-REGEX
                            // matches don't exist
                            regexId = entry.getKey();
                            regexMessage = triggerText.replace(matcher.group(), m.group());
                            context.put("keyword", new ElementTag(m.group()));
                            if (replace != null) {
                                regexMessage = replace;
                            }
                        }
                    }
                    else if (keyword.contains("|")) {
                        for (String subkeyword : CoreUtilities.split(keyword, '|')) {
                            if (messageLow.contains(CoreUtilities.toLowerCase(subkeyword))) {
                                id = entry.getKey();
                                replacementText = triggerText.replace(matcher.group(), subkeyword);
                                matched = true;
                                context.put("keyword", new ElementTag(subkeyword));
                                if (replace != null) {
                                    replacementText = replace;
                                }
                            }
                        }
                    }
                    else if (keyword.equals("*")) {
                        id = entry.getKey();
                        replacementText = triggerText.replace("/*/", message);
                        matched = true;
                        if (replace != null) {
                            replacementText = replace;
                        }
                    }
                    else if (keywordLow.startsWith("strict:") && messageLow.equals(keywordLow.substring("strict:".length()))) {
                        id = entry.getKey();
                        replacementText = triggerText.replace(matcher.group(), keyword.substring("strict:".length()));
                        matched = true;
                        if (replace != null) {
                            replacementText = replace;
                        }
                    }
                    else if (messageLow.contains(keywordLow)) {
                        id = entry.getKey();
                        replacementText = triggerText.replace(matcher.group(), keyword);
                        matched = true;
                        if (replace != null) {
                            replacementText = replace;
                        }
                    }
                }
                if (matched) {
                    break;
                }
            }
        }

        if (!matched && regexId != null) {
            id = regexId;
            replacementText = regexMessage;
        }

        // If there was a match, the id of the match should have been returned.
        String showNormalChat = script.getString("STEPS." + step + ".CHAT TRIGGER." + id + ".SHOW AS NORMAL CHAT", "false");
        if (id != null) {
            String hideTriggerMessage = script.getString("STEPS." + step + ".CHAT TRIGGER." + id + ".HIDE TRIGGER MESSAGE", "false");
            if (!hideTriggerMessage.equalsIgnoreCase("true")) {
                Utilities.talkToNPC(replacementText, denizenPlayer, npc, Settings.chatToNpcOverhearingRange(), new ScriptTag(script));
            }
            parse(npc, denizenPlayer, script, id, context);
            if (Debug.verbose) {
                Debug.log("chat to NPC");
            }
            return new ChatContext(!showNormalChat.equalsIgnoreCase("true"));
        }
        else {
            if (!Settings.chatGloballyIfFailedChatTriggers()) {
                Utilities.talkToNPC(message, denizenPlayer, npc, Settings.chatToNpcOverhearingRange(), new ScriptTag(script));
                if (Debug.verbose) {
                    Debug.log("Chat globally");
                }
                return new ChatContext(!showNormalChat.equalsIgnoreCase("true"));
            }
            // No matching chat triggers, and the config.yml says we
            // should just ignore the interaction...
        }
        if (Debug.verbose) {
            Debug.log("Finished calculating");
        }
        return new ChatContext(message, ret);
    }

    @SubscribeEvent
    public void asyncChatTrigger(final AsyncPlayerChatEvent event) {
        if (Debug.verbose) {
            Debug.log("Chat trigger seen, cancelled: " + event.isCancelled()
                    + ", chatasync: " + Settings.chatAsynchronous());
        }
        if (event.isCancelled()) {
            return;
        }

        // Return if "Use asynchronous event" is false in config file
        if (!Settings.chatAsynchronous()) {
            return;
        }

        if (!event.isAsynchronous()) {
            syncChatTrigger(new PlayerChatEvent(event.getPlayer(), event.getMessage(), event.getFormat(), event.getRecipients()));
            return;
        }
        FutureTask<ChatContext> futureTask = new FutureTask<>(() -> process(event.getPlayer(), event.getMessage()));

        Bukkit.getScheduler().runTask(Denizen.getInstance(), futureTask);

        try {
            ChatContext context = futureTask.get();
            if (context.wasTriggered()) {
                event.setCancelled(true);
            }
            if (context.hasChanges()) {
                event.setMessage(context.getChanges());
            }
        }
        catch (Exception e) {
            Debug.echoError(e);
        }
    }

    @SubscribeEvent
    public void syncChatTrigger(final PlayerChatEvent event) {
        if (event.isCancelled()) {
            return;
        }

        // Return if "Use asynchronous event" is true in config file
        if (Settings.chatAsynchronous()) {
            return;
        }

        ChatContext chat = process(event.getPlayer(), event.getMessage());

        if (chat.wasTriggered()) {
            event.setCancelled(true);
        }

        if (chat.hasChanges()) {
            event.setMessage(chat.getChanges());
        }
    }

    /**
     * Contains whether the chat trigger successfully 'triggered' and any context that was
     * available while triggering or attempting to trigger.
     */
    public class ChatContext {

        public ChatContext(boolean triggered) {
            this.triggered = triggered;
        }

        public ChatContext(String changed_text, boolean triggered) {
            this.changed_text = changed_text;
            this.triggered = triggered;
        }

        String changed_text;
        boolean triggered;

        public boolean hasChanges() {
            return changed_text != null;
        }

        public String getChanges() {
            return changed_text != null ? changed_text : DetermineCommand.DETERMINE_NONE;
        }

        public boolean wasTriggered() {
            return triggered;
        }

    }
}
