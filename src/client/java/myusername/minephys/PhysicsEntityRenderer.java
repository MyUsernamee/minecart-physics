package myusername.minephys;

import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory.Context;
import net.minecraft.client.render.entity.model.MinecartEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;

import org.jetbrains.annotations.ApiStatus.OverrideOnly;
import org.joml.*;

import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.debug.DebugRenderer;

public class PhysicsEntityRenderer extends EntityRenderer<PhysicsEntity> {

    MinecartEntityModel<PhysicsEntity> model;

    public PhysicsEntityRenderer(Context context) {
        super(context);
        model = new MinecartEntityModel<PhysicsEntity>(context.getPart(MinecartphysicsClient.CUBE_LAYER));
    }

    @Override
    public Identifier getTexture(PhysicsEntity entity) {
        // TODO Auto-generated method stub
        return Identifier.of("minecraft", "textures/entity/minecart.png");
    }

    /**
     * {@return the packed overlay color for an entity} It is determined by the
     * entity's death progress and whether the entity is flashing.
     */
    public static int getOverlay() {
        return OverlayTexture.packUv(OverlayTexture.getU(0),
                OverlayTexture.getV(false));
    }

    @Override
    public void render(PhysicsEntity entity, float yaw, float tickDelta, MatrixStack matrices,
            VertexConsumerProvider vertexConsumers, int light) {

        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
        matrices.push();

        Quaternionf q = entity.getDataTracker().get(PhysicsEntity.orientation);
        matrices.multiply(q);
        model.render(matrices, vertexConsumers.getBuffer(model.getLayer(getTexture(entity))), light, getOverlay(),
                654311423);

        // DebugRenderer.drawBox(matrices, vertexConsumers, new Box(-0.5, -.5, -0.5,
        // 0.5, 0.5, 0.5), 1, 1, 1, 1);
        matrices.pop();

    }

}
