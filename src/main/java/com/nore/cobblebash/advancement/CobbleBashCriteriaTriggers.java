package com.nore.cobblebash.advancement;

import com.nore.cobblebash.CobbleBash;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class CobbleBashCriteriaTriggers {
    public static final DeferredRegister<CriterionTrigger<?>> TRIGGERS = DeferredRegister.create(
            Registries.TRIGGER_TYPE,
            CobbleBash.MODID
    );

    public static final DeferredHolder<CriterionTrigger<?>, GymBossDefeatedTrigger> GYM_BOSS_DEFEATED =
            TRIGGERS.register("gym_boss_defeated", GymBossDefeatedTrigger::new);

    public static void triggerGymBossDefeated(ServerPlayer player, String gymType) {
        GYM_BOSS_DEFEATED.get().trigger(player, gymType);
    }
}
