package com.example.alieninvasion.logic;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;

/**
 * Positions of the rare generated dungeons (mothership, monolith), recorded at
 * worldgen time so the Radio Transmitter can point players at the nearest one.
 * Worldgen runs on worker threads — all access is synchronized.
 */
public final class StructureLocationsData extends SavedData {
    private static final String KEY = "alien_structure_locations";

    private static final Factory<StructureLocationsData> FACTORY = new Factory<>(
            StructureLocationsData::new,
            (tag, provider) -> load(tag),
            null
    );

    public record Entry(String type, BlockPos pos) {}

    private final List<Entry> entries = new ArrayList<>();

    public static StructureLocationsData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, KEY);
    }

    public synchronized void add(String type, BlockPos pos) {
        entries.add(new Entry(type, pos.immutable()));
        setDirty();
    }

    /** Nearest recorded structure of the given type, or null. */
    public synchronized Entry nearestOfType(String type, BlockPos from) {
        Entry best = null;
        double bestSq = Double.MAX_VALUE;
        for (Entry e : entries) {
            if (!e.type().equals(type)) continue;
            double d = e.pos().distSqr(from);
            if (d < bestSq) {
                bestSq = d;
                best = e;
            }
        }
        return best;
    }

    /** Nearest recorded structure to the given position, or null if none exist yet. */
    public synchronized Entry nearest(BlockPos from) {
        Entry best = null;
        double bestSq = Double.MAX_VALUE;
        for (Entry e : entries) {
            double d = e.pos().distSqr(from);
            if (d < bestSq) {
                bestSq = d;
                best = e;
            }
        }
        return best;
    }

    private static StructureLocationsData load(CompoundTag tag) {
        StructureLocationsData d = new StructureLocationsData();
        ListTag list = tag.getList("entries", net.minecraft.nbt.Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag e = list.getCompound(i);
            d.entries.add(new Entry(e.getString("type"),
                    new BlockPos(e.getInt("x"), e.getInt("y"), e.getInt("z"))));
        }
        return d;
    }

    @Override
    public synchronized CompoundTag save(CompoundTag tag, net.minecraft.core.HolderLookup.Provider provider) {
        ListTag list = new ListTag();
        for (Entry e : entries) {
            CompoundTag t = new CompoundTag();
            t.putString("type", e.type());
            t.putInt("x", e.pos().getX());
            t.putInt("y", e.pos().getY());
            t.putInt("z", e.pos().getZ());
            list.add(t);
        }
        tag.put("entries", list);
        return tag;
    }
}
