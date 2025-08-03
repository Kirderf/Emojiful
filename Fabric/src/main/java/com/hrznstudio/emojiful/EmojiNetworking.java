package com.hrznstudio.emojiful;

import com.hrznstudio.emojiful.datapack.EmojiRecipe;
import com.hrznstudio.emojiful.datapack.EmojiRecipeType;
import com.hrznstudio.emojiful.networking.EmojiData;
import com.hrznstudio.emojiful.networking.EmojiSyncPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;

import java.util.Collection;
import java.util.List;

public class EmojiNetworking {
    public static final ResourceLocation EMOJI_SYNC_PACKET =
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "emoji_sync");

    public static void registerNetworking() {
        // Server -> Client packet
        PayloadTypeRegistry.playS2C().register(EmojiSyncPayload.TYPE, EmojiSyncPayload.CODEC);

        // Client handler
        ClientPlayNetworking.registerGlobalReceiver(EmojiSyncPayload.TYPE, (payload, context) -> {
            // Update client emojis from server data
            context.client().execute(() -> {
                CommonClass.updateEmojisFromNetwork(payload.emojis());
            });
        });
    }

    // Send custom emoji data to client when they join
    public static void sendEmojiDataToClient(ServerPlayer player) {
        Constants.LOG.info("Sending emoji data to player: {}", player.getGameProfile().getName());
        RecipeManager recipeManager = player.getServer().getRecipeManager();
        List<EmojiData> emojiData = extractEmojisFromRecipes(recipeManager);

        EmojiSyncPayload payload = new EmojiSyncPayload(emojiData);
        ServerPlayNetworking.send(player, payload);
    }

    private static List<EmojiData> extractEmojisFromRecipes(RecipeManager recipeManager) {
            Collection<RecipeHolder<EmojiRecipe>> emojiRecipes =
                    recipeManager.getAllOfType(EmojiRecipeType.INSTANCE);
            return emojiRecipes.stream()
                    .map(holder -> new EmojiData(
                            holder.value().name(),
                            holder.value().category(),
                            holder.value().url()
                    ))
                    .toList();
    }
}
