package myusername.minephys.mixin.client;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import myusername.minephys.PhysicsEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;

@Mixin(Camera.class)
public abstract class CameraMixin {

    @Shadow
    protected abstract void setPos(Vec3d pos);

    @Shadow
    @Final
    Quaternionf rotation;

    @Inject(method = "update(Lnet/minecraft/world/BlockView;Lnet/minecraft/entity/Entity;ZZF)V", at = @At(value = "INVOKE", shift = At.Shift.AFTER, ordinal = 0, target = "Lnet/minecraft/client/render/Camera;setPos(DDD)V"))
    private void update(BlockView area, Entity self, boolean thirdPerson, boolean inverseView, float tickDelta,
            CallbackInfo info) {
        Entity vehicle = self.getVehicle();

        if (vehicle instanceof PhysicsEntity && self.getWorld().isClient) {
            PhysicsEntity _vehicle = (PhysicsEntity) vehicle;
            // We are riding a physics cart :)
            Quaternionf q = _vehicle.getDataTracker().get(_vehicle.orientation);
            RotationAxis.POSITIVE_Y.rotationDegrees(180.0f + vehicle.getYaw(tickDelta)).mul(rotation, rotation);
            RotationAxis.POSITIVE_X.rotationDegrees(180.0f).mul(rotation, rotation);

            _vehicle.getDataTracker().get(_vehicle.orientation).mul(rotation, rotation);

            q.invert();

            this.setPos(_vehicle.getPos()
                    .subtract(
                            new Vec3d(q.positiveY(new Vector3f()).mul(self.getEyeHeight(EntityPose.SITTING) / 2.0f))));

            q.invert();
        }
    }
}
