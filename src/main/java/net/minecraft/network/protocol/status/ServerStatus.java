package net.minecraft.network.protocol.status;

import com.mojang.authlib.GameProfile;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import net.minecraft.SharedConstants;
import net.minecraft.WorldVersion;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ExtraCodecs;

public record ServerStatus(Component description, Optional<ServerStatus.Players> players, Optional<ServerStatus.Version> version, Optional<ServerStatus.Favicon> favicon, boolean enforcesSecureChat) {
    public static final Codec<ServerStatus> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(ExtraCodecs.COMPONENT.optionalFieldOf("description", CommonComponents.EMPTY).forGetter(ServerStatus::description), ServerStatus.Players.CODEC.optionalFieldOf("players").forGetter(ServerStatus::players), ServerStatus.Version.CODEC.optionalFieldOf("version").forGetter(ServerStatus::version), ServerStatus.Favicon.CODEC.optionalFieldOf("favicon").forGetter(ServerStatus::favicon), Codec.BOOL.optionalFieldOf("enforcesSecureChat", Boolean.valueOf(false)).forGetter(ServerStatus::enforcesSecureChat)).apply(instance, ServerStatus::new);
    });

    public static record Favicon(byte[] iconBytes) {
        public static final int WIDTH = 64;
        public static final int HEIGHT = 64;
        private static final String PREFIX = "data:image/png;base64,";
        public static final Codec<ServerStatus.Favicon> CODEC = Codec.STRING.comapFlatMap((uri) -> {
            if (!uri.startsWith("data:image/png;base64,")) {
                return DataResult.error(() -> {
                    return "Unknown format";
                });
            } else {
                try {
                    String string = uri.substring("data:image/png;base64,".length()).replaceAll("\n", "");
                    byte[] bs = Base64.getDecoder().decode(string.getBytes(StandardCharsets.UTF_8));
                    return DataResult.success(new ServerStatus.Favicon(bs));
                } catch (IllegalArgumentException var3) {
                    return DataResult.error(() -> {
                        return "Malformed base64 server icon";
                    });
                }
            }
        }, (iconBytes) -> {
            return "data:image/png;base64," + new String(Base64.getEncoder().encode(iconBytes.iconBytes), StandardCharsets.UTF_8);
        });
    }

    public static record Players(int max, int online, List<GameProfile> sample) {
        private static final Codec<GameProfile> PROFILE_CODEC = RecordCodecBuilder.create((instance) -> {
            return instance.group(UUIDUtil.STRING_CODEC.fieldOf("id").forGetter(GameProfile::getId), Codec.STRING.fieldOf("name").forGetter(GameProfile::getName)).apply(instance, GameProfile::new);
        });
        public static final Codec<ServerStatus.Players> CODEC = RecordCodecBuilder.create((instance) -> {
            return instance.group(Codec.INT.fieldOf("max").forGetter(ServerStatus.Players::max), Codec.INT.fieldOf("online").forGetter(ServerStatus.Players::online), PROFILE_CODEC.listOf().optionalFieldOf("sample", List.of()).forGetter(ServerStatus.Players::sample)).apply(instance, ServerStatus.Players::new);
        });
    }

    public static record Version(String name, int protocol) {
        public static final Codec<ServerStatus.Version> CODEC = RecordCodecBuilder.create((instance) -> {
            return instance.group(Codec.STRING.fieldOf("name").forGetter(ServerStatus.Version::name), Codec.INT.fieldOf("protocol").forGetter(ServerStatus.Version::protocol)).apply(instance, ServerStatus.Version::new);
        });

        public static ServerStatus.Version current() {
            WorldVersion worldVersion = SharedConstants.getCurrentVersion();
            return new ServerStatus.Version(worldVersion.getName(), worldVersion.getProtocolVersion());
        }
    }
}
