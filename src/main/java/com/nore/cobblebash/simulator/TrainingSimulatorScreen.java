package com.nore.cobblebash.simulator;

import com.nore.cobblebash.CobbleBash;
import com.nore.cobblebash.elitefour.EliteFourMember;
import com.nore.cobblebash.gym.GymType;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class TrainingSimulatorScreen extends AbstractContainerScreen<TrainingSimulatorMenu> {
    private static final int PANEL_WIDTH = 320;
    private static final int PANEL_HEIGHT = 222;
    private static final int GRID_TOP = 58;
    private static final int CARD_WIDTH = 88;
    private static final int CARD_HEIGHT = 40;
    private static final int CARD_GAP = 5;
    private static final int ROW_HEIGHT = CARD_HEIGHT + CARD_GAP;
    private static final int COLUMNS = 3;
    private static final int VISIBLE_ROWS = 3;
    private static final int SCROLL_TRACK_X = 304;
    private static final int SCROLL_TRACK_HEIGHT = 130;
    private static final int SCROLL_THUMB_HEIGHT = 58;
    private static final double WHEEL_SCROLL_PIXELS = 24.0D;
    private static final double SCROLL_RESPONSE = 18.0D;
    private static final SoundEvent GUI_CLICK = SoundEvent.createVariableRangeEvent(
            ResourceLocation.fromNamespaceAndPath("cobblemon", "gui.click")
    );
    private static final SoundEvent PC_UNLOCK = SoundEvent.createVariableRangeEvent(
            ResourceLocation.fromNamespaceAndPath("cobblemon", "pc.wallpaper.unlock")
    );

    private Tab tab = Tab.GYMS;
    private double scrollOffset;
    private double targetScrollOffset;
    private long lastRenderNanos;
    private int pendingChallenge = -1;
    private boolean openingSoundPlayed;
    private boolean draggingScroll;
    private int observedUnlockRevision;

    public TrainingSimulatorScreen(TrainingSimulatorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = PANEL_WIDTH;
        imageHeight = PANEL_HEIGHT;
        titleLabelY = 10000;
        inventoryLabelY = 10000;
    }

    @Override
    protected void init() {
        super.init();
        observedUnlockRevision = menu.getUnlockRevision();
        if (!openingSoundPlayed) {
            openingSoundPlayed = true;
            playGuiClick();
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (menu.getUnlockRevision() != observedUnlockRevision) {
            observedUnlockRevision = menu.getUnlockRevision();
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(PC_UNLOCK, 1.0F));
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        updateSmoothScroll();
        drawPanel(graphics);
        drawTabs(graphics, mouseX, mouseY);
        if (tab == Tab.GYMS) {
            drawGymGrid(graphics, mouseX, mouseY);
        } else {
            drawEliteFour(graphics, mouseX, mouseY);
        }
        drawStatusBar(graphics);
        if (pendingChallenge >= 0) {
            drawConfirmation(graphics, mouseX, mouseY);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, Component.translatable("screen.cobblebash.simulator.title"), 18, 13, 0xEAFBFF, false);
        graphics.drawString(font, Component.translatable("screen.cobblebash.simulator.subtitle"), 18, 25, 0x69CDE8, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        int x = (int) mouseX - leftPos;
        int y = (int) mouseY - topPos;
        if (pendingChallenge >= 0) {
            if (inside(x, y, 93, 155, 64, 20)) {
                playGuiClick();
                pendingChallenge = -1;
                return true;
            }
            if (inside(x, y, 163, 155, 64, 20) && canChallenge(pendingChallenge)) {
                playGuiClick();
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, pendingChallenge);
                pendingChallenge = -1;
                return true;
            }
            return true;
        }

        if (inside(x, y, 68, 39, 82, 15)) {
            playGuiClick();
            tab = Tab.GYMS;
            return true;
        }
        if (inside(x, y, 158, 39, 94, 15)) {
            playGuiClick();
            tab = Tab.ELITE_FOUR;
            return true;
        }

        if (tab == Tab.GYMS) {
            if (inside(x, y, SCROLL_TRACK_X - 2, GRID_TOP, 8, SCROLL_TRACK_HEIGHT)) {
                draggingScroll = true;
                updateScrollFromMouse(y);
                playGuiClick();
                return true;
            }
            for (int index = 0; index < GymType.values().length; index++) {
                int cardX = 16 + (index % COLUMNS) * (CARD_WIDTH + CARD_GAP);
                int cardY = GRID_TOP + (index / COLUMNS) * ROW_HEIGHT - (int) Math.round(scrollOffset);
                if (inside(x, y, 16, GRID_TOP, 277, SCROLL_TRACK_HEIGHT)
                        && inside(x, y, cardX, cardY, CARD_WIDTH, CARD_HEIGHT)) {
                    playGuiClick();
                    if (menu.isUnlocked(index)) {
                        pendingChallenge = index;
                    } else if (menu.getDiskCount(index) > 0) {
                        requestUnlock(index);
                    }
                    return true;
                }
            }
        } else if (inside(x, y, 43, 67, 234, 83)) {
            playGuiClick();
            int challenge = TrainingSimulatorMenu.ELITE_FOUR_BUTTON_ID;
            if (menu.isUnlocked(challenge)) {
                pendingChallenge = challenge;
            } else if (menu.getCompletedCount() >= GymType.values().length
                    && menu.getDiskCount(challenge) > 0) {
                requestUnlock(challenge);
            }
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (tab == Tab.GYMS && pendingChallenge < 0) {
            double nextOffset = Mth.clamp(
                    targetScrollOffset - scrollY * WHEEL_SCROLL_PIXELS,
                    0.0D,
                    maxScrollPixels()
            );
            if (Math.abs(nextOffset - targetScrollOffset) > 0.001D) {
                targetScrollOffset = nextOffset;
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && draggingScroll && tab == Tab.GYMS && pendingChallenge < 0) {
            updateScrollFromMouse((int) mouseY - topPos);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && draggingScroll) {
            draggingScroll = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (pendingChallenge >= 0 && keyCode == 256) {
            pendingChallenge = -1;
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void drawPanel(GuiGraphics graphics) {
        graphics.fill(leftPos, topPos, leftPos + PANEL_WIDTH, topPos + PANEL_HEIGHT, 0xF20A1420);
        graphics.fill(leftPos + 2, topPos + 2, leftPos + PANEL_WIDTH - 2, topPos + 34, 0xFF102E43);
        graphics.fill(leftPos + 2, topPos + 34, leftPos + PANEL_WIDTH - 2, topPos + 36, 0xFF21B9D6);
        outline(graphics, 0, 0, PANEL_WIDTH, PANEL_HEIGHT, 0xFF55E6FF);
        outline(graphics, 4, 4, PANEL_WIDTH - 8, PANEL_HEIGHT - 8, 0x8046AFC4);
        graphics.fill(leftPos + 8, topPos + 12, leftPos + 12, topPos + 28, 0xFF48E4FF);
    }

    private void drawTabs(GuiGraphics graphics, int mouseX, int mouseY) {
        drawTab(graphics, 68, 39, 82, Component.translatable("screen.cobblebash.simulator.tab.gyms"), tab == Tab.GYMS, mouseX, mouseY);
        drawTab(graphics, 158, 39, 94, Component.translatable("screen.cobblebash.simulator.tab.elite_four"), tab == Tab.ELITE_FOUR, mouseX, mouseY);
    }

    private void drawTab(GuiGraphics graphics, int x, int y, int width, Component label, boolean selected, int mouseX, int mouseY) {
        boolean hovered = inside(mouseX - leftPos, mouseY - topPos, x, y, width, 15);
        int color = selected ? 0xFF167E9C : hovered ? 0xFF174B61 : 0xFF102B3C;
        graphics.fill(leftPos + x, topPos + y, leftPos + x + width, topPos + y + 15, color);
        outline(graphics, x, y, width, 15, selected ? 0xFF5AE9FF : 0xFF28677A);
        graphics.drawCenteredString(font, label, leftPos + x + width / 2, topPos + y + 3, selected ? 0xFFFFFFFF : 0xFF9ECBD5);
    }

    private void drawGymGrid(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.enableScissor(
                leftPos + 16,
                topPos + GRID_TOP,
                leftPos + 293,
                topPos + GRID_TOP + SCROLL_TRACK_HEIGHT
        );
        for (int index = 0; index < GymType.values().length; index++) {
            int x = 16 + (index % COLUMNS) * (CARD_WIDTH + CARD_GAP);
            int y = GRID_TOP + (index / COLUMNS) * ROW_HEIGHT - (int) Math.round(scrollOffset);
            if (y + CARD_HEIGHT < GRID_TOP || y > GRID_TOP + SCROLL_TRACK_HEIGHT) {
                continue;
            }
            drawGymCard(graphics, index, x, y, mouseX, mouseY);
        }
        graphics.disableScissor();

        graphics.fill(leftPos + SCROLL_TRACK_X, topPos + GRID_TOP, leftPos + SCROLL_TRACK_X + 4, topPos + GRID_TOP + SCROLL_TRACK_HEIGHT, 0xFF173543);
        int thumbY = getScrollThumbY();
        graphics.fill(leftPos + SCROLL_TRACK_X, topPos + thumbY, leftPos + SCROLL_TRACK_X + 4, topPos + thumbY + SCROLL_THUMB_HEIGHT, 0xFF50DDF5);
    }

    private void drawGymCard(GuiGraphics graphics, int index, int x, int y, int mouseX, int mouseY) {
        GymType type = GymType.values()[index];
        boolean completed = menu.hasCompleted(index);
        boolean unlocked = menu.isUnlocked(index);
        boolean available = unlocked || menu.getDiskCount(index) > 0;
        boolean hovered = inside(mouseX - leftPos, mouseY - topPos, x, y, CARD_WIDTH, CARD_HEIGHT);
        int accent = typeColor(type);
        graphics.fill(leftPos + x, topPos + y, leftPos + x + CARD_WIDTH, topPos + y + CARD_HEIGHT, hovered ? 0xFF173A4C : 0xFF102735);
        graphics.fill(leftPos + x, topPos + y, leftPos + x + 4, topPos + y + CARD_HEIGHT, accent);
        outline(graphics, x, y, CARD_WIDTH, CARD_HEIGHT, hovered ? 0xFF73EBFF : 0xFF285D70);

        ItemStack disk = new ItemStack(CobbleBash.TRAINING_DISKS.get(type).get());
        graphics.renderItem(disk, leftPos + x + 6, topPos + y + 4);
        graphics.drawString(font, Component.translatable("screen.cobblebash.simulator.gym_name." + type.getId()), leftPos + x + 26, topPos + y + 5, 0xFFF2FAFC, false);
        graphics.drawString(font, Component.translatable("screen.cobblebash.simulator.gym"), leftPos + x + 26, topPos + y + 16, 0xFF8ABCC8, false);
        Component state = completed
                ? Component.translatable("screen.cobblebash.simulator.completed").withStyle(ChatFormatting.GREEN)
                : unlocked
                ? Component.translatable("screen.cobblebash.simulator.unlocked").withStyle(ChatFormatting.GREEN)
                : available
                ? Component.translatable("screen.cobblebash.simulator.can_unlock").withStyle(ChatFormatting.AQUA)
                : Component.translatable("screen.cobblebash.simulator.no_disc").withStyle(ChatFormatting.RED);
        graphics.drawString(font, state, leftPos + x + 6, topPos + y + 28, 0xFFFFFFFF, false);
        Component clears = Component.translatable(
                "screen.cobblebash.simulator.clear_count",
                menu.getGymClearCount(index)
        );
        graphics.drawString(
                font,
                clears,
                leftPos + x + CARD_WIDTH - 5 - font.width(clears),
                topPos + y + 28,
                0xFFD0EAF0,
                false
        );
    }

    private void drawEliteFour(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = 43;
        int y = 67;
        boolean unlocked = menu.getCompletedCount() >= GymType.values().length;
        boolean challengeUnlocked = menu.isUnlocked(TrainingSimulatorMenu.ELITE_FOUR_BUTTON_ID);
        boolean hovered = inside(mouseX - leftPos, mouseY - topPos, x, y, 234, 83);
        graphics.fill(leftPos + x, topPos + y, leftPos + x + 234, topPos + y + 83, hovered ? 0xFF263B55 : 0xFF172A41);
        graphics.fill(leftPos + x, topPos + y, leftPos + x + 5, topPos + y + 83, unlocked ? 0xFFFFC247 : 0xFF526372);
        outline(graphics, x, y, 234, 83, hovered ? 0xFFFFD36A : 0xFF536F87);
        graphics.renderItem(new ItemStack(CobbleBash.ELITE_FOUR_TRAINING_DISK.get()), leftPos + x + 14, topPos + y + 14);
        graphics.drawString(font, Component.translatable("screen.cobblebash.simulator.elite_title"), leftPos + x + 39, topPos + y + 12, 0xFFFFE7A4, false);
        int memberX = x + 14;
        int memberY = y + 34;
        for (int index = 0; index < EliteFourMember.ordered().size(); index++) {
            EliteFourMember member = EliteFourMember.ordered().get(index);
            int column = index % 2;
            int row = index / 2;
            graphics.drawString(
                    font,
                    Component.translatable("screen.cobblebash.simulator.elite_member." + member.getId()),
                    leftPos + memberX + column * 106,
                    topPos + memberY + row * 11,
                    0xFFBBD0DD,
                    false
            );
        }
        Component state = !unlocked
                ? Component.translatable("screen.cobblebash.simulator.requires_gyms", menu.getCompletedCount(), GymType.values().length).withStyle(ChatFormatting.RED)
                : challengeUnlocked
                ? Component.translatable("screen.cobblebash.simulator.unlocked").withStyle(ChatFormatting.GREEN)
                : menu.getDiskCount(TrainingSimulatorMenu.ELITE_FOUR_BUTTON_ID) > 0
                ? Component.translatable("screen.cobblebash.simulator.can_unlock").withStyle(ChatFormatting.AQUA)
                : Component.translatable("screen.cobblebash.simulator.no_elite_disc").withStyle(ChatFormatting.RED);
        graphics.drawString(font, state, leftPos + x + 14, topPos + y + 62, 0xFFFFFFFF, false);
    }

    private void drawStatusBar(GuiGraphics graphics) {
        graphics.fill(leftPos + 16, topPos + 190, leftPos + 304, topPos + 216, 0xFF0C202D);
        outline(graphics, 16, 190, 288, 26, 0xFF245D70);
        graphics.drawString(font, Component.translatable("screen.cobblebash.simulator.completed_count", menu.getCompletedCount(), GymType.values().length), leftPos + 24, topPos + 195, 0xFF8FE8F5, false);
        graphics.drawString(font, Component.translatable("screen.cobblebash.simulator.challenge_count", menu.getChallengesStarted()), leftPos + 160, topPos + 195, 0xFF8FE8F5, false);
        Component active = menu.hasActiveChallenge()
                ? Component.translatable("screen.cobblebash.simulator.active").withStyle(ChatFormatting.YELLOW)
                : Component.translatable("screen.cobblebash.simulator.standby").withStyle(ChatFormatting.GRAY);
        graphics.drawString(font, active, leftPos + 24, topPos + 206, 0xFFFFFFFF, false);
    }

    private void drawConfirmation(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 300.0F);
        graphics.fill(leftPos + 1, topPos + 36, leftPos + PANEL_WIDTH - 1, topPos + PANEL_HEIGHT - 1, 0xD900080D);
        int x = 59;
        int y = 78;
        int width = 202;
        int height = 104;
        graphics.fill(leftPos + x, topPos + y, leftPos + x + width, topPos + y + height, 0xFF102A3A);
        outline(graphics, x, y, width, height, 0xFF56E3FA);
        graphics.drawCenteredString(font, Component.translatable("screen.cobblebash.simulator.confirm_title"), leftPos + x + width / 2, topPos + y + 13, 0xFFFFFFFF);

        Component target = pendingChallenge == TrainingSimulatorMenu.ELITE_FOUR_BUTTON_ID
                ? Component.translatable("screen.cobblebash.simulator.elite_title")
                : Component.translatable(
                        "screen.cobblebash.simulator.gym_target",
                        Component.translatable("screen.cobblebash.simulator.gym_name." + GymType.values()[pendingChallenge].getId())
                );
        graphics.drawCenteredString(font, target, leftPos + x + width / 2, topPos + y + 34, 0xFF8DEEFF);
        graphics.drawCenteredString(font, Component.translatable("screen.cobblebash.simulator.confirm_enter"), leftPos + x + width / 2, topPos + y + 49, 0xFFC5D7DC);

        drawDialogButton(graphics, 93, 155, 64, Component.translatable("gui.cancel"), true, mouseX, mouseY);
        drawDialogButton(graphics, 163, 155, 64, Component.translatable("screen.cobblebash.simulator.enter"), canChallenge(pendingChallenge), mouseX, mouseY);
        graphics.pose().popPose();
    }

    private void drawDialogButton(GuiGraphics graphics, int x, int y, int width, Component label, boolean enabled, int mouseX, int mouseY) {
        boolean hovered = enabled && inside(mouseX - leftPos, mouseY - topPos, x, y, width, 20);
        int color = !enabled ? 0xFF27323A : hovered ? 0xFF1A9DBB : 0xFF17637A;
        graphics.fill(leftPos + x, topPos + y, leftPos + x + width, topPos + y + 20, color);
        outline(graphics, x, y, width, 20, enabled ? 0xFF69E9FF : 0xFF48545A);
        graphics.drawCenteredString(font, label, leftPos + x + width / 2, topPos + y + 6, enabled ? 0xFFFFFFFF : 0xFF77848A);
    }

    private boolean canChallenge(int challenge) {
        if (challenge == TrainingSimulatorMenu.ELITE_FOUR_BUTTON_ID) {
            return menu.getCompletedCount() >= GymType.values().length
                    && menu.isUnlocked(challenge);
        }
        return challenge >= 0 && challenge < GymType.values().length
                && menu.isUnlocked(challenge);
    }

    private int maxScrollPixels() {
        int totalRows = (GymType.values().length + COLUMNS - 1) / COLUMNS;
        return Math.max(0, (totalRows - VISIBLE_ROWS) * ROW_HEIGHT);
    }

    private int getScrollThumbY() {
        if (maxScrollPixels() == 0) {
            return GRID_TOP;
        }
        return GRID_TOP + Math.round(
                (float) ((SCROLL_TRACK_HEIGHT - SCROLL_THUMB_HEIGHT) * scrollOffset / maxScrollPixels())
        );
    }

    private void updateScrollFromMouse(int mouseY) {
        int travel = SCROLL_TRACK_HEIGHT - SCROLL_THUMB_HEIGHT;
        int thumbTop = Mth.clamp(mouseY - GRID_TOP - SCROLL_THUMB_HEIGHT / 2, 0, travel);
        double offset = Mth.clamp(thumbTop * maxScrollPixels() / (double) travel, 0.0D, maxScrollPixels());
        scrollOffset = offset;
        targetScrollOffset = offset;
    }

    private void updateSmoothScroll() {
        long now = System.nanoTime();
        if (lastRenderNanos == 0L) {
            lastRenderNanos = now;
            return;
        }
        double elapsedSeconds = Math.min(0.05D, (now - lastRenderNanos) / 1_000_000_000.0D);
        lastRenderNanos = now;
        if (draggingScroll) {
            return;
        }
        double blend = 1.0D - Math.exp(-SCROLL_RESPONSE * elapsedSeconds);
        scrollOffset += (targetScrollOffset - scrollOffset) * blend;
        if (Math.abs(targetScrollOffset - scrollOffset) < 0.05D) {
            scrollOffset = targetScrollOffset;
        }
    }

    private void playGuiClick() {
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(GUI_CLICK, 1.0F));
    }

    private void requestUnlock(int challenge) {
        minecraft.gameMode.handleInventoryButtonClick(
                menu.containerId,
                TrainingSimulatorMenu.UNLOCK_BUTTON_OFFSET + challenge
        );
    }

    private void outline(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        graphics.fill(leftPos + x, topPos + y, leftPos + x + width, topPos + y + 1, color);
        graphics.fill(leftPos + x, topPos + y + height - 1, leftPos + x + width, topPos + y + height, color);
        graphics.fill(leftPos + x, topPos + y, leftPos + x + 1, topPos + y + height, color);
        graphics.fill(leftPos + x + width - 1, topPos + y, leftPos + x + width, topPos + y + height, color);
    }

    private static boolean inside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private static int typeColor(GymType type) {
        return switch (type.getId()) {
            case "fire" -> 0xFFE65B45;
            case "water" -> 0xFF4AA8E8;
            case "grass" -> 0xFF58B85B;
            case "electric" -> 0xFFF2C84B;
            case "psychic", "fairy" -> 0xFFE77DB5;
            case "poison", "ghost" -> 0xFF9B65C7;
            case "ice", "flying" -> 0xFF82D7E8;
            case "rock", "ground" -> 0xFFB78B58;
            case "dragon" -> 0xFF6677D8;
            case "dark" -> 0xFF59616E;
            case "steel", "normal" -> 0xFFA7B2B8;
            case "bug" -> 0xFF93B83F;
            case "fighting" -> 0xFFC35B4D;
            default -> 0xFF5BD4E8;
        };
    }

    private enum Tab {
        GYMS,
        ELITE_FOUR
    }
}
