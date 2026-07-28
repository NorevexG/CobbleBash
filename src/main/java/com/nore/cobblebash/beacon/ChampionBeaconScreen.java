package com.nore.cobblebash.beacon;

import com.nore.cobblebash.CobbleBash;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ChampionBeaconScreen extends AbstractContainerScreen<ChampionBeaconMenu> {
    private static final ResourceLocation UI = texture("champion_beacon_ui");
    private static final ResourceLocation BUTTON_UNLOCKED = texture("button_unlocked");
    private static final ResourceLocation BUTTON_LOCKED = texture("button_locked");
    private static final ResourceLocation BUTTON_HOVERED = texture("button_hovered");
    private static final ResourceLocation BUTTON_SELECTED = texture("button_selected");
    private static final ResourceLocation BUTTON_CHECK_ACTIVE = texture("button_check_active");
    private static final ResourceLocation BUTTON_CLOSE_HOVER = texture("button_close_hover");
    private static final ResourceLocation CHECK = texture("check");
    private static final ResourceLocation CLOSE = texture("close");
    private static final ResourceLocation INFO_BUTTON_TEXTURE = texture("info_button");
    private static final ResourceLocation INFO_WINDOW_TEXTURE = texture("info_window");
    private static final ResourceLocation INFO_WINDOW_CLOSE_BUTTON_TEXTURE = texture("info_window_close_button");
    private static final ResourceLocation INFO_SCROLL_WHEEL_TEXTURE = texture("scroll_wheel");
    private static final Component PRIMARY_EFFECT_LABEL = Component.translatable("block.minecraft.beacon.primary");
    private static final Component SECONDARY_EFFECT_LABEL = Component.translatable("block.minecraft.beacon.secondary");
    private static final Component DEFAULT_INFO_TITLE = Component.literal("Champion Beacon");
    private static final Component DEFAULT_INFO_TEXT = Component.literal(
            "Champion Beacons can have one active primary power and one active secondary power. The pyramid is built from evolution stone blocks, and fueled by evolution stones."
    );
    private static final String DEFAULT_INFO_ICON = "shiny_stone";

    private static final int BUTTON_SIZE = 22;
    private static final int INFO_BUTTON_WIDTH = 11;
    private static final int INFO_BUTTON_HEIGHT = 37;
    private static final int INFO_WINDOW_WIDTH = 132;
    private static final int INFO_WINDOW_HEIGHT = 132;
    private static final int INFO_CLOSE_BUTTON_WIDTH = 12;
    private static final int INFO_CLOSE_BUTTON_HEIGHT = 37;
    private static final int INFO_SCROLL_THUMB_WIDTH = 10;
    private static final int INFO_SCROLL_THUMB_HEIGHT = 13;
    private static final ButtonArea CLOSE_BUTTON = new ButtonArea(16, 130);
    private static final ButtonArea CONFIRM_BUTTON = new ButtonArea(42, 130);
    private static final PowerButton[] PRIMARY_BUTTONS = {
            new PowerButton(ChampionBeaconPower.REPEL, 51, 21, true, "terrain_extender"),
            new PowerButton(ChampionBeaconPower.LURE, 77, 21, true, "magnet"),
            new PowerButton(ChampionBeaconPower.APRICORN, 51, 46, true, "red_apricorn"),
            new PowerButton(ChampionBeaconPower.BERRY, 77, 46, true, "oran_berry"),
            new PowerButton(ChampionBeaconPower.DAYCARE, 64, 71, true, "exp_share"),
            new PowerButton(ChampionBeaconPower.EV, 64, 96, true, "hp_up")
    };
    private static final PowerButton[] SECONDARY_BUTTONS = {
            new PowerButton(ChampionBeaconPower.SHINY, 170, 46, false, "max_revive"),
            new PowerButton(ChampionBeaconPower.REPEL, 143, 71, false, "terrain_extender"),
            new PowerButton(ChampionBeaconPower.LURE, 170, 71, false, "magnet")
    };
    private static final ButtonArea UPGRADE_BUTTON = new ButtonArea(143, 46);
    private static final ButtonArea INFO_BUTTON = new ButtonArea(230, 15, INFO_BUTTON_WIDTH, INFO_BUTTON_HEIGHT);
    private static final ButtonArea INFO_WINDOW = new ButtonArea(230, 15, INFO_WINDOW_WIDTH, INFO_WINDOW_HEIGHT);
    private static final ButtonArea INFO_CLOSE_BUTTON = new ButtonArea(361, 15, INFO_CLOSE_BUTTON_WIDTH, INFO_CLOSE_BUTTON_HEIGHT);
    private static final ButtonArea INFO_ICON_BOX = new ButtonArea(235, 21, 16, 16);
    private static final ButtonArea INFO_TITLE_BOX = new ButtonArea(255, 29, 101, 11);
    private static final ButtonArea INFO_TEXT_BOX = new ButtonArea(239, 45, 94, 92);
    private static final ButtonArea INFO_SCROLL_TRACK = new ButtonArea(340, 59, INFO_SCROLL_THUMB_WIDTH, 70);
    private static final ItemIcon[] PAYMENT_ICONS = {
            new ItemIcon("ice_stone", 120, 108),
            new ItemIcon("fire_stone", 142, 108),
            new ItemIcon("moon_stone", 164, 108),
            new ItemIcon("leaf_stone", 186, 108),
            new ItemIcon("dawn_stone", 208, 108),
            new ItemIcon("dusk_stone", 120, 134),
            new ItemIcon("thunder_stone", 142, 134),
            new ItemIcon("water_stone", 164, 134),
            new ItemIcon("sun_stone", 186, 134),
            new ItemIcon("shiny_stone", 208, 134)
    };

    private boolean infoOpen;
    private Component infoTitle = DEFAULT_INFO_TITLE;
    private Component infoText = DEFAULT_INFO_TEXT;
    private String infoIcon = DEFAULT_INFO_ICON;
    private int infoScrollLine;
    private boolean draggingInfoScroll;

    public ChampionBeaconScreen(ChampionBeaconMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = 256;
        imageHeight = 256;
        inventoryLabelY = 10000;
        titleLabelY = 10000;
    }

    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath(CobbleBash.MODID, "textures/gui/champion_beacon/" + name + ".png");
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(UI, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);

        for (PowerButton button : PRIMARY_BUTTONS) {
            drawPowerButton(guiGraphics, button, mouseX, mouseY);
        }
        drawUpgradeButton(guiGraphics, mouseX, mouseY);
        for (PowerButton button : SECONDARY_BUTTONS) {
            drawPowerButton(guiGraphics, button, mouseX, mouseY);
        }
        drawControlButtons(guiGraphics, mouseX, mouseY);
        drawPaymentIcons(guiGraphics);
        drawInfo(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawCenteredString(font, PRIMARY_EFFECT_LABEL, 62, 10, 14737632);
        guiGraphics.drawCenteredString(font, SECONDARY_EFFECT_LABEL, 169, 10, 14737632);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (CLOSE_BUTTON.contains(leftPos, topPos, mouseX, mouseY)) {
                onClose();
                return true;
            }
            if (CONFIRM_BUTTON.contains(leftPos, topPos, mouseX, mouseY)) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, ChampionBeaconMenu.CONFIRM_BUTTON_ID);
                return true;
            }
            if (infoOpen && INFO_CLOSE_BUTTON.contains(leftPos, topPos, mouseX, mouseY)) {
                infoOpen = false;
                resetInfoText();
                return true;
            }
            if (infoOpen && INFO_SCROLL_TRACK.contains(leftPos, topPos, mouseX, mouseY) && getMaxInfoScroll() > 0) {
                draggingInfoScroll = true;
                updateInfoScrollFromMouse(mouseY);
                return true;
            }
            if (INFO_BUTTON.contains(leftPos, topPos, mouseX, mouseY)) {
                infoOpen = true;
                return true;
            }
            if (UPGRADE_BUTTON.contains(leftPos, topPos, mouseX, mouseY)) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, ChampionBeaconMenu.UPGRADE_BUTTON_ID);
                return true;
            }
            for (PowerButton powerButton : PRIMARY_BUTTONS) {
                if (powerButton.area().contains(leftPos, topPos, mouseX, mouseY)) {
                    minecraft.gameMode.handleInventoryButtonClick(
                            menu.containerId,
                            ChampionBeaconMenu.PRIMARY_BUTTON_OFFSET + powerButton.power.id()
                    );
                    return true;
                }
            }
            for (PowerButton powerButton : SECONDARY_BUTTONS) {
                if (powerButton.area().contains(leftPos, topPos, mouseX, mouseY)) {
                    minecraft.gameMode.handleInventoryButtonClick(
                            menu.containerId,
                            ChampionBeaconMenu.SECONDARY_BUTTON_OFFSET + powerButton.power.id()
                    );
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && draggingInfoScroll) {
            draggingInfoScroll = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && draggingInfoScroll) {
            updateInfoScrollFromMouse(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (infoOpen && INFO_WINDOW.contains(leftPos, topPos, mouseX, mouseY) && getMaxInfoScroll() > 0) {
            infoScrollLine = clamp(infoScrollLine - (int) Math.signum(scrollY), 0, getMaxInfoScroll());
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void drawPowerButton(GuiGraphics guiGraphics, PowerButton button, int mouseX, int mouseY) {
        boolean enabled = button.primary ? menu.canSelectPrimary(button.power) : menu.canSelectSecondary(button.power);
        boolean selected = button.primary ? menu.getPrimaryPower() == button.power : menu.getSecondaryPower() == button.power;
        drawButton(guiGraphics, button.area(), enabled, selected, mouseX, mouseY);
        drawCobblemonItem(guiGraphics, button.iconItem, button.x + 3, button.y + 3);
    }

    private void drawUpgradeButton(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        drawButton(guiGraphics, UPGRADE_BUTTON, menu.canUpgradePrimary(), menu.isUpgraded(), mouseX, mouseY);
        drawCobblemonItem(guiGraphics, "relic_coin", UPGRADE_BUTTON.x + 3, UPGRADE_BUTTON.y + 3);
    }

    private void drawControlButtons(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        boolean closeHovered = CLOSE_BUTTON.contains(leftPos, topPos, mouseX, mouseY);
        blitButton(guiGraphics, closeHovered ? BUTTON_CLOSE_HOVER : BUTTON_UNLOCKED, CLOSE_BUTTON);
        blitButton(guiGraphics, CLOSE, CLOSE_BUTTON);

        boolean confirmHovered = CONFIRM_BUTTON.contains(leftPos, topPos, mouseX, mouseY);
        boolean confirmEnabled = menu.hasPayment() && menu.getPrimaryPower() != ChampionBeaconPower.NONE;
        ResourceLocation confirmBackground = confirmEnabled && confirmHovered ? BUTTON_CHECK_ACTIVE : confirmEnabled ? BUTTON_UNLOCKED : BUTTON_LOCKED;
        blitButton(guiGraphics, confirmBackground, CONFIRM_BUTTON);
        blitButton(guiGraphics, CHECK, CONFIRM_BUTTON);
    }

    private void drawInfo(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (!infoOpen) {
            guiGraphics.blit(
                    INFO_BUTTON_TEXTURE,
                    leftPos + INFO_BUTTON.x,
                    topPos + INFO_BUTTON.y,
                    0,
                    0,
                    INFO_BUTTON_WIDTH,
                    INFO_BUTTON_HEIGHT,
                    INFO_BUTTON_WIDTH,
                    INFO_BUTTON_HEIGHT
            );
            return;
        }

        updateInfoText(mouseX, mouseY);
        guiGraphics.blit(
                INFO_WINDOW_TEXTURE,
                leftPos + INFO_WINDOW.x,
                topPos + INFO_WINDOW.y,
                0,
                0,
                INFO_WINDOW_WIDTH,
                INFO_WINDOW_HEIGHT,
                INFO_WINDOW_WIDTH,
                INFO_WINDOW_HEIGHT
        );
        guiGraphics.blit(
                INFO_WINDOW_CLOSE_BUTTON_TEXTURE,
                leftPos + INFO_CLOSE_BUTTON.x,
                topPos + INFO_CLOSE_BUTTON.y,
                0,
                0,
                INFO_CLOSE_BUTTON_WIDTH,
                INFO_CLOSE_BUTTON_HEIGHT,
                INFO_CLOSE_BUTTON_WIDTH,
                INFO_CLOSE_BUTTON_HEIGHT
        );
        drawCobblemonItem(guiGraphics, infoIcon, INFO_ICON_BOX.x, INFO_ICON_BOX.y);

        int titleX = leftPos + INFO_TITLE_BOX.x;
        int titleY = topPos + INFO_TITLE_BOX.y;
        int textX = leftPos + INFO_TEXT_BOX.x;
        int textY = topPos + INFO_TEXT_BOX.y;
        guiGraphics.enableScissor(titleX, titleY, titleX + INFO_TITLE_BOX.width, titleY + INFO_TITLE_BOX.height);
        guiGraphics.drawString(font, infoTitle, titleX, titleY, 0xFFFFFF, true);
        guiGraphics.disableScissor();

        guiGraphics.enableScissor(textX, textY, textX + INFO_TEXT_BOX.width, textY + INFO_TEXT_BOX.height);
        drawWordWrapWithShadow(guiGraphics, infoText, textX, textY, INFO_TEXT_BOX.width, INFO_TEXT_BOX.height, infoScrollLine);
        guiGraphics.disableScissor();
        drawInfoScrollThumb(guiGraphics);
    }

    private void updateInfoText(int mouseX, int mouseY) {
        for (PowerButton button : PRIMARY_BUTTONS) {
            if (button.area().contains(leftPos, topPos, mouseX, mouseY)) {
                setInfoText(describePowerTitle(button.power), describePower(button.power, true), button.iconItem);
                return;
            }
        }

        for (PowerButton button : SECONDARY_BUTTONS) {
            if (button.area().contains(leftPos, topPos, mouseX, mouseY)) {
                setInfoText(describePowerTitle(button.power), describePower(button.power, false), button.iconItem);
                return;
            }
        }

        if (UPGRADE_BUTTON.contains(leftPos, topPos, mouseX, mouseY)) {
            setInfoText(
                    Component.literal("Upgrade"),
                    Component.literal("Upgrade boosts the selected primary aura. Only Apricorn, Berry, Daycare and EV auras can be upgraded."),
                    "relic_coin"
            );
        }
    }

    private void resetInfoText() {
        setInfoText(DEFAULT_INFO_TITLE, DEFAULT_INFO_TEXT, DEFAULT_INFO_ICON);
    }

    private void setInfoText(Component title, Component text, String icon) {
        if (!infoTitle.equals(title) || !infoText.equals(text) || !infoIcon.equals(icon)) {
            infoTitle = title;
            infoText = text;
            infoIcon = icon;
            infoScrollLine = 0;
            draggingInfoScroll = false;
        }
    }

    private void drawWordWrapWithShadow(GuiGraphics guiGraphics, Component text, int x, int y, int width, int height, int scrollLine) {
        List<FormattedCharSequence> lines = splitLines(text, width);
        int visibleLines = Math.max(1, height / font.lineHeight);
        int startLine = clamp(scrollLine, 0, Math.max(0, lines.size() - visibleLines));
        int lineY = y;
        for (int i = startLine; i < lines.size(); i++) {
            if (lineY + font.lineHeight > y + height) {
                break;
            }
            guiGraphics.drawString(font, lines.get(i), x, lineY, 0xFFFFFF, true);
            lineY += font.lineHeight;
        }
    }

    private List<FormattedCharSequence> splitLines(Component text, int width) {
        List<FormattedCharSequence> lines = new ArrayList<>();
        String[] paragraphs = text.getString().split("\\n", -1);
        for (String paragraph : paragraphs) {
            if (paragraph.isEmpty()) {
                lines.add(FormattedCharSequence.EMPTY);
            } else {
                lines.addAll(font.split(Component.literal(paragraph), width));
            }
        }
        return lines;
    }

    private void drawInfoScrollThumb(GuiGraphics guiGraphics) {
        int maxScroll = getMaxInfoScroll();
        int thumbX = leftPos + INFO_SCROLL_TRACK.x;
        int thumbY = topPos + getScrollThumbY(maxScroll);
        guiGraphics.blit(
                INFO_SCROLL_WHEEL_TEXTURE,
                thumbX,
                thumbY,
                0,
                0,
                INFO_SCROLL_THUMB_WIDTH,
                INFO_SCROLL_THUMB_HEIGHT,
                INFO_SCROLL_THUMB_WIDTH,
                INFO_SCROLL_THUMB_HEIGHT
        );
    }

    private int getMaxInfoScroll() {
        int visibleLines = Math.max(1, INFO_TEXT_BOX.height / font.lineHeight);
        return Math.max(0, splitLines(infoText, INFO_TEXT_BOX.width).size() - visibleLines);
    }

    private int getScrollThumbY(int maxScroll) {
        int range = INFO_SCROLL_TRACK.height - INFO_SCROLL_THUMB_HEIGHT;
        if (maxScroll <= 0 || range <= 0) {
            return INFO_SCROLL_TRACK.y;
        }
        return INFO_SCROLL_TRACK.y + Math.round(range * (infoScrollLine / (float) maxScroll));
    }

    private void updateInfoScrollFromMouse(double mouseY) {
        int maxScroll = getMaxInfoScroll();
        int range = INFO_SCROLL_TRACK.height - INFO_SCROLL_THUMB_HEIGHT;
        if (maxScroll <= 0 || range <= 0) {
            infoScrollLine = 0;
            return;
        }

        double relative = mouseY - (topPos + INFO_SCROLL_TRACK.y) - INFO_SCROLL_THUMB_HEIGHT / 2.0D;
        infoScrollLine = clamp(Math.round((float) (relative / range * maxScroll)), 0, maxScroll);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private Component describePowerTitle(ChampionBeaconPower power) {
        return switch (power) {
            case REPEL -> Component.literal("Repel Aura");
            case LURE -> Component.literal("Lure Aura");
            case APRICORN -> Component.literal("Apricorn Aura");
            case BERRY -> Component.literal("Berry Aura");
            case DAYCARE -> Component.literal("Daycare Aura");
            case EV -> Component.literal("EV Aura");
            case SHINY -> Component.literal("Shiny Aura");
            case NONE -> DEFAULT_INFO_TITLE;
        };
    }

    private Component describePower(ChampionBeaconPower power, boolean primary) {
        return switch (power) {
            case REPEL -> Component.literal(primary
                    ? "Repel Aura prevents wild Pokemon from spawning inside the beacon radius."
                    : "Secondary Repel gives spawn protection while your primary aura handles another job.");
            case LURE -> Component.literal(primary
                    ? "Lure Aura increases wild Pokemon spawn activity inside the beacon radius."
                    : "Secondary Lure adds extra spawn activity while your primary aura stays active.");
            case APRICORN -> Component.literal("Apricorn Aura speeds up apricorn growth inside the beacon radius. The upgrade option makes the growth boost stronger.");
            case BERRY -> Component.literal("Berry Aura speeds up Cobblemon berry plant growth inside the beacon radius. The upgrade option makes the growth boost stronger.");
            case DAYCARE -> Component.literal("Daycare Aura grants a passive 60 XP evenly dispersed to Pokemon in pasture blocks inside the beacon radius. Upgraded version gives 120 XP.");
            case EV -> Component.literal("EV Aura slowly grants selected EVs to Pokemon in pasture blocks withing beacon radius.\nEV type can be chosed by stone type used to ignite beacon.\nFire Stone / Shiny Stone: Attack\nWater Stone / Leaf Stone: HP\nThunder Stone: Speed\nDawn Stone / Sun Stone: Special Attack\nIce Stone / Dusk Stone: Defense\nMoon Stone: Special Defense");
            case SHINY -> Component.literal("Shiny Aura adds a small extra shiny chance for wild Pokemon that spawn inside the beacon radius.");
            case NONE -> DEFAULT_INFO_TEXT;
        };
    }

    private void drawButton(GuiGraphics guiGraphics, ButtonArea area, boolean enabled, boolean selected, int mouseX, int mouseY) {
        ResourceLocation texture = BUTTON_LOCKED;
        if (enabled) {
            texture = selected ? BUTTON_SELECTED : area.contains(leftPos, topPos, mouseX, mouseY) ? BUTTON_HOVERED : BUTTON_UNLOCKED;
        }
        blitButton(guiGraphics, texture, area);
    }

    private void blitButton(GuiGraphics guiGraphics, ResourceLocation texture, ButtonArea area) {
        guiGraphics.blit(texture, leftPos + area.x, topPos + area.y, 0, 0, BUTTON_SIZE, BUTTON_SIZE, BUTTON_SIZE, BUTTON_SIZE);
    }

    private void drawPaymentIcons(GuiGraphics guiGraphics) {
        for (ItemIcon icon : PAYMENT_ICONS) {
            drawCobblemonItem(guiGraphics, icon.itemName, icon.x, icon.y);
        }
    }

    private void drawCobblemonItem(GuiGraphics guiGraphics, String itemName, int x, int y) {
        ItemStack stack = BuiltInRegistries.ITEM
                .get(ResourceLocation.fromNamespaceAndPath("cobblemon", itemName))
                .getDefaultInstance();
        guiGraphics.renderItem(stack, leftPos + x, topPos + y);
    }

    private record ButtonArea(int x, int y, int width, int height) {
        ButtonArea(int x, int y) {
            this(x, y, BUTTON_SIZE, BUTTON_SIZE);
        }

        boolean contains(int left, int top, double mouseX, double mouseY) {
            return mouseX >= left + x
                    && mouseX < left + x + width
                    && mouseY >= top + y
                    && mouseY < top + y + height;
        }
    }

    private record PowerButton(ChampionBeaconPower power, int x, int y, boolean primary, String iconItem) {
        ButtonArea area() {
            return new ButtonArea(x, y);
        }
    }

    private record ItemIcon(String itemName, int x, int y) {
    }
}
