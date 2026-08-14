package com.p1nero.cobblebashscreen;

import com.p1nero.cobblebashscreen.simulator.TrainingSimulatorScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = CobbleBash.MOD_ID, value = Dist.CLIENT)
public final class CobbleBashClient {
    private CobbleBashClient() {
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(CobbleBash.TRAINING_SIMULATOR_MENU.get(), TrainingSimulatorScreen::new);
    }
}
