package com.denizenscript.denizen.scripts.commands.item;

import com.denizenscript.denizen.scripts.containers.core.ItemScriptHelper;
import com.denizenscript.denizen.utilities.Utilities;
import com.denizenscript.denizen.utilities.debugging.Debug;
import com.denizenscript.denizen.utilities.inventory.SlotHelper;
import com.denizenscript.denizen.objects.InventoryTag;
import com.denizenscript.denizen.objects.ItemTag;
import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.objects.Argument;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.ArgumentHelper;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.denizenscript.denizencore.utilities.Deprecations;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.function.Consumer;

public class GiveCommand extends AbstractCommand {

    public GiveCommand() {
        setName("give");
        setSyntax("give [money/xp/<item>|...] (quantity:<#>) (unlimit_stack_size) (to:<inventory>) (slot:<slot>)");
        setRequiredArguments(1, 5);
        isProcedural = false;
    }

    // <--[command]
    // @Name Give
    // @Syntax give [money/xp/<item>|...] (quantity:<#>) (unlimit_stack_size) (to:<inventory>) (slot:<slot>)
    // @Required 1
    // @Maximum 5
    // @Short Gives the player an item, xp, or money.
    // @Group item
    //
    // @Description
    // Gives the linked player or inventory items, xp, or money.
    // Optionally specify a slot to put the items into. If the slot is already filled, the next available slot will be used.
    // If the player's inventory is full, the items will be dropped on the ground at the inventory's location.
    // Specifying "unlimit_stack_size" will allow an item to stack up to 64. This is useful for stacking items
    // with a max stack size that is less than 64 (for example, most weapon and armor items have a stack size of 1).
    // If an economy is registered, specifying money instead of a item will give money to the player's economy.
    //
    // @Tags
    // <PlayerTag.money>
    //
    // @Usage
    // Use to give money to the player.
    // - give money quantity:10
    //
    // @Usage
    // Use to give XP to the player.
    // - give xp quantity:10
    //
    // @Usage
    // Use to give an item to the player.
    // - give iron_sword
    //
    // @Usage
    // Use to give an item and place it in a specific slot if possible.
    // - give WATCH slot:5
    // -->

    enum Type {ITEM, MONEY, EXP}

    @Override
    public void addCustomTabCompletions(String arg, Consumer<String> addOne) {
        for (Material material : Material.values()) {
            if (material.isItem()) {
                addOne.accept(material.name());
            }
        }
        for (String itemScript : ItemScriptHelper.item_scripts.keySet()) {
            addOne.accept(itemScript);
        }
    }

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {
        for (Argument arg : scriptEntry.getProcessedArgs()) {
            if (!scriptEntry.hasObject("quantity")
                    && arg.matchesPrefix("q", "qty", "quantity")
                    && arg.matchesFloat()) {
                if (arg.matchesPrefix("q", "qty")) {
                    Deprecations.qtyTags.warn(scriptEntry);
                }
                scriptEntry.addObject("quantity", arg.asElement());
                scriptEntry.addObject("set_quantity", new ElementTag(true));
            }
            else if (!scriptEntry.hasObject("type")
                    && arg.matches("money", "coins")) {
                scriptEntry.addObject("type", Type.MONEY);
            }
            else if (!scriptEntry.hasObject("type")
                    && arg.matches("xp", "exp", "experience")) {
                scriptEntry.addObject("type", Type.EXP);
            }
            else if (!scriptEntry.hasObject("unlimit_stack_size")
                    && arg.matches("unlimit_stack_size")) {
                scriptEntry.addObject("unlimit_stack_size", new ElementTag(true));
            }
            else if (!scriptEntry.hasObject("items")
                    && !scriptEntry.hasObject("type")
                    && (arg.matchesArgumentList(ItemTag.class))) {
                scriptEntry.addObject("items", arg.asType(ListTag.class).filter(ItemTag.class, scriptEntry));
            }
            else if (!scriptEntry.hasObject("inventory")
                    && arg.matchesPrefix("t", "to")
                    && arg.matchesArgumentType(InventoryTag.class)) {
                scriptEntry.addObject("inventory", arg.asType(InventoryTag.class));
            }
            else if (!scriptEntry.hasObject("slot")
                    && arg.matchesPrefix("slot")) {
                scriptEntry.addObject("slot", arg.asElement());
            }
            else {
                arg.reportUnhandled();
            }
        }
        scriptEntry.defaultObject("type", Type.ITEM)
                .defaultObject("unlimit_stack_size", new ElementTag(false))
                .defaultObject("quantity", new ElementTag(1))
                .defaultObject("slot", new ElementTag(1));
        Type type = (Type) scriptEntry.getObject("type");
        if (type != Type.MONEY && scriptEntry.getObject("inventory") == null) {
            scriptEntry.addObject("inventory", Utilities.entryHasPlayer(scriptEntry) ? Utilities.getEntryPlayer(scriptEntry).getInventory() : null);
        }
        if (!scriptEntry.hasObject("inventory") && type != Type.MONEY) {
            throw new InvalidArgumentsException("Must specify an inventory to give to!");
        }
        if (type == Type.ITEM && scriptEntry.getObject("items") == null) {
            throw new InvalidArgumentsException("Must specify item/items!");
        }
    }

    @Override
    public void execute(ScriptEntry scriptEntry) {
        ElementTag unlimit_stack_size = scriptEntry.getElement("unlimit_stack_size");
        InventoryTag inventory = scriptEntry.getObjectTag("inventory");
        ElementTag quantity = scriptEntry.getElement("quantity");
        Type type = (Type) scriptEntry.getObject("type");
        ElementTag slot = scriptEntry.getElement("slot");
        Object items_object = scriptEntry.getObject("items");
        List<ItemTag> items = null;
        if (items_object != null) {
            items = (List<ItemTag>) items_object;
        }
        if (scriptEntry.dbCallShouldDebug()) {
            Debug.report(scriptEntry, getName(),
                    ArgumentHelper.debugObj("Type", type.name())
                            + (inventory != null ? inventory.debug() : "")
                            + quantity.debug()
                            + unlimit_stack_size.debug()
                            + (items != null ? ArgumentHelper.debugObj("Items", items) : "")
                            + slot.debug());
        }
        switch (type) {
            case EXP:
                Utilities.getEntryPlayer(scriptEntry).getPlayerEntity().giveExp(quantity.asInt());
                break;
            case ITEM:
                boolean set_quantity = scriptEntry.hasObject("set_quantity");
                boolean limited = !unlimit_stack_size.asBoolean();
                for (ItemTag item : items) {
                    ItemStack is = item.getItemStack();
                    if (is.getType() == Material.AIR) {
                        Debug.echoError("Cannot give air!");
                        continue;
                    }
                    if (set_quantity) {
                        is.setAmount(quantity.asInt());
                    }
                    int slotId = SlotHelper.nameToIndexFor(slot.asString(), inventory.getInventory().getHolder());
                    if (slotId == -1) {
                        Debug.echoError(scriptEntry.getResidingQueue(), "The input '" + slot.asString() + "' is not a valid slot!");
                        return;
                    }
                    List<ItemStack> leftovers = inventory.addWithLeftovers(slotId, limited, is);
                    if (!leftovers.isEmpty()) {
                        Debug.echoDebug(scriptEntry, "The inventory didn't have enough space, the rest of the items have been placed on the floor.");
                        for (ItemStack leftoverItem : leftovers) {
                            inventory.getLocation().getWorld().dropItem(inventory.getLocation(), leftoverItem);
                        }
                    }
                }
                break;
        }
    }
}
