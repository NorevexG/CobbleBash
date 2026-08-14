package com.p1nero.cobblebashscreen.event;

import com.nore.cobblebash.CobbleBash;
import com.p1nero.cobblebashscreen.api.event.TrainingSimulatorOpenEvent;
import com.p1nero.cobblebashscreen.simulator.TrainingSimulatorMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = com.p1nero.cobblebashscreen.CobbleBash.MOD_ID)
public final class TrainingSimulatorInteraction {
    private TrainingSimulatorInteraction() {
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        BlockState state = event.getLevel().getBlockState(event.getPos());
        if (!state.is(CobbleBash.TRAINING_SIMULATOR.get())) {
            return;
        }

        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
        event.setCanceled(true);

        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        TrainingSimulatorOpenEvent.Pre openEvent = new TrainingSimulatorOpenEvent.Pre(
                player,
                event.getLevel(),
                event.getPos()
        );
        NeoForge.EVENT_BUS.post(openEvent);
        if (openEvent.isCanceled()) {
            return;
        }

        player.openMenu(new SimpleMenuProvider(
                (containerId, inventory, ignored) -> new TrainingSimulatorMenu(
                        containerId,
                        inventory,
                        ContainerLevelAccess.create(event.getLevel(), event.getPos())
                ),
                Component.translatable("container.cobblebash.training_simulator")
        ));
    }
}
