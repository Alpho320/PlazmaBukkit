package org.plazmamc.plazma.entity.path;

import net.minecraft.world.level.pathfinder.NodeEvaluator;
import org.jetbrains.annotations.NotNull;

public interface NodeEvaluatorGenerator {

    @NotNull NodeEvaluator generate(NodeEvaluatorFeatures nodeEvaluatorFeatures);

}