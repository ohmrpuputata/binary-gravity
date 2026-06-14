package com.example.alieninvasion.block;

import com.example.alieninvasion.registry.ModBlocks;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Shaped recipe for the Platinum Anvil crafting station.
 * <p>
 * JSON format (place in {@code data/alien-invasion/recipe/}):
 * <pre>{@code
 * {
 *   "type": "alien-invasion:platinum_anvil",
 *   "pattern": ["PPP", " I ", "III"],
 *   "key": {
 *     "P": {"item": "alien-invasion:platinum_block"},
 *     "I": {"item": "alien-invasion:platinum_ingot"}
 *   },
 *   "catalyst": {"item": "minecraft:amethyst_shard"},
 *   "result": {"id": "alien-invasion:nibirium_ingot", "count": 1}
 * }
 * }</pre>
 */
public class PlatinumAnvilRecipe implements Recipe<RecipeInput> {

    private final List<String> pattern;
    private final Map<String, Ingredient> key;
    private final Ingredient catalyst;
    private final ItemStack result;
    private final NonNullList<Ingredient> resolvedGrid;

    public PlatinumAnvilRecipe(List<String> pattern, Map<String, Ingredient> key,
                               Ingredient catalyst, ItemStack result) {
        this.pattern = pattern;
        this.key = key;
        this.catalyst = catalyst;
        this.result = result;
        this.resolvedGrid = resolvePattern(pattern, key);
    }

    /* ---- accessors (needed by codec getters) ---- */
    public List<String> getPattern()        { return pattern; }
    public Map<String, Ingredient> getKey() { return key; }
    public Ingredient getCatalyst()         { return catalyst; }
    public ItemStack getResult()            { return result; }

    public NonNullList<Ingredient> getIngredientsForJei() {
        NonNullList<Ingredient> ingredients = NonNullList.withSize(9, Ingredient.EMPTY);
        for (int i = 0; i < 9; i++) {
            if (i == 4) {
                ingredients.set(4, catalyst);
            } else {
                ingredients.set(i, resolvedGrid.get(i));
            }
        }
        return ingredients;
    }

    /* ---- resolve pattern + key → flat 9-ingredient grid ---- */
    private static NonNullList<Ingredient> resolvePattern(List<String> pattern, Map<String, Ingredient> key) {
        NonNullList<Ingredient> grid = NonNullList.withSize(9, Ingredient.EMPTY);
        for (int row = 0; row < Math.min(pattern.size(), 3); row++) {
            String line = pattern.get(row);
            for (int col = 0; col < Math.min(line.length(), 3); col++) {
                char c = line.charAt(col);
                if (c != ' ') {
                    Ingredient ing = key.get(String.valueOf(c));
                    if (ing != null) grid.set(row * 3 + col, ing);
                }
            }
        }
        return grid;
    }

    /* ---- Recipe implementation ---- */

    @Override
    public boolean matches(RecipeInput input, Level level) {
        for (int i = 0; i < 9; i++) {
            if (i == 4) {
                if (!catalyst.test(input.getItem(4))) return false;
                continue;
            }
            Ingredient ing = resolvedGrid.get(i);
            ItemStack stack = input.getItem(i);
            boolean empty = ing.isEmpty();
            if (empty) {
                if (!stack.isEmpty()) return false;
            } else {
                if (!ing.test(stack)) return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack assemble(RecipeInput input, HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int w, int h) {
        return w >= 3 && h >= 3;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModBlocks.PLATINUM_ANVIL_RECIPE_SERIALIZER;
    }

    @Override
    public RecipeType<?> getType() {
        return ModBlocks.PLATINUM_ANVIL_RECIPE_TYPE;
    }

    /* ================================================================
     *  Serializer (MapCodec + StreamCodec)
     * ================================================================ */
    public static class Serializer implements RecipeSerializer<PlatinumAnvilRecipe> {

        public static final MapCodec<PlatinumAnvilRecipe> CODEC =
                RecordCodecBuilder.mapCodec(inst -> inst.group(
                        Codec.STRING.listOf().fieldOf("pattern")
                                .forGetter(PlatinumAnvilRecipe::getPattern),
                        Codec.unboundedMap(Codec.STRING, Ingredient.CODEC_NONEMPTY)
                                .fieldOf("key")
                                .forGetter(PlatinumAnvilRecipe::getKey),
                        Ingredient.CODEC_NONEMPTY.fieldOf("catalyst")
                                .forGetter(PlatinumAnvilRecipe::getCatalyst),
                        ItemStack.STRICT_CODEC.fieldOf("result")
                                .forGetter(PlatinumAnvilRecipe::getResult)
                ).apply(inst, PlatinumAnvilRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, PlatinumAnvilRecipe> STREAM =
                StreamCodec.of(Serializer::write, Serializer::read);

        private static void write(RegistryFriendlyByteBuf buf, PlatinumAnvilRecipe r) {
            buf.writeVarInt(r.pattern.size());
            for (String s : r.pattern) buf.writeUtf(s);
            buf.writeVarInt(r.key.size());
            for (var e : r.key.entrySet()) {
                buf.writeUtf(e.getKey());
                Ingredient.CONTENTS_STREAM_CODEC.encode(buf, e.getValue());
            }
            Ingredient.CONTENTS_STREAM_CODEC.encode(buf, r.catalyst);
            ItemStack.STREAM_CODEC.encode(buf, r.result);
        }

        private static PlatinumAnvilRecipe read(RegistryFriendlyByteBuf buf) {
            int pLen = buf.readVarInt();
            List<String> pat = new ArrayList<>(pLen);
            for (int i = 0; i < pLen; i++) pat.add(buf.readUtf());
            int kLen = buf.readVarInt();
            Map<String, Ingredient> keyMap = new HashMap<>(kLen);
            for (int i = 0; i < kLen; i++)
                keyMap.put(buf.readUtf(), Ingredient.CONTENTS_STREAM_CODEC.decode(buf));
            Ingredient cat = Ingredient.CONTENTS_STREAM_CODEC.decode(buf);
            ItemStack res = ItemStack.STREAM_CODEC.decode(buf);
            return new PlatinumAnvilRecipe(pat, keyMap, cat, res);
        }

        @Override public MapCodec<PlatinumAnvilRecipe> codec()                                    { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, PlatinumAnvilRecipe> streamCodec()   { return STREAM; }
    }
}
