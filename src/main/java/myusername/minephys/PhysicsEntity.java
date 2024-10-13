package myusername.minephys;

import java.util.Map;

import org.joml.Math;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Pair;

import net.minecraft.block.AbstractRailBlock;
import net.minecraft.block.Blocks;
import net.minecraft.block.enums.RailShape;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.MinecartEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
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

public class PhysicsEntity extends MinecartEntity {

    PxRigidDynamic body;
    PxShape pxShape;
    private BlockPos b_pos;
    private boolean on_rail;

    public Quaternionf client_orientation;

    public static final TrackedData<Quaternionf> orientation = DataTracker.registerData(PhysicsEntity.class,
            TrackedDataHandlerRegistry.QUATERNIONF);
    public static final TrackedData<Vector3f> last_ang_vel = DataTracker.registerData(PhysicsEntity.class,
            TrackedDataHandlerRegistry.VECTOR3F);
    public static final TrackedData<Vector3f> last_vel = DataTracker.registerData(PhysicsEntity.class,
            TrackedDataHandlerRegistry.VECTOR3F);
    private static final Map<RailShape, Pair<Vec3i, Vec3i>> ADJACENT_RAIL_POSITIONS_BY_SHAPE = Util
            .make(Maps.newEnumMap(RailShape.class), map -> {
                Vec3i vec3i = Direction.WEST.getVector();
                Vec3i vec3i2 = Direction.EAST.getVector();
                Vec3i vec3i3 = Direction.NORTH.getVector();
                Vec3i vec3i4 = Direction.SOUTH.getVector();
                Vec3i vec3i5 = vec3i.down();
                Vec3i vec3i6 = vec3i2.down();
                Vec3i vec3i7 = vec3i3.down();
                Vec3i vec3i8 = vec3i4.down();
                map.put(RailShape.NORTH_SOUTH, Pair.of(vec3i3, vec3i4));
                map.put(RailShape.EAST_WEST, Pair.of(vec3i, vec3i2));
                map.put(RailShape.ASCENDING_EAST, Pair.of(vec3i5, vec3i2.up()));
                map.put(RailShape.ASCENDING_WEST, Pair.of(vec3i.up(), vec3i6));
                map.put(RailShape.ASCENDING_NORTH, Pair.of(vec3i3.up(), vec3i8));
                map.put(RailShape.ASCENDING_SOUTH, Pair.of(vec3i7, vec3i4.up()));
                map.put(RailShape.SOUTH_EAST, Pair.of(vec3i4, vec3i2));
                map.put(RailShape.SOUTH_WEST, Pair.of(vec3i4, vec3i));
                map.put(RailShape.NORTH_WEST, Pair.of(vec3i3, vec3i));
                map.put(RailShape.NORTH_EAST, Pair.of(vec3i3, vec3i2));
            });

    private static Pair<Vec3i, Vec3i> getAdjacentRailPositionsByShape(RailShape shape) {
        return (Pair<Vec3i, Vec3i>) ADJACENT_RAIL_POSITIONS_BY_SHAPE.get(shape);
    }

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
        super.initDataTracker(builder);
        builder.add(orientation, new Quaternionf());
        builder.add(last_vel, new Vector3f(1.0f, 0.0f, 0.0f));
        builder.add(last_ang_vel, new Vector3f());
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

            if (player.isHolding(GravityGun.GRAVITY_GUN) || player.isInPose(EntityPose.CROUCHING)) {
                this.setVelocity(
                        new Vec3d(-Math.sin(player.getHeadYaw() * (3.14 / 180.0f)), 0.0f,
                                Math.cos(player.getHeadYaw() * (3.14 / 180.f)))
                                .multiply(player.isHolding(GravityGun.GRAVITY_GUN) ? 5.f : 1.0f)
                                .add(this.getVelocity()));
                return ActionResult.CONSUME;
            }

