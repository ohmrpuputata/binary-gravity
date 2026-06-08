import gzip
with gzip.open(r"C:\Users\hacke\AppData\Roaming\.minecraft\logs\2026-06-07-2.log.gz", "rb") as f:
    content = f.read().decode("utf-8", errors="ignore")
    open("temp_log2.txt", "w", encoding="utf-8").write(content)
print("Decompressed log 2 successfully")
