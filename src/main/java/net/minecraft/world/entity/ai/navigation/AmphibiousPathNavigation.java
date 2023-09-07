package net.minecraft.world.entity.ai.navigation;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.AmphibiousNodeEvaluator;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.phys.Vec3;

public class AmphibiousPathNavigation extends PathNavigation {
    public AmphibiousPathNavigation(Mob mob, Level world) {
        super(mob, world);
    }

    // Plamza start - async path processing
    private static final org.plazmamc.plazma.entity.path.NodeEvaluatorGenerator nodeEvaluatorGenerator = (org.plazmamc.plazma.entity.path.NodeEvaluatorFeatures nodeEvaluatorFeatures) -> {
        AmphibiousNodeEvaluator nodeEvaluator = new AmphibiousNodeEvaluator(false);
        nodeEvaluator.setCanPassDoors(nodeEvaluatorFeatures.canPassDoors());
        nodeEvaluator.setCanFloat(nodeEvaluatorFeatures.canFloat());
        nodeEvaluator.setCanWalkOverFences(nodeEvaluatorFeatures.canWalkOverFences());
        nodeEvaluator.setCanOpenDoors(nodeEvaluatorFeatures.canOpenDoors());
        return nodeEvaluator;
    };
    // Plazma end

    @Override
    protected PathFinder createPathFinder(int range) {
        this.nodeEvaluator = new AmphibiousNodeEvaluator(false);
        this.nodeEvaluator.setCanPassDoors(true);
        if (this.level.plazmaLevelConfiguration().entity.asyncPathProcessing.enabled) return new PathFinder(this.nodeEvaluator, range, nodeEvaluatorGenerator); // Plazma - async path processing
        return new PathFinder(this.nodeEvaluator, range);
    }

    @Override
    protected boolean canUpdatePath() {
        return true;
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
        return this.isInLiquid() ? isClearForMovementBetween(this.mob, origin, target, false) : false;
    }

    @Override
    public boolean isStableDestination(BlockPos pos) {
        return !this.level.getBlockState(pos.below()).isAir();
    }

    @Override
    public void setCanFloat(boolean canSwim) {
    }
}
