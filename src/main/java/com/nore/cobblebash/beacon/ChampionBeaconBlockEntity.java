package com.nore.cobblebash.beacon;

import com.nore.cobblebash.CobbleBash;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class ChampionBeaconBlockEntity extends BlockEntity implements MenuProvider {
    public static final int MAX_LEVELS = 5;
    public static final int RADIUS_PER_LEVEL = 10;
    public static final int BASE_RADIUS = 10;
    public static final TagKey<net.minecraft.world.level.block.Block> BASE_BLOCKS = TagKey.create(
            Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath("cobblemon", "evolution_stone_blocks")
    );

    private static final Component DEFAULT_NAME = Component.translatable("container.cobblebash.champion_beacon");
    private static final String TAG_LEVELS = "Levels";
    private static final String TAG_BEAM_CLEAR = "BeamClear";
    private static final String TAG_PRIMARY = "PrimaryPower";
    private static final String TAG_SECONDARY = "SecondaryPower";
    private static final String TAG_UPGRADED = "Upgraded";
    private static final String TAG_PAYMENT_ITEM = "PaymentItem";

    private int levels;
    private boolean beamClear;
    private ChampionBeaconPower primaryPower = ChampionBeaconPower.NONE;
    private ChampionBeaconPower secondaryPower = ChampionBeaconPower.NONE;
    private boolean upgraded;
    private ResourceLocation paymentItem = ResourceLocation.fromNamespaceAndPath("cobblemon", "water_stone");

    public ChampionBeaconBlockEntity(BlockPos pos, BlockState blockState) {
        super(CobbleBash.CHAMPION_BEACON_BLOCK_ENTITY.get(), pos, blockState);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ChampionBeaconBlockEntity blockEntity) {
        if (!level.isClientSide) {
            if (level.getGameTime() % 80L == 0L) {
                blockEntity.refreshLevels();
                ChampionBeaconAuras.track(blockEntity);
                if (blockEntity.hasBeam()) {
                    blockEntity.playSound(SoundEvents.BEACON_AMBIENT);
                }
            }
            ChampionBeaconAuras.tickBeacon(blockEntity);
        }
    }

    public void refreshLevels() {
        if (level == null) {
            return;
        }

        int oldLevels = levels;
        boolean oldBeamClear = beamClear;
        levels = calculateBaseLevels(level, worldPosition);
        beamClear = levels > 0 && hasClearBeam(level, worldPosition);

        if (oldLevels != levels || oldBeamClear != beamClear) {
            if (oldLevels <= 0 && levels > 0 && beamClear) {
                playSound(SoundEvents.BEACON_ACTIVATE);
            } else if (oldLevels > 0 && (levels <= 0 || !beamClear)) {
                playSound(SoundEvents.BEACON_DEACTIVATE);
            }
            setChangedAndSync();
        }
    }

    private static int calculateBaseLevels(Level level, BlockPos pos) {
        int result = 0;
        for (int layer = 1; layer <= MAX_LEVELS; layer++) {
            int y = pos.getY() - layer;
            if (y < level.getMinBuildHeight()) {
                break;
            }

            boolean valid = true;
            for (int x = pos.getX() - layer; x <= pos.getX() + layer && valid; x++) {
                for (int z = pos.getZ() - layer; z <= pos.getZ() + layer; z++) {
                    if (!level.getBlockState(new BlockPos(x, y, z)).is(BASE_BLOCKS)) {
                        valid = false;
                        break;
                    }
                }
            }

            if (!valid) {
                break;
            }
            result = layer;
        }
        return result;
    }

    private static boolean hasClearBeam(Level level, BlockPos pos) {
        int surfaceY = level.getHeight();
        for (BlockPos scan = pos.above(); scan.getY() < surfaceY; scan = scan.above()) {
            BlockState state = level.getBlockState(scan);
            if (state.getBeaconColorMultiplier(level, scan, pos) == null
                    && state.getLightBlock(level, scan) >= 15
                    && !state.is(Blocks.BEDROCK)) {
                return false;
            }
        }
        return true;
    }

    public int getLevels() {
        return levels;
    }

    public int getRadius() {
        return levels <= 0 ? 0 : BASE_RADIUS + levels * RADIUS_PER_LEVEL;
    }

    public boolean hasBeam() {
        return levels > 0 && beamClear;
    }

    public ChampionBeaconPower getPrimaryPower() {
        return primaryPower;
    }

    public ChampionBeaconPower getSecondaryPower() {
        return secondaryPower;
    }

    public boolean isUpgraded() {
        return upgraded;
    }

    public void applyPowers(ChampionBeaconPower primaryPower, ChampionBeaconPower secondaryPower, boolean upgraded) {
        applyPowers(primaryPower, secondaryPower, upgraded, paymentItem);
    }

    public void applyPowers(ChampionBeaconPower primaryPower, ChampionBeaconPower secondaryPower, boolean upgraded, ResourceLocation paymentItem) {
        this.primaryPower = primaryPower.isPrimary() ? primaryPower : ChampionBeaconPower.NONE;
        this.secondaryPower = secondaryPower.isSecondary() && !conflicts(this.primaryPower, secondaryPower)
                ? secondaryPower
                : ChampionBeaconPower.NONE;
        this.upgraded = upgraded && this.primaryPower.isUpgradeable();
        this.paymentItem = paymentItem == null ? ResourceLocation.fromNamespaceAndPath("cobblemon", "water_stone") : paymentItem;
        playSound(SoundEvents.BEACON_POWER_SELECT);
        setChangedAndSync();
        ChampionBeaconAuras.track(this);
    }

    public ResourceLocation getPaymentItem() {
        return paymentItem;
    }

    private void playSound(net.minecraft.sounds.SoundEvent sound) {
        if (level != null) {
            level.playSound(null, worldPosition, sound, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }

    private void setChangedAndSync() {
        setChanged();
        if (level != null) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        levels = tag.getInt(TAG_LEVELS);
        beamClear = tag.getBoolean(TAG_BEAM_CLEAR);
        primaryPower = ChampionBeaconPower.byId(tag.getInt(TAG_PRIMARY));
        secondaryPower = ChampionBeaconPower.byId(tag.getInt(TAG_SECONDARY));
        if (conflicts(primaryPower, secondaryPower)) {
            secondaryPower = ChampionBeaconPower.NONE;
        }
        upgraded = tag.getBoolean(TAG_UPGRADED);
        if (tag.contains(TAG_PAYMENT_ITEM)) {
            paymentItem = ResourceLocation.tryParse(tag.getString(TAG_PAYMENT_ITEM));
            if (paymentItem == null) {
                paymentItem = ResourceLocation.fromNamespaceAndPath("cobblemon", "water_stone");
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt(TAG_LEVELS, levels);
        tag.putBoolean(TAG_BEAM_CLEAR, beamClear);
        tag.putInt(TAG_PRIMARY, primaryPower.id());
        tag.putInt(TAG_SECONDARY, secondaryPower.id());
        tag.putBoolean(TAG_UPGRADED, upgraded);
        tag.putString(TAG_PAYMENT_ITEM, paymentItem.toString());
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    @Override
    public Component getDisplayName() {
        return DEFAULT_NAME;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        refreshLevels();
        return new ChampionBeaconMenu(
                containerId,
                playerInventory,
                ContainerLevelAccess.create(level, worldPosition),
                this
        );
    }

    @Override
    public void setRemoved() {
        ChampionBeaconAuras.untrack(level, worldPosition);
        super.setRemoved();
    }

    private static boolean conflicts(ChampionBeaconPower primary, ChampionBeaconPower secondary) {
        return (primary == ChampionBeaconPower.REPEL && secondary == ChampionBeaconPower.LURE)
                || (primary == ChampionBeaconPower.LURE && secondary == ChampionBeaconPower.REPEL);
    }
}
