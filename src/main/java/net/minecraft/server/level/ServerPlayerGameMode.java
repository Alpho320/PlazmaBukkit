package net.minecraft.server.level;

import com.mojang.logging.LogUtils;
import java.util.Objects;
import javax.annotation.Nullable;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.GameMasterBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

// CraftBukkit start
import java.util.ArrayList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CakeBlock;
import net.minecraft.world.level.block.DoorBlock;
import org.bukkit.GameMode;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
// CraftBukkit end

public class ServerPlayerGameMode {

    private static final Logger LOGGER = LogUtils.getLogger();
    public ServerLevel level; // Paper - Anti-Xray - protected -> public
    protected final ServerPlayer player;
    private GameType gameModeForPlayer;
    @Nullable
    private GameType previousGameModeForPlayer;
    private boolean isDestroyingBlock;
    private int destroyProgressStart;
    private BlockPos destroyPos;
    private int gameTicks;
    private boolean hasDelayedDestroy;
    private BlockPos delayedDestroyPos;
    private int delayedTickStart;
    private int lastSentState;

    public ServerPlayerGameMode(ServerPlayer player) {
        this.gameModeForPlayer = GameType.DEFAULT_MODE;
        this.destroyPos = BlockPos.ZERO;
        this.delayedDestroyPos = BlockPos.ZERO;
        this.lastSentState = -1;
        this.player = player;
        this.level = player.getLevel();
    }

