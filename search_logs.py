import gzip
import glob
import os

logs = glob.glob(r"C:\Users\hacke\AppData\Roaming\.minecraft\logs\*.gz")
for log in sorted(logs):
    if "2026-06-07" in log:
        try:
            with gzip.open(log, "rb") as f:
                content = f.read().decode("utf-8", errors="ignore")
                lines = content.splitlines()
                # Print last 10 lines of each log to see if it crashed at startup or in-game
                print(f"=== {os.path.basename(log)} (lines: {len(lines)}) ===")
                for line in lines[-10:]:
                    print("  " + line)
        except Exception as e:
            print(f"Error reading {log}: {e}")
