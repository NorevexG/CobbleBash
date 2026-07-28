package com.nore.cobblebash.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.nore.cobblebash.CobbleBash;
import com.nore.cobblebash.entity.GymTrainerEntity;
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
import net.minecraft.util.Mth;

public class GymTrainerFemaleV2Model extends EntityModel<GymTrainerEntity> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(CobbleBash.MODID, "gym_trainer_female_v2"),
            "main"
    );

    private final ModelPart root;
    private final ModelPart bodyRoot;
    private final ModelPart waist;
    private final ModelPart head;
    private final ModelPart rightArm;
    private final ModelPart leftArm;

    public GymTrainerFemaleV2Model(ModelPart root) {
        this.root = root;
        this.bodyRoot = root.getChild("Entity");
        this.waist = this.bodyRoot.getChild("Waist");
        this.head = this.waist.getChild("Head");
        this.rightArm = this.waist.getChild("Right_Arm");
        this.leftArm = this.waist.getChild("Left_Arm");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshDefinition = new MeshDefinition();
        PartDefinition rootDefinition = meshDefinition.getRoot();

        PartDefinition Entity = rootDefinition.addOrReplaceChild("Entity", CubeListBuilder.create(), PartPose.offset(1.9F, 12.0F, 0.0F));

        PartDefinition Leggs = Entity.addOrReplaceChild("Leggs", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));

        PartDefinition Left_Leg = Leggs.addOrReplaceChild("Left_Leg", CubeListBuilder.create().texOffs(0, 48).addBox(-2.0F, 1.8F, -2.0F, 4.0F, 1.0F, 4.0F, new CubeDeformation(0.1F))
        .texOffs(0, 53).addBox(-2.0F, 4.9F, -2.0F, 4.0F, 1.0F, 4.0F, new CubeDeformation(0.05F))
        .texOffs(0, 32).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

        PartDefinition Shoe_Left = Left_Leg.addOrReplaceChild("Shoe_Left", CubeListBuilder.create().texOffs(0, 123).addBox(-2.0F, -2.0F, -1.0F, 4.0F, 1.0F, 4.0F, new CubeDeformation(0.15F))
        .texOffs(0, 116).addBox(-2.0F, 1.0F, -2.0F, 4.0F, 2.0F, 5.0F, new CubeDeformation(0.1F))
        .texOffs(12, 125).addBox(-1.0F, 0.0F, -1.2F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 9.0F, -1.0F));

        PartDefinition Right_Leg = Leggs.addOrReplaceChild("Right_Leg", CubeListBuilder.create().texOffs(0, 58).addBox(-2.0F, 1.8F, -2.0F, 4.0F, 1.0F, 4.0F, new CubeDeformation(0.1F))
        .texOffs(0, 63).addBox(-2.0F, 4.9F, -2.0F, 4.0F, 1.0F, 4.0F, new CubeDeformation(0.05F))
        .texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(-3.8F, 0.0F, 0.0F));

        PartDefinition Shoe_Right = Right_Leg.addOrReplaceChild("Shoe_Right", CubeListBuilder.create().texOffs(0, 123).addBox(-1.8F, -2.0F, -1.0F, 4.0F, 1.0F, 4.0F, new CubeDeformation(0.1F))
        .texOffs(0, 116).addBox(-1.8F, 1.0F, -2.0F, 4.0F, 2.0F, 5.0F, new CubeDeformation(0.1F))
        .texOffs(12, 125).addBox(-0.8F, 0.0F, -1.2F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(-0.2F, 9.0F, -1.0F));

        PartDefinition Waist = Entity.addOrReplaceChild("Waist", CubeListBuilder.create(), PartPose.offset(-1.9F, 0.0F, 0.0F));

        PartDefinition Head = Waist.addOrReplaceChild("Head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -12.0F, 0.0F));

        PartDefinition PonyTail_Hair = Head.addOrReplaceChild("PonyTail_Hair", CubeListBuilder.create().texOffs(16, 48).addBox(-5.5F, -8.5F, -4.5F, 9.0F, 9.0F, 9.0F, new CubeDeformation(0.0F)), PartPose.offset(1.0F, 0.0F, 0.0F));

        PartDefinition PonyTail = PonyTail_Hair.addOrReplaceChild("PonyTail", CubeListBuilder.create().texOffs(6, 81).addBox(-4.0F, 0.0F, -1.0F, 2.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
        .texOffs(4, 84).addBox(-4.0F, -1.0F, 0.0F, 2.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
        .texOffs(0, 73).addBox(-5.0F, 0.0F, 0.0F, 4.0F, 6.0F, 2.0F, new CubeDeformation(0.0F))
        .texOffs(2, 68).addBox(-4.0F, 6.0F, 0.0F, 3.0F, 3.0F, 2.0F, new CubeDeformation(0.0F))
        .texOffs(0, 81).addBox(-4.0F, 9.0F, 1.0F, 2.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(2.0F, -8.0F, 5.0F));

        PartDefinition Tie = PonyTail.addOrReplaceChild("Tie", CubeListBuilder.create().texOffs(2, 84).addBox(0.5F, -1.6F, 0.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
        .texOffs(2, 84).addBox(0.5F, 0.6F, 0.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
        .texOffs(2, 84).addBox(-0.7F, 0.15F, 0.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
        .texOffs(2, 84).addBox(-0.7F, -1.15F, 0.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
        .texOffs(2, 84).addBox(1.7F, -1.15F, 0.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
        .texOffs(2, 84).addBox(1.7F, 0.15F, 0.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(-4.0F, 1.0F, -1.0F));

        PartDefinition Bangs = PonyTail_Hair.addOrReplaceChild("Bangs", CubeListBuilder.create().texOffs(39, 14).addBox(2.5F, -27.5F, -3.5F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
        .texOffs(45, 14).addBox(7.5F, -27.5F, -3.5F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
        .texOffs(39, 11).addBox(6.5F, -27.5F, -5.5F, 4.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
        .texOffs(39, 8).addBox(1.5F, -27.5F, -5.5F, 4.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
        .texOffs(33, 13).addBox(8.5F, -26.5F, -5.5F, 2.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
        .texOffs(36, 10).addBox(9.5F, -24.5F, -5.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
        .texOffs(32, 7).addBox(9.5F, -26.5F, -4.5F, 1.0F, 6.0F, 1.0F, new CubeDeformation(0.0F))
        .texOffs(40, 5).addBox(1.5F, -26.5F, -5.5F, 2.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
        .texOffs(46, 5).addBox(1.5F, -24.5F, -5.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
        .texOffs(36, 3).addBox(1.5F, -26.5F, -4.5F, 1.0F, 6.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(-7.0F, 19.0F, 1.0F));

        PartDefinition Body = Waist.addOrReplaceChild("Body", CubeListBuilder.create().texOffs(16, 16).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.0F))
        .texOffs(30, 32).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.15F)), PartPose.offset(0.0F, -12.0F, 0.0F));

        PartDefinition Belt = Body.addOrReplaceChild("Belt", CubeListBuilder.create().texOffs(0, 111).addBox(-4.0F, -2.0F, 0.0F, 8.0F, 1.0F, 4.0F, new CubeDeformation(0.25F))
        .texOffs(16, 124).addBox(-1.0F, -2.5F, -0.5F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 12.0F, -2.0F));

        PartDefinition Badge = Body.addOrReplaceChild("Badge", CubeListBuilder.create().texOffs(0, 5).addBox(-0.85F, -1.3F, -0.1F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.2F))
        .texOffs(0, 3).addBox(-0.85F, -1.3F, -0.4F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(3.0F, 4.0F, -2.0F));

        PartDefinition Right_Arm = Waist.addOrReplaceChild("Right_Arm", CubeListBuilder.create().texOffs(0, 105).addBox(-2.05F, 5.25F, -2.0F, 3.0F, 2.0F, 4.0F, new CubeDeformation(0.1F))
        .texOffs(40, 16).addBox(-2.0F, -2.0F, -2.0F, 3.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(-5.0F, -10.0F, 0.0F));

        PartDefinition Left_Arm = Waist.addOrReplaceChild("Left_Arm", CubeListBuilder.create().texOffs(0, 105).addBox(-0.95F, 5.25F, -2.0F, 3.0F, 2.0F, 4.0F, new CubeDeformation(0.1F))
        .texOffs(16, 32).addBox(-1.0F, -2.0F, -2.0F, 3.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(5.0F, -10.0F, 0.0F));

        return LayerDefinition.create(meshDefinition, 128, 128);
    }

    @Override
    public void setupAnim(GymTrainerEntity trainer, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        float slow = ageInTicks * 0.055F;
        float fast = ageInTicks * 0.09F;

        this.bodyRoot.xRot = 0.0F;
        this.bodyRoot.yRot = 0.0F;
        this.bodyRoot.zRot = 0.0F;
        this.waist.xRot = 0.0F;
        this.waist.yRot = 0.0F;
        this.waist.zRot = 0.0F;
        this.head.xRot = Mth.clamp(headPitch, -18.0F, 18.0F) * Mth.DEG_TO_RAD + Mth.sin(fast) * 0.012F;
        this.head.yRot = Mth.clamp(netHeadYaw, -45.0F, 45.0F) * Mth.DEG_TO_RAD;
        this.head.zRot = Mth.sin(slow + 2.0F) * 0.01F;
        this.rightArm.xRot = Mth.sin(slow + 0.3F) * 0.02F;
        this.leftArm.xRot = -Mth.sin(slow + 0.3F) * 0.02F;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int color) {
        this.root.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
    }
}
