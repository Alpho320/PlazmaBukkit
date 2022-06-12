package net.minecraft.world.item;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

public class AxeItem extends DiggerItem {
    protected static final Map<Block, Block> STRIPPABLES = (new ImmutableMap.Builder<Block, Block>()).put(Blocks.OAK_WOOD, Blocks.STRIPPED_OAK_WOOD).put(Blocks.OAK_LOG, Blocks.STRIPPED_OAK_LOG).put(Blocks.DARK_OAK_WOOD, Blocks.STRIPPED_DARK_OAK_WOOD).put(Blocks.DARK_OAK_LOG, Blocks.STRIPPED_DARK_OAK_LOG).put(Blocks.ACACIA_WOOD, Blocks.STRIPPED_ACACIA_WOOD).put(Blocks.ACACIA_LOG, Blocks.STRIPPED_ACACIA_LOG).put(Blocks.CHERRY_WOOD, Blocks.STRIPPED_CHERRY_WOOD).put(Blocks.CHERRY_LOG, Blocks.STRIPPED_CHERRY_LOG).put(Blocks.BIRCH_WOOD, Blocks.STRIPPED_BIRCH_WOOD).put(Blocks.BIRCH_LOG, Blocks.STRIPPED_BIRCH_LOG).put(Blocks.JUNGLE_WOOD, Blocks.STRIPPED_JUNGLE_WOOD).put(Blocks.JUNGLE_LOG, Blocks.STRIPPED_JUNGLE_LOG).put(Blocks.SPRUCE_WOOD, Blocks.STRIPPED_SPRUCE_WOOD).put(Blocks.SPRUCE_LOG, Blocks.STRIPPED_SPRUCE_LOG).put(Blocks.WARPED_STEM, Blocks.STRIPPED_WARPED_STEM).put(Blocks.WARPED_HYPHAE, Blocks.STRIPPED_WARPED_HYPHAE).put(Blocks.CRIMSON_STEM, Blocks.STRIPPED_CRIMSON_STEM).put(Blocks.CRIMSON_HYPHAE, Blocks.STRIPPED_CRIMSON_HYPHAE).put(Blocks.MANGROVE_WOOD, Blocks.STRIPPED_MANGROVE_WOOD).put(Blocks.MANGROVE_LOG, Blocks.STRIPPED_MANGROVE_LOG).put(Blocks.BAMBOO_BLOCK, Blocks.STRIPPED_BAMBOO_BLOCK).build();

    protected AxeItem(Tier material, float attackDamage, float attackSpeed, Item.Properties settings) {
        super(attackDamage, attackSpeed, material, BlockTags.MINEABLE_WITH_AXE, settings);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos blockPos = context.getClickedPos();
        Player player = context.getPlayer();
        BlockState blockState = level.getBlockState(blockPos);
        // Purpur start
        Block clickedBlock = level.getBlockState(blockPos).getBlock();
        Optional<org.purpurmc.purpur.tool.Actionable> optional = Optional.ofNullable(level.purpurConfig.axeStrippables.get(blockState.getBlock()));
        Optional<org.purpurmc.purpur.tool.Actionable> optional2 = Optional.ofNullable(level.purpurConfig.axeWeatherables.get(blockState.getBlock()));
        Optional<org.purpurmc.purpur.tool.Actionable> optional3 = Optional.ofNullable(level.purpurConfig.axeWaxables.get(blockState.getBlock()));
        // Purpur end
        ItemStack itemStack = context.getItemInHand();
        Optional<org.purpurmc.purpur.tool.Actionable> optional4 = Optional.empty(); // Purpur
        if (optional.isPresent()) {
            if (!STRIPPABLES.containsKey(clickedBlock)) level.playSound(null, blockPos, SoundEvents.AXE_STRIP, SoundSource.BLOCKS, 1.0F, 1.0F); // Purpur - force sound
            optional4 = optional;
        } else if (optional2.isPresent()) {
            if (!HoneycombItem.WAXABLES.get().containsKey(clickedBlock)) level.playSound(null, blockPos, SoundEvents.AXE_SCRAPE, SoundSource.BLOCKS, 1.0F, 1.0F); // Purpur - force sound
            level.levelEvent(player, 3005, blockPos, 0);
            optional4 = optional2;
        } else if (optional3.isPresent()) {
            if (!HoneycombItem.WAX_OFF_BY_BLOCK.get().containsKey(clickedBlock)) level.playSound(null, blockPos, SoundEvents.AXE_WAX_OFF, SoundSource.BLOCKS, 1.0F, 1.0F); // Purpur - force sound
            level.levelEvent(player, 3004, blockPos, 0);
            optional4 = optional3;
        }

        if (optional4.isPresent()) {
            org.purpurmc.purpur.tool.Actionable actionable = optional4.get();
            BlockState state = actionable.into().withPropertiesOf(blockState); // Purpur
            // Paper start - EntityChangeBlockEvent
            if (org.bukkit.craftbukkit.event.CraftEventFactory.callEntityChangeBlockEvent(player, blockPos, state).isCancelled()) { // Purpur
                return InteractionResult.PASS;
            }
            // Paper end
            if (player instanceof ServerPlayer) {
                CriteriaTriggers.ITEM_USED_ON_BLOCK.trigger((ServerPlayer)player, blockPos, itemStack);
            }

            // Purpur start
            level.setBlock(blockPos, state, 11);
            actionable.drops().forEach((drop, chance) -> {
                if (level.random.nextDouble() < chance) {
                    Block.popResourceFromFace(level, blockPos, context.getClickedFace(), new ItemStack(drop));
                }
            });
            level.gameEvent(GameEvent.BLOCK_CHANGE, blockPos, GameEvent.Context.of(player, state));
            // Purpur end
            if (player != null) {
                itemStack.hurtAndBreak(1, player, (p) -> {
                    p.broadcastBreakEvent(context.getHand());
                });
            }

            return InteractionResult.SUCCESS; // Purpur - force arm swing
        } else {
            return InteractionResult.PASS;
        }
    }

    private Optional<BlockState> getStripped(BlockState state) {
        return Optional.ofNullable(STRIPPABLES.get(state.getBlock())).map((block) -> {
            return block.defaultBlockState().setValue(RotatedPillarBlock.AXIS, state.getValue(RotatedPillarBlock.AXIS));
        });
    }
}
