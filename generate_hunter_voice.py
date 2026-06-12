# -*- coding: utf-8 -*-
"""Голос Макса Максбетова: нейронный диктор Microsoft Edge TTS
(ru-RU-DmitryNeural — плотный мужской голос), полные реплики, слегка
заниженный тон. mp3 -> ogg vorbis через ffmpeg из imageio-ffmpeg.
Требует интернет (edge-tts ходит в облако)."""
import asyncio
import os
import subprocess
import sys
import tempfile

import edge_tts
import imageio_ffmpeg

OUT_DIR = "src/main/resources/assets/alien-invasion/sounds/hunter"
VOICE = "ru-RU-DmitryNeural"
RATE = "-8%"     # чуть неторопливее — уверенность
PITCH = "-6Hz"   # чуть ниже — солиднее

LINES = {
    "hello": "Ну хоть кто-то выжил. Лучшее, что осталось у человечества... Печально. "
             "Я такие рои в одиночку гасил, пока вы по землянкам сидели.",
    "gift": "Ладно, хорош сопли жевать. Вот вам мой реактор — настоящая пушка, не ваши палки. "
            "Тащите через портал, ставьте у главного улья — и валите со всех ног. "
            "Минута сорок — и их поганый шарик станет салютом.",
    "angry": "Ты чё творишь, дебил?! На меня?! Я тебя сейчас обратно в каменный век отправлю!",
    "kill": "И стоило оно того, бомжина?",
    "death": "Хех... красиво вышло... Реактор забери, бродяга. Закончи начатое. "
             "Теперь ты тут... самый крутой.",
    "idle": "Шевелитесь, бомжи. Планета сама себя не взорвёт.",
}


async def render(text, mp3_path):
    tts = edge_tts.Communicate(text, VOICE, rate=RATE, pitch=PITCH)
    await tts.save(mp3_path)


def main():
    os.makedirs(OUT_DIR, exist_ok=True)
    ffmpeg = imageio_ffmpeg.get_ffmpeg_exe()
    tmp = tempfile.mkdtemp()
    for name, text in LINES.items():
        mp3 = os.path.join(tmp, name + ".mp3")
        asyncio.run(render(text, mp3))
        ogg = os.path.join(OUT_DIR, name + ".ogg")
        subprocess.run([ffmpeg, "-y", "-i", mp3, "-c:a", "libvorbis", "-q:a", "4",
                        "-ar", "44100", "-ac", "1", ogg],
                       check=True, capture_output=True)
        size = os.path.getsize(ogg)
        print(f"OK {ogg} ({size // 1024} KB)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
