package net.minecraft.world.entity;

import net.minecraft.sounds.SoundSource;

public interface Shearable {
    // Purpur start
    default void shear(SoundSource shearedSoundCategory) {
        shear(shearedSoundCategory, 0);
    }

    void shear(SoundSource shearedSoundCategory, int looting);
    // Purpur end

    boolean readyForShearing();
}
