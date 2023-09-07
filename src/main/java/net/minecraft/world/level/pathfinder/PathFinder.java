package net.minecraft.world.level.pathfinder;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.profiling.metrics.MetricCategory;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.PathNavigationRegion;

public class PathFinder {
    private static final float FUDGING = 1.5F;
    private final Node[] neighbors = new Node[32];
    private final int maxVisitedNodes;
    public final NodeEvaluator nodeEvaluator;
    private static final boolean DEBUG = false;
    private final BinaryHeap openSet = new BinaryHeap();
    private final @Nullable org.plazmamc.plazma.entity.path.NodeEvaluatorGenerator nodeEvaluatorGenerator; // Plazma - we use this later to generate an evaluator

    // Plazma start - support nodeEvaluatorgenerators
    public PathFinder(NodeEvaluator pathNodeMaker, int range) {
        this(pathNodeMaker, range, null);
    }

    public PathFinder(NodeEvaluator pathNodeMaker, int range, @Nullable org.plazmamc.plazma.entity.path.NodeEvaluatorGenerator nodeEvaluatorGenerator) {
        this.nodeEvaluator = pathNodeMaker;
        this.maxVisitedNodes = range;
        this.nodeEvaluatorGenerator = nodeEvaluatorGenerator;
    }
    // Plazma end

    @Nullable
    public Path findPath(PathNavigationRegion world, Mob mob, Set<BlockPos> positions, float followRange, int distance, float rangeMultiplier) {
        if (!mob.level.plazmaLevelConfiguration().entity.asyncPathProcessing.enabled) this.openSet.clear();// Plazma - it's always cleared in processPath
        // Plazma start - use a generated evaluator if we have one otherwise run sync
        NodeEvaluator nodeEvaluator = this.nodeEvaluatorGenerator == null ? this.nodeEvaluator : org.plazmamc.plazma.entity.path.NodeEvaluatorCache.takeNodeEvaluator(this.nodeEvaluatorGenerator, this.nodeEvaluator);
        nodeEvaluator.prepare(world, mob);
        Node node = nodeEvaluator.getStart();
        // Plazma end
        if (node == null) {
            org.plazmamc.plazma.entity.path.NodeEvaluatorCache.removeNodeEvaluator(nodeEvaluator); // Plazma - handle nodeEvaluatorGenerator
            return null;
        } else {
            // Paper start - remove streams - and optimize collection
            List<Map.Entry<Target, BlockPos>> map = Lists.newArrayList();
            for (BlockPos pos : positions) {
                map.add(new java.util.AbstractMap.SimpleEntry<>(nodeEvaluator.getGoal(pos.getX(), pos.getY(), pos.getZ()), pos)); // Plazma - handle nodeEvaluatorGenerator
            }
            // Paper end
            // Plazma start - async path processing
            if (this.nodeEvaluatorGenerator == null) {
                // run sync :(
                org.plazmamc.plazma.entity.path.NodeEvaluatorCache.removeNodeEvaluator(nodeEvaluator);
                return this.findPath(world.getProfiler(), node, map, followRange, distance, rangeMultiplier);
            }

            return new org.plazmamc.plazma.entity.path.AsyncPath(Lists.newArrayList(), positions, () -> {
                try {
                    return this.processPath(nodeEvaluator, node, map, followRange, distance, rangeMultiplier);
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                } finally {
                    nodeEvaluator.done();
                    org.plazmamc.plazma.entity.path.NodeEvaluatorCache.returnNodeEvaluator(nodeEvaluator);
                }
            });
            // Plazma end
        }
    }

    // Plazma start - split pathfinding into the original sync method for compat and processing for delaying
    //@Nullable // Plazma - Always not null
    // Paper start - optimize collection
    private Path findPath(ProfilerFiller profiler, Node startNode, List<Map.Entry<Target, BlockPos>> positions, float followRange, int distance, float rangeMultiplier) {
        try {
            return this.processPath(this.nodeEvaluator, startNode, positions, followRange, distance, rangeMultiplier);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            this.nodeEvaluator.done();
        }
    }

