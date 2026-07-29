package com.nore.cobblebash.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.nore.cobblebash.CobbleBash;
import com.nore.cobblebash.Config;
import com.nore.cobblebash.advancement.CobbleBashCriteriaTriggers;
import com.nore.cobblebash.beacon.ChampionBeaconAuras;
import com.nore.cobblebash.beacon.ChampionBeaconPower;
import com.nore.cobblebash.dimension.CobbleBashDimensions;
import com.nore.cobblebash.elitefour.EliteFourMember;
import com.nore.cobblebash.gym.GymLevelSystem;
import com.nore.cobblebash.gym.GymTrainerUnit;
import com.nore.cobblebash.gym.GymType;
import com.nore.cobblebash.instance.GymInstance;
import com.nore.cobblebash.instance.GymInstanceManager;
import com.nore.cobblebash.instance.GymSlotPosition;
import com.nore.cobblebash.integration.CobbleDollarsCompat;
import com.nore.cobblebash.progress.GymProgressManager;
import com.nore.cobblebash.progress.GymRewardData;
import com.nore.cobblebash.progress.GymReturnData;
import com.nore.cobblebash.progress.PlayerGymProgress;
import com.nore.cobblebash.stats.CobbleBashStats;
import com.nore.cobblebash.structure.GymPlatformBuilder;
import com.nore.cobblebash.util.DelayedTaskScheduler;
import com.nore.cobblebash.structure.EliteFourStructure;
import com.nore.cobblebash.structure.GymDoorController;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import com.gitlab.srcmc.rctapi.api.RCTApi;
import com.nore.cobblebash.integration.RctApiProbe;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class GymCommand {
    private static final ResourceKey<LootTable> GYM_CLEAR_REWARD_TABLE = ResourceKey.create(
            Registries.LOOT_TABLE,
            ResourceLocation.fromNamespaceAndPath(CobbleBash.MODID, "rewards/gym_clear")
    );
    private static final Map<String, UUID> DEBUG_SLOT_RESERVATIONS = new HashMap<>();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var cobbleBashRoot = Commands.literal("cobblebash")
                .requires(source -> source.hasPermission(2));
        var gymRoot = Commands.literal("gym");

        var enterNode = Commands.literal("enter");
        for (GymType type : GymType.values()) {
            enterNode.then(
                    Commands.literal(type.getId())
                            .executes(context -> enterGym(context.getSource(), type.getId()))
            );
        }
        enterNode.then(Commands.literal(EliteFourStructure.GYM_TYPE)
                .executes(context -> enterEliteFour(context.getSource(), false))
        );
        gymRoot.then(enterNode);

        var battleNode = Commands.literal("battle");
        var defeatNode = Commands.literal("defeat");
        for (GymType type : GymType.values()) {
            battleNode.then(trainerTarget(type, GymCommand::startTrainerBattle));
            defeatNode.then(trainerTarget(type, GymCommand::defeatTrainer));
        }
        gymRoot.then(battleNode);
        gymRoot.then(defeatNode);

        var completeNode = Commands.literal("complete");
        for (GymType type : GymType.values()) {
            completeNode.then(
                    Commands.literal(type.getId())
                            .executes(context -> completeGym(context.getSource(), type.getId()))
            );
        }
        gymRoot.then(completeNode);

        gymRoot.then(Commands.literal("advance")
                .executes(context -> advanceGym(context.getSource()))
        );

        dispatcher.register(cobbleBashRoot.then(gymRoot));
    }

    private static int enterGym(CommandSourceStack source, String gymType) {
        return enterGym(source.getPlayer(), source, gymType);
    }

    public static boolean enterGym(ServerPlayer player, String gymType) {
        return enterGym(player, null, gymType) > 0;
    }

    public static boolean enterEliteFour(ServerPlayer player, boolean bypassRequirements) {
        return enterEliteFour(player, null, bypassRequirements) > 0;
    }

    private static int enterGym(ServerPlayer player, CommandSourceStack source, String gymType) {
        ServerLevel gymLevel = player.server.getLevel(CobbleBashDimensions.GYM_VOID);

        if (gymLevel == null) {
            sendFailure(player, source, "CobbleBash gym dimension was not found.");
            return 0;
        }

        PlayerGymProgress progress = GymProgressManager.get(player.getUUID());

        clearActiveGym(player, true, true);

        boolean alreadyCompleted = progress.hasCompleted(gymType);
        GameType returnGameMode = player.gameMode.getGameModeForPlayer();

        int[] trainerLevels = GymLevelSystem.getTrainerLevels(progress.getCompletedGymCount());
        GymReturnData.ReturnLocation returnLocation = GymReturnData.ReturnLocation.from(
                (ServerLevel) player.level(),
                player.getX(),
                player.getY(),
                player.getZ(),
                player.getYRot(),
                player.getXRot()
        );

        GymInstance instance = GymInstanceManager.createOrGet(
                player.getUUID(),
                gymType,
                alreadyCompleted,
                trainerLevels,
                returnLocation.dimension(),
                returnLocation.x(),
                returnLocation.y(),
                returnLocation.z(),
                returnLocation.yRot(),
                returnLocation.xRot(),
                returnGameMode
        );
        GymReturnData.get(player.server).put(player.getUUID(), returnLocation);

        progress.setActiveGymType(instance.getGymType());

        String mode = instance.isRepeatClear() ? "REPEAT" : "FIRST CLEAR";
        int[] instanceLevels = instance.getTrainerLevels();
        BlockPos origin = GymSlotPosition.getOriginForSlot(instance.getSlotId());
        BlockPos playerSpawn = GymPlatformBuilder.getPlayerSpawn(origin, instance.getGymType());

        GymPlatformBuilder.buildGym(gymLevel, origin, instance.getGymType(), instance.getSlotId(), instanceLevels);

        player.teleportTo(
                gymLevel,
                playerSpawn.getX() + 0.5,
                playerSpawn.getY(),
                playerSpawn.getZ() + 0.5,
                GymPlatformBuilder.getPlayerSpawnYaw(instance.getGymType(), player.getYRot()),
                GymPlatformBuilder.getPlayerSpawnPitch(instance.getGymType(), player.getXRot())
        );
        player.setGameMode(GameType.ADVENTURE);

        sendSuccess(
                player,
                source,
                "Entering " + instance.getGymType()
                        + " gym [" + mode + "]. Slot = "
                        + instance.getSlotId()
                        + ". Origin = "
                        + formatPos(origin)
                        + ". Trainer levels: {"
                        + instanceLevels[0] + ", "
                        + instanceLevels[1] + ", "
                        + instanceLevels[2] + "}"
        );

        CobbleBashCriteriaTriggers.triggerGymEntered(player);
        return 1;
    }

    private static int enterEliteFour(CommandSourceStack source, boolean bypassRequirements) {
        return enterEliteFour(source.getPlayer(), source, bypassRequirements);
    }

    private static int enterEliteFour(ServerPlayer player, CommandSourceStack source, boolean bypassRequirements) {
        ServerLevel gymLevel = player.server.getLevel(CobbleBashDimensions.GYM_VOID);

        if (gymLevel == null) {
            sendFailure(player, source, "CobbleBash gym dimension was not found.");
            return 0;
        }

        PlayerGymProgress progress = GymProgressManager.get(player.getUUID());
        if (!bypassRequirements && !hasCompletedAllElementalGyms(progress)) {
            sendFailure(player, source, "The Elite Four requires all 18 type gyms to be completed first.");
            return 0;
        }

        clearActiveGym(player, true, true);

        GameType returnGameMode = player.gameMode.getGameModeForPlayer();
        GymReturnData.ReturnLocation returnLocation = GymReturnData.ReturnLocation.from(
                (ServerLevel) player.level(),
                player.getX(),
                player.getY(),
                player.getZ(),
                player.getYRot(),
                player.getXRot()
        );

        GymInstance instance = GymInstanceManager.createOrGet(
                player.getUUID(),
                EliteFourStructure.GYM_TYPE,
                false,
                new int[]{0, 0, 0},
                returnLocation.dimension(),
                returnLocation.x(),
                returnLocation.y(),
                returnLocation.z(),
                returnLocation.yRot(),
                returnLocation.xRot(),
                returnGameMode
        );
        GymReturnData.get(player.server).put(player.getUUID(), returnLocation);
        progress.setActiveGymType(EliteFourStructure.GYM_TYPE);

        BlockPos origin = GymSlotPosition.getOriginForSlot(instance.getSlotId());
        EliteFourStructure.build(gymLevel, origin, instance.getSlotId());
        BlockPos playerSpawn = EliteFourStructure.getPlayerSpawn(gymLevel, origin);

        player.teleportTo(
                gymLevel,
                playerSpawn.getX() + 0.5D,
                playerSpawn.getY(),
                playerSpawn.getZ() + 0.5D,
                180.0F,
                0.0F
        );
        player.setGameMode(GameType.ADVENTURE);

        sendSuccess(
                player,
                source,
                "Entering Elite Four. Slot = "
                        + instance.getSlotId()
                        + ". Origin = "
                        + formatPos(origin)
                        + "."
        );

        return 1;
    }

    private static int completeGym(CommandSourceStack source, String gymType) {
        return completeGym(source.getPlayer(), gymType, source);
    }

    private static int completeGym(ServerPlayer player, String gymType, CommandSourceStack source) {

        PlayerGymProgress progress = GymProgressManager.get(player.getUUID());

        if (!progress.isActiveGym(gymType)) {
            sendFailure(player, source, "Cannot complete " + gymType + " gym because your active gym is " + progress.getActiveGymType() + ".");
            return 0;
        }

        boolean alreadyCompleted = progress.hasCompleted(gymType);
        progress.completeGym(gymType);
        CobbleBashStats.syncGymsCompleted(player);
        awardTrainerRibbonIfEligible(player, gymType);
        awardEliteFourDiskIfEligible(player, progress);
        awardGymClearLoot(player);

        GymInstance clearedInstance = GymInstanceManager.clear(player.getUUID());
        clearInstancePlatform(player, clearedInstance);
        teleportToReturnLocation(player, clearedInstance);
        GymReturnData.get(player.server).remove(player.getUUID());

        String rewardMode = alreadyCompleted ? "repeat rewards" : "first clear rewards + badge";
        String slotText = clearedInstance == null ? "none" : String.valueOf(clearedInstance.getSlotId());

        sendSuccess(
                player,
                source,
                "Completed " + gymType
                        + " gym. Reward mode: " + rewardMode
                        + ". Freed slot: " + slotText + "."
        );

        return 1;
    }

    private static void awardTrainerRibbonIfEligible(ServerPlayer player, String completedGymType) {
        GymRewardData rewards = GymRewardData.get(player.server);
        String ribbonGymType = rewards.getOrSetTrainerRibbonGym(player.getUUID(), completedGymType);

        if (!ribbonGymType.equals(completedGymType)) {
            return;
        }

        ItemStack ribbon = new ItemStack(CobbleBash.TRAINER_RIBBON.get());
        boolean added = player.getInventory().add(ribbon);
        if (!added && !ribbon.isEmpty()) {
            player.drop(ribbon, false);
        }

        player.sendSystemMessage(Component.literal("Received a Trainer Ribbon from the " + completedGymType + " Gym Leader."));
    }

    private static void awardEliteFourDiskIfEligible(ServerPlayer player, PlayerGymProgress progress) {
        if (!hasCompletedAllElementalGyms(progress)) {
            return;
        }

        GymRewardData rewards = GymRewardData.get(player.server);
        if (!rewards.markEliteFourDiskAwarded(player.getUUID())) {
            return;
        }

        giveOrDrop(player, new ItemStack(CobbleBash.ELITE_FOUR_TRAINING_DISK.get()));
        player.sendSystemMessage(Component.literal("Received an Elite Four Training Disk for conquering all 18 gyms."));
    }

    private static void awardGymClearLoot(ServerPlayer player) {
        LootTable lootTable = player.server.reloadableRegistries().getLootTable(GYM_CLEAR_REWARD_TABLE);
        LootParams lootParams = new LootParams.Builder(player.serverLevel())
                .withLuck(player.getLuck())
                .create(LootContextParamSets.EMPTY);

        lootTable.getRandomItems(lootParams, player.getRandom()).forEach(stack -> giveOrDrop(player, stack));
    }

    private static void giveOrDrop(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }

        boolean added = player.getInventory().add(stack);
        if (!added && !stack.isEmpty()) {
            player.drop(stack, false);
        }
    }

    private static int exitGym(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();

        GymInstance clearedInstance = GymInstanceManager.clear(player.getUUID());

        PlayerGymProgress progress = GymProgressManager.get(player.getUUID());
        progress.setActiveGymType("none");

        if (clearedInstance == null) {
            source.sendFailure(Component.literal("You do not have an active gym instance."));
            return 0;
        }

        clearInstancePlatform(player, clearedInstance);
        teleportToReturnLocation(player, clearedInstance);
        GymReturnData.get(player.server).remove(player.getUUID());

        source.sendSuccess(
                () -> Component.literal(
                        "Exited " + clearedInstance.getGymType()
                                + " gym. Freed slot: "
                                + clearedInstance.getSlotId()
                                + "."
                ),
                false
        );

        return 1;
    }

    private static int leaveGym(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();

        GymInstance clearedInstance = GymInstanceManager.clear(player.getUUID());

        PlayerGymProgress progress = GymProgressManager.get(player.getUUID());
        progress.setActiveGymType("none");

        clearInstancePlatform(player, clearedInstance);
        teleportToReturnLocation(player, clearedInstance);
        GymReturnData.get(player.server).remove(player.getUUID());

        source.sendSuccess(
                () -> Component.literal("Returned to gym entry point."),
                false
        );

        return 1;
    }

    private static int advanceGym(CommandSourceStack source) {
        return advanceGym(source.getPlayer(), source);
    }

    private static int advanceGym(ServerPlayer player, CommandSourceStack source) {
        GymInstance instance = GymInstanceManager.getActive(player.getUUID());

        if (instance == null) {
            sendFailure(player, source, "You do not have an active gym instance.");
            return 0;
        }

        if (EliteFourStructure.GYM_TYPE.equals(instance.getGymType())) {
            return advanceEliteFour(player, source, instance);
        }

        boolean advanced = instance.advanceTrainerStage();

        if (!advanced) {
            sendFailure(player, source, "Gym instance is already cleared.");
            return 0;
        }

        sendSuccess(
                player,
                source,
                "Advanced " + instance.getGymType()
                        + " gym to stage "
                        + instance.getTrainerStage()
                        + "."
        );

        ServerLevel gymLevel = player.server.getLevel(CobbleBashDimensions.GYM_VOID);
        if (gymLevel != null) {
            BlockPos origin = GymSlotPosition.getOriginForSlot(instance.getSlotId());
            GymDoorController.openDoorForStage(gymLevel, origin, instance.getGymType(), instance.getTrainerStage());
        }

        if (instance.getTrainerStage() == 3) {
            CobbleBashCriteriaTriggers.triggerGymBossDefeated(player, instance.getGymType());
            sendSuccess(player, source, "Boss defeated. Leaving gym in 5 seconds...");

            DelayedTaskScheduler.schedule(100, () -> {
                if (source != null) {
                    completeGym(source, instance.getGymType());
                } else {
                    completeGym(player, instance.getGymType(), null);
                }
            });
        }

        return 1;
    }

    private static int advanceEliteFour(ServerPlayer player, CommandSourceStack source, GymInstance instance) {
        if (!instance.hasActiveEliteFourMember()) {
            sendFailure(player, source, "Choose an Elite Four plaque first.");
            return 0;
        }

        EliteFourMember completedMember = EliteFourMember.fromId(instance.getActiveEliteFourMember());
        if (completedMember == null || !instance.completeActiveEliteFourMember()) {
            sendFailure(player, source, "Could not complete the active Elite Four member.");
            return 0;
        }

        sendSuccess(player, source, "Defeated " + completedMember.getDisplayName() + ".");

        if (instance.getDefeatedEliteFourMemberCount() >= EliteFourMember.ordered().size()) {
            instance.unlockEliteFourChampion();
            instance.setEliteFourChampionBeamTicks(-1);
            ServerLevel gymLevel = player.server.getLevel(CobbleBashDimensions.GYM_VOID);
            if (gymLevel != null) {
                BlockPos origin = GymSlotPosition.getOriginForSlot(instance.getSlotId());
                EliteFourStructure.openChampionGate(gymLevel, origin);
            }
            sendSuccess(player, source, "All Elite Four members defeated. Champion gate opened.");
            return 1;
        }

        List<EliteFourMember> remainingMembers = new ArrayList<>();
        for (EliteFourMember member : EliteFourMember.ordered()) {
            if (!instance.hasDefeatedEliteFourMember(member.getId())) {
                remainingMembers.add(member);
            }
        }

        EliteFourMember nextMember = remainingMembers.get(player.getRandom().nextInt(remainingMembers.size()));
        instance.selectEliteFourMember(nextMember.getId());

        ServerLevel gymLevel = player.server.getLevel(CobbleBashDimensions.GYM_VOID);
        if (gymLevel != null) {
            BlockPos origin = GymSlotPosition.getOriginForSlot(instance.getSlotId());
            EliteFourStructure.openMemberGate(gymLevel, origin, nextMember);
        }

        sendSuccess(player, source, "Next Elite Four member unlocked: " + nextMember.getDisplayName() + ".");
        return 1;
    }

    private static int registerRct(CommandSourceStack source) {
        RctApiProbe.registerTestTrainer(source.getServer());

        source.sendSuccess(
                () -> Component.literal("Registered RCT trainer: cobblebash_bug_trainer_1"),
                false
        );

        return 1;
    }

    private static int getRct(CommandSourceStack source) {
        var trainerNpc = RctApiProbe.getTestTrainer();

        if (trainerNpc == null) {
            source.sendFailure(Component.literal("Trainer not found."));
            return 0;
        }

        source.sendSuccess(
                () -> Component.literal(
                        "Found trainer: "
                                + trainerNpc.getName()
                                + ", team size = "
                                + trainerNpc.getTeam().length
                ),
                false
        );

        return 1;
    }

    private static int debugRct(CommandSourceStack source) {
        StringBuilder builder = new StringBuilder("RCT API instances: ");

        RCTApi.getInstances().forEach(entry ->
                builder.append("[")
                        .append(entry.getKey())
                        .append("] ")
        );

        source.sendSuccess(
                () -> Component.literal(builder.toString()),
                false
        );

        return 1;
    }

    private static int startRctBattle(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();

        boolean started = RctApiProbe.startTestBattle(player);

        if (!started) {
            source.sendFailure(Component.literal("Failed to start RCT battle."));
            return 0;
        }

        source.sendSuccess(
                () -> Component.literal("Started RCT test battle."),
                false
        );

        return 1;
    }

    private static int reserveDebugSlot(CommandSourceStack source, String label) {
        if (DEBUG_SLOT_RESERVATIONS.containsKey(label)) {
            UUID existingId = DEBUG_SLOT_RESERVATIONS.get(label);
            GymInstance existing = GymInstanceManager.getActive(existingId);
            if (existing != null) {
                source.sendFailure(Component.literal("Debug slot label '" + label + "' already reserves slot " + existing.getSlotId() + "."));
                return 0;
            }
        }

        ServerPlayer player = source.getPlayer();
        UUID reservationId = UUID.nameUUIDFromBytes(("cobblebash:slot_debug:" + label).getBytes(StandardCharsets.UTF_8));
        GymReturnData.ReturnLocation returnLocation = GymReturnData.ReturnLocation.from(
                (ServerLevel) player.level(),
                player.getX(),
                player.getY(),
                player.getZ(),
                player.getYRot(),
                player.getXRot()
        );

        GymInstance instance = GymInstanceManager.createOrGet(
                reservationId,
                "slot_debug",
                true,
                new int[]{0, 0, 0},
                returnLocation.dimension(),
                returnLocation.x(),
                returnLocation.y(),
                returnLocation.z(),
                returnLocation.yRot(),
                returnLocation.xRot(),
                player.gameMode.getGameModeForPlayer()
        );
        DEBUG_SLOT_RESERVATIONS.put(label, reservationId);

        source.sendSuccess(
                () -> Component.literal("Reserved debug slot " + instance.getSlotId() + " as '" + label + "'."),
                false
        );
        return 1;
    }

    private static int releaseDebugSlot(CommandSourceStack source, String label) {
        UUID reservationId = DEBUG_SLOT_RESERVATIONS.remove(label);
        if (reservationId == null) {
            source.sendFailure(Component.literal("No debug slot reservation exists for '" + label + "'."));
            return 0;
        }

        GymInstance cleared = GymInstanceManager.clear(reservationId);
        if (cleared == null) {
            source.sendFailure(Component.literal("Debug slot label '" + label + "' was tracked, but no active slot was reserved."));
            return 0;
        }

        source.sendSuccess(
                () -> Component.literal("Released debug slot " + cleared.getSlotId() + " from '" + label + "'."),
                false
        );
        return 1;
    }

    private static int debugSlotStatus(CommandSourceStack source) {
        StringBuilder reservations = new StringBuilder();
        DEBUG_SLOT_RESERVATIONS.forEach((label, playerId) -> {
            GymInstance instance = GymInstanceManager.getActive(playerId);
            if (instance != null) {
                if (!reservations.isEmpty()) {
                    reservations.append(", ");
                }
                reservations.append(label).append("=").append(instance.getSlotId());
            }
        });

        source.sendSuccess(
                () -> Component.literal(
                        "Slot debug: active instances = "
                                + GymInstanceManager.getActiveCount()
                                + ", free slots = "
                                + GymInstanceManager.getFreeSlotCount()
                                + ", next slot id = "
                                + GymInstanceManager.getNextSlotId()
                                + ", reservations = "
                                + (reservations.isEmpty() ? "none" : reservations)
                ),
                false
        );
        return 1;
    }

    private static int startTrainerBattle(CommandSourceStack source, GymType gymType, GymTrainerUnit unit) {
        return startTrainerBattle(source.getPlayer(), source, gymType.getId(), null, unit);
    }

    public static boolean startTrainerBattle(ServerPlayer player, String gymType, int slotId, GymTrainerUnit unit) {
        return startTrainerBattle(player, null, gymType, slotId, unit) > 0;
    }

    private static int startTrainerBattle(ServerPlayer player, CommandSourceStack source, String gymType, Integer requiredSlotId, GymTrainerUnit unit) {
        GymInstance instance = GymInstanceManager.getActive(player.getUUID());

        if (instance == null) {
            sendFailure(player, source, "You do not have an active gym instance.");
            return 0;
        }

        if (EliteFourStructure.GYM_TYPE.equals(instance.getGymType())) {
            return startEliteFourTrainerBattle(player, source, gymType, requiredSlotId, unit, instance);
        }

        if (!instance.getGymType().equals(gymType)) {
            sendFailure(player, source, "Your active gym is " + instance.getGymType() + ", not " + gymType + ".");
            return 0;
        }

        if (requiredSlotId != null && instance.getSlotId() != requiredSlotId) {
            sendFailure(player, source, "That trainer belongs to slot " + requiredSlotId + ", but your active slot is " + instance.getSlotId() + ".");
            return 0;
        }

        if (instance.getTrainerStage() != unit.getRequiredStage()) {
            GymTrainerUnit expectedUnit = getExpectedTrainerUnit(instance.getTrainerStage());
            String targetName = getTrainerDisplayName(player, gymType, unit);
            String expectedName = expectedUnit == null
                    ? "the previous trainer"
                    : getTrainerDisplayName(player, gymType, expectedUnit);
            sendFailure(
                    player,
                    source,
                    "Cannot battle " + targetName + ". Beat " + expectedName + " first."
            );
            return 0;
        }

        int level = instance.getTrainerLevels()[unit.getLevelIndex()];
        ServerLevel gymLevel = player.server.getLevel(CobbleBashDimensions.GYM_VOID);
        if (gymLevel == null) {
            sendFailure(player, source, "CobbleBash gym dimension was not found.");
            return 0;
        }

        BlockPos origin = GymSlotPosition.getOriginForSlot(instance.getSlotId());
        if (RctApiProbe.getGymTrainer(gymType, instance.getSlotId(), unit.getTrainerIdPart()) == null
                && !RctApiProbe.registerGymTrainer(player.server, gymType, instance.getSlotId(), unit.getTrainerIdPart(), level)) {
            sendFailure(player, source, "Failed to register " + gymType + " " + unit.getDisplayName() + " trainer.");
            return 0;
        }

        GymPlatformBuilder.attachTrainerEntity(gymLevel, origin, gymType, instance.getSlotId(), unit.getTrainerIdPart());

        boolean started = RctApiProbe.startGymBattle(player, gymType, instance.getSlotId(), unit.getTrainerIdPart());

        if (!started) {
            sendFailure(player, source, "Failed to start " + gymType + " " + unit.getDisplayName() + " battle.");
            return 0;
        }

        sendSuccess(player, source, "Started " + gymType + " " + unit.getDisplayName() + " battle.");

        return 1;
    }

    private static int startEliteFourTrainerBattle(
            ServerPlayer player,
            CommandSourceStack source,
            String gymType,
            Integer requiredSlotId,
            GymTrainerUnit unit,
            GymInstance instance
    ) {
        EliteFourMember member = EliteFourMember.fromTrainerGymType(gymType);
        if (EliteFourStructure.CHAMPION_TRAINER_GYM_TYPE.equals(gymType)) {
            return startEliteFourChampionBattle(player, source, requiredSlotId, unit, instance);
        }

        if (member == null || unit != GymTrainerUnit.BOSS) {
            sendFailure(player, source, "That trainer is not part of the active Elite Four challenge.");
            return 0;
        }

        if (requiredSlotId != null && instance.getSlotId() != requiredSlotId) {
            sendFailure(player, source, "That trainer belongs to slot " + requiredSlotId + ", but your active slot is " + instance.getSlotId() + ".");
            return 0;
        }

        if (!instance.getActiveEliteFourMember().equals(member.getId())) {
            sendFailure(player, source, "Complete previous Elite Four member.");
            return 0;
        }

        int level = member == EliteFourMember.FIRE_FAIRY || member == EliteFourMember.WATER_STEEL ? 98 : 95;
        ServerLevel gymLevel = player.server.getLevel(CobbleBashDimensions.GYM_VOID);
        if (gymLevel == null) {
            sendFailure(player, source, "CobbleBash gym dimension was not found.");
            return 0;
        }

        BlockPos origin = GymSlotPosition.getOriginForSlot(instance.getSlotId());
        if (RctApiProbe.getGymTrainer(gymType, instance.getSlotId(), unit.getTrainerIdPart()) == null
                && !RctApiProbe.registerGymTrainer(player.server, gymType, instance.getSlotId(), unit.getTrainerIdPart(), level)) {
            sendFailure(player, source, "Failed to register " + member.getDisplayName() + " Elite Four trainer.");
            return 0;
        }

        GymPlatformBuilder.attachTrainerEntity(gymLevel, origin, gymType, instance.getSlotId(), unit.getTrainerIdPart());

        boolean started = RctApiProbe.startGymBattle(player, gymType, instance.getSlotId(), unit.getTrainerIdPart());
        if (!started) {
            sendFailure(player, source, "Failed to start " + member.getDisplayName() + " Elite Four battle.");
            return 0;
        }

        sendSuccess(player, source, "Started " + member.getDisplayName() + " Elite Four battle.");
        return 1;
    }

    private static int startEliteFourChampionBattle(
            ServerPlayer player,
            CommandSourceStack source,
            Integer requiredSlotId,
            GymTrainerUnit unit,
            GymInstance instance
    ) {
        if (unit != GymTrainerUnit.BOSS) {
            sendFailure(player, source, "That trainer is not the Elite Four Champion.");
            return 0;
        }

        if (requiredSlotId != null && instance.getSlotId() != requiredSlotId) {
            sendFailure(player, source, "That trainer belongs to slot " + requiredSlotId + ", but your active slot is " + instance.getSlotId() + ".");
            return 0;
        }

        if (!instance.isEliteFourChampionUnlocked()) {
            sendFailure(player, source, "Defeat all four Elite Four members first.");
            return 0;
        }

        ServerLevel gymLevel = player.server.getLevel(CobbleBashDimensions.GYM_VOID);
        if (gymLevel == null) {
            sendFailure(player, source, "CobbleBash gym dimension was not found.");
            return 0;
        }

        BlockPos origin = GymSlotPosition.getOriginForSlot(instance.getSlotId());
        if (RctApiProbe.getGymTrainer(EliteFourStructure.CHAMPION_TRAINER_GYM_TYPE, instance.getSlotId(), unit.getTrainerIdPart()) == null
                && !RctApiProbe.registerGymTrainer(player.server, EliteFourStructure.CHAMPION_TRAINER_GYM_TYPE, instance.getSlotId(), unit.getTrainerIdPart(), 100)) {
            sendFailure(player, source, "Failed to register Elite Four Champion trainer.");
            return 0;
        }

        GymPlatformBuilder.attachTrainerEntity(gymLevel, origin, EliteFourStructure.CHAMPION_TRAINER_GYM_TYPE, instance.getSlotId(), unit.getTrainerIdPart());

        boolean started = RctApiProbe.startGymBattle(player, EliteFourStructure.CHAMPION_TRAINER_GYM_TYPE, instance.getSlotId(), unit.getTrainerIdPart());
        if (!started) {
            sendFailure(player, source, "Failed to start Elite Four Champion battle.");
            return 0;
        }

        sendSuccess(player, source, "Started Elite Four Champion battle.");
        return 1;
    }

    private static int defeatTrainer(CommandSourceStack source, GymType gymType, GymTrainerUnit unit) {
        ServerPlayer player = source.getPlayer();
        GymInstance instance = GymInstanceManager.getActive(player.getUUID());

        if (instance == null) {
            source.sendFailure(Component.literal("You do not have an active gym instance."));
            return 0;
        }

        if (!instance.getGymType().equals(gymType.getId())) {
            source.sendFailure(Component.literal("Your active gym is " + instance.getGymType() + ", not " + gymType.getId() + "."));
            return 0;
        }

        if (instance.getTrainerStage() != unit.getRequiredStage()) {
            source.sendFailure(Component.literal(
                    "Cannot defeat " + unit.getDisplayName()
                            + " at stage " + instance.getTrainerStage()
                            + ". Expected " + getExpectedTrainerName(instance.getTrainerStage()) + "."
            ));
            return 0;
        }

        return advanceGym(source);
    }

    public static void handleTrainerVictory(ServerPlayer player, String gymType, int slotId, GymTrainerUnit unit) {
        GymInstance instance = GymInstanceManager.getActive(player.getUUID());

        if (instance != null && EliteFourStructure.GYM_TYPE.equals(instance.getGymType())) {
            handleEliteFourTrainerVictory(player, gymType, slotId, unit, instance);
            return;
        }

        if (instance == null
                || !instance.getGymType().equals(gymType)
                || instance.getSlotId() != slotId
                || instance.getTrainerStage() != unit.getRequiredStage()) {
            return;
        }

        awardCobbleDollarsForTrainerVictory(player, instance, unit);
        advanceGym(player, null);
    }

    private static void handleEliteFourTrainerVictory(ServerPlayer player, String gymType, int slotId, GymTrainerUnit unit, GymInstance instance) {
        if (EliteFourStructure.CHAMPION_TRAINER_GYM_TYPE.equals(gymType)
                && unit == GymTrainerUnit.BOSS
                && instance.getSlotId() == slotId
                && instance.isEliteFourChampionUnlocked()) {
            giveOrDrop(player, new ItemStack(CobbleBash.CHAMPION_UPGRADE_SMITHING_TEMPLATE.get()));
            player.sendSystemMessage(Component.literal("Received a Champion Upgrade Smithing Template."));
            player.sendSystemMessage(Component.literal("Elite Four Champion defeated. Exit flow is ready for the next implementation pass."));
            return;
        }

        EliteFourMember member = EliteFourMember.fromTrainerGymType(gymType);
        if (member == null
                || unit != GymTrainerUnit.BOSS
                || instance.getSlotId() != slotId
                || !instance.getActiveEliteFourMember().equals(member.getId())) {
            return;
        }

        playEliteFourVictoryAdvance(player);
    }

    private static void playEliteFourVictoryAdvance(ServerPlayer player) {
        advanceGym(player, null);
    }

    private static void awardCobbleDollarsForTrainerVictory(ServerPlayer player, GymInstance instance, GymTrainerUnit unit) {
        if (!instance.isRepeatClear()) {
            return;
        }

        int reward = unit == GymTrainerUnit.BOSS
                ? Config.COBBLE_DOLLARS_REPEAT_BOSS_REWARD.get()
                : Config.COBBLE_DOLLARS_REPEAT_TRAINER_REWARD.get();

        if (CobbleDollarsCompat.award(player, reward)) {
            player.sendSystemMessage(Component.translatable("message.cobblebash.cobble_dollars_reward", reward));
        }
    }

    public static void clearActiveGym(ServerPlayer player, boolean teleport) {
        clearActiveGym(player, teleport, teleport);
    }

    public static void clearActiveGym(ServerPlayer player, boolean teleport, boolean consumeSavedReturn) {
        GymInstance clearedInstance = GymInstanceManager.clear(player.getUUID());

        PlayerGymProgress progress = GymProgressManager.get(player.getUUID());
        progress.setActiveGymType("none");

        clearInstancePlatform(player, clearedInstance);

        if (teleport && player.level().dimension().equals(CobbleBashDimensions.GYM_VOID)) {
            teleportToReturnLocation(player, clearedInstance);
        } else if (clearedInstance != null) {
            restoreReturnGameMode(player, clearedInstance);
        }

        if (consumeSavedReturn) {
            GymReturnData.get(player.server).remove(player.getUUID());
        }
    }

    private static int debugProgress(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();

        PlayerGymProgress progress = GymProgressManager.get(player.getUUID());
        GymInstance instance = GymInstanceManager.getActive(player.getUUID());

        int[] trainerLevels = GymLevelSystem.getTrainerLevels(progress.getCompletedGymCount());

        String instanceText = instance == null
                ? "none"
                : "slot " + instance.getSlotId()
                + ", type " + instance.getGymType()
                + ", stage " + instance.getTrainerStage()
                + ", origin " + formatPos(GymSlotPosition.getOriginForSlot(instance.getSlotId()));

        source.sendSuccess(
                () -> Component.literal(
                        "CobbleBash debug: completed gyms = "
                                + progress.getCompletedGymCount()
                                + ", active gym = "
                                + progress.getActiveGymType()
                                + ", trainer levels = {"
                                + trainerLevels[0] + ", "
                                + trainerLevels[1] + ", "
                                + trainerLevels[2] + "}"
                                + ", active instance = "
                                + instanceText
                                + ", active instances = "
                                + GymInstanceManager.getActiveCount()
                                + ", free slots = "
                                + GymInstanceManager.getFreeSlotCount()
                                + ", next slot id = "
                                + GymInstanceManager.getNextSlotId()
                ),
                false
        );

        return 1;
    }

    private static int debugBeaconAuras(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        ChampionBeaconAuras.PlayerDebugInfo debug = ChampionBeaconAuras.debugForPlayer(player);
        String powers = debug.auraInfo().powers().isEmpty()
                ? "none"
                : debug.auraInfo().powers().stream()
                .map(ChampionBeaconPower::name)
                .reduce((left, right) -> left + ", " + right)
                .orElse("none");
        String shinyExtra = debug.activeExtraShinyChance() <= 0.0D
                ? "inactive"
                : "active +" + formatOneIn(debug.activeExtraShinyChance());
        String estimatedShiny = debug.activeExtraShinyChance() <= 0.0D
                ? "base only"
                : formatEstimatedCombinedShiny(debug.cobblemonShinyRate(), debug.activeExtraShinyChance());

        source.sendSuccess(
                () -> Component.literal(
                        "Champion Beacon aura debug at "
                                + formatPos(player.blockPosition())
                                + ": active beacons in range = "
                                + debug.auraInfo().activeBeacons()
                                + ", powers = "
                                + powers
                                + ", player spawner active = "
                                + debug.spawnerActive()
                                + ", spawn timer = "
                                + formatFloat(debug.ticksUntilNextSpawn())
                                + "/"
                                + formatFloat(debug.ticksBetweenSpawnAttempts())
                                + " ticks"
                                + ", base progress/tick = "
                                + formatFloat(debug.baseProgressPerTick())
                                + ", lure active = "
                                + debug.lureActive()
                                + ", lure bonus progress/tick = "
                                + formatFloat(debug.lureProgressPerTick())
                                + ", effective interval = "
                                + formatFloat(debug.effectiveTicksBetweenSpawnAttempts())
                                + " ticks ("
                                + formatFloat(debug.effectiveTicksBetweenSpawnAttempts() / 20.0F)
                                + "s)"
                                + ", Cobblemon shinyRate = "
                                + formatFloat(debug.cobblemonShinyRate())
                                + ", shiny aura = "
                                + shinyExtra
                                + ", estimated combined shiny = "
                                + estimatedShiny
                ),
                false
        );

        return 1;
    }

    private static int toggleBeaconPulseDebug(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        boolean enabled = ChampionBeaconAuras.togglePulseDebug(player);
        source.sendSuccess(
                () -> Component.literal("Champion Beacon pulse debug " + (enabled ? "enabled" : "disabled") + "."),
                false
        );
        return 1;
    }

    private static int visualizeBeaconAuras(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        ChampionBeaconAuras.VisualizationInfo info = ChampionBeaconAuras.visualizeForPlayer(player);
        source.sendSuccess(
                () -> Component.literal(
                        "Champion Beacon visualization: active beacons in range = "
                                + info.activeBeacons()
                                + ", cached apricorns = "
                                + info.apricorns()
                                + ", cached berries = "
                                + info.berries()
                                + ", pastures = "
                                + info.pastures()
                                + ". Green particles mark cached crops and detected pastures."
                ),
                false
        );
        return 1;
    }

    private static void teleportToSpawn(ServerPlayer player) {
        ServerLevel overworld = player.server.overworld();
        BlockPos spawn = overworld.getSharedSpawnPos();

        player.teleportTo(
                overworld,
                spawn.getX() + 0.5,
                spawn.getY(),
                spawn.getZ() + 0.5,
                player.getYRot(),
                player.getXRot()
        );
    }

    private static void teleportToReturnLocation(ServerPlayer player, GymInstance instance) {
        if (instance == null) {
            GymReturnData.get(player.server).remove(player.getUUID())
                    .ifPresentOrElse(
                            location -> teleportToReturnLocation(player, location),
                            () -> teleportToSpawn(player)
                    );
            return;
        }

        teleportToReturnLocation(player, new GymReturnData.ReturnLocation(
                instance.getReturnDimension(),
                instance.getReturnX(),
                instance.getReturnY(),
                instance.getReturnZ(),
                instance.getReturnYRot(),
                instance.getReturnXRot()
        ));
        restoreReturnGameMode(player, instance);
    }

    public static void teleportToSavedReturnOrSpawn(ServerPlayer player) {
        GymReturnData.get(player.server).remove(player.getUUID())
                .ifPresentOrElse(
                        location -> teleportToReturnLocation(player, location),
                        () -> teleportToSpawn(player)
                );
    }

    private static void teleportToReturnLocation(ServerPlayer player, GymReturnData.ReturnLocation location) {
        ServerLevel returnLevel = player.server.getLevel(location.dimension());
        if (returnLevel == null || returnLevel.dimension().equals(CobbleBashDimensions.GYM_VOID)) {
            teleportToSpawn(player);
            return;
        }

        player.teleportTo(
                returnLevel,
                location.x(),
                location.y(),
                location.z(),
                location.yRot(),
                location.xRot()
        );
    }

    private static String formatPos(BlockPos pos) {
        return "(" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ")";
    }

    private static String formatFloat(float value) {
        if (Float.isInfinite(value)) {
            return "infinite";
        }

        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static String formatOneIn(double chance) {
        if (chance <= 0.0D) {
            return "0";
        }

        return "1/" + Math.round(1.0D / chance);
    }

    private static String formatEstimatedCombinedShiny(float cobblemonShinyRate, double extraChance) {
        if (cobblemonShinyRate <= 0.0F) {
            return formatOneIn(extraChance);
        }

        double baseChance = 1.0D / cobblemonShinyRate;
        double combinedChance = baseChance + (1.0D - baseChance) * extraChance;
        return formatOneIn(combinedChance);
    }

    private static boolean hasCompletedAllElementalGyms(PlayerGymProgress progress) {
        for (GymType type : GymType.values()) {
            if (!progress.hasCompleted(type.getId())) {
                return false;
            }
        }

        return true;
    }

    private static void clearInstancePlatform(ServerPlayer player, GymInstance instance) {
        if (instance == null) {
            return;
        }

        ServerLevel gymLevel = player.server.getLevel(CobbleBashDimensions.GYM_VOID);
        if (gymLevel == null) {
            return;
        }

        BlockPos origin = GymSlotPosition.getOriginForSlot(instance.getSlotId());
        if (EliteFourStructure.GYM_TYPE.equals(instance.getGymType())) {
            EliteFourStructure.clear(gymLevel, origin);
        } else {
            GymPlatformBuilder.clearGym(gymLevel, origin, instance.getGymType());
            RctApiProbe.unregisterGymTrainers(instance.getGymType(), instance.getSlotId());
        }
    }

    private static void restoreReturnGameMode(ServerPlayer player, GymInstance instance) {
        if (instance.getReturnGameMode() != null) {
            player.setGameMode(instance.getReturnGameMode());
        }
    }

    private static void sendSuccess(ServerPlayer player, CommandSourceStack source, String message) {
        if (source != null) {
            source.sendSuccess(() -> Component.literal(message), false);
        } else {
            player.sendSystemMessage(Component.literal(message));
        }
    }

    private static void sendFailure(ServerPlayer player, CommandSourceStack source, String message) {
        if (source != null) {
            source.sendFailure(Component.literal(message));
        } else {
            player.sendSystemMessage(Component.literal(message));
        }
    }

    private static int debugTrainerEntities(CommandSourceStack source) {
        ActiveGymDebugContext context = getActiveGymDebugContext(source);
        if (context == null) {
            return 0;
        }

        int removed = GymPlatformBuilder.cleanupTrainerEntities(
                context.gymLevel(),
                context.origin(),
                context.gymType(),
                context.instance().getSlotId()
        );
        if (removed > 0) {
            source.sendSuccess(
                    () -> Component.literal("Cleaned " + removed + " stale trainer display entities before counting."),
                    false
            );
            CobbleBash.LOGGER.warn(
                    "Trainer debug count normalized {} gym {} slot {} before reporting; removed {} entities.",
                    context.player().getGameProfile().getName(),
                    context.gymType(),
                    context.instance().getSlotId(),
                    removed
            );
        }

        List<GymPlatformBuilder.TrainerEntityDebug> groups = GymPlatformBuilder.debugTrainerEntities(
                context.gymLevel(),
                context.origin(),
                context.gymType(),
                context.instance().getSlotId()
        );
        int total = 0;
        for (GymPlatformBuilder.TrainerEntityDebug group : groups) {
            total += group.total();
            source.sendSuccess(
                    () -> Component.literal(
                            group.trainerIdPart()
                                    + ": total="
                                    + group.total()
                                    + ", exactTagged="
                                    + group.exactTagged()
                                    + ", nearbyDisplays="
                                    + group.nearbyDisplays()
                    ),
                    false
            );
            CobbleBash.LOGGER.info(
                    "Trainer debug for {} gym {} slot {} {}: total={}, exactTagged={}, nearbyDisplays={}, trainerId={}, entries={}",
                    context.player().getGameProfile().getName(),
                    context.gymType(),
                    context.instance().getSlotId(),
                    group.trainerIdPart(),
                    group.total(),
                    group.exactTagged(),
                    group.nearbyDisplays(),
                    group.trainerId(),
                    group.entries()
            );
            for (String entry : group.entries()) {
                source.sendSuccess(() -> Component.literal("  " + entry), false);
            }
        }

        return Math.max(1, total);
    }

    private static int cleanupTrainerEntities(CommandSourceStack source) {
        ActiveGymDebugContext context = getActiveGymDebugContext(source);
        if (context == null) {
            return 0;
        }

        int removed = GymPlatformBuilder.cleanupTrainerEntities(
                context.gymLevel(),
                context.origin(),
                context.gymType(),
                context.instance().getSlotId()
        );
        source.sendSuccess(
                () -> Component.literal("Removed " + removed + " stale or duplicate trainer display entities."),
                false
        );
        CobbleBash.LOGGER.warn(
                "Trainer debug cleanup for {} gym {} slot {} removed {} entities.",
                context.player().getGameProfile().getName(),
                context.gymType(),
                context.instance().getSlotId(),
                removed
        );
        return Math.max(1, removed);
    }

    private static int discardOneTrainerDisplay(CommandSourceStack source, GymTrainerUnit unit) {
        ActiveGymDebugContext context = getActiveGymDebugContext(source);
        if (context == null) {
            return 0;
        }

        int remaining = GymPlatformBuilder.discardOneTrainerDisplay(
                context.gymLevel(),
                context.origin(),
                context.gymType(),
                context.instance().getSlotId(),
                unit.getTrainerIdPart()
        );
        source.sendSuccess(
                () -> Component.literal(
                        "Discarded one "
                                + unit.getDisplayName()
                                + " display entity. Remaining nearby/tagged displays: "
                                + remaining
                                + "."
                ),
                false
        );
        return 1;
    }

    private static int discardTrainerDisplays(CommandSourceStack source, GymTrainerUnit unit) {
        ActiveGymDebugContext context = getActiveGymDebugContext(source);
        if (context == null) {
            return 0;
        }

        int removed = GymPlatformBuilder.discardTrainerDisplays(
                context.gymLevel(),
                context.origin(),
                context.gymType(),
                context.instance().getSlotId(),
                unit.getTrainerIdPart()
        );
        source.sendSuccess(
                () -> Component.literal("Discarded " + removed + " " + unit.getDisplayName() + " display entities."),
                false
        );
        return Math.max(1, removed);
    }

    private static ActiveGymDebugContext getActiveGymDebugContext(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        GymInstance instance = GymInstanceManager.getActive(player.getUUID());
        if (instance == null) {
            source.sendFailure(Component.literal("You do not have an active gym instance."));
            return null;
        }

        if (EliteFourStructure.GYM_TYPE.equals(instance.getGymType())) {
            source.sendFailure(Component.literal("Trainer debug currently targets regular gyms. Use a regular gym instance."));
            return null;
        }

        ServerLevel gymLevel = player.server.getLevel(CobbleBashDimensions.GYM_VOID);
        if (gymLevel == null) {
            source.sendFailure(Component.literal("Gym void dimension is not loaded."));
            return null;
        }

        return new ActiveGymDebugContext(
                player,
                gymLevel,
                instance,
                GymSlotPosition.getOriginForSlot(instance.getSlotId()),
                instance.getGymType()
        );
    }

    private static LiteralArgumentBuilder<CommandSourceStack> trainerDebugTarget(TrainerDebugCommandExecutor executor) {
        return Commands.literal("trainer")
                .then(Commands.literal("one").executes(context -> executor.run(context.getSource(), GymTrainerUnit.TRAINER_ONE)))
                .then(Commands.literal("two").executes(context -> executor.run(context.getSource(), GymTrainerUnit.TRAINER_TWO)));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> trainerTarget(GymType type, TrainerCommandExecutor executor) {
        return Commands.literal(type.getId())
                .then(Commands.literal("trainer")
                        .then(Commands.literal("one").executes(context -> executor.run(context.getSource(), type, GymTrainerUnit.TRAINER_ONE)))
                        .then(Commands.literal("two").executes(context -> executor.run(context.getSource(), type, GymTrainerUnit.TRAINER_TWO))))
                .then(Commands.literal("boss").executes(context -> executor.run(context.getSource(), type, GymTrainerUnit.BOSS)));
    }

    private static String getExpectedTrainerName(int stage) {
        GymTrainerUnit unit = getExpectedTrainerUnit(stage);
        return unit == null ? "no remaining trainer" : unit.getDisplayName();
    }

    private static GymTrainerUnit getExpectedTrainerUnit(int stage) {
        return switch (stage) {
            case 0 -> GymTrainerUnit.TRAINER_ONE;
            case 1 -> GymTrainerUnit.TRAINER_TWO;
            case 2 -> GymTrainerUnit.BOSS;
            default -> null;
        };
    }

    private static String getTrainerDisplayName(ServerPlayer player, String gymType, GymTrainerUnit unit) {
        return RctApiProbe.getTrainerDisplayName(player.server, gymType, unit.getTrainerIdPart());
    }

    private interface TrainerCommandExecutor {
        int run(CommandSourceStack source, GymType gymType, GymTrainerUnit unit);
    }

    private interface TrainerDebugCommandExecutor {
        int run(CommandSourceStack source, GymTrainerUnit unit);
    }

    private record ActiveGymDebugContext(
            ServerPlayer player,
            ServerLevel gymLevel,
            GymInstance instance,
            BlockPos origin,
            String gymType
    ) {
    }
}
