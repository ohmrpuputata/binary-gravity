package com.example.alieninvasion.block;

import com.example.alieninvasion.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Хранит ТОЧНЫЙ исходный блок, который был залит кровью. Кровавый «двойник» —
 * это один из нескольких обобщённых кровавых кубов (по типу звука), а при
 * смывании/протирке возвращается именно сохранённый оригинал — будь то ваниль
 * или заражённый блок Роя. Так кровь ложится на ЛЮБОЙ блок, не теряя его.
 */
public class BloodyBlockEntity extends BlockEntity {
    private BlockState original;

    public BloodyBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.BLOODY_BLOCK_ENTITY, pos, state);
    }

    public void setOriginal(BlockState s) {
        this.original = s;
        setChanged();
    }

    public BlockState getOriginal() {
        return original;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (original != null) {
            tag.put("Original", NbtUtils.writeBlockState(original));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Original")) {
            this.original = NbtUtils.readBlockState(registries.lookupOrThrow(Registries.BLOCK), tag.getCompound("Original"));
        }
    }
}
