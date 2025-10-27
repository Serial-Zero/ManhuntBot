package com.manhuntbot.entity;

import com.manhuntbot.HunterDifficulty;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.pathing.AmphibiousSwimNavigation;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.registry.tag.FluidTags;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

public class HunterEntity extends PathAwareEntity {

    private static final double REACHED_DISTANCE_SQUARED = 0.65D;
    private final Deque<Vec3d> currentPath = new ArrayDeque<>();
    private HunterDifficulty difficulty = HunterDifficulty.MEDIUM;
    private int stuckTicks;
    private Vec3d activeWaypoint;
    private double lastDistanceSq = Double.MAX_VALUE;

    public HunterEntity(EntityType<? extends PathAwareEntity> type, World world) {
        super(type, world);
        this.setPersistent();
        this.setPathfindingPenalty(PathNodeType.WATER, 0.0F);
        this.setPathfindingPenalty(PathNodeType.WATER_BORDER, 0.0F);
        this.getNavigation().setCanSwim(true);
    }

    @Override
    protected EntityNavigation createNavigation(World world) {
        return new AmphibiousSwimNavigation(this, world);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return PathAwareEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 40.0D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, HunterDifficulty.MEDIUM.getAttackDamage())
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, HunterDifficulty.MEDIUM.getMoveSpeed())
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 64.0D)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.4D);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new MeleeAttackGoal(this, 1.1D, true));
        this.goalSelector.add(4, new LookAtEntityGoal(this, PlayerEntity.class, 10.0F));
        this.goalSelector.add(5, new LookAtEntityGoal(this, IronGolemEntity.class, 8.0F));
        this.goalSelector.add(6, new LookAroundGoal(this));
        this.targetSelector.add(1, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        if (!getWorld().isClient()) {
            boolean swimming = isSubmergedInWater() || (isTouchingWater() && !isOnGround());
            setSwimming(swimming);
            setSprinting(swimming);
        }
        if (getWorld().isClient()) {
            return;
        }
        if (difficulty == HunterDifficulty.CALM) {
            currentPath.clear();
            getNavigation().stop();
            setTarget(null);
            return;
        }
        ensureTarget();
        followPath();
    }

    private void ensureTarget() {
        LivingEntity currentTarget = getTarget();
        if (currentTarget == null || !currentTarget.isAlive()) {
            PlayerEntity player = getWorld().getClosestPlayer(this, 128.0D);
            if (player != null) {
                setTarget(player);
            }
        }
    }

    private void followPath() {
        if (currentPath.isEmpty()) {
            activeWaypoint = null;
            lastDistanceSq = Double.MAX_VALUE;
            LivingEntity target = getTarget();
            if (target != null) {
                if (getNavigation().isIdle()) {
                    getNavigation().startMovingTo(target, getNavigationSpeed());
                } else {
                    getNavigation().setSpeed(getNavigationSpeed());
                }
            } else {
                getNavigation().stop();
            }
            return;
        }
        Vec3d next = currentPath.peek();
        if (next == null) {
            return;
        }
        double distanceSq = getPos().squaredDistanceTo(next);
        if (distanceSq <= REACHED_DISTANCE_SQUARED) {
            currentPath.poll();
            stuckTicks = 0;
            activeWaypoint = null;
            lastDistanceSq = Double.MAX_VALUE;
            if (currentPath.isEmpty()) {
                getNavigation().stop();
            }
            return;
        }
        if (distanceSq < lastDistanceSq - 0.05D) {
            stuckTicks = 0;
        } else {
            stuckTicks++;
        }
        lastDistanceSq = distanceSq;
        if (stuckTicks > 120) {
            currentPath.clear();
            stuckTicks = 0;
            activeWaypoint = null;
            lastDistanceSq = Double.MAX_VALUE;
            return;
        }
        if (isSubmergedInWater() || (isTouchingWater() && !isOnGround())) {
            moveThroughWater(next);
        } else {
            if (activeWaypoint == null || activeWaypoint.squaredDistanceTo(next) > 1.0E-4D || getNavigation().isIdle()) {
                startNavigationTo(next);
            } else {
                getNavigation().setSpeed(getNavigationSpeed());
            }
            getLookControl().lookAt(next.x, next.y, next.z);
        }
    }

    private void startNavigationTo(Vec3d target) {
        activeWaypoint = target;
        getNavigation().startMovingTo(target.x, target.y, target.z, getNavigationSpeed());
    }

    private double getNavigationSpeed() {
        return MathHelper.clamp(1.1D + difficulty.getMoveSpeed() * 2.25D, 1.1D, 2.8D);
    }

    private double getSwimSpeed() {
        return MathHelper.clamp(0.3D + difficulty.getMoveSpeed() * 1.8D, 0.4D, 1.5D);
    }

    private void moveThroughWater(Vec3d target) {
        getNavigation().stop();
        Vec3d diff = target.subtract(getPos());
        if (diff.lengthSquared() < 1.0E-6D) {
            setVelocity(getVelocity().multiply(0.8D));
            return;
        }
        Vec3d direction = diff.normalize();
        double swimSpeed = getSwimSpeed();
        Vec3d desiredVelocity = direction.multiply(swimSpeed);
        Vec3d currentVelocity = getVelocity();
        Vec3d blendedVelocity = currentVelocity.add(desiredVelocity.subtract(currentVelocity).multiply(0.35D));
        double upwardBias = 0.02D;
        if (diff.y > 0) {
            blendedVelocity = blendedVelocity.add(0.0D, Math.min(diff.y * 0.06D, 0.08D), 0.0D);
        } else if (getFluidHeight(FluidTags.WATER) < 0.6D) {
            blendedVelocity = blendedVelocity.add(0.0D, upwardBias, 0.0D);
        }
        setVelocity(blendedVelocity);
        move(MovementType.SELF, getVelocity());
        setSwimming(true);
        setSprinting(true);
        float yaw = (float)(MathHelper.atan2(direction.z, direction.x) * 180.0F / (float)Math.PI) - 90.0F;
        float pitch = (float)(-MathHelper.atan2(direction.y, Math.sqrt(direction.x * direction.x + direction.z * direction.z)) * 180.0F / (float)Math.PI);
        setYaw(yaw);
        setHeadYaw(yaw);
        setPitch(pitch);
        lastDistanceSq = getPos().squaredDistanceTo(target);
        stuckTicks = 0;
    }

    public void applyBaritonePath(List<BlockPos> nodes) {
        currentPath.clear();
        stuckTicks = 0;
        activeWaypoint = null;
        lastDistanceSq = Double.MAX_VALUE;
        if (nodes.isEmpty()) {
            return;
        }
        BlockPos currentBlock = getBlockPos();
        for (BlockPos pos : nodes) {
            if (pos.equals(currentBlock)) {
                continue;
            }
            currentPath.add(Vec3d.ofCenter(pos));
        }
    }

    public void setDifficulty(HunterDifficulty difficulty) {
        this.difficulty = difficulty;
        getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED)
                .setBaseValue(difficulty.getMoveSpeed());
        getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE)
                .setBaseValue(difficulty.getAttackDamage());
        getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH)
                .setBaseValue(Math.max(20.0D, 30.0D + difficulty.getAttackDamage() * 2.0D));
        setHealth(getMaxHealth());
        getNavigation().setSpeed(getNavigationSpeed());
    }

    public HunterDifficulty getDifficulty() {
        return difficulty;
    }

    @Override
    protected void dropInventory() {
        // Prevent dropping loot to avoid farming
    }

    @Override
    public void onDeath(DamageSource damageSource) {
        super.onDeath(damageSource);
        if (!getWorld().isClient()) {
            currentPath.clear();
        }
    }
}



