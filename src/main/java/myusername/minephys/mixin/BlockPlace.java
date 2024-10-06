package myusername.minephys.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import myusername.minephys.Minecartphysics;

@Mixin(World.class)
public class BlockPlace {
	@Inject(at = @At("HEAD"), method = "onBlockChanged")
	private void changed(BlockPos pos, BlockState oldBlock, BlockState newBlock, CallbackInfo info) {

		if (newBlock.isAir()) {
			Minecartphysics.unloadBlock(pos);
		} else {
			Minecartphysics.ensureLoaded(pos, (World) (Object) this);
		}
	}
}