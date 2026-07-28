package com.nore.cobblebash.beacon;

import com.nore.cobblebash.CobbleBash;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ChampionBeaconMenu extends AbstractContainerMenu {
    public static final int PAYMENT_SLOT = 0;
    public static final int SLOT_COUNT = 1;
    public static final int DATA_LEVELS = 0;
    public static final int DATA_PRIMARY = 1;
    public static final int DATA_SECONDARY = 2;
    public static final int DATA_UPGRADED = 3;
    public static final int DATA_COUNT = 4;
    public static final int PRIMARY_BUTTON_OFFSET = 10;
    public static final int SECONDARY_BUTTON_OFFSET = 30;
    public static final int UPGRADE_BUTTON_ID = 50;
    public static final int CONFIRM_BUTTON_ID = 60;
    public static final TagKey<Item> PAYMENT_ITEMS = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("cobblemon", "evolution_stones")
    );

    private static final int INV_SLOT_START = 1;
    private static final int INV_SLOT_END = 28;
    private static final int HOTBAR_SLOT_START = 28;
    private static final int HOTBAR_SLOT_END = 37;

    private final Container paymentContainer = new SimpleContainer(SLOT_COUNT) {
        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return isPaymentItem(stack);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    };
    private final PaymentSlot paymentSlot;
    private final ContainerLevelAccess access;
    private final ChampionBeaconBlockEntity blockEntity;
    private int levels;
    private int primaryPower;
    private int secondaryPower;
    private int upgraded;

    public ChampionBeaconMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, ContainerLevelAccess.NULL, null);
    }

    public ChampionBeaconMenu(
            int containerId,
            Inventory inventory,
            ContainerLevelAccess access,
            ChampionBeaconBlockEntity blockEntity
    ) {
        super(CobbleBash.CHAMPION_BEACON_MENU.get(), containerId);
        this.access = access;
        this.blockEntity = blockEntity;
        if (blockEntity != null) {
            this.levels = blockEntity.getLevels();
            this.primaryPower = blockEntity.getPrimaryPower().id();
            this.secondaryPower = blockEntity.getSecondaryPower().id();
            this.upgraded = blockEntity.isUpgraded() ? 1 : 0;
        }

        this.paymentSlot = new PaymentSlot(paymentContainer, 0, 80, 133);
        addSlot(paymentSlot);
        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return ChampionBeaconMenu.this.levels;
            }

            @Override
            public void set(int value) {
                ChampionBeaconMenu.this.levels = value;
            }
        });
        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return ChampionBeaconMenu.this.primaryPower;
            }

            @Override
            public void set(int value) {
                ChampionBeaconMenu.this.primaryPower = value;
            }
        });
        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return ChampionBeaconMenu.this.secondaryPower;
            }

            @Override
            public void set(int value) {
                ChampionBeaconMenu.this.secondaryPower = value;
            }
        });
        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return ChampionBeaconMenu.this.upgraded;
            }

            @Override
            public void set(int value) {
                ChampionBeaconMenu.this.upgraded = value;
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 36 + column * 18, 160 + row * 18));
            }
        }

        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 36 + column * 18, 218));
        }
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id >= PRIMARY_BUTTON_OFFSET && id < PRIMARY_BUTTON_OFFSET + 20) {
            ChampionBeaconPower power = ChampionBeaconPower.byId(id - PRIMARY_BUTTON_OFFSET);
            if (canSelectPrimary(power)) {
                primaryPower = power.id();
                ChampionBeaconPower secondary = ChampionBeaconPower.byId(secondaryPower);
                if (secondary == power || conflicts(power, secondary)) {
                    secondaryPower = ChampionBeaconPower.NONE.id();
                }
                if (!power.isUpgradeable()) {
                    upgraded = 0;
                }
                broadcastChanges();
                return true;
            }
            return false;
        }

        if (id >= SECONDARY_BUTTON_OFFSET && id < SECONDARY_BUTTON_OFFSET + 20) {
            ChampionBeaconPower power = ChampionBeaconPower.byId(id - SECONDARY_BUTTON_OFFSET);
            if (canSelectSecondary(power)) {
                secondaryPower = power.id();
                upgraded = 0;
                broadcastChanges();
                return true;
            }
            return false;
        }

        if (id == UPGRADE_BUTTON_ID) {
            if (canUpgradePrimary()) {
                upgraded = upgraded == 0 ? 1 : 0;
                if (upgraded != 0) {
                    secondaryPower = ChampionBeaconPower.NONE.id();
                }
                broadcastChanges();
                return true;
            }
            return false;
        }

        if (id == CONFIRM_BUTTON_ID) {
            return confirmSelection(player);
        }

        return false;
    }

    private boolean confirmSelection(Player player) {
        ChampionBeaconPower primary = getPrimaryPower();
        ChampionBeaconPower secondary = getSecondaryPower();
        boolean hasUpgrade = isUpgraded();
        if (blockEntity == null || !paymentSlot.hasItem() || !canSelectPrimary(primary)) {
            return false;
        }
        if (secondary != ChampionBeaconPower.NONE && !canSelectSecondary(secondary)) {
            return false;
        }
        if (hasUpgrade && !canUpgradePrimary()) {
            return false;
        }

        ResourceLocation paymentItemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(paymentSlot.getItem().getItem());
        paymentSlot.remove(1);
        blockEntity.applyPowers(primary, secondary, hasUpgrade, paymentItemId);
        access.execute(Level::blockEntityChanged);
        broadcastChanges();
        return true;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide) {
            ItemStack stack = paymentSlot.remove(paymentSlot.getMaxStackSize());
            if (!stack.isEmpty()) {
                player.getInventory().placeItemBackInInventory(stack);
            }
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, CobbleBash.CHAMPION_BEACON.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack moved = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            moved = stack.copy();
            if (index == PAYMENT_SLOT) {
                if (!moveItemStackTo(stack, INV_SLOT_START, HOTBAR_SLOT_END, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(stack, moved);
            } else if (isPaymentItem(stack) && moveItemStackTo(stack, PAYMENT_SLOT, PAYMENT_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            } else if (index >= INV_SLOT_START && index < INV_SLOT_END) {
                if (!moveItemStackTo(stack, HOTBAR_SLOT_START, HOTBAR_SLOT_END, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index >= HOTBAR_SLOT_START && index < HOTBAR_SLOT_END) {
                if (!moveItemStackTo(stack, INV_SLOT_START, INV_SLOT_END, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, INV_SLOT_START, HOTBAR_SLOT_END, false)) {
                return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (stack.getCount() == moved.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, stack);
        }

        return moved;
    }

    public int getLevels() {
        return levels;
    }

    public ChampionBeaconPower getPrimaryPower() {
        return ChampionBeaconPower.byId(primaryPower);
    }

    public ChampionBeaconPower getSecondaryPower() {
        return ChampionBeaconPower.byId(secondaryPower);
    }

    public boolean isUpgraded() {
        return upgraded != 0;
    }

    public boolean hasPayment() {
        return paymentSlot.hasItem();
    }

    public boolean canSelectPrimary(ChampionBeaconPower power) {
        return power.isPrimary() && levels >= power.requiredLevel();
    }

    public boolean canSelectSecondary(ChampionBeaconPower power) {
        return power.isSecondary()
                && levels >= ChampionBeaconBlockEntity.MAX_LEVELS
                && power != getPrimaryPower()
                && !conflicts(getPrimaryPower(), power);
    }

    public boolean canUpgradePrimary() {
        return levels >= ChampionBeaconBlockEntity.MAX_LEVELS && getPrimaryPower().isUpgradeable();
    }

    private static boolean isPaymentItem(ItemStack stack) {
        return stack.is(PAYMENT_ITEMS);
    }

    private static boolean conflicts(ChampionBeaconPower primary, ChampionBeaconPower secondary) {
        return (primary == ChampionBeaconPower.REPEL && secondary == ChampionBeaconPower.LURE)
                || (primary == ChampionBeaconPower.LURE && secondary == ChampionBeaconPower.REPEL);
    }

    private static class PaymentSlot extends Slot {
        PaymentSlot(Container container, int containerIndex, int xPosition, int yPosition) {
            super(container, containerIndex, xPosition, yPosition);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return isPaymentItem(stack);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }
}
