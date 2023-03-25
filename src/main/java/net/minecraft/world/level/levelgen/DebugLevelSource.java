package net.minecraft.world.level.levelgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.blending.Blender;

public class DebugLevelSource extends ChunkGenerator {
    public static final Codec<DebugLevelSource> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(RegistryOps.retrieveElement(Biomes.PLAINS)).apply(instance, instance.stable(DebugLevelSource::new));
    });
    private static final int BLOCK_MARGIN = 2;
    private static final List<BlockState> ALL_BLOCKS = StreamSupport.stream(BuiltInRegistries.BLOCK.spliterator(), false).flatMap((block) -> {
        return block.getStateDefinition().getPossibleStates().stream();
    }).collect(Collectors.toList());
    private static final int GRID_WIDTH = Mth.ceil(Mth.sqrt((float)ALL_BLOCKS.size()));
    private static final int GRID_HEIGHT = Mth.ceil((float)ALL_BLOCKS.size() / (float)GRID_WIDTH);
    protected static final BlockState AIR = Blocks.AIR.defaultBlockState();
    protected static final BlockState BARRIER = Blocks.BARRIER.defaultBlockState();
    public static final int HEIGHT = 70;
    public static final int BARRIER_HEIGHT = 60;

    public DebugLevelSource(Holder.Reference<Biome> biomeEntry) {
        super(new FixedBiomeSource(biomeEntry));
    }

    @Override
    protected Codec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public void buildSurface(WorldGenRegion region, StructureManager structures, RandomState noiseConfig, ChunkAccess chunk) {
    }

    @Override
    public void applyBiomeDecoration(WorldGenLevel world, ChunkAccess chunk, StructureManager structureAccessor) {
        BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();
        ChunkPos chunkPos = chunk.getPos();
        int i = chunkPos.x;
        int j = chunkPos.z;

        for(int k = 0; k < 16; ++k) {
            for(int l = 0; l < 16; ++l) {
                int m = SectionPos.sectionToBlockCoord(i, k);
                int n = SectionPos.sectionToBlockCoord(j, l);
                world.setBlock(mutableBlockPos.set(m, 60, n), BARRIER, 2);
                BlockState blockState = getBlockStateFor(m, n);
                world.setBlock(mutableBlockPos.set(m, 70, n), blockState, 2);
            }
        }

    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Executor executor, Blender blender, RandomState noiseConfig, StructureManager structureAccessor, ChunkAccess chunk) {
        return CompletableFuture.completedFuture(chunk);
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types heightmap, LevelHeightAccessor world, RandomState noiseConfig) {
        return 0;
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor world, RandomState noiseConfig) {
        return new NoiseColumn(0, new BlockState[0]);
    }

    @Override
    public void addDebugScreenInfo(List<String> text, RandomState noiseConfig, BlockPos pos) {
    }

    public static BlockState getBlockStateFor(int x, int z) {
        BlockState blockState = AIR;
        if (x > 0 && z > 0 && x % 2 != 0 && z % 2 != 0) {
            x /= 2;
            z /= 2;
            if (x <= GRID_WIDTH && z <= GRID_HEIGHT) {
                int i = Mth.abs(x * GRID_WIDTH + z);
                if (i < ALL_BLOCKS.size()) {
                    blockState = ALL_BLOCKS.get(i);
                }
            }
        }

        return blockState;
    }

    @Override
    public void applyCarvers(WorldGenRegion chunkRegion, long seed, RandomState noiseConfig, BiomeManager biomeAccess, StructureManager structureAccessor, ChunkAccess chunk, GenerationStep.Carving carverStep) {
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion region) {
    }

    @Override
    public int getMinY() {
        return 0;
    }

    @Override
    public int getGenDepth() {
        return 384;
    }

    @Override
    public int getSeaLevel() {
        return 63;
    }
}