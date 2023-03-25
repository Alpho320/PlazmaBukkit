package net.minecraft.world.entity.ai.navigation;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.SwimNodeEvaluator;
import net.minecraft.world.phys.Vec3;

public class WaterBoundPathNavigation extends PathNavigation {
    private boolean allowBreaching;

    public WaterBoundPathNavigation(Mob entity, Level world) {
        super(entity, world);
    }

    @Override
    protected PathFinder createPathFinder(int range) {
        this.allowBreaching = this.mob.getType() == EntityType.DOLPHIN;
        this.nodeEvaluator = new SwimNodeEvaluator(this.allowBreaching);
        return new PathFinder(this.nodeEvaluator, range);
    }

    @Override
    protected boolean canUpdatePath() {
        return this.allowBreaching || this.isInLiquid();
    }

    @Override
    protected Vec3 getTempMobPos() {
        return new Vec3(this.mob.getX(), this.mob.getY(0.5D), this.mob.getZ());
    }

    @Override
    protected double getGroundY(Vec3 pos) {
        return pos.y;
    }

    @Override
    protected boolean canMoveDirectly(Vec3 origin, Vec3 target) {
        return isClearForMovementBetween(this.mob, origin, target, false);
    }

    @Override
    public boolean isStableDestination(BlockPos pos) {
        return !this.level.getBlockState(pos).isSolidRender(this.level, pos);
    }

    @Override
    public void setCanFloat(boolean canSwim) {
    }
}