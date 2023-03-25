package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.schedule.Activity;

public class VillagerPanicTrigger extends Behavior<Villager> {
    public VillagerPanicTrigger() {
        super(ImmutableMap.of());
    }

    @Override
    protected boolean canStillUse(ServerLevel world, Villager entity, long time) {
        return isHurt(entity) || hasHostile(entity);
    }

    @Override
    protected void start(ServerLevel serverLevel, Villager villager, long l) {
        if (isHurt(villager) || hasHostile(villager)) {
            Brain<?> brain = villager.getBrain();
            if (!brain.isActive(Activity.PANIC)) {
                brain.eraseMemory(MemoryModuleType.PATH);
                brain.eraseMemory(MemoryModuleType.WALK_TARGET);
                brain.eraseMemory(MemoryModuleType.LOOK_TARGET);
                brain.eraseMemory(MemoryModuleType.BREED_TARGET);
                brain.eraseMemory(MemoryModuleType.INTERACTION_TARGET);
            }

            brain.setActiveActivityIfPossible(Activity.PANIC);
        }

    }

    @Override
    protected void tick(ServerLevel world, Villager entity, long time) {
        // Pufferfish start
        if (entity.nextGolemPanic < 0) entity.nextGolemPanic = time + 100;
        if (--entity.nextGolemPanic < time) {
            entity.nextGolemPanic = -1;
            // Pufferfish end
            entity.spawnGolemIfNeeded(world, time, 3);
        }

    }

    public static boolean hasHostile(LivingEntity entity) {
        return entity.getBrain().hasMemoryValue(MemoryModuleType.NEAREST_HOSTILE);
    }

    public static boolean isHurt(LivingEntity entity) {
        return entity.getBrain().hasMemoryValue(MemoryModuleType.HURT_BY);
    }
}