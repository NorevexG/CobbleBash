package com.nore.cobblebash.client.tooltip;

import com.nore.cobblebash.item.RibbonAttributeManager;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ClientRibbonTooltipComponent implements ClientTooltipComponent {
    private static final ResourceLocation TYPE_ICONS = ResourceLocation.fromNamespaceAndPath(
            "cobblemon",
            "textures/gui/types_small.png"
    );
    private static final int ICON_SIZE = 18;
    private static final int ICON_SHEET_WIDTH = 324;
    private static final int ICON_SHEET_HEIGHT = 18;
    private static final int ROW_HEIGHT = 20;
    private static final int ICON_TEXT_GAP = 4;
    private static final int TEXT_COLOR = 0xB6FFB6;

    private final List<Row> rows;

    public ClientRibbonTooltipComponent(RibbonTooltipComponent component) {
        this.rows = component.rows().stream()
                .map(Row::from)
                .filter(row -> !row.text.isEmpty())
                .toList();
    }

    @Override
    public int getHeight() {
        return rows.isEmpty() ? 0 : rows.size() * ROW_HEIGHT;
    }

    @Override
    public int getWidth(Font font) {
        int width = 0;
        for (Row row : rows) {
            width = Math.max(width, ICON_SIZE + ICON_TEXT_GAP + font.width(row.text));
        }
        return width;
    }

    @Override
    public void renderImage(Font font, int x, int y, GuiGraphics graphics) {
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            Row row = rows.get(rowIndex);
            int rowY = y + rowIndex * ROW_HEIGHT;
            graphics.blit(
                    TYPE_ICONS,
                    x,
                    rowY,
                    row.textureIndex * ICON_SIZE,
                    0,
                    ICON_SIZE,
                    ICON_SIZE,
                    ICON_SHEET_WIDTH,
                    ICON_SHEET_HEIGHT
            );
            graphics.drawString(font, row.text, x + ICON_SIZE + ICON_TEXT_GAP, rowY + 5, TEXT_COLOR, false);
        }
    }

    private record Row(int textureIndex, String text) {
        private static Row from(RibbonAttributeManager.TooltipTypeBonus bonus) {
            List<String> parts = new ArrayList<>();
            for (Map.Entry<RibbonAttributeManager.AttributeKey, Double> entry : bonus.attributes().entrySet()) {
                if (Math.abs(entry.getValue()) < 0.000001D) {
                    continue;
                }
                parts.add(formatSigned(entry.getValue()) + " " + entry.getKey().displayName());
            }
            return new Row(bonus.textureIndex(), String.join(", ", parts));
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
}
