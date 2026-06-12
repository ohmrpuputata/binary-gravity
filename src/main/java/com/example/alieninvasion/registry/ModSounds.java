package com.example.alieninvasion.registry;

import com.example.alieninvasion.AlienInvasionMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

/** Голос охотника Макса: реплики отрендерены заранее (см. generate_hunter_voice.py). */
public class ModSounds {
    public static final SoundEvent HUNTER_HELLO = register("hunter.hello");
    public static final SoundEvent HUNTER_GIFT  = register("hunter.gift");
    public static final SoundEvent HUNTER_ANGRY = register("hunter.angry");
    public static final SoundEvent HUNTER_KILL  = register("hunter.kill");
    public static final SoundEvent HUNTER_DEATH = register("hunter.death");
    public static final SoundEvent HUNTER_IDLE  = register("hunter.idle");

    private static SoundEvent register(String name) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, name);
        return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
    }

    public static void registerSounds() {
        AlienInvasionMod.LOGGER.info("Registering ModSounds for " + AlienInvasionMod.MODID);
    }
}
