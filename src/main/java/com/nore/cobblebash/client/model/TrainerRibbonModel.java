package com.nore.cobblebash.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.nore.cobblebash.CobbleBash;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

public class TrainerRibbonModel<T extends Entity> extends EntityModel<T> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(CobbleBash.MODID, "trainer_ribbon"),
            "main"
    );

    private final ModelPart trainerRibbon;

    public TrainerRibbonModel(ModelPart root) {
        this.trainerRibbon = root.getChild("trainer_ribbon");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshDefinition = new MeshDefinition();
        PartDefinition root = meshDefinition.getRoot();

        PartDefinition trainerRibbon = root.addOrReplaceChild(
                "trainer_ribbon",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-2.0F, -2.0F, -0.45F, 4.0F, 4.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(10, 6).addBox(-1.0F, -1.0F, -0.47F, 2.0F, 2.0F, 0.0F, new CubeDeformation(0.0F)),
                PartPose.offset(4.35F, 6.8F, -7.0F)
        );

        trainerRibbon.addOrReplaceChild(
                "ribbon_right_r1",
                CubeListBuilder.create()
                        .texOffs(5, 6).addBox(-2.0F, 0.0F, 0.0F, 2.0F, 4.0F, 0.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.25F, 2.0F, 0.05F, 0.0F, 0.0F, 0.3927F)
        );

        trainerRibbon.addOrReplaceChild(
                "ribbon_left_r1",
                CubeListBuilder.create()
                        .texOffs(0, 6).addBox(0.0F, 0.0F, 0.0F, 2.0F, 4.0F, 0.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-0.25F, 2.0F, 0.05F, 0.0F, 0.0F, -0.3927F)
        );

        return LayerDefinition.create(meshDefinition, 16, 16);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int color) {
        trainerRibbon.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
    }
}
