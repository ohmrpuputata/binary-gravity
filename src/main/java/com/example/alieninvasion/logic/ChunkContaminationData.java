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
    /** Chunks reclaimed by a Purifier: contamination never touches them while marked. */
    private final java.util.HashSet<Long> purified   = new java.util.HashSet<>();
    /** Chunks whose Infection Heart was destroyed: existing rot stays, but never grows. */
    private final java.util.HashSet<Long> inert      = new java.util.HashSet<>();

    private ChunkContaminationData() {}

    public static ChunkContaminationData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, KEY);
    }

    public int getSurfaceDay(ChunkPos pos) { return surfaceDays.getOrDefault(pos.toLong(), -1); }
    public int getOreDay   (ChunkPos pos) { return oreDays    .getOrDefault(pos.toLong(), -1); }

    public void setSurfaceDay(ChunkPos pos, int day) { surfaceDays.put(pos.toLong(), day); setDirty(); }
    public void setOreDay   (ChunkPos pos, int day) { oreDays    .put(pos.toLong(), day); setDirty(); }

    public boolean isPurified(ChunkPos pos) { return purified.contains(pos.toLong()); }

    public void setPurified(ChunkPos pos, boolean value) {
        if (value) purified.add(pos.toLong());
        else       purified.remove(pos.toLong());
        setDirty();
    }

    public boolean isInert(ChunkPos pos) { return inert.contains(pos.toLong()); }

    public void setInert(ChunkPos pos, boolean value) {
        if (value) inert.add(pos.toLong());
        else       inert.remove(pos.toLong());
        setDirty();
    }

    /** True when no NEW contamination may be written into this chunk. */
    public boolean isProtectedChunk(ChunkPos pos) {
        return isPurified(pos) || isInert(pos);
    }

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
        for (long p : tag.getLongArray("purified")) {
            d.purified.add(p);
        }
        for (long p : tag.getLongArray("inert")) {
            d.inert.add(p);
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
        tag.putLongArray("purified", purified.stream().mapToLong(Long::longValue).toArray());
        tag.putLongArray("inert", inert.stream().mapToLong(Long::longValue).toArray());
        return tag;
    }
}
