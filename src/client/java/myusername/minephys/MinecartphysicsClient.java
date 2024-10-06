package myusername.minephys;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.util.Identifier;
import myusername.minephys.PhysicsEntity;

public class MinecartphysicsClient implements ClientModInitializer {

	public static final EntityModelLayer CUBE_LAYER = new EntityModelLayer(
			Identifier.of(Minecartphysics.MOD_ID, "cube"), "main");

	@Override
	public void onInitializeClient() {
		// This entrypoint is suitable for setting up client-specific logic, such as
		// rendering.

		EntityRendererRegistry.register(Minecartphysics.PHYS_ENTITY, (context) -> {
			return new PhysicsEntityRenderer(context);
		});

		EntityModelLayerRegistry.registerModelLayer(CUBE_LAYER, CubeModel::getTexturedModelData);
	}
}