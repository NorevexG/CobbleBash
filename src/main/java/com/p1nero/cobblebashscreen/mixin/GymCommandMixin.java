package com.p1nero.cobblebashscreen.mixin;

import com.nore.cobblebash.command.GymCommand;
import com.p1nero.cobblebashscreen.progress.TrainingSimulatorData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
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
        recordSuccessfulStart(player, callback);
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
        recordSuccessfulStart(player, callback);
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
            TrainingSimulatorData.get(player.server).incrementClearCount(player.getUUID(), gymType);
        }
    }

    private static void recordSuccessfulStart(ServerPlayer player, CallbackInfoReturnable<Integer> callback) {
        if (callback.getReturnValueI() > 0) {
            TrainingSimulatorData.get(player.server).incrementChallengesStarted(player.getUUID());
        }
    }
}
