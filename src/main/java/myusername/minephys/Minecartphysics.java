package myusername.minephys;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.BlockEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldEvents;
import physx.PxTopLevelFunctions;
import physx.common.*;
import physx.geometry.PxBoxGeometry;
import physx.physics.*;

import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

public class Minecartphysics implements ModInitializer {
	public static final String MOD_ID = "minecart-physics";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static Map<BlockPos, Optional<PxRigidStatic>> blocks;

	public static PxPhysics physics;

	public static boolean locked = false;

	public static PxScene phys_world;
	public static PxMaterial default_material;
	public static final PxShapeFlags px_flags = new PxShapeFlags(
			(byte) (PxShapeFlagEnum.eSCENE_QUERY_SHAPE.value | PxShapeFlagEnum.eSIMULATION_SHAPE.value));

	public static List<PhysicsEntity> carts;

	public static final EntityType<PhysicsEntity> PHYS_ENTITY = Registry.register(
			Registries.ENTITY_TYPE,
			Identifier.of(MOD_ID, "test_phys"),
			EntityType.Builder.create(PhysicsEntity::new, SpawnGroup.MISC).dimensions(1.0f, 1.0f).eyeHeight(0.0f)
					.build("test_phys"));

	public static void ensureLoaded(BlockPos pos, World world) {

		if (!world.isChunkLoaded(pos.getX() / 16, pos.getZ() / 16))
			return;

		ensureLoaded(pos, world, world.getBlockState(pos));
	}

	public static boolean isSolid(BlockState state) {

		return !state.isAir() && !state.isIn(BlockTags.RAILS);

	}

	public static void ensureLoaded(BlockPos pos, World world, BlockState state) {

		if (!world.isChunkLoaded(pos.getX() / 16, pos.getZ() / 16))
			return;

		if (pos.getY() > world.getBottomY()) {

			if (blocks.containsKey(pos) && blocks.get(pos).isPresent()) {
				blocks.get(pos).get().release();
			}

			if (isSolid(state)) {

				PxRigidStatic b = physics.createRigidStatic(new PxTransform(
						new PxVec3((float) pos.getX() + 0.5f, (float) pos.getY() + 0.5f, (float) pos.getZ() + 0.5f),
						new PxQuat(PxIDENTITYEnum.PxIdentity)));

				PxShape s = physics.createShape(new PxBoxGeometry(0.5f, 0.5f, 0.5f), default_material, true, px_flags);
				s.setSimulationFilterData(new PxFilterData(1, 1, 0, 0));
				b.attachShape(s);
				phys_world.addActor(b);

				blocks.put(pos, Optional.of(b));
			} else if (!isSolid(state)) {

				blocks.put(pos, Optional.absent());

			}
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
			if (blocks.get(pos).isPresent())
				blocks.get(pos).get().release();
			blocks.remove(pos);
		}
	}

	public static void recheckLoadedBlocks(World world) {

		for (var cart : carts) {

			enusureLoadedArea(cart.getBlockPos(), world);

		}

	}

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		GravityGun.initalize();

		carts = new ArrayList<>();

		blocks = new HashMap<>();
		int version = PxTopLevelFunctions.getPHYSICS_VERSION();

		PxFoundation foundation = PxTopLevelFunctions.CreateFoundation(version, new PxDefaultAllocator(),
				new PxDefaultErrorCallback());

		PxTolerancesScale tolerancesScale = new PxTolerancesScale();
		physics = PxTopLevelFunctions.CreatePhysics(version, foundation, tolerancesScale);
		default_material = physics.createMaterial(0.5f, 0.5f, 0.0f);

		PxSceneDesc desc = new PxSceneDesc(tolerancesScale);
		desc.setGravity(new PxVec3(0.0f, -9.8f / 8.0f, 0.0f)); // Shhhh :)
		desc.setFilterShader(PxTopLevelFunctions.DefaultFilterShader());
		desc.setCpuDispatcher(PxTopLevelFunctions.DefaultCpuDispatcherCreate(4));

		phys_world = physics.createScene(desc);
		ServerTickEvents.END_WORLD_TICK.register((world) -> {
			if (world.isClient())
				return;
			for (int i = 0; i < 8; i++) {
				while (locked) {
				}
				locked = true;
				phys_world.simulate(1.0f / 20.0f / 8.0f);
				phys_world.fetchResults(true);
				locked = false;

				for (var cart : carts) {

					cart.doPhysics(1.0f / 20.0f / 8.0f);

				}
			}
		});

		LOGGER.info("Hello Fabric world!");

	}
}