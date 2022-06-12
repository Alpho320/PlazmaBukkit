package org.bukkit.craftbukkit;

import com.mojang.authlib.GameProfile;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.players.UserWhiteListEntry;
import net.minecraft.stats.ServerStatsCounter;
import net.minecraft.world.level.storage.PlayerDataStorage;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.Statistic;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.craftbukkit.entity.memory.CraftMemoryMapper;
import org.bukkit.craftbukkit.profile.CraftPlayerProfile;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.profile.PlayerProfile;

@SerializableAs("Player")
public class CraftOfflinePlayer implements OfflinePlayer, ConfigurationSerializable {
    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger(); // Paper
    private final GameProfile profile;
    private final CraftServer server;
    private final PlayerDataStorage storage;

    protected CraftOfflinePlayer(CraftServer server, GameProfile profile) {
        this.server = server;
        this.profile = profile;
        this.storage = server.console.playerDataStorage;

    }

    @Override
    public boolean isOnline() {
        return this.getPlayer() != null;
    }

    @Override
    public String getName() {
        Player player = this.getPlayer();
        if (player != null) {
            return player.getName();
        }

        // This might not match lastKnownName but if not it should be more correct
        if (this.profile.getName() != null) {
            return this.profile.getName();
        }

        CompoundTag data = this.getBukkitData();

        if (data != null) {
            if (data.contains("lastKnownName")) {
                return data.getString("lastKnownName");
            }
        }

        return null;
    }

    @Override
    public UUID getUniqueId() {
        return this.profile.getId();
    }

    @Override
    public com.destroystokyo.paper.profile.PlayerProfile getPlayerProfile() { // Paper
        return new com.destroystokyo.paper.profile.CraftPlayerProfile(this.profile); // Paper
    }

    public Server getServer() {
        return this.server;
    }

    @Override
    public boolean isOp() {
        return this.server.getHandle().isOp(profile);
    }

    @Override
    public void setOp(boolean value) {
        if (value == this.isOp()) {
            return;
        }

        if (value) {
            this.server.getHandle().op(profile);
        } else {
            this.server.getHandle().deop(profile);
        }
    }

    @Override
    public boolean isBanned() {
        if (this.getName() == null) {
            return false;
        }

        return this.server.getBanList(BanList.Type.NAME).isBanned(this.getName());
    }

    public void setBanned(boolean value) {
        if (this.getName() == null) {
            return;
        }

        if (value) {
            this.server.getBanList(BanList.Type.NAME).addBan(this.getName(), null, null, null);
        } else {
            this.server.getBanList(BanList.Type.NAME).pardon(this.getName());
        }
    }

    @Override
    public boolean isWhitelisted() {
        return this.server.getHandle().getWhiteList().isWhiteListed(profile);
    }

    @Override
    public void setWhitelisted(boolean value) {
        if (value) {
            this.server.getHandle().getWhiteList().add(new UserWhiteListEntry(this.profile));
        } else {
            this.server.getHandle().getWhiteList().remove(profile);
        }
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> result = new LinkedHashMap<String, Object>();

        result.put("UUID", this.profile.getId().toString());

        return result;
    }

    public static OfflinePlayer deserialize(Map<String, Object> args) {
        // Backwards comparability
        if (args.get("name") != null) {
            return Bukkit.getServer().getOfflinePlayer((String) args.get("name"));
        }

        return Bukkit.getServer().getOfflinePlayer(UUID.fromString((String) args.get("UUID")));
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[UUID=" + this.profile.getId() + "]";
    }

