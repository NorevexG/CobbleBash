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
    public static final DeferredHolder<CriterionTrigger<?>, SimpleEventTrigger> LEAGUE_REPRESENTATIVE_MET =
            TRIGGERS.register("league_representative_met", SimpleEventTrigger::new);
    public static final DeferredHolder<CriterionTrigger<?>, SimpleEventTrigger> LEAGUE_REPRESENTATIVE_TRADED =
            TRIGGERS.register("league_representative_traded", SimpleEventTrigger::new);
    public static final DeferredHolder<CriterionTrigger<?>, SimpleEventTrigger> GYM_ENTERED =
            TRIGGERS.register("gym_entered", SimpleEventTrigger::new);

    public static void triggerGymBossDefeated(ServerPlayer player, String gymType) {
        GYM_BOSS_DEFEATED.get().trigger(player, gymType);
    }

    public static void triggerLeagueRepresentativeMet(ServerPlayer player) {
        LEAGUE_REPRESENTATIVE_MET.get().trigger(player);
    }

    public static void triggerLeagueRepresentativeTraded(ServerPlayer player) {
        LEAGUE_REPRESENTATIVE_TRADED.get().trigger(player);
    }

    public static void triggerGymEntered(ServerPlayer player) {
        GYM_ENTERED.get().trigger(player);
    }
}
