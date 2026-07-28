package com.nore.cobblebash.beacon;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.util.FastColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class ChampionBeaconRenderer implements BlockEntityRenderer<ChampionBeaconBlockEntity> {
    private static final int BEAM_COLOR = 0xFFFFFFFF;

    public ChampionBeaconRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(
            ChampionBeaconBlockEntity blockEntity,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        if (!blockEntity.hasBeam() || blockEntity.getLevel() == null) {
            return;
        }

        long gameTime = blockEntity.getLevel().getGameTime();
        int yOffset = 0;
        List<BeamSection> sections = collectBeamSections(blockEntity.getLevel(), blockEntity.getBlockPos());
        for (int index = 0; index < sections.size(); index++) {
            BeamSection section = sections.get(index);
            int height = index == sections.size() - 1 ? BeaconRenderer.MAX_RENDER_Y : section.height;
            BeaconRenderer.renderBeaconBeam(
                    poseStack,
                    bufferSource,
                    BeaconRenderer.BEAM_LOCATION,
                    partialTick,
                    1.0F,
                    gameTime,
                    yOffset,
                    height,
                    section.color,
                    0.2F,
                    0.25F
            );
            yOffset += section.height;
        }
    }

    private static List<BeamSection> collectBeamSections(Level level, BlockPos beaconPos) {
        List<BeamSection> sections = new ArrayList<>();
        BeamSection current = new BeamSection(BEAM_COLOR);
        sections.add(current);

        int topY = level.getHeight();
        for (BlockPos scan = beaconPos.above(); scan.getY() < topY; scan = scan.above()) {
            BlockState state = level.getBlockState(scan);
            Integer color = state.getBeaconColorMultiplier(level, scan, beaconPos);
            if (color != null) {
                if (current.color == BEAM_COLOR && current.height == 1) {
                    current.color = color;
                } else if (current.color == color) {
                    current.height++;
                } else {
                    current = new BeamSection(FastColor.ARGB32.average(current.color, color));
                    sections.add(current);
                }
            } else if (state.getLightBlock(level, scan) >= 15 && !state.is(Blocks.BEDROCK)) {
                return List.of();
            } else {
                current.height++;
            }
        }
        return sections;
    }

    @Override
    public boolean shouldRenderOffScreen(ChampionBeaconBlockEntity blockEntity) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public boolean shouldRender(ChampionBeaconBlockEntity blockEntity, Vec3 cameraPos) {
        return Vec3.atCenterOf(blockEntity.getBlockPos())
                .multiply(1.0, 0.0, 1.0)
                .closerThan(cameraPos.multiply(1.0, 0.0, 1.0), getViewDistance());
    }

    @Override
    public AABB getRenderBoundingBox(ChampionBeaconBlockEntity blockEntity) {
        var pos = blockEntity.getBlockPos();
        return new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1.0, BeaconRenderer.MAX_RENDER_Y, pos.getZ() + 1.0);
    }

    private static class BeamSection {
        private int color;
        private int height = 1;

        private BeamSection(int color) {
            this.color = color;
        }
    }
}
