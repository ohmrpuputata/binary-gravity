"""Добавляет ТРЕТИЙ вариант (v2) кровавой декали, не переписывая блокстейт с нуля.

1) Для каждой модели *_1.json в blood_layer/ делает копию *_2.json, заменив текстуру
   _v1 -> _v2 (геометрия пола/стены сохраняется ровно как в v1).
2) В blockstates/blood_layer.json к КАЖДОЙ записи варианта с моделью ..._1 добавляет
   такую же запись с моделью ..._2 (те же повороты x/y/uvlock). Существующие записи
   (v0, v1) не трогаются — просто появляется ещё один случайный вариант.

Итог: на каждое состояние теперь v0/v1/v2 × повороты — заметно больше разнообразия.
"""
import glob
import json
import os

ROOT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources",
                    "assets", "alien-invasion")
MODELS = os.path.join(ROOT, "models", "block", "blood_layer")
BS = os.path.join(ROOT, "blockstates", "blood_layer.json")

# 1) модели v2 из v1
made = 0
for f in glob.glob(os.path.join(MODELS, "*_1.json")):
    with open(f, "r", encoding="utf-8") as fh:
        data = fh.read()
    data = data.replace("_v1", "_v2")
    out = f[:-len("_1.json")] + "_2.json"
    with open(out, "w", encoding="utf-8") as fh:
        fh.write(data)
    made += 1
print("v2 models written:", made)

# 2) дополняем блокстейт
with open(BS, "r", encoding="utf-8") as fh:
    bs = json.load(fh)

added = 0
for key, val in bs["variants"].items():
    entries = val if isinstance(val, list) else [val]
    extra = []
    for e in entries:
        m = e.get("model", "")
        if m.endswith("_1"):
            ne = dict(e)
            ne["model"] = m[:-1] + "2"
            extra.append(ne)
            added += 1
    bs["variants"][key] = entries + extra

with open(BS, "w", encoding="utf-8") as fh:
    json.dump(bs, fh, indent=2, ensure_ascii=False)
print("v2 blockstate entries added:", added)
