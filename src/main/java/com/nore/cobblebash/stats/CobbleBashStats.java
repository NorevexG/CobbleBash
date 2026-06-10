package com.nore.cobblebash.stats;

import com.nore.cobblebash.CobbleBash;
import com.nore.cobblebash.gym.GymType;
import com.nore.cobblebash.progress.GymProgressManager;
import com.nore.cobblebash.progress.PlayerGymProgress;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import net.minecraft.stats.StatFormatter;
import net.minecraft.stats.Stats;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.HashSet;
import java.util.Set;

public class CobbleBashStats {
    public static final ResourceLocation GYMS_COMPLETED_ID = ResourceLocation.fromNamespaceAndPath(CobbleBash.MODID, "gyms_completed");
    public static final DeferredRegister<ResourceLocation> CUSTOM_STATS = DeferredRegister.create(Registries.CUSTOM_STAT, CobbleBash.MODID);
    public static final DeferredHolder<ResourceLocation, ResourceLocation> GYMS_COMPLETED = CUSTOM_STATS.register(
            "gyms_completed",
            () -> GYMS_COMPLETED_ID
    );

    public static void bootstrap() {
        Stats.CUSTOM.get(GYMS_COMPLETED.get(), StatFormatter.DEFAULT);
    }

    public static void syncGymsCompleted(ServerPlayer player) {
        PlayerGymProgress progress = GymProgressManager.get(player.getUUID());
        progress.markCompletedGyms(getCompletedGymAdvancements(player));
        setStatValue(player, progress.getCompletedGymCount());
    }

    private static Set<String> getCompletedGymAdvancements(ServerPlayer player) {
        Set<String> completed = new HashSet<>();

        for (GymType type : GymType.values()) {
            AdvancementHolder advancement = player.server.getAdvancements().get(
                    ResourceLocation.fromNamespaceAndPath(CobbleBash.MODID, "gym/" + type.getId())
            );

            if (advancement != null && player.getAdvancements().getOrStartProgress(advancement).isDone()) {
                completed.add(type.getId());
            }
        }

        return completed;
    }

    private static void setStatValue(ServerPlayer player, int value) {
        Stat<ResourceLocation> stat = Stats.CUSTOM.get(GYMS_COMPLETED.get());
        int currentValue = player.getStats().getValue(stat);
        if (currentValue == value) {
            return;
        }

        player.resetStat(stat);
        if (value > 0) {
            player.awardStat(stat, value);
        }
        player.getStats().sendStats(player);
    }
}