    public boolean changeGameModeForPlayer(GameType gameMode) {
        if (gameMode == this.gameModeForPlayer) {
            return false;
        } else {
            // CraftBukkit start
            PlayerGameModeChangeEvent event = new PlayerGameModeChangeEvent(this.player.getBukkitEntity(), GameMode.getByValue(gameMode.getId()));
            this.level.getCraftServer().getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                return false;
            }
            // CraftBukkit end
            this.setGameModeForPlayer(gameMode, this.previousGameModeForPlayer);
            this.player.onUpdateAbilities();
            this.player.server.getPlayerList().broadcastAll(new ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_GAME_MODE, this.player), this.player); // CraftBukkit
            this.level.updateSleepingPlayerList();
            return true;
        }
    }

    protected void setGameModeForPlayer(GameType gameMode, @Nullable GameType previousGameMode) {
        this.previousGameModeForPlayer = previousGameMode;
        this.gameModeForPlayer = gameMode;
        gameMode.updatePlayerAbilities(this.player.getAbilities());
    }

    public GameType getGameModeForPlayer() {
        return this.gameModeForPlayer;
    }

    @Nullable
    public GameType getPreviousGameModeForPlayer() {
        return this.previousGameModeForPlayer;
    }

    public boolean isSurvival() {
        return this.gameModeForPlayer.isSurvival();
    }

    public boolean isCreative() {
        return this.gameModeForPlayer.isCreative();
    }

    public void tick() {
        this.gameTicks = MinecraftServer.currentTick; // CraftBukkit;
        BlockState iblockdata;

        if (this.hasDelayedDestroy) {
            iblockdata = this.level.getBlockStateIfLoaded(this.delayedDestroyPos); // Paper
            if (iblockdata == null || iblockdata.isAir()) { // Paper
                this.hasDelayedDestroy = false;
            } else {
                float f = this.incrementDestroyProgress(iblockdata, this.delayedDestroyPos, this.delayedTickStart);

                if (f >= 1.0F) {
                    this.hasDelayedDestroy = false;
                    this.destroyBlock(this.delayedDestroyPos);
                }
            }
        } else if (this.isDestroyingBlock) {
            // Paper start - don't want to do same logic as above, return instead
            iblockdata = this.level.getBlockStateIfLoaded(this.destroyPos);
            if (iblockdata == null) {
                this.isDestroyingBlock = false;
                return;
            }
            // Paper end
            if (iblockdata.isAir()) {
                this.level.destroyBlockProgress(this.player.getId(), this.destroyPos, -1);
                this.lastSentState = -1;
                this.isDestroyingBlock = false;
            } else {
                this.incrementDestroyProgress(iblockdata, this.destroyPos, this.destroyProgressStart);
            }
        }

    }

    private float incrementDestroyProgress(BlockState state, BlockPos pos, int failedStartMiningTime) {
        int j = this.gameTicks - failedStartMiningTime;
        float f = state.getDestroyProgress(this.player, this.player.level, pos) * (float) (j + 1);
        int k = (int) (f * 10.0F);

        if (k != this.lastSentState) {
            this.level.destroyBlockProgress(this.player.getId(), pos, k);
            this.lastSentState = k;
        }

        return f;
    }

    private void debugLogging(BlockPos pos, boolean success, int sequence, String reason) {}

    public void handleBlockBreakAction(BlockPos pos, ServerboundPlayerActionPacket.Action action, Direction direction, int worldHeight, int sequence) {
        if (this.player.getEyePosition().distanceToSqr(Vec3.atCenterOf(pos)) > ServerGamePacketListenerImpl.MAX_INTERACTION_DISTANCE) {
            if (true) return; // Paper - Don't notify if unreasonably far away
            this.debugLogging(pos, false, sequence, "too far");
        } else if (pos.getY() >= worldHeight) {
            this.player.connection.send(new ClientboundBlockUpdatePacket(pos, this.level.getBlockState(pos)));
            this.debugLogging(pos, false, sequence, "too high");
        } else {
            BlockState iblockdata;

            if (action == ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK) {
                if (!this.level.mayInteract(this.player, pos)) {
                    // CraftBukkit start - fire PlayerInteractEvent
                    CraftEventFactory.callPlayerInteractEvent(this.player, Action.LEFT_CLICK_BLOCK, pos, direction, this.player.getInventory().getSelected(), InteractionHand.MAIN_HAND);
                    this.player.connection.send(new ClientboundBlockUpdatePacket(pos, this.level.getBlockState(pos)));
                    this.debugLogging(pos, false, sequence, "may not interact");
                    // Update any tile entity data for this block
                    BlockEntity tileentity = this.level.getBlockEntity(pos);
                    if (tileentity != null) {
                        this.player.connection.send(tileentity.getUpdatePacket());
                    }
                    // CraftBukkit end
                    return;
                }

                // CraftBukkit start
                PlayerInteractEvent event = CraftEventFactory.callPlayerInteractEvent(this.player, Action.LEFT_CLICK_BLOCK, pos, direction, this.player.getInventory().getSelected(), InteractionHand.MAIN_HAND);
                if (event.isCancelled()) {
                    // Let the client know the block still exists
                    // Paper start - brute force neighbor blocks for any attached blocks
                    for (Direction dir : Direction.values()) {
                        this.player.connection.send(new ClientboundBlockUpdatePacket(level, pos.relative(dir)));
                    }
                    // Paper end
                    this.player.connection.send(new ClientboundBlockUpdatePacket(this.level, pos));
                    // Update any tile entity data for this block
                    BlockEntity tileentity = this.level.getBlockEntity(pos);
                    if (tileentity != null) {
                        this.player.connection.send(tileentity.getUpdatePacket());
                    }
                    return;
                }
                // CraftBukkit end

                if (this.isCreative()) {
                    this.destroyAndAck(pos, sequence, "creative destroy");
                    return;
                }

                // Spigot start - handle debug stick left click for non-creative
                if (this.player.getMainHandItem().is(net.minecraft.world.item.Items.DEBUG_STICK)
                        && ((net.minecraft.world.item.DebugStickItem) net.minecraft.world.item.Items.DEBUG_STICK).handleInteraction(this.player, this.level.getBlockState(pos), this.level, pos, false, this.player.getMainHandItem())) {
                    this.player.connection.send(new ClientboundBlockUpdatePacket(this.level, pos));
                    return;
                }
                // Spigot end

                if (this.player.blockActionRestricted(this.level, pos, this.gameModeForPlayer)) {
                    this.player.connection.send(new ClientboundBlockUpdatePacket(pos, this.level.getBlockState(pos)));
                    this.debugLogging(pos, false, sequence, "block action restricted");
                    return;
                }

                this.destroyProgressStart = this.gameTicks;
                float f = 1.0F;

                iblockdata = this.level.getBlockState(pos);
                // CraftBukkit start - Swings at air do *NOT* exist.
                if (event.useInteractedBlock() == Event.Result.DENY) {
                    // If we denied a door from opening, we need to send a correcting update to the client, as it already opened the door.
                    BlockState data = this.level.getBlockState(pos);
                    if (data.getBlock() instanceof DoorBlock) {
                        // For some reason *BOTH* the bottom/top part have to be marked updated.
                        boolean bottom = data.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER;
                        this.player.connection.send(new ClientboundBlockUpdatePacket(this.level, pos));
                        this.player.connection.send(new ClientboundBlockUpdatePacket(this.level, bottom ? pos.above() : pos.below()));
                    } else if (data.getBlock() instanceof TrapDoorBlock) {
                        this.player.connection.send(new ClientboundBlockUpdatePacket(this.level, pos));
                    }
                } else if (!iblockdata.isAir()) {
                    iblockdata.attack(this.level, pos, this.player);
                    f = iblockdata.getDestroyProgress(this.player, this.player.level, pos);
                }

                if (event.useItemInHand() == Event.Result.DENY) {
                    // If we 'insta destroyed' then the client needs to be informed.
                    if (f > 1.0f) {
                        this.player.connection.send(new ClientboundBlockUpdatePacket(this.level, pos));
                    }
                    return;
                }
                org.bukkit.event.block.BlockDamageEvent blockEvent = CraftEventFactory.callBlockDamageEvent(this.player, pos, this.player.getInventory().getSelected(), f >= 1.0f);

                if (blockEvent.isCancelled()) {
                    // Let the client know the block still exists
                    this.player.connection.send(new ClientboundBlockUpdatePacket(this.level, pos));
                    return;
                }

                if (blockEvent.getInstaBreak()) {
                    f = 2.0f;
                }
                // CraftBukkit end

                if (!iblockdata.isAir() && f >= 1.0F) {
                    this.destroyAndAck(pos, sequence, "insta mine");
                } else {
                    if (this.isDestroyingBlock) {
                        this.player.connection.send(new ClientboundBlockUpdatePacket(this.destroyPos, this.level.getBlockState(this.destroyPos)));
                        this.debugLogging(pos, false, sequence, "abort destroying since another started (client insta mine, server disagreed)");
                    }

                    this.isDestroyingBlock = true;
                    this.destroyPos = pos.immutable();
                    int k = (int) (f * 10.0F);

                    this.level.destroyBlockProgress(this.player.getId(), pos, k);
                    this.debugLogging(pos, true, sequence, "actual start of destroying");
                    this.lastSentState = k;
                }
            } else if (action == ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK) {
                if (pos.equals(this.destroyPos)) {
                    int l = this.gameTicks - this.destroyProgressStart;

                    iblockdata = this.level.getBlockState(pos);
                    if (!iblockdata.isAir()) {
                        float f1 = iblockdata.getDestroyProgress(this.player, this.player.level, pos) * (float) (l + 1);

                        if (f1 >= 0.7F) {
                            this.isDestroyingBlock = false;
                            this.level.destroyBlockProgress(this.player.getId(), pos, -1);
                            this.destroyAndAck(pos, sequence, "destroyed");
                            return;
                        }

                        if (!this.hasDelayedDestroy) {
                            this.isDestroyingBlock = false;
                            this.hasDelayedDestroy = true;
                            this.delayedDestroyPos = pos;
                            this.delayedTickStart = this.destroyProgressStart;
                        }
                    }
                }

                this.debugLogging(pos, true, sequence, "stopped destroying");
            } else if (action == ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK) {
                this.isDestroyingBlock = false;
                if (!Objects.equals(this.destroyPos, pos) && !BlockPos.ZERO.equals(this.destroyPos)) { // Paper
                    ServerPlayerGameMode.LOGGER.debug("Mismatch in destroy block pos: {} {}", this.destroyPos, pos); // CraftBukkit - SPIGOT-5457 sent by client when interact event cancelled
                    BlockState type = this.level.getBlockStateIfLoaded(this.destroyPos); // Paper - don't load unloaded chunks for stale records here
                    if (type != null) this.level.destroyBlockProgress(this.player.getId(), this.destroyPos, -1);
                    if (type != null) this.debugLogging(pos, true, sequence, "aborted mismatched destroying");
                    this.destroyPos = BlockPos.ZERO; // Paper
                }

                this.level.destroyBlockProgress(this.player.getId(), pos, -1);
                this.debugLogging(pos, true, sequence, "aborted destroying");

                CraftEventFactory.callBlockDamageAbortEvent(this.player, pos, this.player.getInventory().getSelected()); // CraftBukkit
            }

        }

        this.level.chunkPacketBlockController.onPlayerLeftClickBlock(this, pos, action, direction, worldHeight, sequence); // Paper - Anti-Xray
    }

    public void destroyAndAck(BlockPos pos, int sequence, String reason) {
        if (this.destroyBlock(pos)) {
            this.debugLogging(pos, true, sequence, reason);
        } else {
            this.player.connection.send(new ClientboundBlockUpdatePacket(pos, this.level.getBlockState(pos)));
            this.debugLogging(pos, false, sequence, reason);
        }

    }

    public boolean destroyBlock(BlockPos pos) {
        BlockState iblockdata = this.level.getBlockState(pos);
        // CraftBukkit start - fire BlockBreakEvent
        org.bukkit.block.Block bblock = CraftBlock.at(level, pos);
        BlockBreakEvent event = null;

        if (this.player instanceof ServerPlayer) {
            // Sword + Creative mode pre-cancel
            boolean isSwordNoBreak = !this.player.getMainHandItem().getItem().canAttackBlock(iblockdata, this.level, pos, this.player);

            // Tell client the block is gone immediately then process events
            // Don't tell the client if its a creative sword break because its not broken!
            if (this.level.getBlockEntity(pos) == null && !isSwordNoBreak) {
                ClientboundBlockUpdatePacket packet = new ClientboundBlockUpdatePacket(pos, Blocks.AIR.defaultBlockState());
                this.player.connection.send(packet);
            }

            event = new BlockBreakEvent(bblock, this.player.getBukkitEntity());

            // Sword + Creative mode pre-cancel
            event.setCancelled(isSwordNoBreak);

            // Calculate default block experience
            BlockState nmsData = this.level.getBlockState(pos);
            Block nmsBlock = nmsData.getBlock();

            ItemStack itemstack = this.player.getItemBySlot(EquipmentSlot.MAINHAND);

            if (nmsBlock != null && !event.isCancelled() && !this.isCreative() && this.player.hasCorrectToolForDrops(nmsBlock.defaultBlockState())) {
                event.setExpToDrop(nmsBlock.getExpDrop(nmsData, this.level, pos, itemstack, true));
            }

            this.level.getCraftServer().getPluginManager().callEvent(event);

            if (event.isCancelled()) {
                if (isSwordNoBreak) {
                    return false;
                }
                // Let the client know the block still exists
                this.player.connection.send(new ClientboundBlockUpdatePacket(this.level, pos));

                // Brute force all possible updates
                for (Direction dir : Direction.values()) {
                    this.player.connection.send(new ClientboundBlockUpdatePacket(this.level, pos.relative(dir)));
                }

                // Update any tile entity data for this block
                BlockEntity tileentity = this.level.getBlockEntity(pos);
                if (tileentity != null) {
                    this.player.connection.send(tileentity.getUpdatePacket());
                }
                return false;
            }
        }
        // CraftBukkit end

        if (false && !this.player.getMainHandItem().getItem().canAttackBlock(iblockdata, this.level, pos, this.player)) { // CraftBukkit - false
            return false;
        } else {
            iblockdata = this.level.getBlockState(pos); // CraftBukkit - update state from plugins
            if (iblockdata.isAir()) return false; // CraftBukkit - A plugin set block to air without cancelling
            BlockEntity tileentity = this.level.getBlockEntity(pos);
            Block block = iblockdata.getBlock();

            if (block instanceof GameMasterBlock && !this.player.canUseGameMasterBlocks() && !(block instanceof net.minecraft.world.level.block.CommandBlock && (this.player.isCreative() && this.player.getBukkitEntity().hasPermission("minecraft.commandblock")))) { // Paper - command block permission
                this.level.sendBlockUpdated(pos, iblockdata, iblockdata, 3);
                return false;
            } else if (this.player.blockActionRestricted(this.level, pos, this.gameModeForPlayer)) {
                return false;
            } else {
                // CraftBukkit start
                org.bukkit.block.BlockState state = bblock.getState();
                level.captureDrops = new ArrayList<>();
                // CraftBukkit end
                block.playerWillDestroy(this.level, pos, iblockdata, this.player);
                boolean flag = this.level.removeBlock(pos, false);

                if (flag) {
                    block.destroy(this.level, pos, iblockdata);
                }

                if (this.isCreative()) {
                    // return true; // CraftBukkit
                } else {
                    ItemStack itemstack = this.player.getMainHandItem();
                    ItemStack itemstack1 = itemstack.copy();
                    boolean flag1 = this.player.hasCorrectToolForDrops(iblockdata);

                    itemstack.mineBlock(this.level, iblockdata, pos, this.player);
                    if (flag && flag1 && event.isDropItems()) { // CraftBukkit - Check if block should drop items
                        block.playerDestroy(this.level, this.player, pos, iblockdata, tileentity, itemstack1);
                    }

                    // return true; // CraftBukkit
                }
                // CraftBukkit start
                java.util.List<net.minecraft.world.entity.item.ItemEntity> itemsToDrop = level.captureDrops; // Paper - store current list
                level.captureDrops = null; // Paper - Remove this earlier so that we can actually drop stuff
                if (event.isDropItems()) {
                    org.bukkit.craftbukkit.event.CraftEventFactory.handleBlockDropItemEvent(bblock, state, this.player, itemsToDrop); // Paper - use stored ref
                }
                //world.captureDrops = null; // Paper - move up

                // Drop event experience
                if (flag && event != null) {
                    iblockdata.getBlock().popExperience(this.level, pos, event.getExpToDrop(), this.player); // Paper
                }

                return true;
                // CraftBukkit end
            }
        }
    }

    public InteractionResult useItem(ServerPlayer player, Level world, ItemStack stack, InteractionHand hand) {
        if (this.gameModeForPlayer == GameType.SPECTATOR) {
            return InteractionResult.PASS;
        } else if (player.getCooldowns().isOnCooldown(stack.getItem())) {
            return InteractionResult.PASS;
        } else {
            int i = stack.getCount();
            int j = stack.getDamageValue();
            InteractionResultHolder<ItemStack> interactionresultwrapper = stack.use(world, player, hand);
            ItemStack itemstack1 = (ItemStack) interactionresultwrapper.getObject();

            if (itemstack1 == stack && itemstack1.getCount() == i && itemstack1.getUseDuration() <= 0 && itemstack1.getDamageValue() == j) {
                return interactionresultwrapper.getResult();
            } else if (interactionresultwrapper.getResult() == InteractionResult.FAIL && itemstack1.getUseDuration() > 0 && !player.isUsingItem()) {
                return interactionresultwrapper.getResult();
            } else {
                if (stack != itemstack1) {
                    player.setItemInHand(hand, itemstack1);
                }

                if (this.isCreative()) {
                    itemstack1.setCount(i);
                    if (itemstack1.isDamageableItem() && itemstack1.getDamageValue() != j) {
                        itemstack1.setDamageValue(j);
                    }
                }

                if (itemstack1.isEmpty()) {
                    player.setItemInHand(hand, ItemStack.EMPTY);
                }

                if (!player.isUsingItem()) {
                    player.inventoryMenu.sendAllDataToRemote();
                }

                return interactionresultwrapper.getResult();
            }
        }
    }

    // CraftBukkit start - whole method
    public boolean interactResult = false;
    public boolean firedInteract = false;
    public BlockPos interactPosition;
    public InteractionHand interactHand;
    public ItemStack interactItemStack;
    public InteractionResult useItemOn(ServerPlayer player, Level world, ItemStack stack, InteractionHand hand, BlockHitResult hitResult) {
        BlockPos blockposition = hitResult.getBlockPos();
        BlockState iblockdata = world.getBlockState(blockposition);
        InteractionResult enuminteractionresult = InteractionResult.PASS;
        boolean cancelledBlock = false;

        if (!iblockdata.getBlock().isEnabled(world.enabledFeatures())) {
            return InteractionResult.FAIL;
        } else if (this.gameModeForPlayer == GameType.SPECTATOR) {
            MenuProvider itileinventory = iblockdata.getMenuProvider(world, blockposition);
            cancelledBlock = !(itileinventory instanceof MenuProvider);
        }

        if (player.getCooldowns().isOnCooldown(stack.getItem())) {
            cancelledBlock = true;
        }

        PlayerInteractEvent event = CraftEventFactory.callPlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, blockposition, hitResult.getDirection(), stack, cancelledBlock, hand);
        this.firedInteract = true;
        this.interactResult = event.useItemInHand() == Event.Result.DENY;
        this.interactPosition = blockposition.immutable();
        this.interactHand = hand;
        this.interactItemStack = stack.copy();

        if (event.useInteractedBlock() == Event.Result.DENY) {
            // If we denied a door from opening, we need to send a correcting update to the client, as it already opened the door.
            if (iblockdata.getBlock() instanceof DoorBlock) {
                boolean bottom = iblockdata.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER;
                player.connection.send(new ClientboundBlockUpdatePacket(world, bottom ? blockposition.above() : blockposition.below()));
            } else if (iblockdata.getBlock() instanceof CakeBlock) {
                player.getBukkitEntity().sendHealthUpdate(); // SPIGOT-1341 - reset health for cake
            } else if (this.interactItemStack.getItem() instanceof DoubleHighBlockItem) {
                // send a correcting update to the client, as it already placed the upper half of the bisected item
                player.connection.send(new ClientboundBlockUpdatePacket(world, blockposition.relative(hitResult.getDirection()).above()));

                // send a correcting update to the client for the block above as well, this because of replaceable blocks (such as grass, sea grass etc)
                player.connection.send(new ClientboundBlockUpdatePacket(world, blockposition.above()));
            // Paper start  - extend Player Interact cancellation // TODO: consider merging this into the extracted method
            } else if (iblockdata.getBlock() instanceof net.minecraft.world.level.block.StructureBlock) {
                player.connection.send(new net.minecraft.network.protocol.game.ClientboundContainerClosePacket(this.player.containerMenu.containerId));
            } else if (iblockdata.getBlock() instanceof net.minecraft.world.level.block.CommandBlock) {
                player.connection.send(new net.minecraft.network.protocol.game.ClientboundContainerClosePacket(this.player.containerMenu.containerId));
            }
            // Paper end - extend Player Interact cancellation
            player.getBukkitEntity().updateInventory(); // SPIGOT-2867
            enuminteractionresult = (event.useItemInHand() != Event.Result.ALLOW) ? InteractionResult.SUCCESS : InteractionResult.PASS;
        } else if (this.gameModeForPlayer == GameType.SPECTATOR) {
            MenuProvider itileinventory = iblockdata.getMenuProvider(world, blockposition);

            if (itileinventory != null) {
                player.openMenu(itileinventory);
                return InteractionResult.SUCCESS;
            } else {
                return InteractionResult.PASS;
            }
        } else {
            boolean flag = !player.getMainHandItem().isEmpty() || !player.getOffhandItem().isEmpty();
            boolean flag1 = player.isSecondaryUseActive() && flag;
            ItemStack itemstack1 = stack.copy();

            if (!flag1) {
                enuminteractionresult = iblockdata.use(world, player, hand, hitResult);

                if (enuminteractionresult.consumesAction()) {
                    CriteriaTriggers.ITEM_USED_ON_BLOCK.trigger(player, blockposition, itemstack1);
                    return enuminteractionresult;
                }
            }

            if (!stack.isEmpty() && enuminteractionresult != InteractionResult.SUCCESS && !this.interactResult) { // add !interactResult SPIGOT-764
                UseOnContext itemactioncontext = new UseOnContext(player, hand, hitResult);
                InteractionResult enuminteractionresult1;

                if (this.isCreative()) {
                    int i = stack.getCount();

                    enuminteractionresult1 = stack.useOn(itemactioncontext, hand);
                    stack.setCount(i);
                } else {
                    enuminteractionresult1 = stack.useOn(itemactioncontext, hand);
                }

                if (enuminteractionresult1.consumesAction()) {
                    CriteriaTriggers.ITEM_USED_ON_BLOCK.trigger(player, blockposition, itemstack1);
                }

                return enuminteractionresult1;
            }
        }
        return enuminteractionresult;
        // CraftBukkit end
    }

    public void setLevel(ServerLevel world) {
        this.level = world;
    }
}
