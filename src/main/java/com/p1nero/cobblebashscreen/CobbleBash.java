package com.p1nero.cobblebashscreen;

import com.p1nero.cobblebashscreen.simulator.TrainingSimulatorMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(CobbleBash.MOD_ID)
public final class CobbleBash {
    public static final String MOD_ID = "cobblebash_screen";

    private static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<TrainingSimulatorMenu>> TRAINING_SIMULATOR_MENU =
            MENUS.register(
                    "training_simulator",
                    () -> new MenuType<>(TrainingSimulatorMenu::new, FeatureFlags.DEFAULT_FLAGS)
            );

    public CobbleBash(IEventBus modEventBus) {
        MENUS.register(modEventBus);
    }
}
