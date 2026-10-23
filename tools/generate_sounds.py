#!/usr/bin/env python3
"""Generates the app's sound effects into android/app/src/main/res/raw/.

The sounds are synthesized (no third-party audio), mirroring the design
prototype's WebAudio `tone()` calls: an oscillator with a 5 ms exponential
attack, an exponential decay to silence, and an optional exponential pitch
slide. Run from anywhere:  python3 tools/generate_sounds.py
"""
import math
import os
import struct
import wave

RATE = 22050
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "android", "app", "src", "main", "res", "raw")

# Peak level per kind after normalization, so every theme is equally loud.
TICK_PEAK = 0.5
WIN_PEAK = 0.8
LOSE_PEAK = 0.6

# From the prototype's THEMES[...].snd.
THEMES = {
    "default": {"tick": (1700, "square", 0.025, 0.04, None), "win": [523, 659, 784, 1047], "wave": "triangle"},
    "noel": {"tick": (2637, "sine", 0.08, 0.07, None),
             "win": [659, 659, 659, 659, 659, 659, 659, 784, 523, 587, 659], "wave": "sine", "gap": 0.12},
    "tet": {"tick": (150, "sine", 0.09, 0.35, 60), "win": [392, 523, 659, 784], "wave": "square", "gong": True},
    "trung_thu": {"tick": (900, "triangle", 0.045, 0.18, None), "win": [440, 554, 659, 880], "wave": "triangle"},
    "love": {"tick": (3000, "sine", 0.025, 0.05, None), "win": [784, 988, 1175, 1568], "wave": "sine"},
    "halloween": {"tick": (200, "sawtooth", 0.06, 0.05, 150), "win": [392, 466, 392, 311, 392, 523], "wave": "square"},
}


def oscillator(kind, phase):
    """Band-limited-ish waveforms: a few harmonics keep square/saw from sounding harsh."""
    if kind == "sine":
        return math.sin(phase)
    if kind == "square":
        return sum(math.sin(phase * k) / k for k in (1, 3, 5, 7, 9)) * 4 / math.pi
    if kind == "triangle":
        return sum(((-1) ** i) * math.sin(phase * k) / (k * k) for i, k in enumerate((1, 3, 5, 7))) * 8 / (math.pi ** 2)
    if kind == "sawtooth":
        return sum(((-1) ** (k + 1)) * math.sin(phase * k) / k for k in range(1, 9)) * 2 / math.pi
    raise ValueError(kind)


def tone(buf, freq, kind, dur, vol, when, slide=None):
    start = int(when * RATE)
    n = int((dur + 0.03) * RATE)
    need = start + n
    if len(buf) < need:
        buf.extend([0.0] * (need - len(buf)))
    phase = 0.0
    attack = 0.005
    for i in range(n):
        t = i / RATE
        f = freq * ((slide / freq) ** min(1.0, t / dur)) if slide else freq
        phase += 2 * math.pi * f / RATE
        if t < attack:
            gain = 0.0001 * ((vol / 0.0001) ** (t / attack))
        elif t < dur:
            gain = vol * ((0.0001 / vol) ** ((t - attack) / (dur - attack)))
        else:
            gain = 0.0
        buf[start + i] += oscillator(kind, phase) * gain


def write(name, buf, peak):
    top = max(abs(x) for x in buf) or 1.0
    scale = peak / top
    path = os.path.join(OUT, name + ".wav")
    with wave.open(path, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(RATE)
        w.writeframes(b"".join(struct.pack("<h", int(max(-1.0, min(1.0, x * scale)) * 32767)) for x in buf))
    print("wrote", os.path.relpath(path), f"{len(buf) / RATE:.2f}s")


def main():
    os.makedirs(OUT, exist_ok=True)
    for theme, snd in THEMES.items():
        f, kind, dur, vol, slide = snd["tick"]
        buf = []
        tone(buf, f, kind, dur, vol, 0, slide)
        write(f"tick_{theme}", buf, TICK_PEAK)

        buf = []
        gap = snd.get("gap", 0.11)
        loud = snd["wave"] == "square"
        for i, note in enumerate(snd["win"]):
            tone(buf, note, snd["wave"], 0.32, 0.05 if loud else 0.12, i * gap)
        tone(buf, snd["win"][-1], snd["wave"], 0.9, 0.04 if loud else 0.09, len(snd["win"]) * gap)
        if snd.get("gong"):
            tone(buf, 98, "sine", 1.8, 0.3, 0, 90)
        write(f"win_{theme}", buf, WIN_PEAK)

    buf = []
    tone(buf, 523, "sine", 0.3, 0.07, 0)
    tone(buf, 392, "sine", 0.55, 0.06, 0.16)
    write("lose", buf, LOSE_PEAK)


if __name__ == "__main__":
    main()
