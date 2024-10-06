package myusername.minephys;

import org.joml.Matrix4d;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4d;
import org.joml.Vector4dc;
import org.joml.Vector4fc;
import org.ode4j.math.*;
import org.ode4j.ode.*;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandler;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.data.DataTracker.Builder;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class PhysicsEntity extends Entity {

    DBody body;
    private BlockPos b_pos;
    DBox b;
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

    public PhysicsEntity(EntityType<? extends Entity> entityType, World world) {
        super(entityType, world);

        this.dataTracker.set(rx, new Vector3f());
        this.dataTracker.set(ry, new Vector3f());
        this.dataTracker.set(rz, new Vector3f());
        if (world.isClient)
            return;
        body = OdeHelper.createBody(Minecartphysics.phys_world);

        DMass m = OdeHelper.createMass();

        b = OdeHelper.createBox(Minecartphysics.phys_space, 1.0, 1.0, 1.0);
        m.setBox(0.1, 1.0, 1.0, 1.0);
        b.setBody(body);
        body.setMass(m);

        body.setPosition(new DVector3(this.getPos().x, this.getPos().y + 10.0, this.getPos().z));
        body.setLinearVel(0.0, 0.0, 0.0);
    }

    @Override
    public void tick() {
        // super.tick();
        if (getWorld().isClient)
            return;
        this.move(MovementType.SELF,
                new Vec3d(body.getPosition().get0() - this.getPos().x,
                        body.getPosition().get1() - this.getPos().y,
                        body.getPosition().get2() - this.getPos().z));

        DVector3 c0 = new DVector3();
        DVector3 c1 = new DVector3();
        DVector3 c2 = new DVector3();

        body.getRotation().getColumn0(c0);
        body.getRotation().getColumn1(c1);
        body.getRotation().getColumn2(c2);

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

        body.destroy();
        b.destroy();
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
