package com.example.alieninvasion.logic;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;

/** Persists per-chunk contamination state across save/load. */
public final class ChunkContaminationData extends SavedData {

    private static final String KEY = "alien_chunk_contamination";

    private static final Factory<ChunkContaminationData> FACTORY = new Factory<>(
            ChunkContaminationData::new,
            (tag, provider) -> load(tag),
            null
    );

    private final HashMap<Long, Integer> surfaceDays = new HashMap<>();
    private final HashMap<Long, Integer> oreDays     = new HashMap<>();

    private ChunkContaminationData() {}

    public static ChunkContaminationData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, KEY);
    }

    public int getSurfaceDay(ChunkPos pos) { return surfaceDays.getOrDefault(pos.toLong(), -1); }
    public int getOreDay   (ChunkPos pos) { return oreDays    .getOrDefault(pos.toLong(), -1); }

    public void setSurfaceDay(ChunkPos pos, int day) { surfaceDays.put(pos.toLong(), day); setDirty(); }
    public void setOreDay   (ChunkPos pos, int day) { oreDays    .put(pos.toLong(), day); setDirty(); }

    private static ChunkContaminationData load(CompoundTag tag) {
        ChunkContaminationData d = new ChunkContaminationData();
        ListTag surface = tag.getList("surface", net.minecraft.nbt.Tag.TAG_COMPOUND);
        for (int i = 0; i < surface.size(); i++) {
            CompoundTag e = surface.getCompound(i);
            d.surfaceDays.put(e.getLong("pos"), e.getInt("day"));
        }
        ListTag ore = tag.getList("ore", net.minecraft.nbt.Tag.TAG_COMPOUND);
        for (int i = 0; i < ore.size(); i++) {
            CompoundTag e = ore.getCompound(i);
            d.oreDays.put(e.getLong("pos"), e.getInt("day"));
        }
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag, net.minecraft.core.HolderLookup.Provider provider) {
        ListTag surface = new ListTag();
        surfaceDays.forEach((k, v) -> { CompoundTag e = new CompoundTag(); e.putLong("pos", k); e.putInt("day", v); surface.add(e); });
        tag.put("surface", surface);

        ListTag ore = new ListTag();
        oreDays.forEach((k, v) -> { CompoundTag e = new CompoundTag(); e.putLong("pos", k); e.putInt("day", v); ore.add(e); });
        tag.put("ore", ore);
        return tag;
    }
}
