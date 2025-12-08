package net.cyberpunk042.network.gui;

import net.cyberpunk042.log.Logging;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

/**
 * G105: Client -> Server profile save request.
 */
public record ProfileSaveC2SPayload(
    String profileName,
    String profileJson
) implements CustomPayload {
    
    public static final Id<ProfileSaveC2SPayload> ID = new Id<>(GuiPacketIds.PROFILE_SAVE_C2S);
    
    public static final PacketCodec<RegistryByteBuf, ProfileSaveC2SPayload> CODEC = PacketCodec.of(
        (payload, buf) -> {
            buf.writeString(payload.profileName);
            buf.writeString(payload.profileJson);
        },
        buf -> new ProfileSaveC2SPayload(
            buf.readString(),
            buf.readString()
        )
    );
    
    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
