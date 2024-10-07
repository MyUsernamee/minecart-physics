package myusername.minephys;

import org.joml.Matrix4d;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4d;
import org.joml.Vector4dc;
import org.joml.Vector4fc;
import org.ode4j.math.*;
import org.ode4j.ode.*;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents.DebugRender;
import net.minecraft.block.entity.StructureBlockBlockEntity.Action;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandler;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.data.DataTracker.Builder;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Hand;
import net.minecraft.util.annotation.Debug;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import physx.common.PxQuat;
import physx.common.PxTransform;
import physx.common.PxVec3;
import physx.geometry.PxBoxGeometry;
import physx.physics.PxFilterData;
import physx.physics.PxRigidBody;
import physx.physics.PxRigidDynamic;
import physx.physics.PxShape;

public class PhysicsEntity extends Entity {

    PxRigidDynamic body;
    private BlockPos b_pos;
    public static final TrackedData<Vector3f> rx = DataTracker.registerData(PhysicsEntity.class,
            TrackedDataHandlerRegistry.VECTOR3F);
    public static final TrackedData<Vector3f> ry = DataTracker.registerData(PhysicsEntity.class,
            TrackedDataHandlerRegistry.VECTOR3F);
    public static final TrackedData<Vector3f> rz = DataTracker.registerData(PhysicsEntity.class,
            TrackedDataHandlerRegistry.VECTOR3F);

    public BlockPos getBlockPosition() {

        return b_pos;

    }

    public void setBlockPosition(BlockPos p) {
        if (!p.equals(b_pos)) {
            b_pos = p;
            this.onBlockPositionChanged();
            return;
        }
        b_pos = p;

    }

    protected void onBlockPositionChanged() {
        Minecartphysics.enusureLoadedArea(this.getBlockPosition(), getWorld());
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(rx, new Vector3f());
        builder.add(ry, new Vector3f());
        builder.add(rz, new Vector3f());
    }

    @Override
    public boolean isPushable() {
        return true;
    }

    @Override
    public ActionResult interact(PlayerEntity player, Hand hand) {
        if (this.getWorld().isClient) {
            return ActionResult.SUCCESS;
        } else {

            return player.startRiding(this) ? ActionResult.SUCCESS : ActionResult.PASS;

        }
    }

    public PhysicsEntity(EntityType<? extends Entity> entityType, World world) {
        super(entityType, world);
        this.calculateDimensions();
        this.setBoundingBox(new Box(0.0f, 0.0f, 0.0f, 10.0f, 10.0f, 10.0f));
        this.intersectionChecked = true;

        this.dataTracker.set(rx, new Vector3f());
        this.dataTracker.set(ry, new Vector3f());
        this.dataTracker.set(rz, new Vector3f());
        if (world.isClient)
            return;
        Vec3d pos = this.getPos();
        PxVec3 pxVec3 = new PxVec3((float) pos.x, (float) pos.y, (float) pos.z);
        PxQuat pxQuat = new PxQuat(0.0f, 0.0f, 0.0f, 0.0f);
        PxTransform pxTransform = new PxTransform(pxVec3, pxQuat);
        body = Minecartphysics.physics.createRigidDynamic(pxTransform);

        PxBoxGeometry pxBoxGeometry = new PxBoxGeometry(0.55f, 0.25f, 0.49f);
        PxShape pxShape = Minecartphysics.physics.createShape(pxBoxGeometry, Minecartphysics.default_material, true,
                Minecartphysics.px_flags);
        PxFilterData pxFilterData = new PxFilterData(1, 1, 0, 0);
        pxShape.setSimulationFilterData(pxFilterData);
        body.attachShape(pxShape);
        Minecartphysics.phys_world.addActor(body);
        body.setSleepThreshold(0.0f);
        pxVec3.setX(0.0f);
        pxVec3.setY(0.0f);
        pxVec3.setZ(0.0f);
        body.setLinearVelocity(pxVec3);

        pxVec3.destroy();
        pxQuat.destroy();
        pxBoxGeometry.destroy();
        pxFilterData.destroy();

    }

    public void setPosition(double x, double y, double z) {
        super.setPosition(x, y, z);
        if (body == null)
            return;
        // Create temppxVec3 and temp PxQuat
        PxVec3 pxVec3 = new PxVec3((float) x, (float) y, (float) z);
        PxQuat pxQuat = new PxQuat(0.0f, 0.0f, 1.0f, 0.0f);
        PxTransform pxTransform = new PxTransform(pxVec3, pxQuat);
        body.setGlobalPose(pxTransform);
        pxTransform.destroy();
        pxVec3.destroy();
        pxQuat.destroy();
    }

    @Override
    public void tick() {
        // super.tick();
        if (getWorld().isClient)
            return;

        if (Double.isNaN(getPos().getX())) {
            setPosition(new Vec3d(0.0, 0.0, 0.0));
        }

        super.setPos(body.getGlobalPose().getP().getX(), body.getGlobalPose().getP().getY(),
                body.getGlobalPose().getP().getZ());

        DVector3 c0 = new DVector3();
        DVector3 c1 = new DVector3();
        DVector3 c2 = new DVector3();

        c0.set(body.getGlobalPose().getQ().getBasisVector0().getX(),
                body.getGlobalPose().getQ().getBasisVector0().getY(),
                body.getGlobalPose().getQ().getBasisVector0().getZ());

        c1.set(body.getGlobalPose().getQ().getBasisVector1().getX(),
                body.getGlobalPose().getQ().getBasisVector1().getY(),
                body.getGlobalPose().getQ().getBasisVector1().getZ());

        c2.set(body.getGlobalPose().getQ().getBasisVector2().getX(),
                body.getGlobalPose().getQ().getBasisVector2().getY(),
                body.getGlobalPose().getQ().getBasisVector2().getZ());

        this.dataTracker.set(rx, new Vector3f(c0.toFloatArray4()));
        this.dataTracker.set(ry, new Vector3f(c1.toFloatArray4()));
        this.dataTracker.set(rz, new Vector3f(c2.toFloatArray4()));

        this.setBlockPosition(new BlockPos((int) this.getPos().x, (int) this.getPos().y, (int) this.getPos().z));

        // this.setVelocity(this.getVelocity().multiply(0.0f));
    }

    public Vector3f getRx() {
        return this.dataTracker.get(rx);
    }

    public Vector3f getRy() {
        return this.dataTracker.get(ry);
    }

    public Vector3f getRz() {
        return this.dataTracker.get(rz);
    }

    @Override
    public void remove(Entity.RemovalReason reason) {

        body.release();
        super.remove(reason);

    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        // TODO Auto-generated method stub
        // throw new UnsupportedOperationException("Unimplemented method
        // 'readCustomDataFromNbt'");
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        // TODO Auto-generated method stub
        // throw new UnsupportedOperationException("Unimplemented method
        // 'writeCustomDataToNbt'");
    }

}
