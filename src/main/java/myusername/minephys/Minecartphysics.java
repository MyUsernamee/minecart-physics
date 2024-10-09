package myusername.minephys;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import physx.PxTopLevelFunctions;
import physx.common.*;
import physx.geometry.PxBoxGeometry;
import physx.physics.*;

import java.util.Map;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Minecartphysics implements ModInitializer {
	public static final String MOD_ID = "minecart-physics";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static Map<BlockPos, PxRigidStatic> blocks;

	public static PxPhysics physics;

	public static PxScene phys_world;
	public static PxMaterial default_material;
	public static final PxShapeFlags px_flags = new PxShapeFlags(
			(byte) (PxShapeFlagEnum.eSCENE_QUERY_SHAPE.value | PxShapeFlagEnum.eSIMULATION_SHAPE.value));

	public static final EntityType<PhysicsEntity> PHYS_ENTITY = Registry.register(
			Registries.ENTITY_TYPE,
			Identifier.of(MOD_ID, "test_phys"),
			EntityType.Builder.create(PhysicsEntity::new, SpawnGroup.MISC).dimensions(1.0f, 1.0f).eyeHeight(0.0f)
					.build("test_phys"));

	public static void ensureLoaded(BlockPos pos, World world) {

		if (!world.isChunkLoaded(pos.getX() / 16, pos.getZ() / 16))
			return;

		if (pos.getY() > world.getBottomY() && !blocks.containsKey(pos) && !world.getBlockState(pos).isAir()) {

			PxRigidStatic b = physics.createRigidStatic(new PxTransform(
					new PxVec3((float) pos.getX() + 0.5f, (float) pos.getY() + 0.5f, (float) pos.getZ() + 0.5f),
					new PxQuat(PxIDENTITYEnum.PxIdentity)));

			PxShape s = physics.createShape(new PxBoxGeometry(0.5f, 0.5f, 0.5f), default_material, true, px_flags);
			s.setSimulationFilterData(new PxFilterData(1, 1, 0, 0));
			b.attachShape(s);
			phys_world.addActor(b);

			blocks.put(pos, b);
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
			blocks.get(pos).release();
			blocks.remove(pos);
		}
	}

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		blocks = new HashMap<>();
		int version = PxTopLevelFunctions.getPHYSICS_VERSION();

		PxFoundation foundation = PxTopLevelFunctions.CreateFoundation(version, new PxDefaultAllocator(),
				new PxDefaultErrorCallback());

		PxTolerancesScale tolerancesScale = new PxTolerancesScale();
		physics = PxTopLevelFunctions.CreatePhysics(version, foundation, tolerancesScale);
		default_material = physics.createMaterial(0.5f, 0.5f, 0.5f);

		PxSceneDesc desc = new PxSceneDesc(tolerancesScale);
		desc.setGravity(new PxVec3(0.0f, -9.8f / 4.0f, 0.0f)); // Shhhh :)
		desc.setFilterShader(PxTopLevelFunctions.DefaultFilterShader());
		desc.setCpuDispatcher(PxTopLevelFunctions.DefaultCpuDispatcherCreate(4));

		phys_world = physics.createScene(desc);
		ServerTickEvents.START_WORLD_TICK.register((world) -> {
			if (world.isClient())
				return;
			phys_world.simulate(1.0f / 20.0f);
			phys_world.fetchResults(true);
		});

		LOGGER.info("Hello Fabric world!");

	}
}