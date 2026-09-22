package com.nore.cobblebash;

import java.util.List;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.ConfigValue<List<? extends String>> GYM_ITEM_BLACKLIST = BUILDER
            .comment("Items players cannot use while inside CobbleBash gym instances.")
            .defineListAllowEmpty(
                    "gymItemBlacklist",
                    List.of("minecraft:ender_pearl", "minecraft:chorus_fruit", "minecraft:firework_rocket"),
                    () -> "",
                    Config::validateItemName
            );

    public static final ModConfigSpec.BooleanValue CONSUME_TRAINING_DISKS = BUILDER
            .comment("Whether entering a gym consumes the regular or Elite Four Training Disk used to enter. Creative-mode players never consume disks.")
            .define("consumeTrainingDisks", true);

    public static final ModConfigSpec.IntValue COBBLE_DOLLARS_REPEAT_TRAINER_REWARD = BUILDER
            .comment("Bonus Cobble Dollars awarded only for defeating a non-boss CobbleBash gym trainer in an already-completed gym when Cobble Dollars is installed.")
            .defineInRange("cobbleDollarsRepeatTrainerReward", 500, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue COBBLE_DOLLARS_REPEAT_BOSS_REWARD = BUILDER
            .comment("Bonus Cobble Dollars awarded only for defeating a CobbleBash gym leader in an already-completed gym when Cobble Dollars is installed.")
            .defineInRange("cobbleDollarsRepeatBossReward", 1500, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.DoubleValue REPEAT_CLEAR_TRAINER_XP_MULTIPLIER = BUILDER
            .comment("Multiplier applied to Cobblemon battle XP earned from non-boss CobbleBash trainers on repeat clears.")
            .defineInRange("repeatClearTrainerXpMultiplier", 1.2D, 1.0D, 100.0D);

    public static final ModConfigSpec.DoubleValue REPEAT_CLEAR_BOSS_XP_MULTIPLIER = BUILDER
            .comment("Multiplier applied to Cobblemon battle XP earned from CobbleBash gym leaders, Elite Four members, and the Champion on repeat clears.")
            .defineInRange("repeatClearBossXpMultiplier", 1.5D, 1.0D, 100.0D);

    static final ModConfigSpec SPEC = BUILDER.build();

    public static boolean isGymBlacklisted(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return GYM_ITEM_BLACKLIST.get().contains(itemId.toString());
    }

    private static boolean validateItemName(final Object obj) {
        if (!(obj instanceof String itemName)) {
            return false;
        }

        try {
            return BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(itemName));
        } catch (RuntimeException exception) {
            return false;
        }
    }
}
