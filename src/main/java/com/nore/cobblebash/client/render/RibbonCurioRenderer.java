package com.nore.cobblebash.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

public class RibbonCurioRenderer implements ICurioRenderer {
    private final EntityModel<LivingEntity> model;
    private final ResourceLocation texture;

    @SuppressWarnings("unchecked")
    public RibbonCurioRenderer(EntityModel<? extends LivingEntity> model, ResourceLocation texture) {
        this.model = (EntityModel<LivingEntity>) model;
        this.texture = texture;
    }

    @Override
    public <T extends LivingEntity, M extends EntityModel<T>> void render(
            ItemStack stack,
            SlotContext slotContext,
            PoseStack poseStack,
            RenderLayerParent<T, M> renderLayerParent,
            MultiBufferSource buffer,
            int packedLight,
            float limbSwing,
            float limbSwingAmount,
            float partialTicks,
            float ageInTicks,
            float netHeadYaw,
            float headPitch
    ) {
        LivingEntity entity = slotContext.entity();
        poseStack.pushPose();
        ICurioRenderer.translateIfSneaking(poseStack, entity);
        ICurioRenderer.rotateIfSneaking(poseStack, entity);
        if (renderLayerParent.getModel() instanceof HumanoidModel<?> humanoidModel) {
            humanoidModel.body.translateAndRotate(poseStack);
        }
        if (!entity.getItemBySlot(EquipmentSlot.CHEST).isEmpty()) {
            poseStack.translate(0.0D, 0.0D, -0.045D);
        } else {
            poseStack.translate(0.0D, 0.0D, 0.035D);
        }
        poseStack.scale(0.375F, 0.375F, 0.375F);
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityCutoutNoCull(texture));
        model.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        model.renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }
}
