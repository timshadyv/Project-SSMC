package com.sovereignstate.client.renderer;

import com.sovereignstate.entity.GuardEntity;
import net.minecraft.client.render.entity.BipedEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.util.Identifier;

public class GuardEntityRenderer extends BipedEntityRenderer<GuardEntity, BipedEntityModel<GuardEntity>> {

    private static final Identifier TEXTURE =
            new Identifier("sovereignstate", "textures/entity/guard.png");

    public GuardEntityRenderer(EntityRendererFactory.Context ctx) {
        super(ctx,
                new BipedEntityModel<>(ctx.getPart(EntityModelLayers.PLAYER_INNER_ARMOR)),
                0.5f);
    }

    @Override
    public Identifier getTexture(GuardEntity entity) {
        return TEXTURE;
    }
}