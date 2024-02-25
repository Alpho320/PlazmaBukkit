package org.bukkit.craftbukkit.block.sign;

import net.minecraft.world.level.block.entity.SignBlockEntity;
import org.bukkit.DyeColor;
import org.bukkit.block.sign.SignSide;
import org.bukkit.craftbukkit.block.CraftSign;
import org.bukkit.craftbukkit.util.CraftChatMessage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CraftSignSide implements SignSide {

    // Lazily initialized only if requested:
    private String[] originalLines = null;
    private String[] lines = null;
    private final SignBlockEntity signText;

    public CraftSignSide(SignBlockEntity signText) {
        this.signText = signText;
    }

    @NotNull
    @Override
    public String[] getLines() {
        if (this.lines == null) {
            // Lazy initialization:
            this.lines = new String[signText.messages.length];
            System.arraycopy(CraftSign.revertComponents(signText.messages), 0, lines, 0, lines.length);
            this.originalLines = new String[lines.length];
            System.arraycopy(lines, 0, originalLines, 0, originalLines.length);
        }
        return this.lines;
    }

    @NotNull
    @Override
    public String getLine(int index) throws IndexOutOfBoundsException {
        return this.getLines()[index];
    }

    @Override
    public void setLine(int index, @NotNull String line) throws IndexOutOfBoundsException {
        this.getLines()[index] = line;
    }

    @Override
    public boolean isGlowingText() {
        return this.signText.hasGlowingText();
    }

    @Override
    public void setGlowingText(boolean glowing) {
        this.signText.setHasGlowingText(glowing);
    }

    @Nullable
    @Override
    public DyeColor getColor() {
        return DyeColor.getByWoolData((byte) this.signText.getColor().getId());
    }

    @Override
    public void setColor(@NotNull DyeColor color) {
        this.signText.setColor(net.minecraft.world.item.DyeColor.byId(color.getWoolData()));
    }

    public void applyLegacyStringToSignSide() {
        if (this.lines != null) {
            for (int i = 0; i < lines.length; i++) {
                String line = (this.lines[i] == null) ? "" : this.lines[i];
                if (line.equals(this.originalLines[i])) {
                    continue; // The line contents are still the same, skip.
                }
                this.signText.setMessage(i, CraftChatMessage.fromString(line)[0]);
            }
        }
    }
}
