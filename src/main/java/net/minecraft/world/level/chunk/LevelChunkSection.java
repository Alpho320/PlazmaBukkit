package net.minecraft.world.level.chunk;

import java.util.function.Predicate;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

public class LevelChunkSection {

    public static final int SECTION_WIDTH = 16;
    public static final int SECTION_HEIGHT = 16;
    public static final int SECTION_SIZE = 4096;
    public static final int BIOME_CONTAINER_BITS = 2;
    private final int bottomBlockY;
    private short nonEmptyBlockCount;
    private short tickingBlockCount;
    private short tickingFluidCount;
    public final PalettedContainer<BlockState> states;
    // CraftBukkit start - read/write
    private PalettedContainer<Holder<Biome>> biomes;

    public LevelChunkSection(int i, PalettedContainer<BlockState> datapaletteblock, PalettedContainer<Holder<Biome>> palettedcontainerro) {
        // CraftBukkit end
        this.bottomBlockY = LevelChunkSection.getBottomBlockY(i);
        this.states = datapaletteblock;
        this.biomes = palettedcontainerro;
        this.recalcBlockCounts();
    }

    public LevelChunkSection(int chunkPos, Registry<Biome> biomeRegistry) {
        this.bottomBlockY = LevelChunkSection.getBottomBlockY(chunkPos);
        this.states = new PalettedContainer<>(Block.BLOCK_STATE_REGISTRY, Blocks.AIR.defaultBlockState(), PalettedContainer.Strategy.SECTION_STATES);
        this.biomes = new PalettedContainer<>(biomeRegistry.asHolderIdMap(), biomeRegistry.getHolderOrThrow(Biomes.PLAINS), PalettedContainer.Strategy.SECTION_BIOMES);
    }

    public static int getBottomBlockY(int chunkPos) {
        return chunkPos << 4;
    }

    public BlockState getBlockState(int x, int y, int z) {
        return (BlockState) this.states.get(x, y, z);
    }

    public FluidState getFluidState(int x, int y, int z) {
        return ((BlockState) this.states.get(x, y, z)).getFluidState();
    }

    public void acquire() {
        this.states.acquire();
    }

    public void release() {
        this.states.release();
    }

    public BlockState setBlockState(int x, int y, int z, BlockState state) {
        return this.setBlockState(x, y, z, state, true);
    }

    public BlockState setBlockState(int x, int y, int z, BlockState state, boolean lock) {
        BlockState iblockdata1;

        if (lock) {
            iblockdata1 = (BlockState) this.states.getAndSet(x, y, z, state);
        } else {
            iblockdata1 = (BlockState) this.states.getAndSetUnchecked(x, y, z, state);
        }

        FluidState fluid = iblockdata1.getFluidState();
        FluidState fluid1 = state.getFluidState();

        if (!iblockdata1.isAir()) {
            --this.nonEmptyBlockCount;
            if (iblockdata1.isRandomlyTicking()) {
                --this.tickingBlockCount;
            }
        }

        if (!fluid.isEmpty()) {
            --this.tickingFluidCount;
        }

        if (!state.isAir()) {
            ++this.nonEmptyBlockCount;
            if (state.isRandomlyTicking()) {
                ++this.tickingBlockCount;
            }
        }

        if (!fluid1.isEmpty()) {
            ++this.tickingFluidCount;
        }

        return iblockdata1;
    }

    public boolean hasOnlyAir() {
        return this.nonEmptyBlockCount == 0;
    }

    public boolean isRandomlyTicking() {
        return this.isRandomlyTickingBlocks() || this.isRandomlyTickingFluids();
    }

    public boolean isRandomlyTickingBlocks() {
        return this.tickingBlockCount > 0;
    }

    public boolean isRandomlyTickingFluids() {
        return this.tickingFluidCount > 0;
    }

    public int bottomBlockY() {
        return this.bottomBlockY;
    }

    public void recalcBlockCounts() {
        class a implements PalettedContainer.CountConsumer<BlockState> {

            public int nonEmptyBlockCount;
            public int tickingBlockCount;
            public int tickingFluidCount;

            a() {}

            public void accept(BlockState iblockdata, int i) {
                FluidState fluid = iblockdata.getFluidState();

                if (!iblockdata.isAir()) {
                    this.nonEmptyBlockCount += i;
                    if (iblockdata.isRandomlyTicking()) {
                        this.tickingBlockCount += i;
                    }
                }

                if (!fluid.isEmpty()) {
                    this.nonEmptyBlockCount += i;
                    if (fluid.isRandomlyTicking()) {
                        this.tickingFluidCount += i;
                    }
                }

            }
        }

        a a0 = new a();

        this.states.count(a0);
        this.nonEmptyBlockCount = (short) a0.nonEmptyBlockCount;
        this.tickingBlockCount = (short) a0.tickingBlockCount;
        this.tickingFluidCount = (short) a0.tickingFluidCount;
    }

    public PalettedContainer<BlockState> getStates() {
        return this.states;
    }

    public PalettedContainerRO<Holder<Biome>> getBiomes() {
        return this.biomes;
    }

    public void read(FriendlyByteBuf buf) {
        this.nonEmptyBlockCount = buf.readShort();
        this.states.read(buf);
        PalettedContainer<Holder<Biome>> datapaletteblock = this.biomes.recreate();

        datapaletteblock.read(buf);
        this.biomes = datapaletteblock;
    }

    public void readBiomes(FriendlyByteBuf buf) {
        PalettedContainer<Holder<Biome>> datapaletteblock = this.biomes.recreate();

        datapaletteblock.read(buf);
        this.biomes = datapaletteblock;
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeShort(this.nonEmptyBlockCount);
        this.states.write(buf);
        this.biomes.write(buf);
    }

    public int getSerializedSize() {
        return 2 + this.states.getSerializedSize() + this.biomes.getSerializedSize();
    }

    public boolean maybeHas(Predicate<BlockState> predicate) {
        return this.states.maybeHas(predicate);
    }

    public Holder<Biome> getNoiseBiome(int x, int y, int z) {
        return (Holder) this.biomes.get(x, y, z);
    }

    // CraftBukkit start
    public void setBiome(int i, int j, int k, Holder<Biome> biome) {
        this.biomes.set(i, j, k, biome);
    }
    // CraftBukkit end

    public void fillBiomesFromNoise(BiomeResolver biomeSupplier, Climate.Sampler sampler, int x, int z) {
        PalettedContainer<Holder<Biome>> datapaletteblock = this.biomes.recreate();
        int k = QuartPos.fromBlock(this.bottomBlockY());
        boolean flag = true;

        for (int l = 0; l < 4; ++l) {
            for (int i1 = 0; i1 < 4; ++i1) {
                for (int j1 = 0; j1 < 4; ++j1) {
                    datapaletteblock.getAndSetUnchecked(l, i1, j1, biomeSupplier.getNoiseBiome(x + l, k + i1, z + j1, sampler));
                }
            }
        }

        this.biomes = datapaletteblock;
    }
}