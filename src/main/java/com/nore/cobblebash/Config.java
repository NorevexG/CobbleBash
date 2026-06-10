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
