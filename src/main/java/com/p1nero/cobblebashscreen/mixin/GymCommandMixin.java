package com.p1nero.cobblebashscreen.mixin;

import com.nore.cobblebash.command.GymCommand;
import com.nore.cobblebash.gym.GymTrainerUnit;
import com.nore.cobblebash.instance.GymInstance;
import com.nore.cobblebash.structure.EliteFourStructure;
import com.p1nero.cobblebashscreen.api.event.TrainingSimulatorChallengeStartedEvent;
import com.p1nero.cobblebashscreen.api.event.TrainingSimulatorEliteFourCompletedEvent;
import com.p1nero.cobblebashscreen.api.event.TrainingSimulatorGymClearedEvent;
import com.p1nero.cobblebashscreen.progress.TrainingSimulatorData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = GymCommand.class, remap = false)
public abstract class GymCommandMixin {
    @Inject(
            method = "enterGym(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/commands/CommandSourceStack;Ljava/lang/String;)I",
            at = @At("RETURN")
    )
    private static void cobblebashScreen$recordGymStart(
            ServerPlayer player,
            CommandSourceStack source,
            String gymType,
            CallbackInfoReturnable<Integer> callback
    ) {
        recordSuccessfulStart(player, gymType, false, callback);
    }

    @Inject(
            method = "enterEliteFour(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/commands/CommandSourceStack;Z)I",
            at = @At("RETURN")
    )
    private static void cobblebashScreen$recordEliteFourStart(
            ServerPlayer player,
            CommandSourceStack source,
            boolean bypassRequirements,
            CallbackInfoReturnable<Integer> callback
    ) {
        recordSuccessfulStart(player, EliteFourStructure.GYM_TYPE, true, callback);
    }

    @Inject(
            method = "completeGym(Lnet/minecraft/server/level/ServerPlayer;Ljava/lang/String;Lnet/minecraft/commands/CommandSourceStack;)I",
            at = @At("RETURN")
    )
    private static void cobblebashScreen$recordSuccessfulClear(
            ServerPlayer player,
            String gymType,
            CommandSourceStack source,
            CallbackInfoReturnable<Integer> callback
    ) {
        if (callback.getReturnValueI() > 0) {
            int clearCount = TrainingSimulatorData.get(player.server).incrementClearCount(player.getUUID(), gymType);
            NeoForge.EVENT_BUS.post(new TrainingSimulatorGymClearedEvent.Post(
                    player,
                    gymType,
                    clearCount,
                    clearCount == 1
            ));
        }
    }

    @Inject(
            method = "handleEliteFourTrainerVictory(Lnet/minecraft/server/level/ServerPlayer;Ljava/lang/String;ILcom/nore/cobblebash/gym/GymTrainerUnit;Lcom/nore/cobblebash/instance/GymInstance;)V",
            at = @At("RETURN")
    )
    private static void cobblebashScreen$postEliteFourCompleted(
            ServerPlayer player,
            String gymType,
            int slotId,
            GymTrainerUnit unit,
            GymInstance instance,
            CallbackInfo callback
    ) {
        if (EliteFourStructure.CHAMPION_TRAINER_GYM_TYPE.equals(gymType)
                && unit == GymTrainerUnit.BOSS
                && instance.getSlotId() == slotId
                && instance.isEliteFourChampionUnlocked()) {
            NeoForge.EVENT_BUS.post(new TrainingSimulatorEliteFourCompletedEvent.Post(player));
        }
    }

    private static void recordSuccessfulStart(
            ServerPlayer player,
            String gymId,
            boolean eliteFour,
            CallbackInfoReturnable<Integer> callback
    ) {
        if (callback.getReturnValueI() > 0) {
            TrainingSimulatorData.get(player.server).incrementChallengesStarted(player.getUUID());
            NeoForge.EVENT_BUS.post(new TrainingSimulatorChallengeStartedEvent.Post(player, gymId, eliteFour));
        }
    }
}
