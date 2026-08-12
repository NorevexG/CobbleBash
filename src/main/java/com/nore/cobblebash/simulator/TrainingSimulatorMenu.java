package com.nore.cobblebash.simulator;

import com.nore.cobblebash.CobbleBash;
import com.nore.cobblebash.command.GymCommand;
import com.nore.cobblebash.gym.GymType;
import com.nore.cobblebash.progress.GymProgressManager;
import com.nore.cobblebash.progress.PlayerGymProgress;
import com.nore.cobblebash.stats.CobbleBashStats;
import com.nore.cobblebash.progress.TrainingSimulatorUnlockData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class TrainingSimulatorMenu extends AbstractContainerMenu {
    public static final int ELITE_FOUR_BUTTON_ID = GymType.values().length;
    public static final int UNLOCK_BUTTON_OFFSET = 100;

    private final ContainerLevelAccess access;
    private final Inventory inventory;
    private final int[] diskCounts = new int[GymType.values().length + 1];
    private final int[] gymClearCounts = new int[GymType.values().length];
    private int completedMask;
    private int unlockMask;
    private int challengesStarted;
    private int activeChallenge;
    private int unlockRevision;

    public TrainingSimulatorMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, ContainerLevelAccess.NULL);
    }

    public TrainingSimulatorMenu(int containerId, Inventory inventory, ContainerLevelAccess access) {
        super(CobbleBash.TRAINING_SIMULATOR_MENU.get(), containerId);
        this.inventory = inventory;
        this.access = access;

        for (int index = 0; index < diskCounts.length; index++) {
            final int dataIndex = index;
            addDataSlot(new DataSlot() {
                @Override
                public int get() {
                    return countDisk(dataIndex);
                }

                @Override
                public void set(int value) {
                    diskCounts[dataIndex] = value;
                }
            });
        }

        for (int index = 0; index < gymClearCounts.length; index++) {
            final int gymIndex = index;
            addDataSlot(syncedValue(
                    () -> getServerGymClearCount(gymIndex),
                    value -> gymClearCounts[gymIndex] = value
            ));
        }

        addDataSlot(syncedValue(() -> getServerCompletedMask() & 0xFFFF, value ->
                completedMask = (completedMask & 0xFFFF0000) | (value & 0xFFFF)));
        addDataSlot(syncedValue(() -> (getServerCompletedMask() >>> 16) & 0xFFFF, value ->
                completedMask = (completedMask & 0xFFFF) | ((value & 0xFFFF) << 16)));
        addDataSlot(syncedValue(() -> getServerUnlockMask() & 0xFFFF, value ->
                unlockMask = (unlockMask & 0xFFFF0000) | (value & 0xFFFF)));
        addDataSlot(syncedValue(() -> (getServerUnlockMask() >>> 16) & 0xFFFF, value ->
                unlockMask = (unlockMask & 0xFFFF) | ((value & 0xFFFF) << 16)));
        addDataSlot(syncedValue(() -> getServerChallengesStarted() & 0xFFFF, value ->
                challengesStarted = (challengesStarted & 0xFFFF0000) | (value & 0xFFFF)));
        addDataSlot(syncedValue(() -> (getServerChallengesStarted() >>> 16) & 0xFFFF, value ->
                challengesStarted = (challengesStarted & 0xFFFF) | ((value & 0xFFFF) << 16)));
        addDataSlot(syncedValue(this::getServerActiveChallenge, value -> activeChallenge = value));
        addDataSlot(syncedValue(() -> unlockRevision, value -> unlockRevision = value));
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!(player instanceof ServerPlayer serverPlayer) || !stillValid(player)) {
            return false;
        }

        if (id >= UNLOCK_BUTTON_OFFSET
                && id <= UNLOCK_BUTTON_OFFSET + ELITE_FOUR_BUTTON_ID) {
            return unlockChallenge(serverPlayer, id - UNLOCK_BUTTON_OFFSET);
        }

        if (id >= 0 && id < GymType.values().length) {
            if (!isChallengeUnlocked(serverPlayer, id)) {
                return false;
            }
            GymType type = GymType.values()[id];
            return GymCommand.enterGym(serverPlayer, type.getId());
        }

        if (id == ELITE_FOUR_BUTTON_ID) {
            PlayerGymProgress progress = GymProgressManager.get(serverPlayer.getUUID());
            if (progress.getCompletedGymCount() < GymType.values().length
                    || !isChallengeUnlocked(serverPlayer, id)) {
                return false;
            }
            return GymCommand.enterEliteFour(serverPlayer, false);
        }

        return false;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, CobbleBash.TRAINING_SIMULATOR.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    public int getDiskCount(int index) {
        return index >= 0 && index < diskCounts.length ? diskCounts[index] : 0;
    }

    public boolean hasCompleted(int gymIndex) {
        return gymIndex >= 0 && gymIndex < GymType.values().length && (completedMask & (1 << gymIndex)) != 0;
    }

    public int getGymClearCount(int gymIndex) {
        return gymIndex >= 0 && gymIndex < gymClearCounts.length ? gymClearCounts[gymIndex] : 0;
    }

    public boolean isUnlocked(int challengeIndex) {
        return challengeIndex >= 0 && challengeIndex < diskCounts.length
                && (unlockMask & (1 << challengeIndex)) != 0;
    }

    public int getCompletedCount() {
        return Integer.bitCount(completedMask);
    }

    public int getChallengesStarted() {
        return challengesStarted;
    }

    public boolean hasActiveChallenge() {
        return activeChallenge != 0;
    }

    public int getUnlockRevision() {
        return unlockRevision;
    }

    private int countDisk(int index) {
        if (inventory.player.level().isClientSide) {
            return diskCounts[index];
        }
        Item item = index < GymType.values().length
                ? CobbleBash.TRAINING_DISKS.get(GymType.values()[index]).get()
                : CobbleBash.ELITE_FOUR_TRAINING_DISK.get();
        int count = 0;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(item)) {
                count += stack.getCount();
            }
        }
        return inventory.player.getAbilities().instabuild ? Math.max(1, count) : count;
    }

    private boolean hasDisk(Item item) {
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(item) && !stack.isEmpty()) {
                return true;
            }
        }
        return inventory.player.getAbilities().instabuild;
    }

    private boolean isChallengeUnlocked(ServerPlayer player, int challengeIndex) {
        return TrainingSimulatorUnlockData.get(player.server).isUnlocked(player.getUUID(), challengeIndex);
    }

    private boolean unlockChallenge(ServerPlayer player, int challengeIndex) {
        if (challengeIndex < 0 || challengeIndex > ELITE_FOUR_BUTTON_ID
                || isChallengeUnlocked(player, challengeIndex)) {
            return false;
        }

        if (challengeIndex == ELITE_FOUR_BUTTON_ID
                && GymProgressManager.get(player.getUUID()).getCompletedGymCount() < GymType.values().length) {
            return false;
        }

        Item disk = challengeIndex < GymType.values().length
                ? CobbleBash.TRAINING_DISKS.get(GymType.values()[challengeIndex]).get()
                : CobbleBash.ELITE_FOUR_TRAINING_DISK.get();
        if (!hasDisk(disk)) {
            return false;
        }

        if (!player.getAbilities().instabuild) {
            consumeDisk(player, disk);
        }
        if (!TrainingSimulatorUnlockData.get(player.server).unlock(player.getUUID(), challengeIndex)) {
            return false;
        }
        unlockRevision++;
        broadcastChanges();
        return true;
    }

    private void consumeDisk(ServerPlayer player, Item item) {
        if (player.getAbilities().instabuild) {
            return;
        }
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(item) && !stack.isEmpty()) {
                stack.shrink(1);
                inventory.setChanged();
                return;
            }
        }
    }

    private int getServerCompletedMask() {
        if (inventory.player.level().isClientSide) {
            return completedMask;
        }
        PlayerGymProgress progress = GymProgressManager.get(inventory.player.getUUID());
        int mask = 0;
        for (int index = 0; index < GymType.values().length; index++) {
            if (progress.hasCompleted(GymType.values()[index].getId())) {
                mask |= 1 << index;
            }
        }
        return mask;
    }

    private int getServerGymClearCount(int gymIndex) {
        if (inventory.player.level().isClientSide) {
            return gymClearCounts[gymIndex];
        }
        if (!(inventory.player instanceof ServerPlayer serverPlayer)) {
            return 0;
        }
        int count = TrainingSimulatorUnlockData.get(serverPlayer.server).getClearCount(
                serverPlayer.getUUID(),
                GymType.values()[gymIndex].getId()
        );
        return Math.min(Short.MAX_VALUE, count);
    }

    private int getServerChallengesStarted() {
        if (inventory.player.level().isClientSide) {
            return challengesStarted;
        }
        return inventory.player instanceof ServerPlayer serverPlayer
                ? serverPlayer.getStats().getValue(Stats.CUSTOM.get(CobbleBashStats.CHALLENGES_STARTED.get()))
                : challengesStarted;
    }

    private int getServerUnlockMask() {
        if (inventory.player.level().isClientSide) {
            return unlockMask;
        }
        if (!(inventory.player instanceof ServerPlayer serverPlayer)) {
            return unlockMask;
        }
        TrainingSimulatorUnlockData unlocks = TrainingSimulatorUnlockData.get(serverPlayer.server);
        PlayerGymProgress progress = GymProgressManager.get(serverPlayer.getUUID());
        for (int index = 0; index < GymType.values().length; index++) {
            if (progress.hasCompleted(GymType.values()[index].getId())) {
                unlocks.unlock(serverPlayer.getUUID(), index);
            }
        }
        return unlocks.getUnlockMask(serverPlayer.getUUID());
    }

    private int getServerActiveChallenge() {
        if (inventory.player.level().isClientSide) {
            return activeChallenge;
        }
        return "none".equals(GymProgressManager.get(inventory.player.getUUID()).getActiveGymType()) ? 0 : 1;
    }

    private static DataSlot syncedValue(IntGetter getter, IntSetter setter) {
        return new DataSlot() {
            @Override
            public int get() {
                return getter.get();
            }

            @Override
            public void set(int value) {
                setter.set(value);
            }
        };
    }

    @FunctionalInterface
    private interface IntGetter {
        int get();
    }

    @FunctionalInterface
    private interface IntSetter {
        void set(int value);
    }
}
