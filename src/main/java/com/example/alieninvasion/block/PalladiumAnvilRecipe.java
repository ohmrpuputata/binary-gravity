package com.example.alieninvasion.block;

import com.example.alieninvasion.registry.ModBlocks;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * Palladium Anvil Smithing-style recipe.
 * <p>
 * JSON format:
 * <pre>{@code
 * {
 *   "type": "alien-invasion:palladium_anvil",
 *   "catalyst": {"item": "minecraft:amethyst_shard"},
 *   "base": {"item": "minecraft:diamond_sword"},
 *   "addition": {"item": "alien-invasion:nibirium_ingot"},
 *   "result": {"id": "alien-invasion:nibirium_sword", "count": 1}
 * }
 * }</pre>
 */
public class PalladiumAnvilRecipe implements Recipe<RecipeInput> {

    private final Ingredient catalyst;
    private final Ingredient base;
    private final Ingredient addition;
    private final ItemStack result;

    public PalladiumAnvilRecipe(Ingredient catalyst, Ingredient base, Ingredient addition, ItemStack result) {
        this.catalyst = catalyst;
        this.base = base;
        this.addition = addition;
        this.result = result;
    }

    public Ingredient getCatalyst() { return catalyst; }
    public Ingredient getBase()     { return base; }
    public Ingredient getAddition() { return addition; }
    public ItemStack getResult()    { return result; }

    @Override
    public boolean matches(RecipeInput input, Level level) {
        if (input.size() < 3) return false;
        return catalyst.test(input.getItem(0))
                && base.test(input.getItem(1))
                && addition.test(input.getItem(2));
    }

    @Override
    public ItemStack assemble(RecipeInput input, HolderLookup.Provider registries) {
        ItemStack baseStack = input.getItem(1);
        if (baseStack.isEmpty()) {
            return result.copy();
        }
        ItemStack resultStack = new ItemStack(result.getItem(), result.getCount());
        resultStack.applyComponents(baseStack.getComponentsPatch());
        return resultStack;
    }

    @Override
    public boolean canCraftInDimensions(int w, int h) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModBlocks.PALLADIUM_ANVIL_RECIPE_SERIALIZER;
    }

    @Override
    public RecipeType<?> getType() {
        return ModBlocks.PALLADIUM_ANVIL_RECIPE_TYPE;
    }

    /* ================================================================
     *  Serializer (MapCodec + StreamCodec)
     * ================================================================ */
    public static class Serializer implements RecipeSerializer<PalladiumAnvilRecipe> {

        public static final MapCodec<PalladiumAnvilRecipe> CODEC =
                RecordCodecBuilder.mapCodec(inst -> inst.group(
                        Ingredient.CODEC_NONEMPTY.fieldOf("catalyst").forGetter(PalladiumAnvilRecipe::getCatalyst),
                        Ingredient.CODEC_NONEMPTY.fieldOf("base").forGetter(PalladiumAnvilRecipe::getBase),
                        Ingredient.CODEC_NONEMPTY.fieldOf("addition").forGetter(PalladiumAnvilRecipe::getAddition),
                        ItemStack.STRICT_CODEC.fieldOf("result").forGetter(PalladiumAnvilRecipe::getResult)
                ).apply(inst, PalladiumAnvilRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, PalladiumAnvilRecipe> STREAM =
                StreamCodec.of(Serializer::write, Serializer::read);

        private static void write(RegistryFriendlyByteBuf buf, PalladiumAnvilRecipe r) {
            Ingredient.CONTENTS_STREAM_CODEC.encode(buf, r.catalyst);
            Ingredient.CONTENTS_STREAM_CODEC.encode(buf, r.base);
            Ingredient.CONTENTS_STREAM_CODEC.encode(buf, r.addition);
            ItemStack.STREAM_CODEC.encode(buf, r.result);
        }

        private static PalladiumAnvilRecipe read(RegistryFriendlyByteBuf buf) {
            Ingredient cat = Ingredient.CONTENTS_STREAM_CODEC.decode(buf);
            Ingredient base = Ingredient.CONTENTS_STREAM_CODEC.decode(buf);
            Ingredient add = Ingredient.CONTENTS_STREAM_CODEC.decode(buf);
            ItemStack res = ItemStack.STREAM_CODEC.decode(buf);
            return new PalladiumAnvilRecipe(cat, base, add, res);
        }

        @Override public MapCodec<PalladiumAnvilRecipe> codec()                                    { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, PalladiumAnvilRecipe> streamCodec()   { return STREAM; }
    }
}
