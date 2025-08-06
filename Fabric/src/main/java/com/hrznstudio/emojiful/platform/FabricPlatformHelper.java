package com.hrznstudio.emojiful.platform;

import com.hrznstudio.emojiful.networking.EmojiData;
import com.hrznstudio.emojiful.platform.services.IPlatformHelper;
import net.fabricmc.loader.api.FabricLoader;

public class FabricPlatformHelper implements IPlatformHelper {

    @Override
    public String getPlatformName() {
        return "Fabric";
    }

    @Override
    public boolean isModLoaded(String modId) {

        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {

        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }

    @Override
    public String getEmojiName(Object emojiData) {
        if (emojiData instanceof EmojiData data) {
            return data.name();
        }
        throw new IllegalArgumentException("Expected EmojiData, got: " + emojiData.getClass());
    }

    @Override
    public String getEmojiCategory(Object emojiData) {
        if (emojiData instanceof EmojiData data) {
            return data.category();
        }
        throw new IllegalArgumentException("Expected EmojiData, got: " + emojiData.getClass());
    }

}
