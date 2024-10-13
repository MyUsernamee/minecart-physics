package myusername.minephys.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import myusername.minephys.Minecartphysics;

@Mixin(World.class)
public abstract class BlockPlace {

	@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;onBlockChanged(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Lnet/minecraft/block/BlockState;)V"), method = "setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;II)Z")
	private void changed(BlockPos pos, BlockState newBlock, int flags, int maxUpdateDepth,
			CallbackInfoReturnable info) {

		if (((World) (Object) this).isClient)
			return;
		while (Minecartphysics.locked) {
		}
		Minecartphysics.locked = true;
		Minecartphysics.LOGGER.info("LOCKED BY BLOCK PLACE");
		Minecartphysics.recheckLoadedBlocks((World) (Object) this);
		Minecartphysics.locked = false;
		// Minecartphysics.LOGGER.info("Block Changed");
	}
}