package net.minecraft.world.level.biome;

import com.google.common.hash.Hashing;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.util.LinearCongruentialGenerator;
import net.minecraft.util.Mth;

public class BiomeManager {
    public static final int CHUNK_CENTER_QUART = QuartPos.fromBlock(8);
    private static final int ZOOM_BITS = 2;
    private static final int ZOOM = 4;
    private static final int ZOOM_MASK = 3;
    private static final double maxOffset = 0.4500000001D; // Plazma - CarpetFixes
    private final BiomeManager.NoiseBiomeSource noiseBiomeSource;
    private final long biomeZoomSeed;

    public BiomeManager(BiomeManager.NoiseBiomeSource storage, long seed) {
        this.noiseBiomeSource = storage;
        this.biomeZoomSeed = seed;
    }

    public static long obfuscateSeed(long seed) {
        return Hashing.sha256().hashLong(seed).asLong();
    }

    public BiomeManager withDifferentSource(BiomeManager.NoiseBiomeSource storage) {
        return new BiomeManager(storage, this.biomeZoomSeed);
    }

    public Holder<Biome> getBiome(BlockPos pos) {
        // Plazma start - CarpetFixes - Optimized getBiome call
        if (org.plazmamc.plazma.configurations.GlobalConfiguration.get().carpetFixes.optimizedBiomeAccess()) {
            int xMinus2 = pos.getX() - 2;
            int yMinus2 = pos.getY() - 2;
            int zMinus2 = pos.getZ() - 2;
            int x = xMinus2 >> 2; // BlockPos to BiomePos
            int y = yMinus2 >> 2;
            int z = zMinus2 >> 2;
            double quartX = (double) (xMinus2 & 3) / 4.0D; // quartLocal divided by 4
            double quartY = (double) (yMinus2 & 3) / 4.0D; // 0/4, 1/4, 2/4, 3/4
            double quartZ = (double) (zMinus2 & 3) / 4.0D; // [0, 0.25, 0.5, 0.75]
            int smallestX = 0;
            double smallestDist = Double.POSITIVE_INFINITY;
            for (int biomeX = 0; biomeX < 8; ++biomeX) {
                boolean everyOtherQuad = (biomeX & 4) == 0; // 1 1 1 1 0 0 0 0
                boolean everyOtherPair = (biomeX & 2) == 0; // 1 1 0 0 1 1 0 0
                boolean everyOther = (biomeX & 1) == 0; // 1 0 1 0 1 0 1 0
                double quartXX = everyOtherQuad ? quartX : quartX - 1.0D; //[-1.0,-0.75,-0.5,-0.25,0.0,0.25,0.5,0.75]
                double quartYY = everyOtherPair ? quartY : quartY - 1.0D;
                double quartZZ = everyOther ? quartZ : quartZ - 1.0D;

                //This code block is new
                double maxQuartYY = 0.0D, maxQuartZZ = 0.0D;
                if (biomeX != 0) {
                    maxQuartYY = Mth.square(Math.max(quartYY + maxOffset, Math.abs(quartYY - maxOffset)));
                    maxQuartZZ = Mth.square(Math.max(quartZZ + maxOffset, Math.abs(quartZZ - maxOffset)));
                    double maxQuartXX = Mth.square(Math.max(quartXX + maxOffset, Math.abs(quartXX - maxOffset)));
                    if (smallestDist < maxQuartXX + maxQuartYY + maxQuartZZ) continue;
                }

                int xx = everyOtherQuad ? x : x + 1;
                int yy = everyOtherPair ? y : y + 1;
                int zz = everyOther ? z : z + 1;

                //I transferred the code from method_38106 to here, so I could call continue halfway through
                long seed = LinearCongruentialGenerator.next(this.biomeZoomSeed, xx);
                seed = LinearCongruentialGenerator.next(seed, yy);
                seed = LinearCongruentialGenerator.next(seed, zz);
                seed = LinearCongruentialGenerator.next(seed, xx);
                seed = LinearCongruentialGenerator.next(seed, yy);
                seed = LinearCongruentialGenerator.next(seed, zz);
                double offsetX = getFiddle(seed);
                double sqrX = Mth.square(quartXX + offsetX);
                if (biomeX != 0 && smallestDist < sqrX + maxQuartYY + maxQuartZZ) continue; //skip the rest of the loop
                seed = LinearCongruentialGenerator.next(seed, this.biomeZoomSeed);
                double offsetY = getFiddle(seed);
                double sqrY = Mth.square(quartYY + offsetY);
                if (biomeX != 0 && smallestDist < sqrX + sqrY + maxQuartZZ) continue; // skip the rest of the loop
                seed = LinearCongruentialGenerator.next(seed, this.biomeZoomSeed);
                double offsetZ = getFiddle(seed);
                double biomeDist = sqrX + sqrY + Mth.square(quartZZ + offsetZ);

                if (smallestDist > biomeDist) {
                    smallestX = biomeX;
                    smallestDist = biomeDist;
                }
            }
            return this.noiseBiomeSource.getNoiseBiome(
                    (smallestX & 4) == 0 ? x : x + 1,
                    (smallestX & 2) == 0 ? y : y + 1,
                    (smallestX & 1) == 0 ? z : z + 1
            );
        } else {
                int i = pos.getX() - 2;
                int j = pos.getY() - 2;
                int k = pos.getZ() - 2;
                int l = i >> 2;
                int m = j >> 2;
                int n = k >> 2;
                double d = (double) (i & 3) / 4.0D;
                double e = (double) (j & 3) / 4.0D;
                double f = (double) (k & 3) / 4.0D;
                int o = 0;
                double g = Double.POSITIVE_INFINITY;

                for (int p = 0; p < 8; ++p) {
                    boolean bl = (p & 4) == 0;
                    boolean bl2 = (p & 2) == 0;
                    boolean bl3 = (p & 1) == 0;
                    int q = bl ? l : l + 1;
                    int r = bl2 ? m : m + 1;
                    int s = bl3 ? n : n + 1;
                    double h = bl ? d : d - 1.0D;
                    double t = bl2 ? e : e - 1.0D;
                    double u = bl3 ? f : f - 1.0D;
                    double v = getFiddledDistance(this.biomeZoomSeed, q, r, s, h, t, u);
                    if (g > v) {
                        o = p;
                        g = v;
                    }
                }

                int w = (o & 4) == 0 ? l : l + 1;
                int x = (o & 2) == 0 ? m : m + 1;
                int y = (o & 1) == 0 ? n : n + 1;
                return this.noiseBiomeSource.getNoiseBiome(w, x, y);
            }
        // Plazma end
    }

