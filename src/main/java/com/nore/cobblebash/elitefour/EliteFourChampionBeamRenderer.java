package com.nore.cobblebash.elitefour;

import com.mojang.blaze3d.vertex.PoseStack;
import com.nore.cobblebash.structure.EliteFourStructure;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class EliteFourChampionBeamRenderer implements BlockEntityRenderer<EliteFourChampionBeamBlockEntity> {
    private static final int BEAM_COLOR = 0xFF102080;

    public EliteFourChampionBeamRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(
            EliteFourChampionBeamBlockEntity blockEntity,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        if (blockEntity.getLevel() == null) {
            return;
        }

        BeaconRenderer.renderBeaconBeam(
                poseStack,
                bufferSource,
                BeaconRenderer.BEAM_LOCATION,
                partialTick,
                1.0F,
                blockEntity.getLevel().getGameTime(),
                0,
                EliteFourStructure.CHAMPION_BEAM_HEIGHT,
                BEAM_COLOR,
                0.2F,
                0.25F
        );
    }

    @Override
    public boolean shouldRenderOffScreen(EliteFourChampionBeamBlockEntity blockEntity) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public boolean shouldRender(EliteFourChampionBeamBlockEntity blockEntity, Vec3 cameraPos) {
        return Vec3.atCenterOf(blockEntity.getBlockPos())
                .multiply(1.0, 0.0, 1.0)
                .closerThan(cameraPos.multiply(1.0, 0.0, 1.0), getViewDistance());
    }

    @Override
    public AABB getRenderBoundingBox(EliteFourChampionBeamBlockEntity blockEntity) {
        var pos = blockEntity.getBlockPos();
        return new AABB(
                pos.getX(),
                pos.getY(),
                pos.getZ(),
                pos.getX() + 1.0D,
                pos.getY() + EliteFourStructure.CHAMPION_BEAM_HEIGHT,
                pos.getZ() + 1.0D
        );
    }
}
