package com.nore.cobblebash.client.render;

import com.nore.cobblebash.CobbleBash;
import com.nore.cobblebash.client.model.GymTrainerFemaleV1Model;
import com.nore.cobblebash.client.model.GymTrainerFemaleV2Model;
import com.nore.cobblebash.client.model.GymTrainerMaleV1Model;
import com.nore.cobblebash.client.model.GymTrainerMaleV2Model;
import com.nore.cobblebash.client.model.GymTrainerOperativeModel;
import com.nore.cobblebash.entity.GymTrainerEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class GymTrainerRenderer extends MobRenderer<GymTrainerEntity, GymTrainerOperativeModel> {
    private static final String[] VARIANT_PREFIXES = {"male_v1", "male_v2", "female_v1", "female_v2"};
    private static final ResourceLocation[][] TEXTURES = createTextures();

    public GymTrainerRenderer(EntityRendererProvider.Context context) {
        super(
                context,
                new GymTrainerOperativeModel(
                        context.bakeLayer(GymTrainerMaleV1Model.LAYER_LOCATION),
                        context.bakeLayer(GymTrainerMaleV2Model.LAYER_LOCATION),
                        context.bakeLayer(GymTrainerFemaleV1Model.LAYER_LOCATION),
                        context.bakeLayer(GymTrainerFemaleV2Model.LAYER_LOCATION)
                ),
                0.35F
        );
    }

    @Override
    public ResourceLocation getTextureLocation(GymTrainerEntity trainer) {
        return TEXTURES[trainer.modelVariant()][trainer.textureVariant()];
    }

    private static ResourceLocation[][] createTextures() {
        ResourceLocation[][] textures = new ResourceLocation[VARIANT_PREFIXES.length][GymTrainerEntity.TEXTURE_VARIANT_COUNT];

        for (int model = 0; model < VARIANT_PREFIXES.length; model++) {
            for (int texture = 0; texture < GymTrainerEntity.TEXTURE_VARIANT_COUNT; texture++) {
                int skinTone = texture / 8 + 1;
                int hairVariant = texture % 8 + 1;
                textures[model][texture] = ResourceLocation.fromNamespaceAndPath(
                        CobbleBash.MODID,
                        "textures/entity/gym_trainer/" + VARIANT_PREFIXES[model] + "_" + skinTone + "_" + hairVariant + ".png"
                );
            }
        }

        return textures;
    }
}
