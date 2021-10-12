package com.denizenscript.denizen.scripts.commands;

import com.denizenscript.denizen.nms.NMSHandler;
import com.denizenscript.denizen.nms.NMSVersion;
import com.denizenscript.denizen.scripts.commands.core.*;
import com.denizenscript.denizen.scripts.commands.entity.*;
import com.denizenscript.denizen.scripts.commands.item.*;
import com.denizenscript.denizen.scripts.commands.npc.*;
import com.denizenscript.denizen.scripts.commands.player.*;
import com.denizenscript.denizen.scripts.commands.server.*;
import com.denizenscript.denizen.scripts.commands.world.*;
import com.denizenscript.denizen.utilities.debugging.Debug;
import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.denizenscript.denizencore.scripts.commands.CommandRegistry;

public class BukkitCommandRegistry extends CommandRegistry {

    public static class AutoNoCitizensCommand extends AbstractCommand {

        public static void registerMany(String... names) {
            for (String name : names) {
                registerFor(name);
            }
        }

        public static void registerFor(String name) {
            AutoNoCitizensCommand cmd = new AutoNoCitizensCommand();
            cmd.name = name;
            cmd.syntax = "(Citizens Required)";
        }

        public String name;

        @Override
        public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {
        }

        @Override
        public void execute(ScriptEntry scriptEntry) {
            Debug.echoError("The command '" + name + "' is only available when Citizens is on the server.");
        }
    }

    public void registerCommands() {

        registerCoreCommands();

        //core
        registerCommand(CooldownCommand.class);
        registerCommand(NoteCommand.class);
        registerCommand(ResetCommand.class);
        registerCommand(ZapCommand.class);
        // entity
        registerCommand(AgeCommand.class);
        registerCommand(AttachCommand.class);
        registerCommand(AttackCommand.class);
        registerCommand(BurnCommand.class);
        registerCommand(CastCommand.class);
        registerCommand(EquipCommand.class);
        registerCommand(FakeEquipCommand.class);
        registerCommand(FeedCommand.class);
        registerCommand(FlyCommand.class);
        registerCommand(FollowCommand.class);
        registerCommand(HeadCommand.class);
        registerCommand(HealCommand.class);
        registerCommand(HealthCommand.class);
        registerCommand(HurtCommand.class);
        registerCommand(InvisibleCommand.class);
        registerCommand(LeashCommand.class);
        registerCommand(LookCommand.class);
        registerCommand(MountCommand.class);
        registerCommand(PushCommand.class);
        registerCommand(RemoveCommand.class);
        registerCommand(RenameCommand.class);
        registerCommand(RotateCommand.class);
        registerCommand(ShootCommand.class);
        registerCommand(SneakCommand.class);
        registerCommand(SpawnCommand.class);
        registerCommand(TeleportCommand.class);
        registerCommand(WalkCommand.class);
        // item
        registerCommand(DisplayItemCommand.class);
        registerCommand(FakeItemCommand.class);
        registerCommand(GiveCommand.class);
        registerCommand(InventoryCommand.class);
        registerCommand(MapCommand.class);
        registerCommand(NBTCommand.class);
        registerCommand(ScribeCommand.class);
        registerCommand(TakeCommand.class);
        // player
        registerCommand(ActionBarCommand.class);
        registerCommand(AdvancementCommand.class);
        registerCommand(BlockCrackCommand.class);
        registerCommand(ClickableCommand.class);
        registerCommand(CompassCommand.class);
        if (NMSHandler.getVersion().isAtLeast(NMSVersion.v1_15)) {
            registerCommand(DisguiseCommand.class);
        }
        registerCommand(ExperienceCommand.class);
        if (NMSHandler.getVersion().isAtLeast(NMSVersion.v1_15)) {
            registerCommand(FakeSpawnCommand.class);
        }
        registerCommand(GlowCommand.class);
        registerCommand(ItemCooldownCommand.class);
        registerCommand(KickCommand.class);
        registerCommand(NarrateCommand.class);
        registerCommand(OpenTradesCommand.class);
        registerCommand(OxygenCommand.class);
        registerCommand(ShowFakeCommand.class);
        registerCommand(SidebarCommand.class);
        registerCommand(StatisticCommand.class);
        registerCommand(TeamCommand.class);
        registerCommand(TitleCommand.class);
        registerCommand(ToastCommand.class);
        // server
        registerCommand(AnnounceCommand.class);
        registerCommand(BanCommand.class);
        registerCommand(BossBarCommand.class);
        registerCommand(ExecuteCommand.class);
        registerCommand(ScoreboardCommand.class);
        // world
        registerCommand(AdjustBlockCommand.class);
        registerCommand(AnimateChestCommand.class);
        registerCommand(ChunkLoadCommand.class);
        registerCommand(CopyBlockCommand.class);
        registerCommand(CreateWorldCommand.class);
        registerCommand(DropCommand.class);
        registerCommand(ExplodeCommand.class);
        registerCommand(FireworkCommand.class);
        registerCommand(GameRuleCommand.class);
        registerCommand(LightCommand.class);
        registerCommand(MidiCommand.class);
        registerCommand(ModifyBlockCommand.class);
        registerCommand(PlayEffectCommand.class);
        registerCommand(PlaySoundCommand.class);
        registerCommand(SchematicCommand.class);
        registerCommand(SignCommand.class);
        registerCommand(StrikeCommand.class);
        registerCommand(SwitchCommand.class);
        registerCommand(TimeCommand.class);
        registerCommand(WeatherCommand.class);
        registerCommand(WorldBorderCommand.class);


        Debug.echoApproval("Loaded core commands: " + instances.keySet().toString());
    }
}
