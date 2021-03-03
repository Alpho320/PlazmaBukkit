package net.minecraft.world.entity.ai.sensing;

import com.google.common.collect.ImmutableSet;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.item.ItemEntity;

public class NearestItemSensor extends Sensor<Mob> {
    private static final long XZ_RANGE = 32L;
    private static final long Y_RANGE = 16L;
    public static final int MAX_DISTANCE_TO_WANTED_ITEM = 32;

    @Override
    public Set<MemoryModuleType<?>> requires() {
        return ImmutableSet.of(MemoryModuleType.NEAREST_VISIBLE_WANTED_ITEM);
    }

    @Override
    protected void doTick(ServerLevel world, Mob entity) {
        Brain<?> brain = entity.getBrain();
        List<ItemEntity> list = world.getEntitiesOfClass(ItemEntity.class, entity.getBoundingBox().inflate(32.0D, 16.0D, 32.0D), (itemEntity) -> {
            return true;
        });
        list.sort(Comparator.comparingDouble(entity::distanceToSqr));
        // Paper start - remove streams in favour of lists
        ItemEntity nearest = null;
        for (ItemEntity entityItem : list) {
            if (entity.wantsToPickUp(entityItem.getItem()) && entityItem.closerThan(entity, 32.0D) && entity.hasLineOfSight(entityItem)) {
                nearest = entityItem;
                break;
            }
        }
        brain.setMemory(MemoryModuleType.NEAREST_VISIBLE_WANTED_ITEM, Optional.ofNullable(nearest));
        // Paper end
    }
}
