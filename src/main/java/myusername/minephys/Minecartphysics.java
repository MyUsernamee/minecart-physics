package myusername.minephys;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.data.TrackedDataHandler;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import physx.PxTopLevelFunctions;
import physx.common.*;
import physx.geometry.PxBoxGeometry;
import physx.physics.*;

import org.ode4j.ode.OdeConstants;
import java.util.Map;
import java.util.HashMap;

import org.ode4j.ode.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Minecartphysics implements ModInitializer {
	public static final String MOD_ID = "minecart-physics";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static Map<BlockPos, DBox> blocks;

	public static DWorld phys_world;
	public static DSpace phys_space;
	public static DJointGroup c_group;

	private static boolean colliding = false;

	public static final EntityType<PhysicsEntity> PHYS_ENTITY = Registry.register(
			Registries.ENTITY_TYPE,
			Identifier.of(MOD_ID, "test_phys"),
			EntityType.Builder.create(PhysicsEntity::new, SpawnGroup.CREATURE).dimensions(1.0f, 1.0f)
					.build("test_phys"));

	public static void nearCallback(Object data, DGeom o1, DGeom o2) {

		if (o1 == o2)
			return;

		final int N = 32;
		DContactBuffer contacts = new DContactBuffer(N);
		int n = OdeHelper.collide(o1, o2, N, contacts.getGeomBuffer());
		if (n > 0) {
			for (int i = 0; i < n; ++i) {

				DContact c = contacts.get(i);

				c.surface.slip1 = 0.0;
				c.surface.slip2 = 0.0;
				c.surface.mode = OdeConstants.dContactSlip1 | OdeConstants.dContactSlip2
						| OdeConstants.dContactApprox1;
				c.surface.mu = 100.0;

				DJoint j = OdeHelper.createContactJoint(phys_world, c_group, c);
				j.attach(o1.getBody(), o2.getBody());

			}
		}

	}

	public static void ensureLoaded(BlockPos pos, World world) {

		Minecartphysics.LOGGER.info("Loading area: {}", pos);

		while (colliding) {
			Minecartphysics.LOGGER.info("Space locked... Waiting...");
		}

		if (!blocks.containsKey(pos) && !world.getBlockState(pos).isAir()) {
			blocks.put(pos, OdeHelper.createBox(phys_space, 1, 1, 1));
			blocks.get(pos).setPosition((float) pos.getX() + 0.5, (float) pos.getY() + 0.5, (float) pos.getZ() + 0.5);
		}

	}

	public static void enusureLoadedArea(BlockPos pos, World world) {

		for (int ix = -2; ix <= 2; ix++) {
			for (int iy = -2; iy <= 2; iy++) {
				for (int iz = -2; iz <= 2; iz++) {
					ensureLoaded(pos.add(ix, iy, iz), world);
				}
			}
		}
	}

	public static void unloadBlock(BlockPos pos) {
		if (blocks.containsKey(pos)) {
			blocks.get(pos).disable();
			blocks.remove(pos);
		}
	}

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		blocks = new HashMap<BlockPos, DBox>();
		OdeHelper.initODE2(0);

		phys_world = OdeHelper.createWorld();
		phys_world.setGravity(0, -9.8, 0.0);

		phys_space = OdeHelper.createSimpleSpace();

		c_group = OdeHelper.createJointGroup();

		DMass m = OdeHelper.createMass();

		ServerTickEvents.START_WORLD_TICK.register((world) -> {
			if (world.isClient())
				return;
			colliding = true;
			phys_space.collide(null, Minecartphysics::nearCallback);
			colliding = false;
			phys_world.quickStep(1.0 / 60.0);
			c_group.empty();
		});

		LOGGER.info("Hello Fabric world!");
	}
}