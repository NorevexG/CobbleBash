package com.nore.cobblebash.client;

import com.mojang.datafixers.util.Either;
import com.nore.cobblebash.CobbleBash;
import com.nore.cobblebash.client.tooltip.RibbonTooltipComponent;
import com.nore.cobblebash.item.RibbonAttributeManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@EventBusSubscriber(modid = CobbleBash.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public final class RibbonTooltipEvents {
    private RibbonTooltipEvents() {
    }

    @SubscribeEvent
    public static void gatherTooltipComponents(RenderTooltipEvent.GatherComponents event) {
        RibbonAttributeManager.RibbonKind kind = getRibbonKind(event.getItemStack());
        if (kind == null || Minecraft.getInstance().player == null || isCuriosScreenOpen()) {
            return;
        }

        double multiplier = getHoveredRibbonMultiplier(event.getItemStack());
        List<RibbonAttributeManager.TooltipTypeBonus> rows = RibbonAttributeManager.calculateTooltipBonuses(
                Minecraft.getInstance().player,
                kind,
                multiplier
        );
        if (rows.isEmpty()) {
            return;
        }

        int insertIndex = Math.min(1, event.getTooltipElements().size());
        event.getTooltipElements().add(insertIndex, Either.right(new RibbonTooltipComponent(rows)));
    }

    @SubscribeEvent
    public static void addCuriosScreenTooltip(ItemTooltipEvent event) {
        RibbonAttributeManager.RibbonKind kind = getRibbonKind(event.getItemStack());
        if (kind == null || event.getEntity() == null || !isCuriosScreenOpen()) {
            return;
        }

        double multiplier = getHoveredRibbonMultiplier(event.getItemStack());
        List<RibbonAttributeManager.TooltipTypeBonus> rows = RibbonAttributeManager.calculateTooltipBonuses(
                event.getEntity(),
                kind,
                multiplier
        );
        if (rows.isEmpty()) {
            return;
        }

        for (RibbonAttributeManager.TooltipTypeBonus row : rows) {
            String line = formatFallbackLine(row);
            if (!line.isEmpty()) {
                event.getToolTip().add(Component.literal(line).withStyle(ChatFormatting.GREEN));
            }
        }
    }

    private static RibbonAttributeManager.RibbonKind getRibbonKind(ItemStack stack) {
        if (stack.is(CobbleBash.TRAINER_RIBBON.get())) {
            return RibbonAttributeManager.RibbonKind.TRAINER;
        }

        if (stack.is(CobbleBash.CHAMPION_RIBBON.get())) {
            return RibbonAttributeManager.RibbonKind.CHAMPION;
        }

        return null;
    }

    private static double getHoveredRibbonMultiplier(ItemStack hoveredStack) {
        if (Minecraft.getInstance().player == null) {
            return 1.0D;
        }

        return CuriosApi.getCuriosInventory(Minecraft.getInstance().player)
                .map(handler -> {
                    List<SlotResult> matches = handler.findCurios(stack -> stack.is(hoveredStack.getItem()));
                    matches.sort(Comparator
                            .comparing((SlotResult result) -> result.slotContext().identifier())
                            .thenComparingInt(result -> result.slotContext().index())
                            .thenComparing(result -> result.slotContext().cosmetic()));
                    for (int index = 0; index < matches.size(); index++) {
                        if (matches.get(index).stack() == hoveredStack) {
                            return Math.pow(0.5D, index);
                        }
                    }
                    return 1.0D;
                })
                .orElse(1.0D);
    }

    private static boolean isCuriosScreenOpen() {
        return Minecraft.getInstance().screen != null
                && "top.theillusivec4.curios.client.gui.CuriosScreen".equals(Minecraft.getInstance().screen.getClass().getName());
    }

    private static String formatFallbackLine(RibbonAttributeManager.TooltipTypeBonus bonus) {
        List<String> parts = new ArrayList<>();
        for (Map.Entry<RibbonAttributeManager.AttributeKey, Double> entry : bonus.attributes().entrySet()) {
            if (Math.abs(entry.getValue()) < 0.000001D) {
                continue;
            }
            parts.add(formatSigned(entry.getValue()) + " " + entry.getKey().displayName());
        }

        if (parts.isEmpty()) {
            return "";
        }

        return capitalize(bonus.typeName()) + ": " + String.join(", ", parts);
    }

    private static String capitalize(String value) {
        if (value.isEmpty()) {
            return value;
        }
        return value.substring(0, 1).toUpperCase(Locale.ROOT) + value.substring(1);
    }

    private static String formatSigned(double value) {
        String sign = value > 0.0D ? "+" : "";
        double rounded = Math.round(value * 1000.0D) / 1000.0D;
        if (Math.abs(rounded - Math.rint(rounded)) < 0.000001D) {
            return sign + String.format(Locale.ROOT, "%.0f", rounded);
        }
        return sign + String.format(Locale.ROOT, "%.3f", rounded).replaceAll("0+$", "").replaceAll("\\.$", "");
    }
}
