package net.minecraft.network.chat.contents;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.Entity;

public class TranslatableContents implements ComponentContents {
    public static final Object[] NO_ARGS = org.plazmamc.plazma.util.Constants.EMPTY_OBJECT;
    private static final FormattedText TEXT_PERCENT = FormattedText.of("%");
    private static final FormattedText TEXT_NULL = FormattedText.of("null");
    private final String key;
    @Nullable
    private final String fallback;
    private final Object[] args;
    @Nullable
    private Language decomposedWith;
    private List<FormattedText> decomposedParts = ImmutableList.of();
    private static final Pattern FORMAT_PATTERN = Pattern.compile("%(?:(\\d+)\\$)?([A-Za-z%]|$)");

    public TranslatableContents(String key, @Nullable String fallback, Object[] args) {
        this.key = key;
        this.fallback = fallback;
        this.args = args;
    }

    private void decompose() {
        Language language = Language.getInstance();
        if (language != this.decomposedWith) {
            this.decomposedWith = language;
            String string = this.fallback != null ? language.getOrDefault(this.key, this.fallback) : language.getOrDefault(this.key);

            try {
                ImmutableList.Builder<FormattedText> builder = ImmutableList.builder();
                this.decomposeTemplate(string, builder::add);
                this.decomposedParts = builder.build();
            } catch (TranslatableFormatException var4) {
                this.decomposedParts = ImmutableList.of(FormattedText.of(string));
            }

        }
    }

    private void decomposeTemplate(String translation, Consumer<FormattedText> partsConsumer) {
        Matcher matcher = FORMAT_PATTERN.matcher(translation);

        try {
            int i = 0;

            int j;
            int l;
            for(j = 0; matcher.find(j); j = l) {
                int k = matcher.start();
                l = matcher.end();
                if (k > j) {
                    String string = translation.substring(j, k);
                    if (string.indexOf(37) != -1) {
                        throw new IllegalArgumentException();
                    }

                    partsConsumer.accept(FormattedText.of(string));
                }

                String string2 = matcher.group(2);
                String string3 = translation.substring(k, l);
                if ("%".equals(string2) && "%%".equals(string3)) {
                    partsConsumer.accept(TEXT_PERCENT);
                } else {
                    if (!"s".equals(string2)) {
                        throw new TranslatableFormatException(this, "Unsupported format: '" + string3 + "'");
                    }

                    String string4 = matcher.group(1);
                    int m = string4 != null ? Integer.parseInt(string4) - 1 : i++;
                    partsConsumer.accept(this.getArgument(m));
                }
            }

            if (j < translation.length()) {
                String string5 = translation.substring(j);
                if (string5.indexOf(37) != -1) {
                    throw new IllegalArgumentException();
                }

                partsConsumer.accept(FormattedText.of(string5));
            }

        } catch (IllegalArgumentException var12) {
            throw new TranslatableFormatException(this, var12);
        }
    }

    private FormattedText getArgument(int index) {
        if (index >= 0 && index < this.args.length) {
            Object object = this.args[index];
            if (object instanceof Component) {
                return (Component)object;
            } else {
                return object == null ? TEXT_NULL : FormattedText.of(object.toString());
            }
        } else {
            throw new TranslatableFormatException(this, index);
        }
    }

    @Override
    public <T> Optional<T> visit(FormattedText.StyledContentConsumer<T> visitor, Style style) {
        this.decompose();

        for(FormattedText formattedText : this.decomposedParts) {
            Optional<T> optional = formattedText.visit(visitor, style);
            if (optional.isPresent()) {
                return optional;
            }
        }

        return Optional.empty();
    }

    @Override
    public <T> Optional<T> visit(FormattedText.ContentConsumer<T> visitor) {
        this.decompose();

        for(FormattedText formattedText : this.decomposedParts) {
            Optional<T> optional = formattedText.visit(visitor);
            if (optional.isPresent()) {
                return optional;
            }
        }

        return Optional.empty();
    }

    @Override
    public MutableComponent resolve(@Nullable CommandSourceStack source, @Nullable Entity sender, int depth) throws CommandSyntaxException {
        Object[] objects = new Object[this.args.length];

        for(int i = 0; i < objects.length; ++i) {
            Object object = this.args[i];
            if (object instanceof Component) {
                objects[i] = ComponentUtils.updateForEntity(source, (Component)object, sender, depth);
            } else {
                objects[i] = object;
            }
        }

        return MutableComponent.create(new TranslatableContents(this.key, this.fallback, objects));
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else {
            if (object instanceof TranslatableContents) {
                TranslatableContents translatableContents = (TranslatableContents)object;
                if (Objects.equals(this.key, translatableContents.key) && Objects.equals(this.fallback, translatableContents.fallback) && Arrays.equals(this.args, translatableContents.args)) {
                    return true;
                }
            }

            return false;
        }
    }

    @Override
    public int hashCode() {
        int i = Objects.hashCode(this.key);
        i = 31 * i + Objects.hashCode(this.fallback);
        return 31 * i + Arrays.hashCode(this.args);
    }

    @Override
    public String toString() {
        return "translation{key='" + this.key + "'" + (this.fallback != null ? ", fallback='" + this.fallback + "'" : "") + ", args=" + Arrays.toString(this.args) + "}";
    }

    public String getKey() {
        return this.key;
    }

    @Nullable
    public String getFallback() {
        return this.fallback;
    }

    public Object[] getArgs() {
        return this.args;
    }
}
