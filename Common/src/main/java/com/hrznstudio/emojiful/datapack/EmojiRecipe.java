package com.hrznstudio.emojiful.datapack;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;


public record EmojiRecipe(String name, String category, String url) implements Recipe<SingleRecipeInput> {

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        // Your matching logic here
        return false; // Usually false for data-only recipes
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider provider) {
        return ItemStack.EMPTY; // Usually empty for data-only recipes
    }

    @Override
    public RecipeType<? extends Recipe<SingleRecipeInput>> getType() {
        return EmojiRecipeType.INSTANCE;
    }

    @Override
    public PlacementInfo placementInfo() {
        return PlacementInfo.NOT_PLACEABLE;
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategories.CRAFTING_MISC;
    }

    @Override
    public RecipeSerializer<? extends Recipe<SingleRecipeInput>> getSerializer() {
        return EmojiRecipeSerializer.INSTANCE;
    }
}
