package myusername.minephys;

import org.jetbrains.annotations.ApiStatus.OverrideOnly;
import org.joml.Matrix3f;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Overwrite;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.render.debug.DebugRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.VehicleEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import physx.common.PxQuat;
import physx.common.PxTransform;
import physx.common.PxVec3;
import physx.geometry.PxBoxGeometry;
import physx.physics.PxFilterData;
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
    public boolean isCollidable() {
        return true;
    }

    // can hit
    @Override
    public boolean canHit() {
        return true;
    }

    @Override
    public ActionResult interact(PlayerEntity player, Hand hand) {
        if (this.getWorld().isClient) {
            return ActionResult.SUCCESS;
        } else {

            return player.startRiding(this) ? ActionResult.CONSUME : ActionResult.PASS;

        }
    }

    public PhysicsEntity(EntityType<? extends Entity> entityType, World world) {
        super(entityType, world);
        // this.calculateDimensions();
        // this.setBoundingBox(new Box(0.0f, 0.0f, 0.0f, 10.0f, 10.0f, 10.0f));
        this.intersectionChecked = true;

        this.dataTracker.set(rx, new Vector3f());
        this.dataTracker.set(ry, new Vector3f());
        this.dataTracker.set(rz, new Vector3f());

        if (world.isClient)
            return;

        Vec3d pos = this.getPos();
        PxVec3 pxVec3 = new PxVec3((float) pos.x, (float) pos.y, (float) pos.z);
        PxQuat pxQuat = new PxQuat(0.0f, 0.0f, 1.0f, 0.0f);
        PxTransform pxTransform = new PxTransform(pxVec3, pxQuat);
        body = Minecartphysics.physics.createRigidDynamic(pxTransform);
        Minecartphysics.phys_world.addActor(body);

        PxBoxGeometry pxBoxGeometry = new PxBoxGeometry(0.55f, 0.25f, 0.49f);
        PxShape pxShape = Minecartphysics.physics.createShape(pxBoxGeometry, Minecartphysics.default_material, true,
                Minecartphysics.px_flags);
        PxFilterData pxFilterData = new PxFilterData(1, 1, 0, 0);
        pxShape.setSimulationFilterData(pxFilterData);
        body.attachShape(pxShape);
        body.setSleepThreshold(0.0f);
        pxVec3.setX(0.0f);
        pxVec3.setY(0.0f);
        pxVec3.setZ(0.0f);
        body.setLinearVelocity(pxVec3);

        Minecartphysics.LOGGER.info("Creating Minecart: {}", body.getGlobalPose().getP().getX());

    }

    @Override
    public void setPosition(double x, double y, double z) {
        super.setPosition(x, y, z);
        if (body == null)
            return;
        // Create temppxVec3 and temp PxQuat
        setPxPos(new Vec3d(x, y, z));
    }

    private Vec3d getPxPos() {

        PxVec3 px_p = body.getGlobalPose().getP();
        Vector3f p = new Vector3f(px_p.getX(), px_p.getY(), px_p.getZ());
        PxQuat q = body.getGlobalPose().getQ();
        Matrix3f r = new Matrix3f();

        r.setRow(0,
                new Vector3f(q.getBasisVector0().getX(), q.getBasisVector0().getY(), q.getBasisVector0().getZ()));
        r.setRow(1,
                new Vector3f(q.getBasisVector1().getX(), q.getBasisVector1().getY(), q.getBasisVector1().getZ()));
        r.setRow(2,
                new Vector3f(q.getBasisVector2().getX(), q.getBasisVector2().getY(), q.getBasisVector2().getZ()));

        // r.invert();

        // p.add(r.transform(new Vector3f(-0.00f, 0.0f, 0.5f)));
        return new Vec3d(p.x, p.y, p.z);

    }

    private void setPxPos(Vec3d pos) {
        PxQuat q = body.getGlobalPose().getQ();
        Matrix3f r = new Matrix3f();

        r.setColumn(0,
                new Vector3f(q.getBasisVector0().getX(), q.getBasisVector0().getY(), q.getBasisVector0().getZ()));
        r.setColumn(1,
                new Vector3f(q.getBasisVector1().getX(), q.getBasisVector1().getY(), q.getBasisVector1().getZ()));
        r.setColumn(2,
                new Vector3f(q.getBasisVector2().getX(), q.getBasisVector2().getY(), q.getBasisVector2().getZ()));
        // r.invert();

        if (Float.isNaN(q.getX()))
            q = new PxQuat(0.0f, 0.0f, 1.0f, 0.0f);

        Vector3f p = new Vector3f((float) pos.x, (float) pos.y, (float) pos.z);

        PxVec3 px_p = new PxVec3(p.x, p.y, p.z);
        PxTransform pxTransform = new PxTransform(px_p, q);
        body.setGlobalPose(pxTransform, true);

        // px_p.destroy();
        pxTransform.destroy();
    }

    @Override
    public Vec3d getVelocity() {

        if (body == null)
            return new Vec3d(0.0, 0.0, 0.0);

        // return super.getVelocity();

        return new Vec3d(body.getLinearVelocity().getX() / 20.0,
                body.getLinearVelocity().getY() / 20.0,
                body.getLinearVelocity().getZ() / 20.0);

    }

    @Override
    public void setVelocity(Vec3d v) {
        super.setVelocity(v);

        if (this.body == null)
            return;
        PxVec3 px = new PxVec3((float) v.x * 20.0f, (float) v.y * 20.0f, (float) v.z * 20.0f);
        body.setLinearVelocity(px);
        px.destroy();

    }

    @Override
    public void tick() {
        // super.tick();
        if (getWorld().isClient)
            return;

        if (Double.isNaN(getPos().getX()) || Float.isNaN(body.getGlobalPose().getP().getX())) {
            setPosition(0.0, 0.0, 0.0);
        }

        this.setPos(getPxPos().x, getPxPos().y, getPxPos().z);
        this.setBoundingBox(this.calculateBoundingBox());
        // this.calculateBoundingBox();
        // this.calculateDimensions();

        Vector3f c0 = new Vector3f();
        Vector3f c1 = new Vector3f();
        Vector3f c2 = new Vector3f();

        c0.set(body.getGlobalPose().getQ().getBasisVector0().getX(),
                body.getGlobalPose().getQ().getBasisVector0().getY(),
                body.getGlobalPose().getQ().getBasisVector0().getZ());

        c1.set(body.getGlobalPose().getQ().getBasisVector1().getX(),
                body.getGlobalPose().getQ().getBasisVector1().getY(),
                body.getGlobalPose().getQ().getBasisVector1().getZ());

        c2.set(body.getGlobalPose().getQ().getBasisVector2().getX(),
                body.getGlobalPose().getQ().getBasisVector2().getY(),
                body.getGlobalPose().getQ().getBasisVector2().getZ());

        this.dataTracker.set(rx, c0);
        this.dataTracker.set(ry, c1);
        this.dataTracker.set(rz, c2);

        this.setBlockPosition(new BlockPos((int) this.getPos().x, (int) this.getPos().y, (int) this.getPos().z));

        this.firstUpdate = false;
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