            return player.startRiding(this) ? ActionResult.CONSUME : ActionResult.PASS;

        }
    }

    public PhysicsEntity(EntityType<? extends Entity> entityType, World world) {
        super(entityType, world);
        // this.calculateDimensions();
        // this.setBoundingBox(new Box(0.0f, 0.0f, 0.0f, 10.0f, 10.0f, 10.0f));
        this.intersectionChecked = true;
        this.client_orientation = new Quaternionf(0.0, 0.0, 1.0, 0.0);

        if (world.isClient)
            return;

        Minecartphysics.carts.add(this);

        Vec3d pos = this.getPos();
        PxVec3 pxVec3 = new PxVec3((float) pos.x, (float) pos.y, (float) pos.z);
        PxQuat pxQuat = new PxQuat(0.0f, 0.0f, 1.0f, 0.0f);
        PxTransform pxTransform = new PxTransform(pxVec3, pxQuat);
        body = Minecartphysics.physics.createRigidDynamic(pxTransform);
        Minecartphysics.phys_world.addActor(body);

        PxBoxGeometry pxBoxGeometry = new PxBoxGeometry(0.55f, 0.25f, 0.49f);
        pxShape = Minecartphysics.physics.createShape(pxBoxGeometry, Minecartphysics.default_material, true,
                Minecartphysics.px_flags);
        PxFilterData pxFilterData = new PxFilterData(1, 1, 0, 0);
        pxShape.setSimulationFilterData(pxFilterData);
        body.attachShape(pxShape);
        pxVec3.setX(0.0f);
        pxVec3.setY(0.0f);
        pxVec3.setZ(0.0f);
        body.setLinearVelocity(pxVec3);

    }

    // public PhysicsEntity(World world, double x, double y, double z) {
    // super(Minecartphysics.PHYS_ENTITY, world);
    // setPosition(x, y, z);
    // }

    @Override
    public void setPosition(double x, double y, double z) {
        super.setPosition(x, y, z);
        if (body == null)
            return;
        Vec3d v = getVelocity();
        // Create temppxVec3 and temp PxQuat
        setPxPos(new Vec3d(x, y, z));
        setVelocity(v);
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

        if (body == null || this.isRemoved())
            return new Vec3d(0.0, 0.0, 0.0);

        // return super.getVelocity();

        return new Vec3d(body.getLinearVelocity().getX(),
                body.getLinearVelocity().getY(),
                body.getLinearVelocity().getZ());

    }

    public void setRotation(Quaternionf q) {

        PxQuat new_q = new PxQuat(q.x, q.y, q.z, q.w);
        body.setGlobalPose(new PxTransform(body.getGlobalPose().getP(), new_q), true);
        body.setAngularVelocity(new PxVec3(0.0f, 0.0f, 0.0f));

    }

    public Quaternionf getRotation() {

        var px_quat = body.getGlobalPose().getQ();
        return new Quaternionf(px_quat.getX(), px_quat.getY(), px_quat.getZ(), px_quat.getZ());

    }

    @Override
    public void setVelocity(Vec3d v) {
        super.setVelocity(new Vec3d(0.0, 0.0, 0.0));

        if (this.body == null)
            return;
        PxVec3 px = new PxVec3((float) v.x, (float) v.y, (float) v.z);
        body.setLinearVelocity(px);
        px.destroy();

    }

    public BlockPos getNearestRail(Vec3d p) {

        int i = (int) Math.floor(p.x);
        int j = (int) Math.floor(p.y);
        int k = (int) Math.floor(p.z);

        var state = getWorld().getBlockState(new BlockPos(i, j, k));
        if (state.isIn(BlockTags.RAILS))
            return new BlockPos(i, j, k);

        --j;
        state = getWorld().getBlockState(new BlockPos(i, j, k));
        if (state.isIn(BlockTags.RAILS))
            return new BlockPos(i, j, k);

        return null;

    }

    public void doPhysics(float dt) {

        setAngles(0, 0);

        if (getPxPos().y < getWorld().getBottomY()) {
            this.remove(RemovalReason.DISCARDED);
            return;
        }

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

        PxQuat px_q = body.getGlobalPose().getQ();

        Quaternionf q = new Quaternionf(px_q.getX(), px_q.getY(), px_q.getZ(), px_q.getW());
        this.dataTracker.set(orientation, q);

        this.setBlockPosition(new BlockPos((int) this.getPos().x, (int) this.getPos().y, (int) this.getPos().z));
        q = q.normalize();

        var cart_forward = q.positiveX(new Vector3f());
        var cart_up = q.positiveY(new Vector3f());
        var cart_pos = new Vector3f((float) this.getPos().x, (float) this.getPos().y, (float) this.getPos().z);
        var cart_front = cart_pos.add(cart_forward.mul(0.5f, new Vector3f()), new Vector3f());
        var cart_back = cart_pos.add(cart_forward.mul(-0.5f, new Vector3f()), new Vector3f());

        // cart_forward.sub(cart_up.mul(0.5f));
        // cart_back.sub(cart_up.mul(0.5f));

        var new_p_1 = this.snapPositionToRail(cart_front.x, cart_front.y, cart_front.z);
        var new_p_2 = this.snapPositionToRail(cart_back.x, cart_back.y, cart_back.z);

        var r_pos = getNearestRail(this.getPos());

        if (new_p_1 != null && new_p_2 != null) {

            setPosition(new_p_1.add(new_p_2).multiply(0.5).add(0.0, 0.5, 0.0));

            if (!on_rail) {
                on_rail = true;
                body.detachShape(pxShape);
            }

            if (r_pos == null)
                return; // How

            var r_state = getRailShape(r_pos);
            var rb_state = getWorld().getBlockState(r_pos);

            // Print new_P

            if (r_state == null) {
                return;
            }

            Vector3f direction = new Vector3f((float) (new_p_1.x - new_p_2.x), (float) (new_p_1.y - new_p_2.y),
                    (float) (new_p_1.z - new_p_2.z));
            direction = direction.mul(-1.0f).normalize();
            Vector3f right = direction.cross(new Vector3f(0.0f, 1.0f, 0.0f), new Vector3f(0.0f, 0.0f, 0.0f))
                    .normalize();
            Vector3f up = direction.cross(right, new Vector3f(0.0f, 0.0f, 0.0f)).normalize();
            Quaternionf rotationQuat = new Quaternionf().setFromUnnormalized(new Matrix3f(
                    right,
                    up,
                    direction));

            rotationQuat.rotateAxis((float) Math.PI / 2.0f, new Vector3f(0.0f, 1.0f, 0.0f));

            Vector3f old_direction = getRotation().positiveX(new Vector3f());
            Vector3f new_direction = rotationQuat.positiveX(new Vector3f());

            Vector3f angular_velocity = old_direction.cross(new_direction, new Vector3f());
            angular_velocity.mul(1.0f / dt);

            setAngularVelocity(angular_velocity);

            setRotation(rotationQuat);
            // body.setAngularVelocity(new PxVec3(0.0f, 0.0f, 0.0f));

            double sign_direction = Math.signum(getVelocity().dotProduct(new Vec3d(direction)));
            sign_direction = sign_direction >= 0.0 ? 1.0 : -1.0;

            Vec3d flat_vel = getVelocity().add(new Vec3d(up.mul((float) -getVelocity().dotProduct(new Vec3d(up)))));

            setVelocity(new Vec3d(flat_vel.x, flat_vel.y, flat_vel.z));
            float vel_before = (float) getVelocity().length();
            setVelocity(new Vec3d(direction.mul((float) sign_direction * vel_before)));

            if (rb_state.isOf(Blocks.POWERED_RAIL)
                    && getVelocity().length() > 0.0f) {
                setVelocity(this.getVelocity().normalize().multiply(Math.min(getVelocity().length() * 1.01F, 16.0)));
            }

        } else {

            if (on_rail) {
                on_rail = false;
                body.attachShape(pxShape);
            }

        }

        this.dataTracker.set(PhysicsEntity.last_vel,
                new Vector3f((float) this.getVelocity().x, (float) this.getVelocity().y, (float) this.getVelocity().z),
                true);
        this.dataTracker.set(PhysicsEntity.last_ang_vel, new Vector3f(body.getAngularVelocity().getX(),
                body.getAngularVelocity().getY(), body.getAngularVelocity().getZ()));

    }

    private void setAngularVelocity(Vector3f angular_velocity) {

        var new_vel = new PxVec3(angular_velocity.x, angular_velocity.y, angular_velocity.z);
        body.setAngularVelocity(new_vel);
    }

    @Override
    public void tick() {
        // super.tick();
        this.firstUpdate = false;
        if (getWorld().isClient) {
            // this.refreshPosition();
            // this.setRotation(this.getYaw(), this.getPitch());

            this.setPosition(
                    this.getPos().add(new Vec3d(this.getDataTracker().get(PhysicsEntity.last_vel).mul(1.0f / 20.0f))));
            // Print last vel
            var ang_vel = this.getDataTracker().get(PhysicsEntity.last_ang_vel);
            if (ang_vel.length() > 0.0) {
                client_orientation.rotateAxis(
                        ang_vel.length() / 20.0f,
                        this.getDataTracker().get(PhysicsEntity.last_ang_vel).normalize());
            }

            // Minecartphysics.LOGGER.info("Orientation: {}, {}, {}, {}.",
            // this.getDataTracker().get(PhysicsEntity.orientation).x,
            // this.getDataTracker().get(PhysicsEntity.orientation).y,
            // this.getDataTracker().get(PhysicsEntity.orientation).z,
            // this.getDataTracker().get(PhysicsEntity.orientation).w);

            return;
        }
        return;
    }

    private RailShape getRailShape(Vec3d new_p) {
        // TODO Auto-generated method stub
        return getRailShape(new BlockPos((int) new_p.x, (int) new_p.y, (int) new_p.z));
    }

    private RailShape getRailShape(BlockPos ps) {
        // TODO Auto-generated method stub
        var state = this.getWorld().getBlockState(ps);
        if (!(state.getBlock() instanceof AbstractRailBlock))
            return null;
        return state.get(((AbstractRailBlock) state.getBlock()).getShapeProperty());
    }

    private RailShape getRailShape() {

        return getRailShape(this.getBlockPos());

    }

    @Override
    public void remove(Entity.RemovalReason reason) {

        super.remove(reason);
        Minecartphysics.carts.remove(this);
        body.release();
        body = null;

    }

    @Override
    public double getLerpTargetX() {
        return this.getPos().x;
    }

    @Override
    public double getLerpTargetY() {
        return this.getPos().y;
    }

    @Override
    public double getLerpTargetZ() {
        return this.getPos().z;
    }

    @Override
    public void updateTrackedPositionAndAngles(double x, double y, double z, float yaw, float pitch,
            int intepolationSteps) {
        // TODO Auto-generated method stub
        this.setPosition(x, y, z);
        client_orientation.set(this.getDataTracker().get(PhysicsEntity.orientation));
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