    private synchronized @org.jetbrains.annotations.NotNull Path processPath(NodeEvaluator nodeEvaluator, Node startNode, List<Map.Entry<Target, BlockPos>> positions, float followRange, int distance, float rangeMultiplier) { // sync to only use the caching functions in this class on a single thread
        org.apache.commons.lang3.Validate.isTrue(!positions.isEmpty()); // ensure that we have at least one position, which means we'll always return a path
        //profiler.push("find_path"); // Purpur
        //profiler.markForCharting(MetricCategory.PATH_FINDING); // Purpur
        // Set<Target> set = positions.keySet();
        startNode.g = 0.0F;
        startNode.h = this.getBestH(startNode, positions); // Paper - optimize collection
        startNode.f = startNode.h;
        this.openSet.clear();
        this.openSet.insert(startNode);
        // Set<Node> set2 = ImmutableSet.of(); // Paper - unused - diff on change
        int i = 0;
        List<Map.Entry<Target, BlockPos>> entryList = Lists.newArrayListWithExpectedSize(positions.size()); // Paper - optimize collection
        int j = (int)((float)this.maxVisitedNodes * rangeMultiplier);

        while(!this.openSet.isEmpty()) {
            ++i;
            if (i >= j) {
                break;
            }

            Node node = this.openSet.pop();
            node.closed = true;

            // Paper start - optimize collection
            for(int i1 = 0; i1 < positions.size(); i1++) {
                final Map.Entry<Target, BlockPos> entry = positions.get(i1);
                Target target = entry.getKey();
                if (node.distanceManhattan(target) <= (float)distance) {
                    target.setReached();
                    entryList.add(entry);
                    // Paper end
                }
            }

            if (!entryList.isEmpty()) { // Paper - rename variable
                break;
            }

            if (!(node.distanceTo(startNode) >= followRange)) {
                int k = nodeEvaluator.getNeighbors(this.neighbors, node); // Plazma - use provided nodeEvaluator

                for(int l = 0; l < k; ++l) {
                    Node node2 = this.neighbors[l];
                    float f = this.distance(node, node2);
                    node2.walkedDistance = node.walkedDistance + f;
                    float g = node.g + f + node2.costMalus;
                    if (node2.walkedDistance < followRange && (!node2.inOpenSet() || g < node2.g)) {
                        node2.cameFrom = node;
                        node2.g = g;
                        node2.h = this.getBestH(node2, positions) * 1.5F; // Paper - list instead of set
                        if (node2.inOpenSet()) {
                            this.openSet.changeCost(node2, node2.g + node2.h);
                        } else {
                            node2.f = node2.g + node2.h;
                            this.openSet.insert(node2);
                        }
                    }
                }
            }
        }

        // Paper start - remove streams - and optimize collection
        Path best = null;
        boolean entryListIsEmpty = entryList.isEmpty();
        Comparator<Path> comparator = entryListIsEmpty ? Comparator.comparingInt(Path::getNodeCount)
            : Comparator.comparingDouble(Path::getDistToTarget).thenComparingInt(Path::getNodeCount);
        for (Map.Entry<Target, BlockPos> entry : entryListIsEmpty ? positions : entryList) {
            Path path = this.reconstructPath(entry.getKey().getBestNode(), entry.getValue(), !entryListIsEmpty);
            if (best == null || comparator.compare(path, best) < 0)
                best = path;
        }
        return best;
        // Paper end
    }
    // Plazma end

    protected float distance(Node a, Node b) {
        return a.distanceTo(b);
    }

    private float getBestH(Node node, List<Map.Entry<Target, BlockPos>> targets) { // Paper - optimize collection - Set<Target> -> List<Map.Entry<Target, BlockPos>>
        float f = Float.MAX_VALUE;

        // Paper start - optimize collection
        for (int i = 0, targetsSize = targets.size(); i < targetsSize; i++) {
            final Target target = targets.get(i).getKey();
            // Paper end
            float g = node.distanceTo(target);
            target.updateBest(g, node);
            f = Math.min(g, f);
        }

        return f;
    }

    private Path reconstructPath(Node endNode, BlockPos target, boolean reachesTarget) {
        List<Node> list = Lists.newArrayList();
        Node node = endNode;
        list.add(0, endNode);

        while(node.cameFrom != null) {
            node = node.cameFrom;
            list.add(0, node);
        }

        return new Path(list, target, reachesTarget);
    }
}
