# -*- coding: utf-8 -*-
"""Вставляет тихие 'чужие' звуки (ambient/hurt/death + pitch/volume/interval)
в сущности роя сразу после строки объявления класса. Идемпотентно."""
import re

E = "src/main/java/com/example/alieninvasion/entity"

# entity -> (ambient, hurt, death, pitch, volume, interval)
SOUNDS = {
    "AlienStalkerEntity":   ("PHANTOM_AMBIENT", "PHANTOM_HURT", "PHANTOM_DEATH", "0.65F", "0.5F", 260),
    "AlienBreacherEntity":  ("SILVERFISH_AMBIENT", "SILVERFISH_HURT", "SILVERFISH_DEATH", "0.5F", "0.7F", 220),
    "PlasmaCasterEntity":   ("BLAZE_AMBIENT", "BLAZE_HURT", "BLAZE_DEATH", "0.6F", "0.55F", 240),
    "HiveShamanEntity":     ("EVOKER_AMBIENT", "EVOKER_HURT", "EVOKER_DEATH", "0.7F", "0.6F", 240),
    "AcidSpitterEntity":    ("SPIDER_AMBIENT", "SPIDER_HURT", "SPIDER_DEATH", "0.55F", "0.6F", 220),
    "ParasiteEntity":       ("SILVERFISH_AMBIENT", "SILVERFISH_HURT", "SILVERFISH_DEATH", "1.45F", "0.5F", 160),
    "AlienTrollEntity":     ("RAVAGER_AMBIENT", "RAVAGER_HURT", "RAVAGER_DEATH", "1.15F", "0.5F", 280),
    "HiveTyrantEntity":     ("WARDEN_AMBIENT", "WARDEN_HURT", "WARDEN_DEATH", "1.25F", "0.6F", 300),
    "SwarmMotherEntity":    ("WARDEN_AMBIENT", "WARDEN_HURT", "WARDEN_DEATH", "0.8F", "0.8F", 280),
}

SNIPPET = """
    // ALIEN VOICE: quiet, pitched vanilla sounds remixed into something wrong -
    // rarer and softer than the originals so the swarm unnerves instead of annoys.
    @Override
    protected net.minecraft.sounds.SoundEvent getAmbientSound() {{
        return net.minecraft.sounds.SoundEvents.{amb};
    }}

    @Override
    protected net.minecraft.sounds.SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source) {{
        return net.minecraft.sounds.SoundEvents.{hurt};
    }}

    @Override
    protected net.minecraft.sounds.SoundEvent getDeathSound() {{
        return net.minecraft.sounds.SoundEvents.{death};
    }}

    @Override
    public int getAmbientSoundInterval() {{
        return {interval};
    }}

    @Override
    protected float getSoundVolume() {{
        return {vol};
    }}

    @Override
    public float getVoicePitch() {{
        return {pitch} + this.random.nextFloat() * 0.1F;
    }}
"""

for name, (amb, hurt, death, pitch, vol, interval) in SOUNDS.items():
    path = f"{E}/{name}.java"
    with open(path, encoding="utf-8") as f:
        src = f.read()
    if "ALIEN VOICE" in src:
        print(f"  skip {name} (уже есть)")
        continue
    m = re.search(r"^public class \w+ extends [^{]+\{\s*$", src, re.M)
    if not m:
        print(f"  !! не нашёл класс в {name}")
        continue
    snippet = SNIPPET.format(amb=amb, hurt=hurt, death=death, pitch=pitch, vol=vol, interval=interval)
    src = src[:m.end()] + snippet + src[m.end():]
    with open(path, "w", encoding="utf-8") as f:
        f.write(src)
    print(f"  + {name}: {amb} pitch {pitch}")
print("done")
