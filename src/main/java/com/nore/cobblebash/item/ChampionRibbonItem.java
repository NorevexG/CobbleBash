package com.nore.cobblebash.item;

import net.minecraft.world.item.Item;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

public class ChampionRibbonItem extends Item implements ICurioItem {
    public ChampionRibbonItem(Properties properties) {
        super(properties);
    }

    @Override
    public void onEquip(SlotContext slotContext, net.minecraft.world.item.ItemStack previousStack, net.minecraft.world.item.ItemStack stack) {
        RibbonAttributeManager.equip(slotContext, RibbonAttributeManager.RibbonKind.CHAMPION);
    }

    @Override
    public void onUnequip(SlotContext slotContext, net.minecraft.world.item.ItemStack newStack, net.minecraft.world.item.ItemStack stack) {
        RibbonAttributeManager.unequip(slotContext, RibbonAttributeManager.RibbonKind.CHAMPION);
    }
}
