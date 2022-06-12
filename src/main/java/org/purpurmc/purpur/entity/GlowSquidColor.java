package org.purpurmc.purpur.entity;

import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public enum GlowSquidColor {
    BLUE, RED, GREEN, PINK, YELLOW, ORANGE, INDIGO, PURPLE, WHITE, GRAY, BLACK;

    @Override
    public String toString() {
        return this.name().toLowerCase(Locale.ROOT);
    }

    public enum Mode {
        RAINBOW(RED, ORANGE, YELLOW, GREEN, BLUE, INDIGO, PURPLE),
        ALL_COLORS(BLUE, RED, GREEN, PINK, YELLOW, ORANGE, INDIGO, PURPLE, WHITE, GRAY, BLACK),
        TRANS_PRIDE(BLUE, WHITE, PINK),
        LESBIAN_PRIDE(RED, ORANGE, WHITE, PINK, PURPLE),
        BI_PRIDE(BLUE, PINK, PURPLE),
        GAY_PRIDE(BLUE, GREEN, WHITE),
        PAN_PRIDE(PINK, YELLOW, BLUE),
        ACE_PRIDE(BLACK, GRAY, WHITE, PURPLE),
        ARO_PRIDE(BLACK, GRAY, WHITE, GREEN),
        ENBY_PRIDE(YELLOW, WHITE, BLACK, PURPLE),
        GENDERFLUID(PURPLE, WHITE, BLACK, PINK, BLUE),
        MONOCHROME(BLACK, GRAY, WHITE),
        VANILLA(BLUE);

        private static final Map<String, Mode> BY_NAME = new HashMap<>();

        static {
            Arrays.stream(values()).forEach(mode -> BY_NAME.put(mode.name(), mode));
        }

        private final List<GlowSquidColor> colors = new ArrayList<>();

        Mode(GlowSquidColor... colors) {
            this.colors.addAll(Arrays.stream(colors).toList());
        }

        public static Mode get(String string) {
            Mode mode = BY_NAME.get(string.toUpperCase(Locale.ROOT));
            return mode == null ? RAINBOW : mode;
        }

        public GlowSquidColor getRandom(RandomSource random) {
            return this.colors.get(random.nextInt(this.colors.size()));
        }

        @Override
        public String toString() {
            return this.name().toLowerCase(Locale.ROOT);
        }
    }
}
