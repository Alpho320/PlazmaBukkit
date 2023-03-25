package net.minecraft.world.entity.boss;

import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.item.ItemStack;

public class EnderDragonPart extends Entity {
    public final EnderDragon parentMob;
    public final String name;
    private final EntityDimensions size;

    public EnderDragonPart(EnderDragon owner, String name, float width, float height) {
        super(owner.getType(), owner.level);
        this.size = EntityDimensions.scalable(width, height);
        this.refreshDimensions();
        this.parentMob = owner;
        this.name = name;
    }

    // Purpur start
    @Override
    public net.minecraft.world.InteractionResult interact(net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand) {
        return parentMob.isAlive() ? parentMob.tryRide(player, hand) : net.minecraft.world.InteractionResult.PASS;
    }
    // Purpur end

    @Override
    protected void defineSynchedData() {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag nbt) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag nbt) {
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Nullable
    @Override
    public ItemStack getPickResult() {
        return this.parentMob.getPickResult();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return this.isInvulnerableTo(source) ? false : this.parentMob.hurt(this, source, amount);
    }

    @Override
    public boolean is(Entity entity) {
        return this == entity || this.parentMob == entity;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        throw new UnsupportedOperationException();
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return this.size;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }
}