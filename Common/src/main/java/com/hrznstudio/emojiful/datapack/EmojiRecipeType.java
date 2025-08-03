package com.hrznstudio.emojiful.datapack;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeType;

public class EmojiRecipeType implements RecipeType<EmojiRecipe> {
    public static final EmojiRecipeType INSTANCE = new EmojiRecipeType();
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("emojiful", "emoji_recipe");

    private EmojiRecipeType() {
    }

    @Override
    public String toString() {
        return ID.toString();
    }
}