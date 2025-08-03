package com.hrznstudio.emojiful.datapack;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;


public class EmojiRecipeSerializer implements RecipeSerializer<EmojiRecipe> {
    public static final EmojiRecipeSerializer INSTANCE = new EmojiRecipeSerializer();

    public static final MapCodec<EmojiRecipe> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    Codec.STRING.fieldOf("name").forGetter(EmojiRecipe::name),
                    Codec.STRING.fieldOf("category").forGetter(EmojiRecipe::category),
                    Codec.STRING.fieldOf("url").forGetter(EmojiRecipe::url)
            ).apply(instance, EmojiRecipe::new)
    );

    @Override
    public MapCodec<EmojiRecipe> codec() {
        return CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, EmojiRecipe> streamCodec() {
        return StreamCodec.of(this::toNetwork, this::fromNetwork);
    }

    private void toNetwork(RegistryFriendlyByteBuf buffer, EmojiRecipe recipe) {
        buffer.writeUtf(recipe.name());
        buffer.writeUtf(recipe.category());
        buffer.writeUtf(recipe.url());
    }

    private EmojiRecipe fromNetwork(RegistryFriendlyByteBuf buffer) {
        String name = buffer.readUtf();
        String category = buffer.readUtf();
        String url = buffer.readUtf();
        return new EmojiRecipe(name, category, url);
    }
}
