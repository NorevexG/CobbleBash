package com.nore.cobblebash;

import java.util.List;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final List<Integer> DEFAULT_GYM_BASE_LEVELS = List.of(
            14, 19, 23, 28, 32, 37, 41, 46, 50,
            55, 59, 64, 68, 73, 77, 82, 86, 91
    );

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

    public static final ModConfigSpec.ConfigValue<List<? extends Integer>> GYM_BASE_LEVELS = BUILDER
            .comment(
                    "Base Pokemon levels for gym attempts 1 through 18, ordered by the number of gyms the player has completed.",
                    "Gym type does not matter: the first value is used for a player's first gym, the second for their second gym, and so on.",
                    "Values are clamped to levels 1 through 100. If fewer than 18 values are supplied, the final supplied value is reused."
            )
            .defineListAllowEmpty(
                    "gymBaseLevels",
                    DEFAULT_GYM_BASE_LEVELS,
                    () -> 10,
                    Config::validatePokemonLevel
            );

    public static final ModConfigSpec.IntValue TRAINER_ONE_LEVEL_OFFSET = BUILDER
            .comment("Level added to the configured gym base level for the first trainer. The final Pokemon level is clamped to 1 through 100.")
            .defineInRange("trainerOneLevelOffset", 0, -99, 99);

    public static final ModConfigSpec.IntValue TRAINER_TWO_LEVEL_OFFSET = BUILDER
            .comment("Level added to the configured gym base level for the second trainer. The final Pokemon level is clamped to 1 through 100.")
            .defineInRange("trainerTwoLevelOffset", 2, -99, 99);

    public static final ModConfigSpec.IntValue GYM_LEADER_LEVEL_OFFSET = BUILDER
            .comment("Level added to the configured gym base level for the Gym Leader. The final Pokemon level is clamped to 1 through 100.")
            .defineInRange("gymLeaderLevelOffset", 4, -99, 99);

    public static final ModConfigSpec.IntValue MAX_BATTLE_ITEM_USES = BUILDER
            .comment(
                    "Maximum number of bag items each player can use during one regular CobbleBash gym trainer battle.",
                    "The counter resets for every battle; item use outside battle is unrestricted. Use -1 for unlimited or 0 to disable battle items."
            )
            .defineInRange("maxBattleItemUses", 5, -1, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue ELITE_FOUR_MAX_ITEM_USES = BUILDER
            .comment(
                    "Maximum number of battle items a player can use across an entire Elite Four and Champion attempt.",
                    "This shared counter includes tagged healing and revival items used between battles and resets when the attempt ends.",
                    "Use -1 for unlimited or 0 to disable battle items throughout the Elite Four challenge."
            )
            .defineInRange("eliteFourMaxItemUses", 25, -1, Integer.MAX_VALUE);

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

    private static boolean validatePokemonLevel(final Object obj) {
        return obj instanceof Number number && number.intValue() >= 1 && number.intValue() <= 100;
    }
}
