"""
Synthesize realistic Geiger-counter click sounds (Ogg Vorbis) for the mod.

A Geiger click through a small speaker is a very short, sharp transient: a
broadband noise spike on the attack plus a fast-damped high resonance ("tick").
We render a few slightly different variants so the counter never repeats the
exact same click — Minecraft picks randomly from the sound pool.

Output: src/main/resources/assets/alien-invasion/sounds/geiger/click{1,2,3}.ogg
Needs: numpy + soundfile (libsndfile with Vorbis). No ffmpeg required.
"""
import os
import numpy as np
import soundfile as sf

SR = 44100
OUT = "src/main/resources/assets/alien-invasion/sounds/geiger"

# (seed, resonance freq Hz, decay tau s) — three distinct but related ticks
VARIANTS = [
    (1, 2700.0, 0.0016),
    (2, 3300.0, 0.0012),
    (3, 3900.0, 0.0019),
]


def click(seed, f0, tau, dur=0.013):
    rng = np.random.default_rng(seed)
    n = int(SR * dur)
    t = np.arange(n) / SR
    # damped resonance (the "tone" of the speaker)
    tone = np.sin(2 * np.pi * f0 * t) * np.exp(-t / tau)
    # broadband noise spike on the attack, decays ~3x faster than the tone
    noise = rng.standard_normal(n) * np.exp(-t / (tau * 0.35))
    y = 0.55 * tone + 0.6 * noise
    # hard initial impulse for the sharp "snap"
    y[0] += 1.0
    if n > 1:
        y[1] -= 0.55
    # fade the last ~1 ms so the buffer ends at zero (no end-click)
    f = max(1, int(SR * 0.001))
    y[-f:] *= np.linspace(1.0, 0.0, f)
    y /= (np.max(np.abs(y)) + 1e-9)
    return (y * 0.7).astype(np.float32)


def main():
    os.makedirs(OUT, exist_ok=True)
    for i, (seed, f0, tau) in enumerate(VARIANTS, start=1):
        y = click(seed, f0, tau)
        path = os.path.join(OUT, f"click{i}.ogg")
        sf.write(path, y, SR, format="OGG", subtype="VORBIS")
        peak = float(np.max(np.abs(y)))
        print(f"  click{i}.ogg  {len(y)} samp  {1000*len(y)/SR:.1f} ms  peak {peak:.2f}  f0 {f0:.0f}Hz")
    print("done")


if __name__ == "__main__":
    main()
