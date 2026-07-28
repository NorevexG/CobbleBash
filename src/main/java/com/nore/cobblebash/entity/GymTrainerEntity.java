package com.nore.cobblebash.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class GymTrainerEntity extends PathfinderMob {
    private static final EntityDataAccessor<Integer> DATA_MODEL_VARIANT =
            SynchedEntityData.defineId(GymTrainerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_TEXTURE_VARIANT =
            SynchedEntityData.defineId(GymTrainerEntity.class, EntityDataSerializers.INT);
    private static final String MODEL_VARIANT_TAG = "CobbleBashTrainerModel";
    private static final String TEXTURE_VARIANT_TAG = "CobbleBashTrainerTexture";
    public static final int MODEL_VARIANT_COUNT = 4;
    public static final int TEXTURE_VARIANT_COUNT = 16;

    public GymTrainerEntity(EntityType<? extends GymTrainerEntity> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_MODEL_VARIANT, 0);
        builder.define(DATA_TEXTURE_VARIANT, 0);
    }

    public int modelVariant() {
        return Mth.clamp(this.entityData.get(DATA_MODEL_VARIANT), 0, modelVariantCount() - 1);
    }

    public int textureVariant() {
        return Math.floorMod(this.entityData.get(DATA_TEXTURE_VARIANT), TEXTURE_VARIANT_COUNT);
    }

    public void setVisual(int modelVariant, int textureVariant) {
        this.entityData.set(DATA_MODEL_VARIANT, Math.floorMod(modelVariant, modelVariantCount()));
        this.entityData.set(DATA_TEXTURE_VARIANT, Math.floorMod(textureVariant, TEXTURE_VARIANT_COUNT));
    }

    protected int modelVariantCount() {
        return MODEL_VARIANT_COUNT;
    }

    @Override
    public void tick() {
        super.tick();
        this.setDeltaMovement(0.0D, this.getDeltaMovement().y, 0.0D);

        if (!this.level().isClientSide) {
            faceNearestPlayer();
        }
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public void push(Entity entity) {
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt(MODEL_VARIANT_TAG, modelVariant());
        compound.putInt(TEXTURE_VARIANT_TAG, textureVariant());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        setVisual(compound.getInt(MODEL_VARIANT_TAG), compound.getInt(TEXTURE_VARIANT_TAG));
    }

    private void faceNearestPlayer() {
        Player player = this.level().getNearestPlayer(this, 24.0D);
        if (player == null) {
            return;
        }

        double dx = player.getX() - this.getX();
        double dz = player.getZ() - this.getZ();
        double dy = player.getEyeY() - this.getEyeY();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
        float pitch = (float) (-(Mth.atan2(dy, horizontal) * Mth.RAD_TO_DEG));

        this.setYRot(yaw);
        this.setYBodyRot(yaw);
        this.setYHeadRot(yaw);
        this.setXRot(Mth.clamp(pitch, -18.0F, 18.0F));
    }
}