    public Holder<Biome> getNoiseBiomeAtPosition(double x, double y, double z) {
        int i = QuartPos.fromBlock(Mth.floor(x));
        int j = QuartPos.fromBlock(Mth.floor(y));
        int k = QuartPos.fromBlock(Mth.floor(z));
        return this.getNoiseBiomeAtQuart(i, j, k);
    }

    public Holder<Biome> getNoiseBiomeAtPosition(BlockPos pos) {
        int i = QuartPos.fromBlock(pos.getX());
        int j = QuartPos.fromBlock(pos.getY());
        int k = QuartPos.fromBlock(pos.getZ());
        return this.getNoiseBiomeAtQuart(i, j, k);
    }

    public Holder<Biome> getNoiseBiomeAtQuart(int biomeX, int biomeY, int biomeZ) {
        return this.noiseBiomeSource.getNoiseBiome(biomeX, biomeY, biomeZ);
    }

    private static double getFiddledDistance(long l, int i, int j, int k, double d, double e, double f) {
        long m = LinearCongruentialGenerator.next(l, (long)i);
        m = LinearCongruentialGenerator.next(m, (long)j);
        m = LinearCongruentialGenerator.next(m, (long)k);
        m = LinearCongruentialGenerator.next(m, (long)i);
        m = LinearCongruentialGenerator.next(m, (long)j);
        m = LinearCongruentialGenerator.next(m, (long)k);
        double g = getFiddle(m);
        m = LinearCongruentialGenerator.next(m, l);
        double h = getFiddle(m);
        m = LinearCongruentialGenerator.next(m, l);
        double n = getFiddle(m);
        return Mth.square(f + n) + Mth.square(e + h) + Mth.square(d + g);
    }

    private static double getFiddle(long l) {
        double d = (double)Math.floorMod(l >> 24, 1024) / 1024.0D;
        return (d - 0.5D) * 0.9D;
    }

    public interface NoiseBiomeSource {
        Holder<Biome> getNoiseBiome(int biomeX, int biomeY, int biomeZ);
    }
}
