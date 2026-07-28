package com.nore.cobblebash.client.tooltip;

import com.nore.cobblebash.item.RibbonAttributeManager;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

import java.util.List;

public record RibbonTooltipComponent(List<RibbonAttributeManager.TooltipTypeBonus> rows) implements TooltipComponent {
}
