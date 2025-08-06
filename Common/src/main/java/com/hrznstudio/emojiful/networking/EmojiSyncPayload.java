package com.hrznstudio.emojiful.networking;

import com.hrznstudio.emojiful.Constants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public record EmojiSyncPayload(List<EmojiData> emojis) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<EmojiSyncPayload> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "emoji_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, EmojiSyncPayload> CODEC = StreamCodec.of(EmojiSyncPayload::write, EmojiSyncPayload::read);

    private static void write(RegistryFriendlyByteBuf buf, EmojiSyncPayload payload) {
        buf.writeInt(payload.emojis.size());
        for (EmojiData emoji : payload.emojis) {
            buf.writeUtf(emoji.name());
            buf.writeUtf(emoji.category());
        }
    }

    private static EmojiSyncPayload read(RegistryFriendlyByteBuf buf) {
        int size = buf.readInt();
        List<EmojiData> emojis = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            emojis.add(new EmojiData(buf.readUtf(), buf.readUtf()));
        }
        return new EmojiSyncPayload(emojis);
    }


    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
