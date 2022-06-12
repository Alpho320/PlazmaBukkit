package net.minecraft.world.entity.monster;

import java.util.EnumSet;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.entity.animal.Squid;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.phys.Vec3;

public class Guardian extends Monster {

    protected static final int ATTACK_TIME = 80;
    private static final EntityDataAccessor<Boolean> DATA_ID_MOVING = SynchedEntityData.defineId(Guardian.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_ID_ATTACK_TARGET = SynchedEntityData.defineId(Guardian.class, EntityDataSerializers.INT);
    private float clientSideTailAnimation;
    private float clientSideTailAnimationO;
    private float clientSideTailAnimationSpeed;
    private float clientSideSpikesAnimation;
    private float clientSideSpikesAnimationO;
    @Nullable
    private LivingEntity clientSideCachedAttackTarget;
    private int clientSideAttackTime;
    private boolean clientSideTouchedGround;
    @Nullable
    public RandomStrollGoal randomStrollGoal;
    public Guardian.GuardianAttackGoal guardianAttackGoal; // CraftBukkit - add field

    public Guardian(EntityType<? extends Guardian> type, Level world) {
        super(type, world);
        this.xpReward = 10;
        this.setPathfindingMalus(BlockPathTypes.WATER, 0.0F);
        this.moveControl = new Guardian.GuardianMoveControl(this);
        // Purpur start
        this.lookControl = new org.purpurmc.purpur.controller.LookControllerWASD(this) {
            @Override
            public void setYawPitch(float yaw, float pitch) {
                super.setYawPitch(yaw, pitch * 0.35F);
            }
        };
        // Purpur end
        this.clientSideTailAnimation = this.random.nextFloat();
        this.clientSideTailAnimationO = this.clientSideTailAnimation;
    }

    // Purpur start
    @Override
    public boolean isRidable() {
        return level.purpurConfig.guardianRidable;
    }

    @Override
    public boolean isControllable() {
        return level.purpurConfig.guardianControllable;
    }

    @Override
    public void initAttributes() {
        this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(this.level.purpurConfig.guardianMaxHealth);
    }

    @Override
    public boolean isSensitiveToWater() {
        return this.level.purpurConfig.guardianTakeDamageFromWater;
    }

    @Override
    protected boolean isAlwaysExperienceDropper() {
        return this.level.purpurConfig.guardianAlwaysDropExp;
    }
    // Purpur end

    @Override
    protected void registerGoals() {
        MoveTowardsRestrictionGoal pathfindergoalmovetowardsrestriction = new MoveTowardsRestrictionGoal(this, 1.0D);

        this.randomStrollGoal = new RandomStrollGoal(this, 1.0D, 80);
        this.goalSelector.addGoal(0, new org.purpurmc.purpur.entity.ai.HasRider(this)); // Purpur
        this.goalSelector.addGoal(4, this.guardianAttackGoal = new Guardian.GuardianAttackGoal(this)); // CraftBukkit - assign field
        this.goalSelector.addGoal(5, pathfindergoalmovetowardsrestriction);
        this.goalSelector.addGoal(7, this.randomStrollGoal);
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Guardian.class, 12.0F, 0.01F));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        this.randomStrollGoal.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        pathfindergoalmovetowardsrestriction.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        this.targetSelector.addGoal(0, new org.purpurmc.purpur.entity.ai.HasRider(this)); // Purpur
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false, new Guardian.GuardianAttackSelector(this)));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.ATTACK_DAMAGE, 6.0D).add(Attributes.MOVEMENT_SPEED, 0.5D).add(Attributes.FOLLOW_RANGE, 16.0D).add(Attributes.MAX_HEALTH, 30.0D);
    }

    @Override
    protected PathNavigation createNavigation(Level world) {
        return new WaterBoundPathNavigation(this, world);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(Guardian.DATA_ID_MOVING, false);
        this.entityData.define(Guardian.DATA_ID_ATTACK_TARGET, 0);
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    @Override
    public MobType getMobType() {
        return MobType.WATER;
    }

    public boolean isMoving() {
        return (Boolean) this.entityData.get(Guardian.DATA_ID_MOVING);
    }

    void setMoving(boolean retracted) {
        this.entityData.set(Guardian.DATA_ID_MOVING, retracted);
    }

    public int getAttackDuration() {
        return 80;
    }

    public void setActiveAttackTarget(int entityId) {
        this.entityData.set(Guardian.DATA_ID_ATTACK_TARGET, entityId);
    }

    public boolean hasActiveAttackTarget() {
        return (Integer) this.entityData.get(Guardian.DATA_ID_ATTACK_TARGET) != 0;
    }

    @Nullable
    public LivingEntity getActiveAttackTarget() {
        if (!this.hasActiveAttackTarget()) {
            return null;
        } else if (this.level.isClientSide) {
            if (this.clientSideCachedAttackTarget != null) {
                return this.clientSideCachedAttackTarget;
            } else {
                Entity entity = this.level.getEntity((Integer) this.entityData.get(Guardian.DATA_ID_ATTACK_TARGET));

                if (entity instanceof LivingEntity) {
                    this.clientSideCachedAttackTarget = (LivingEntity) entity;
                    return this.clientSideCachedAttackTarget;
                } else {
                    return null;
                }
            }
        } else {
            return this.getTarget();
        }
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> data) {
        super.onSyncedDataUpdated(data);
        if (Guardian.DATA_ID_ATTACK_TARGET.equals(data)) {
            this.clientSideAttackTime = 0;
            this.clientSideCachedAttackTarget = null;
        }

    }

    @Override
    public int getAmbientSoundInterval() {
        return 160;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return this.isInWaterOrBubble() ? SoundEvents.GUARDIAN_AMBIENT : SoundEvents.GUARDIAN_AMBIENT_LAND;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return this.isInWaterOrBubble() ? SoundEvents.GUARDIAN_HURT : SoundEvents.GUARDIAN_HURT_LAND;
    }

    @Override
    public SoundEvent getDeathSound() {
        return this.isInWaterOrBubble() ? SoundEvents.GUARDIAN_DEATH : SoundEvents.GUARDIAN_DEATH_LAND;
    }

    @Override
    protected Entity.MovementEmission getMovementEmission() {
        return Entity.MovementEmission.EVENTS;
    }

    @Override
    protected float getStandingEyeHeight(Pose pose, EntityDimensions dimensions) {
        return dimensions.height * 0.5F;
    }

    @Override
    public float getWalkTargetValue(BlockPos pos, LevelReader world) {
        return world.getFluidState(pos).is(FluidTags.WATER) ? 10.0F + world.getPathfindingCostFromLightLevels(pos) : super.getWalkTargetValue(pos, world);
    }

    @Override
    public void aiStep() {
        if (this.isAlive()) {
            if (this.level.isClientSide) {
                this.clientSideTailAnimationO = this.clientSideTailAnimation;
                Vec3 vec3d;

                if (!this.isInWater()) {
                    this.clientSideTailAnimationSpeed = 2.0F;
                    vec3d = this.getDeltaMovement();
                    if (vec3d.y > 0.0D && this.clientSideTouchedGround && !this.isSilent()) {
                        this.level.playLocalSound(this.getX(), this.getY(), this.getZ(), this.getFlopSound(), this.getSoundSource(), 1.0F, 1.0F, false);
                    }

                    this.clientSideTouchedGround = vec3d.y < 0.0D && this.level.loadedAndEntityCanStandOn(this.blockPosition().below(), this);
                } else if (this.isMoving()) {
                    if (this.clientSideTailAnimationSpeed < 0.5F) {
                        this.clientSideTailAnimationSpeed = 4.0F;
                    } else {
                        this.clientSideTailAnimationSpeed += (0.5F - this.clientSideTailAnimationSpeed) * 0.1F;
                    }
                } else {
                    this.clientSideTailAnimationSpeed += (0.125F - this.clientSideTailAnimationSpeed) * 0.2F;
                }

                this.clientSideTailAnimation += this.clientSideTailAnimationSpeed;
                this.clientSideSpikesAnimationO = this.clientSideSpikesAnimation;
                if (!this.isInWaterOrBubble()) {
                    this.clientSideSpikesAnimation = this.random.nextFloat();
                } else if (this.isMoving()) {
                    this.clientSideSpikesAnimation += (0.0F - this.clientSideSpikesAnimation) * 0.25F;
                } else {
                    this.clientSideSpikesAnimation += (1.0F - this.clientSideSpikesAnimation) * 0.06F;
                }

                if (this.isMoving() && this.isInWater()) {
                    vec3d = this.getViewVector(0.0F);

                    for (int i = 0; i < 2; ++i) {
                        this.level.addParticle(ParticleTypes.BUBBLE, this.getRandomX(0.5D) - vec3d.x * 1.5D, this.getRandomY() - vec3d.y * 1.5D, this.getRandomZ(0.5D) - vec3d.z * 1.5D, 0.0D, 0.0D, 0.0D);
                    }
                }

                if (this.hasActiveAttackTarget()) {
                    if (this.clientSideAttackTime < this.getAttackDuration()) {
                        ++this.clientSideAttackTime;
                    }

                    LivingEntity entityliving = this.getActiveAttackTarget();

                    if (entityliving != null) {
                        this.getLookControl().setLookAt(entityliving, 90.0F, 90.0F);
                        this.getLookControl().tick();
                        double d0 = (double) this.getAttackAnimationScale(0.0F);
                        double d1 = entityliving.getX() - this.getX();
                        double d2 = entityliving.getY(0.5D) - this.getEyeY();
                        double d3 = entityliving.getZ() - this.getZ();
                        double d4 = Math.sqrt(d1 * d1 + d2 * d2 + d3 * d3);

                        d1 /= d4;
                        d2 /= d4;
                        d3 /= d4;
                        double d5 = this.random.nextDouble();

                        while (d5 < d4) {
                            d5 += 1.8D - d0 + this.random.nextDouble() * (1.7D - d0);
                            this.level.addParticle(ParticleTypes.BUBBLE, this.getX() + d1 * d5, this.getEyeY() + d2 * d5, this.getZ() + d3 * d5, 0.0D, 0.0D, 0.0D);
                        }
                    }
                }
            }

            if (this.isInWaterOrBubble()) {
                this.setAirSupply(300);
            } else if (this.onGround) {
                this.setDeltaMovement(this.getDeltaMovement().add((double) ((this.random.nextFloat() * 2.0F - 1.0F) * 0.4F), 0.5D, (double) ((this.random.nextFloat() * 2.0F - 1.0F) * 0.4F)));
                this.setYRot(this.random.nextFloat() * 360.0F);
                this.onGround = false;
                this.hasImpulse = true;
            }

            if (this.hasActiveAttackTarget()) {
                this.setYRot(this.yHeadRot);
            }
        }

        super.aiStep();
    }

    protected SoundEvent getFlopSound() {
        return SoundEvents.GUARDIAN_FLOP;
    }

    public float getTailAnimation(float tickDelta) {
        return Mth.lerp(tickDelta, this.clientSideTailAnimationO, this.clientSideTailAnimation);
    }

    public float getSpikesAnimation(float tickDelta) {
        return Mth.lerp(tickDelta, this.clientSideSpikesAnimationO, this.clientSideSpikesAnimation);
    }

    public float getAttackAnimationScale(float tickDelta) {
        return ((float) this.clientSideAttackTime + tickDelta) / (float) this.getAttackDuration();
    }

    public float getClientSideAttackTime() {
        return (float) this.clientSideAttackTime;
    }

    @Override
    public boolean checkSpawnObstruction(LevelReader world) {
        return world.isUnobstructed(this);
    }

    public static boolean checkGuardianSpawnRules(EntityType<? extends Guardian> type, LevelAccessor world, MobSpawnType spawnReason, BlockPos pos, RandomSource random) {
        return (random.nextInt(20) == 0 || !world.canSeeSkyFromBelowWater(pos)) && world.getDifficulty() != Difficulty.PEACEFUL && (spawnReason == MobSpawnType.SPAWNER || world.getFluidState(pos).is(FluidTags.WATER)) && world.getFluidState(pos.below()).is(FluidTags.WATER);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.level.isClientSide) {
            return false;
        } else {
            if (!this.isMoving() && !source.is(DamageTypeTags.AVOIDS_GUARDIAN_THORNS) && !source.is(DamageTypes.THORNS)) {
                Entity entity = source.getDirectEntity();

                if (entity instanceof LivingEntity) {
                    LivingEntity entityliving = (LivingEntity) entity;

                    entityliving.hurt(this.damageSources().thorns(this), 2.0F);
                }
            }

            if (this.randomStrollGoal != null) {
                this.randomStrollGoal.trigger();
            }

            return super.hurt(source, amount);
        }
    }

    @Override
    public int getMaxHeadXRot() {
        return 180;
    }

    @Override
    public void travel(Vec3 movementInput) {
        if (this.isControlledByLocalInstance() && this.isInWater()) {
            this.moveRelative(getRider() != null && this.isControllable() ? getSpeed() : 0.1F, movementInput); // Purpur
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(0.9D));
            if (!this.isMoving() && this.getTarget() == null) {
                this.setDeltaMovement(this.getDeltaMovement().add(0.0D, -0.005D, 0.0D));
            }
        } else {
            super.travel(movementInput);
        }

    }

    private static class GuardianMoveControl extends org.purpurmc.purpur.controller.WaterMoveControllerWASD { // Purpur

        private final Guardian guardian;

        public GuardianMoveControl(Guardian guardian) {
            super(guardian);
            this.guardian = guardian;
        }

        // Purpur start
        @Override
        public void purpurTick(Player rider) {
            super.purpurTick(rider);
            guardian.setDeltaMovement(guardian.getDeltaMovement().add(0.0D, 0.005D, 0.0D));
            guardian.setMoving(guardian.getForwardMot() > 0.0F); // control tail speed
        }
        // Purpur end

        @Override
        public void vanillaTick() { // Purpur
            if (this.operation == MoveControl.Operation.MOVE_TO && !this.guardian.getNavigation().isDone()) {
                Vec3 vec3d = new Vec3(this.wantedX - this.guardian.getX(), this.wantedY - this.guardian.getY(), this.wantedZ - this.guardian.getZ());
                double d0 = vec3d.length();
                double d1 = vec3d.x / d0;
                double d2 = vec3d.y / d0;
                double d3 = vec3d.z / d0;
                float f = (float) (Mth.atan2(vec3d.z, vec3d.x) * 57.2957763671875D) - 90.0F;

                this.guardian.setYRot(this.rotlerp(this.guardian.getYRot(), f, 90.0F));
                this.guardian.yBodyRot = this.guardian.getYRot();
                float f1 = (float) (this.getSpeedModifier() * this.guardian.getAttributeValue(Attributes.MOVEMENT_SPEED)); // Purpur
                float f2 = Mth.lerp(0.125F, this.guardian.getSpeed(), f1);

                this.guardian.setSpeed(f2);
                double d4 = Math.sin((double) (this.guardian.tickCount + this.guardian.getId()) * 0.5D) * 0.05D;
                double d5 = Math.cos((double) (this.guardian.getYRot() * 0.017453292F));
                double d6 = Math.sin((double) (this.guardian.getYRot() * 0.017453292F));
                double d7 = Math.sin((double) (this.guardian.tickCount + this.guardian.getId()) * 0.75D) * 0.05D;

                this.guardian.setDeltaMovement(this.guardian.getDeltaMovement().add(d4 * d5, d7 * (d6 + d5) * 0.25D + (double) f2 * d2 * 0.1D, d4 * d6));
                LookControl controllerlook = this.guardian.getLookControl();
                double d8 = this.guardian.getX() + d1 * 2.0D;
                double d9 = this.guardian.getEyeY() + d2 / d0;
                double d10 = this.guardian.getZ() + d3 * 2.0D;
                double d11 = controllerlook.getWantedX();
                double d12 = controllerlook.getWantedY();
                double d13 = controllerlook.getWantedZ();

                if (!controllerlook.isLookingAtTarget()) {
                    d11 = d8;
                    d12 = d9;
                    d13 = d10;
                }

                this.guardian.getLookControl().setLookAt(Mth.lerp(0.125D, d11, d8), Mth.lerp(0.125D, d12, d9), Mth.lerp(0.125D, d13, d10), 10.0F, 40.0F);
                this.guardian.setMoving(true);
            } else {
                this.guardian.setSpeed(0.0F);
                this.guardian.setMoving(false);
            }
        }
    }

    public static class GuardianAttackGoal extends Goal {

        private final Guardian guardian;
        public int attackTime;
        private final boolean elder;

        public GuardianAttackGoal(Guardian guardian) {
            this.guardian = guardian;
            this.elder = guardian instanceof ElderGuardian;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity entityliving = this.guardian.getTarget();

            return entityliving != null && entityliving.isAlive();
        }

        @Override
        public boolean canContinueToUse() {
            return super.canContinueToUse() && (this.elder || this.guardian.getTarget() != null && this.guardian.distanceToSqr((Entity) this.guardian.getTarget()) > 9.0D);
        }

        @Override
        public void start() {
            this.attackTime = -10;
            this.guardian.getNavigation().stop();
            LivingEntity entityliving = this.guardian.getTarget();

            if (entityliving != null) {
                this.guardian.getLookControl().setLookAt(entityliving, 90.0F, 90.0F);
            }

            this.guardian.hasImpulse = true;
        }

        @Override
        public void stop() {
            this.guardian.setActiveAttackTarget(0);
            this.guardian.setTarget((LivingEntity) null);
            this.guardian.randomStrollGoal.trigger();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity entityliving = this.guardian.getTarget();

            if (entityliving != null) {
                this.guardian.getNavigation().stop();
                this.guardian.getLookControl().setLookAt(entityliving, 90.0F, 90.0F);
                if (!this.guardian.hasLineOfSight(entityliving)) {
                    this.guardian.setTarget((LivingEntity) null);
                } else {
                    ++this.attackTime;
                    if (this.attackTime == 0) {
                        this.guardian.setActiveAttackTarget(entityliving.getId());
                        if (!this.guardian.isSilent()) {
                            this.guardian.level.broadcastEntityEvent(this.guardian, (byte) 21);
                        }
                    } else if (this.attackTime >= this.guardian.getAttackDuration()) {
                        float f = 1.0F;

                        if (this.guardian.level.getDifficulty() == Difficulty.HARD) {
                            f += 2.0F;
                        }

                        if (this.elder) {
                            f += 2.0F;
                        }

                        entityliving.hurt(this.guardian.damageSources().indirectMagic(this.guardian, this.guardian), f);
                        entityliving.hurt(this.guardian.damageSources().mobAttack(this.guardian), (float) this.guardian.getAttributeValue(Attributes.ATTACK_DAMAGE));
                        this.guardian.setTarget((LivingEntity) null);
                    }

                    super.tick();
                }
            }
        }
    }

    private static class GuardianAttackSelector implements Predicate<LivingEntity> {

        private final Guardian guardian;

        public GuardianAttackSelector(Guardian owner) {
            this.guardian = owner;
        }

        public boolean test(@Nullable LivingEntity entityliving) {
            return (entityliving instanceof Player || entityliving instanceof Squid || entityliving instanceof Axolotl) && entityliving.distanceToSqr((Entity) this.guardian) > 9.0D;
        }
    }
}
