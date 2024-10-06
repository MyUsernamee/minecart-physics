package myusername.minephys;

import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory.Context;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;

import org.jetbrains.annotations.ApiStatus.OverrideOnly;
import org.joml.*;
import org.ode4j.math.*;

import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.debug.DebugRenderer;

public class PhysicsEntityRenderer extends EntityRenderer<PhysicsEntity> {

    CubeModel<PhysicsEntity> model;

    public PhysicsEntityRenderer(Context context) {
        super(context);
        model = new CubeModel<PhysicsEntity>(context.getPart(MinecartphysicsClient.CUBE_LAYER));
    }

    @Override
    public Identifier getTexture(PhysicsEntity entity) {
        // TODO Auto-generated method stub
        return Identifier.of("minecart-physics", "image.png");
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

        // Copy the transform matrix
        Matrix4d a = new Matrix4d();
        a.setColumn(0, new Vector4d(entity.getRx().x, entity.getRx().y, entity.getRx().z, 0.0f));
        a.setColumn(1, new Vector4d(entity.getRy().x, entity.getRy().y, entity.getRy().z, 0.0f));
        a.setColumn(2, new Vector4d(entity.getRz().x, entity.getRz().y, entity.getRz().z, 0.0f));
        // Strip the position
        a.setColumn(3, new Vector4d(0f, 0f, 0f, 1.0f));

        Quaternionf q = new Quaternionf();
        q.setFromUnnormalized(a);
        matrices.multiply(q);
        model.render(matrices, vertexConsumers.getBuffer(model.getLayer(getTexture(entity))), light, getOverlay(),
                654311423);

        // DebugRenderer.drawBox(matrices, vertexConsumers, new Box(-0.5, -.5, -0.5,
        // 0.5, 0.5, 0.5), 1, 1, 1, 1);
        matrices.pop();

    }

}