    @Override
    public Player getPlayer() {
        return this.server.getPlayer(this.getUniqueId());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof OfflinePlayer)) {
            return false;
        }

        OfflinePlayer other = (OfflinePlayer) obj;
        if ((this.getUniqueId() == null) || (other.getUniqueId() == null)) {
            return false;
        }

        return this.getUniqueId().equals(other.getUniqueId());
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 97 * hash + (this.getUniqueId() != null ? this.getUniqueId().hashCode() : 0);
        return hash;
    }

    private CompoundTag getData() {
        return this.storage.getPlayerData(this.getUniqueId().toString());
    }

    private CompoundTag getBukkitData() {
        CompoundTag result = this.getData();

        if (result != null) {
            if (!result.contains("bukkit")) {
                result.put("bukkit", new CompoundTag());
            }
            result = result.getCompound("bukkit");
        }

        return result;
    }

    private File getDataFile() {
        return new File(this.storage.getPlayerDir(), this.getUniqueId() + ".dat");
    }

    @Override
    public long getFirstPlayed() {
        Player player = this.getPlayer();
        if (player != null) return player.getFirstPlayed();

        CompoundTag data = this.getBukkitData();

        if (data != null) {
            if (data.contains("firstPlayed")) {
                return data.getLong("firstPlayed");
            } else {
                File file = this.getDataFile();
                return file.lastModified();
            }
        } else {
            return 0;
        }
    }

    @Override
    public long getLastPlayed() {
        Player player = this.getPlayer();
        if (player != null) return player.getLastPlayed();

        CompoundTag data = this.getBukkitData();

        if (data != null) {
            if (data.contains("lastPlayed")) {
                return data.getLong("lastPlayed");
            } else {
                File file = this.getDataFile();
                return file.lastModified();
            }
        } else {
            return 0;
        }
    }

    @Override
    public boolean hasPlayedBefore() {
        return this.getData() != null;
    }

    // Paper start
    @Override
    public long getLastLogin() {
        Player player = getPlayer();
        if (player != null) return player.getLastLogin();

        CompoundTag data = getPaperData();

        if (data != null) {
            if (data.contains("LastLogin")) {
                return data.getLong("LastLogin");
            } else {
                // if the player file cannot provide accurate data, this is probably the closest we can approximate
                File file = getDataFile();
                return file.lastModified();
            }
        } else {
            return 0;
        }
    }

    @Override
    public long getLastSeen() {
        Player player = getPlayer();
        if (player != null) return player.getLastSeen();

        CompoundTag data = getPaperData();

        if (data != null) {
            if (data.contains("LastSeen")) {
                return data.getLong("LastSeen");
            } else {
                // if the player file cannot provide accurate data, this is probably the closest we can approximate
                File file = getDataFile();
                return file.lastModified();
            }
        } else {
            return 0;
        }
    }

    private CompoundTag getPaperData() {
        CompoundTag result = getData();

        if (result != null) {
            if (!result.contains("Paper")) {
                result.put("Paper", new CompoundTag());
            }
            result = result.getCompound("Paper");
        }

        return result;
    }
    // Paper end

    @Override
    public Location getLastDeathLocation() {
        if (this.getData().contains("LastDeathLocation", 10)) {
            return GlobalPos.CODEC.parse(NbtOps.INSTANCE, this.getData().get("LastDeathLocation")).result().map(CraftMemoryMapper::fromNms).orElse(null);
        }
        return null;
    }

    @Override
    public Location getBedSpawnLocation() {
        CompoundTag data = this.getData();
        if (data == null) return null;

        if (data.contains("SpawnX") && data.contains("SpawnY") && data.contains("SpawnZ")) {
            // Paper start - fix wrong world
            final float respawnAngle = data.getFloat("SpawnAngle");
            org.bukkit.World spawnWorld = this.server.getWorld(data.getString("SpawnWorld")); // legacy
            if (data.contains("SpawnDimension")) {
                com.mojang.serialization.DataResult<net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level>> result = net.minecraft.world.level.Level.RESOURCE_KEY_CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE, data.get("SpawnDimension"));
                net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> levelKey = result.resultOrPartial(LOGGER::error).orElse(net.minecraft.world.level.Level.OVERWORLD);
                net.minecraft.server.level.ServerLevel level = this.server.console.getLevel(levelKey);
                spawnWorld = level != null ? level.getWorld() : spawnWorld;
            }
            if (spawnWorld == null) {
                return null;
            }
            return new Location(spawnWorld, data.getInt("SpawnX"), data.getInt("SpawnY"), data.getInt("SpawnZ"), respawnAngle, 0);
            // Paper end
        }
        return null;
    }

    public void setMetadata(String metadataKey, MetadataValue metadataValue) {
        this.server.getPlayerMetadata().setMetadata(this, metadataKey, metadataValue);
    }

    public List<MetadataValue> getMetadata(String metadataKey) {
        return this.server.getPlayerMetadata().getMetadata(this, metadataKey);
    }

    public boolean hasMetadata(String metadataKey) {
        return this.server.getPlayerMetadata().hasMetadata(this, metadataKey);
    }

    public void removeMetadata(String metadataKey, Plugin plugin) {
        this.server.getPlayerMetadata().removeMetadata(this, metadataKey, plugin);
    }

    private ServerStatsCounter getStatisticManager() {
        return this.server.getHandle().getPlayerStats(this.getUniqueId(), this.getName());
    }

    @Override
    public void incrementStatistic(Statistic statistic) {
        if (this.isOnline()) {
            this.getPlayer().incrementStatistic(statistic);
        } else {
            ServerStatsCounter manager = this.getStatisticManager();
            CraftStatistic.incrementStatistic(manager, statistic);
            manager.save();
        }
    }

    @Override
    public void decrementStatistic(Statistic statistic) {
        if (this.isOnline()) {
            this.getPlayer().decrementStatistic(statistic);
        } else {
            ServerStatsCounter manager = this.getStatisticManager();
            CraftStatistic.decrementStatistic(manager, statistic);
            manager.save();
        }
    }

    @Override
    public int getStatistic(Statistic statistic) {
        if (this.isOnline()) {
            return this.getPlayer().getStatistic(statistic);
        } else {
            return CraftStatistic.getStatistic(this.getStatisticManager(), statistic);
        }
    }

    @Override
    public void incrementStatistic(Statistic statistic, int amount) {
        if (this.isOnline()) {
            this.getPlayer().incrementStatistic(statistic, amount);
        } else {
            ServerStatsCounter manager = this.getStatisticManager();
            CraftStatistic.incrementStatistic(manager, statistic, amount);
            manager.save();
        }
    }

    @Override
    public void decrementStatistic(Statistic statistic, int amount) {
        if (this.isOnline()) {
            this.getPlayer().decrementStatistic(statistic, amount);
        } else {
            ServerStatsCounter manager = this.getStatisticManager();
            CraftStatistic.decrementStatistic(manager, statistic, amount);
            manager.save();
        }
    }

    @Override
    public void setStatistic(Statistic statistic, int newValue) {
        if (this.isOnline()) {
            this.getPlayer().setStatistic(statistic, newValue);
        } else {
            ServerStatsCounter manager = this.getStatisticManager();
            CraftStatistic.setStatistic(manager, statistic, newValue);
            manager.save();
        }
    }

    @Override
    public void incrementStatistic(Statistic statistic, Material material) {
        if (this.isOnline()) {
            this.getPlayer().incrementStatistic(statistic, material);
        } else {
            ServerStatsCounter manager = this.getStatisticManager();
            CraftStatistic.incrementStatistic(manager, statistic, material);
            manager.save();
        }
    }

    @Override
    public void decrementStatistic(Statistic statistic, Material material) {
        if (this.isOnline()) {
            this.getPlayer().decrementStatistic(statistic, material);
        } else {
            ServerStatsCounter manager = this.getStatisticManager();
            CraftStatistic.decrementStatistic(manager, statistic, material);
            manager.save();
        }
    }

    @Override
    public int getStatistic(Statistic statistic, Material material) {
        if (this.isOnline()) {
            return this.getPlayer().getStatistic(statistic, material);
        } else {
            return CraftStatistic.getStatistic(this.getStatisticManager(), statistic, material);
        }
    }

    @Override
    public void incrementStatistic(Statistic statistic, Material material, int amount) {
        if (this.isOnline()) {
            this.getPlayer().incrementStatistic(statistic, material, amount);
        } else {
            ServerStatsCounter manager = this.getStatisticManager();
            CraftStatistic.incrementStatistic(manager, statistic, material, amount);
            manager.save();
        }
    }

    @Override
    public void decrementStatistic(Statistic statistic, Material material, int amount) {
        if (this.isOnline()) {
            this.getPlayer().decrementStatistic(statistic, material, amount);
        } else {
            ServerStatsCounter manager = this.getStatisticManager();
            CraftStatistic.decrementStatistic(manager, statistic, material, amount);
            manager.save();
        }
    }

    @Override
    public void setStatistic(Statistic statistic, Material material, int newValue) {
        if (this.isOnline()) {
            this.getPlayer().setStatistic(statistic, material, newValue);
        } else {
            ServerStatsCounter manager = this.getStatisticManager();
            CraftStatistic.setStatistic(manager, statistic, material, newValue);
            manager.save();
        }
    }

    @Override
    public void incrementStatistic(Statistic statistic, EntityType entityType) {
        if (this.isOnline()) {
            this.getPlayer().incrementStatistic(statistic, entityType);
        } else {
            ServerStatsCounter manager = this.getStatisticManager();
            CraftStatistic.incrementStatistic(manager, statistic, entityType);
            manager.save();
        }
    }

    @Override
    public void decrementStatistic(Statistic statistic, EntityType entityType) {
        if (this.isOnline()) {
            this.getPlayer().decrementStatistic(statistic, entityType);
        } else {
            ServerStatsCounter manager = this.getStatisticManager();
            CraftStatistic.decrementStatistic(manager, statistic, entityType);
            manager.save();
        }
    }

    @Override
    public int getStatistic(Statistic statistic, EntityType entityType) {
        if (this.isOnline()) {
            return this.getPlayer().getStatistic(statistic, entityType);
        } else {
            return CraftStatistic.getStatistic(this.getStatisticManager(), statistic, entityType);
        }
    }

    @Override
    public void incrementStatistic(Statistic statistic, EntityType entityType, int amount) {
        if (this.isOnline()) {
            this.getPlayer().incrementStatistic(statistic, entityType, amount);
        } else {
            ServerStatsCounter manager = this.getStatisticManager();
            CraftStatistic.incrementStatistic(manager, statistic, entityType, amount);
            manager.save();
        }
    }

    @Override
    public void decrementStatistic(Statistic statistic, EntityType entityType, int amount) {
        if (this.isOnline()) {
            this.getPlayer().decrementStatistic(statistic, entityType, amount);
        } else {
            ServerStatsCounter manager = this.getStatisticManager();
            CraftStatistic.decrementStatistic(manager, statistic, entityType, amount);
            manager.save();
        }
    }

    @Override
    public void setStatistic(Statistic statistic, EntityType entityType, int newValue) {
        if (this.isOnline()) {
            this.getPlayer().setStatistic(statistic, entityType, newValue);
        } else {
            ServerStatsCounter manager = this.getStatisticManager();
            CraftStatistic.setStatistic(manager, statistic, entityType, newValue);
            manager.save();
        }
    }

    // Purpur start - OfflinePlayer API
    @Override
    public boolean getAllowFlight() {
        if (this.isOnline()) {
            return this.getPlayer().getAllowFlight();
        } else {
            CompoundTag data = this.getData();
            if (data == null) return false;
            if (!data.contains("abilities")) return false;
            CompoundTag abilities = data.getCompound("abilities");
            return abilities.getByte("mayfly") == (byte) 1;
        }
    }

    @Override
    public void setAllowFlight(boolean flight) {
        if (this.isOnline()) {
            this.getPlayer().setAllowFlight(flight);
        } else {
            CompoundTag data = this.getData();
            if (data == null) return;
            if (!data.contains("abilities")) return;
            CompoundTag abilities = data.getCompound("abilities");
            abilities.putByte("mayfly", (byte) (flight ? 1 : 0));
            data.put("abilities", abilities);
            save(data);
        }
    }

    @Override
    public boolean isFlying() {
        if (this.isOnline()) {
            return this.isFlying();
        } else {
            CompoundTag data = this.getData();
            if (data == null) return false;
            if (!data.contains("abilities")) return false;
            CompoundTag abilities = data.getCompound("abilities");
            return abilities.getByte("flying") == (byte) 1;
        }
    }

    @Override
    public void setFlying(boolean value) {
        if (this.isOnline()) {
            this.getPlayer().setFlying(value);
        } else {
            CompoundTag data = this.getData();
            if (data == null) return;
            if (!data.contains("abilities")) return;
            CompoundTag abilities = data.getCompound("abilities");
            abilities.putByte("mayfly", (byte) (value ? 1 : 0));
            data.put("abilities", abilities);
            save(data);
        }
    }

    @Override
    public void setFlySpeed(float value) throws IllegalArgumentException {
        if (value < -1f || value > 1f) throw new IllegalArgumentException("FlySpeed needs to be between -1 and 1");
        if (this.isOnline()) {
            this.getPlayer().setFlySpeed(value);
        } else {
            CompoundTag data = this.getData();
            if (data == null) return;
            if (!data.contains("abilities")) return;
            CompoundTag abilities = data.getCompound("abilities");
            abilities.putFloat("flySpeed", value);
            data.put("abilities", abilities);
            save(data);
        }
    }

    @Override
    public float getFlySpeed() {
        if (this.isOnline()) {
            return this.getPlayer().getFlySpeed();
        } else {
            CompoundTag data = this.getData();
            if (data == null) return 0;
            if (!data.contains("abilities")) return 0;
            CompoundTag abilities = data.getCompound("abilities");
            return abilities.getFloat("flySpeed");
        }
    }

    @Override
    public void setWalkSpeed(float value) throws IllegalArgumentException {
        if (value < -1f || value > 1f) throw new IllegalArgumentException("WalkSpeed needs to be between -1 and 1");
        if (this.isOnline()) {
            this.getPlayer().setWalkSpeed(value);
        } else {
            CompoundTag data = this.getData();
            if (data == null) return;
            if (!data.contains("abilities")) return;
            CompoundTag abilities = data.getCompound("abilities");
            abilities.putFloat("walkSpeed", value);
            data.put("abilities", abilities);
            save(data);
        }
    }

    @Override
    public float getWalkSpeed() {
        if (this.isOnline()) {
            return this.getPlayer().getWalkSpeed();
        } else {
            CompoundTag data = this.getData();
            if (data == null) return 0;
            if (!data.contains("abilities")) return 0;
            CompoundTag abilities = data.getCompound("abilities");
            return abilities.getFloat("walkSpeed");
        }
    }

    @Override
    public Location getLocation() {
        if (this.isOnline()) {
            return this.getPlayer().getLocation();
        } else {
            CompoundTag data = this.getData();
            if (data == null) return null;
            long worldUUIDMost = data.getLong("WorldUUIDMost");
            long worldUUIDLeast = data.getLong("WorldUUIDLeast");
            net.minecraft.nbt.ListTag position = data.getList("Pos", org.bukkit.craftbukkit.util.CraftMagicNumbers.NBT.TAG_DOUBLE);
            net.minecraft.nbt.ListTag rotation = data.getList("Rotation", org.bukkit.craftbukkit.util.CraftMagicNumbers.NBT.TAG_FLOAT);
            UUID worldUuid = new UUID(worldUUIDMost, worldUUIDLeast);
            org.bukkit.World world = server.getWorld(worldUuid);
            double x = position.getDouble(0);
            double y = position.getDouble(1);
            double z = position.getDouble(2);
            float yaw = rotation.getFloat(0);
            float pitch = rotation.getFloat(1);
            return new Location(world, x, y, z, yaw, pitch);
        }
    }

    @Override
    public boolean teleportOffline(Location destination) {
        if (this.isOnline()) {
            return this.getPlayer().teleport(destination);
        } else {
            return setLocation(destination);
        }
    }

    @Override
    public boolean teleportOffline(Location destination, org.bukkit.event.player.PlayerTeleportEvent.TeleportCause cause){
        if (this.isOnline()) {
            return this.getPlayer().teleport(destination, cause);
        } else {
            return setLocation(destination);
        }
    }

    @Override
    public java.util.concurrent.CompletableFuture<Boolean> teleportOfflineAsync(Location destination) {
        if (this.isOnline()) {
            return this.getPlayer().teleportAsync(destination);
        } else {
            return java.util.concurrent.CompletableFuture.completedFuture(setLocation(destination));
        }
    }

    @Override
    public java.util.concurrent.CompletableFuture<Boolean> teleportOfflineAsync(Location destination, org.bukkit.event.player.PlayerTeleportEvent.TeleportCause cause) {
        if (this.isOnline()) {
            return this.getPlayer().teleportAsync(destination, cause);
        } else {
            return java.util.concurrent.CompletableFuture.completedFuture(setLocation(destination));
        }
    }

    private boolean setLocation(Location location) {
        CompoundTag data = this.getData();
        if (data == null) return false;
        data.putLong("WorldUUIDMost", location.getWorld().getUID().getMostSignificantBits());
        data.putLong("WorldUUIDLeast", location.getWorld().getUID().getLeastSignificantBits());
        net.minecraft.nbt.ListTag position = new net.minecraft.nbt.ListTag();
        position.add(net.minecraft.nbt.DoubleTag.valueOf(location.getX()));
        position.add(net.minecraft.nbt.DoubleTag.valueOf(location.getY()));
        position.add(net.minecraft.nbt.DoubleTag.valueOf(location.getZ()));
        data.put("Pos", position);
        net.minecraft.nbt.ListTag rotation = new net.minecraft.nbt.ListTag();
        rotation.add(net.minecraft.nbt.FloatTag.valueOf(location.getYaw()));
        rotation.add(net.minecraft.nbt.FloatTag.valueOf(location.getPitch()));
        data.put("Rotation", rotation);
        save(data);
        return true;
    }

    /**
     * Safely replaces player's .dat file with provided CompoundTag
     * @param compoundTag
     */
    private void save(CompoundTag compoundTag) {
        File playerDir = server.console.playerDataStorage.getPlayerDir();
        try {
            File tempFile = File.createTempFile(this.getUniqueId()+"-", ".dat", playerDir);
            net.minecraft.nbt.NbtIo.writeCompressed(compoundTag, tempFile);
            File playerDataFile = new File(playerDir, this.getUniqueId()+".dat");
            File playerDataFileOld = new File(playerDir, this.getUniqueId()+".dat_old");
            net.minecraft.Util.safeReplaceFile(playerDataFile, tempFile, playerDataFileOld);
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }
    // Purpur end - OfflinePlayer API
}
