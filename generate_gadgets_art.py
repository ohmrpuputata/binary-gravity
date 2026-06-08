"""
Art generator for the new items and entities in v1.13.0:
* Hand-drawn 16x16 shaded item icons with dark outlines and detailed lighting for:
  - borer.png (Handheld drill vehicle spawner)
  - emp_grenade.png (Futuristic EMP tech grenade)
  - herbal_salve.png (Terracotta jar with overflowing herbal remedy)
* Worn/entity vehicle texture sheet for both the Drill and rideable Borer vehicle:
  - drill.png (64x32 industrial hull with warning stripes and spiral bit)
* Dynamically downloads and overlays high-fidelity cybernetic/radioactive graphics for:
  - sky_drone.png (cybernetic steel phantom with glowing neon-cyan circuit lines)
  - cave_lurker.png (obsidian spider with venomous glowing-green bioluminescent spots)
  - acid_spitter.png (corroded yellow-green skeleton skeleton dripping with radioactive acid slime)

Run with:
  .venv/Scripts/python.exe generate_gadgets_art.py
"""
from PIL import Image
import os

PROJECT = os.path.dirname(os.path.abspath(__file__))
ITEM_TEX = os.path.join(PROJECT, "src", "main", "resources", "assets", "alien-invasion", "textures", "item")
ENTITY_TEX = os.path.join(PROJECT, "src", "main", "resources", "assets", "alien-invasion", "textures", "entity")

os.makedirs(ITEM_TEX, exist_ok=True)
os.makedirs(ENTITY_TEX, exist_ok=True)

OUTLINE = (16, 14, 24, 255)

def clamp(v):
    return max(0, min(255, int(v)))

def blank():
    return Image.new("RGBA", (16, 16), (0, 0, 0, 0))

def put(px, x, y, c):
    if 0 <= x < 16 and 0 <= y < 16:
        px[x, y] = c

def rect(px, x0, y0, x1, y1, c):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            put(px, x, y, c)

def put_ent(px, w, h, x, y, c):
    if 0 <= x < w and 0 <= y < h:
        px[x, y] = c

def add_outline(img, color=OUTLINE):
    src = img.copy()
    sp = src.load()
    px = img.load()
    for y in range(16):
        for x in range(16):
            if sp[x, y][3] != 0:
                continue
            touch = False
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1),
                           (1, 1), (1, -1), (-1, 1), (-1, -1)):
                nx, ny = x + dx, y + dy
                if 0 <= nx < 16 and 0 <= ny < 16 and sp[nx, ny][3] != 0:
                    touch = True
                    break
            if touch:
                px[x, y] = color

def save(img, name):
    add_outline(img)
    img.save(os.path.join(ITEM_TEX, name + ".png"))
    print("item", name + ".png")

# ------------------------------------------------------------- borer
def draw_borer():
    img = blank(); px = img.load()
    
    # Palette
    MC = (55, 62, 72, 255)         # casing main metal
    MC_HI = (100, 112, 128, 255)   # casing highlight
    MC_DK = (35, 40, 48, 255)      # casing shadow
    
    BIT = (165, 175, 190, 255)     # drill bit metal
    BIT_HI = (230, 240, 255, 255)  # drill bit highlight
    BIT_DK = (80, 90, 105, 255)    # drill bit shadow/groove
    
    GLOW = (0, 240, 255, 255)      # glowing energy indicator
    GLOW_HI = (180, 255, 255, 255) # glowing core highlights
    
    HAZ_Y = (245, 195, 20, 255)    # warning yellow
    HAZ_B = (25, 25, 30, 255)      # warning black
    
    GRIP = (45, 48, 55, 255)       # handle main
    GRIP_DK = (25, 28, 32, 255)    # handle shadow
    RED_BTN = (240, 50, 50, 255)   # trigger button

    # Handle / Grip (bottom-left)
    rect(px, 2, 12, 3, 14, GRIP_DK)
    rect(px, 3, 11, 4, 13, GRIP)
    put(px, 3, 13, GRIP_DK)
    put(px, 4, 12, RED_BTN)

    # Casing
    rect(px, 4, 6, 9, 11, MC)
    # highlights
    rect(px, 4, 6, 9, 6, MC_HI)
    rect(px, 4, 6, 4, 11, MC_HI)
    # shadows
    rect(px, 9, 7, 9, 11, MC_DK)
    rect(px, 5, 11, 9, 11, MC_DK)

    # Diagonal warning stripes on casing back:
    put(px, 4, 9, HAZ_Y); put(px, 5, 8, HAZ_Y)
    put(px, 4, 10, HAZ_B); put(px, 5, 9, HAZ_B)
    put(px, 4, 8, HAZ_B); put(px, 5, 7, HAZ_B)

    # Glowing plasma core in center:
    rect(px, 6, 8, 7, 9, GLOW)
    put(px, 6, 8, GLOW_HI)

    # Drill bit (pointing top-right)
    # Base:
    put(px, 10, 5, BIT_DK)
    put(px, 10, 6, BIT)
    put(px, 10, 7, BIT_DK)
    # cone:
    put(px, 11, 4, BIT_HI)
    put(px, 11, 5, BIT)
    put(px, 11, 6, BIT_DK)
    # cone:
    put(px, 12, 3, BIT_HI)
    put(px, 12, 4, BIT)
    put(px, 12, 5, BIT_DK)
    # cone:
    put(px, 13, 2, BIT_HI)
    put(px, 13, 3, BIT)
    put(px, 13, 4, BIT_DK)
    # tip:
    put(px, 14, 1, BIT_HI)
    put(px, 14, 2, BIT_HI)
    put(px, 14, 3, BIT_DK)

    # Spiral drill bit grooves (diagonal shading):
    put(px, 10, 6, BIT_DK)
    put(px, 11, 5, BIT_DK)
    put(px, 12, 4, BIT_DK)
    put(px, 13, 3, BIT_DK)
    put(px, 14, 2, BIT_HI)

    save(img, "borer")

# ------------------------------------------------------------- emp_grenade
def draw_emp_grenade():
    img = blank(); px = img.load()
    
    # Palette
    MC = (50, 60, 75, 255)          # main metal
    MC_HI = (120, 140, 160, 255)    # metal highlight
    MC_DK = (30, 35, 45, 255)       # metal shadow
    GLOW = (0, 240, 255, 255)       # cyan circuit lines
    GLOW_HI = (180, 255, 255, 255)  # glowing center core
    COPPER = (210, 110, 50, 255)    # top coil
    COPPER_HI = (250, 160, 90, 255) # coil highlight
    SILVER = (200, 205, 215, 255)   # pull ring
    SILVER_DK = (130, 135, 145, 255)

    # Draw pull-ring at top
    for (x, y) in [(6, 0), (7, 0), (5, 1), (8, 1), (6, 2), (7, 2)]:
        put(px, x, y, SILVER)
    put(px, 5, 2, SILVER_DK); put(px, 8, 2, SILVER_DK)

    # Draw copper coil connector at top
    rect(px, 6, 3, 9, 3, COPPER)
    put(px, 7, 3, COPPER_HI); put(px, 8, 3, COPPER_HI)

    # Spherical body circle at (8, 8) radius ~4.6
    for y in range(4, 13):
        for x in range(4, 13):
            dx, dy = x - 8, y - 8
            dist2 = dx*dx + dy*dy
            if dist2 <= 21:
                px[x, y] = MC

    # Sphere panel highlight / shadow shading:
    for y in range(4, 7):
        for x in range(4, 7):
            dx, dy = x - 8, y - 8
            dist2 = dx*dx + dy*dy
            if dist2 <= 21:
                if x == 4 or y == 4:
                    px[x, y] = MC_HI

    for y in range(9, 13):
        for x in range(9, 13):
            dx, dy = x - 8, y - 8
            dist2 = dx*dx + dy*dy
            if dist2 <= 21:
                if x == 11 or y == 11 or x == 12 or y == 12:
                    px[x, y] = MC_DK
                else:
                    px[x, y] = MC_DK

    # Glowing EMP channel lines:
    for i in range(4, 12):
        if px[i, 8][3] != 0:
            px[i, 8] = GLOW
        if px[8, i][3] != 0:
            px[8, i] = GLOW

    # Bright glow intersection:
    rect(px, 7, 7, 8, 8, GLOW_HI)
    # Segment indicators
    put(px, 5, 5, GLOW)
    put(px, 10, 10, GLOW)

    save(img, "emp_grenade")

# ------------------------------------------------------------- herbal_salve
def draw_herbal_salve():
    img = blank(); px = img.load()
    
    # Palette
    CLAY = (180, 85, 45, 255)       # terracotta pot main
    CLAY_HI = (230, 140, 90, 255)   # terracotta highlight
    CLAY_DK = (110, 45, 25, 255)    # terracotta shadow
    SALVE = (70, 205, 50, 255)      # green remedy
    SALVE_HI = (180, 255, 140, 255) # green highlight
    SALVE_DK = (30, 110, 20, 255)   # green shadow
    ROPE = (160, 120, 70, 255)      # neck rope
    CORK = (90, 55, 30, 255)        # cork stopper

    # Cork stopper
    rect(px, 7, 3, 8, 3, CORK)

    # Overflowing salve at top mouth
    rect(px, 6, 4, 9, 4, SALVE)
    put(px, 7, 4, SALVE_HI)
    put(px, 6, 4, SALVE_DK)

    # Neck rim
    rect(px, 5, 5, 10, 5, CLAY)
    put(px, 5, 5, CLAY_HI)
    put(px, 10, 5, CLAY_DK)

    # Rope wrap
    rect(px, 5, 6, 10, 6, ROPE)
    put(px, 8, 6, ROPE) # tie knot

    # Body belly
    rect(px, 4, 7, 11, 12, CLAY)
    # Highlights
    rect(px, 4, 7, 5, 11, CLAY)
    put(px, 4, 7, CLAY_HI); put(px, 5, 7, CLAY_HI)
    put(px, 4, 8, CLAY_HI); put(px, 4, 9, CLAY_HI); put(px, 4, 10, CLAY_HI)
    # Shadows
    rect(px, 10, 7, 11, 12, CLAY_DK)
    rect(px, 5, 12, 10, 12, CLAY_DK)
    put(px, 9, 11, CLAY_DK)

    # Leaf logo on jar center
    put(px, 6, 9, SALVE_DK)
    put(px, 7, 8, SALVE)
    put(px, 8, 9, SALVE)
    put(px, 7, 9, SALVE_HI)
    put(px, 7, 10, SALVE_DK)

    save(img, "herbal_salve")

# ------------------------------------------------------------- 3D DRILL/BORER VEHICLE
def paint_drill():
    # 64x32 entity texture sheet for DrillEntity and BorerVehicleEntity
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    px = img.load()

    # Colors
    DARK_METAL = (45, 48, 55, 255)
    DARK_METAL_HI = (85, 92, 105, 255)
    DARK_METAL_DK = (28, 30, 35, 255)

    SILVER_METAL = (160, 170, 185, 255)
    SILVER_METAL_HI = (225, 235, 250, 255)
    SILVER_METAL_DK = (90, 100, 115, 255)

    HAZARD_Y = (235, 175, 20, 255)
    HAZARD_B = (25, 25, 30, 255)

    GLOW_CYAN = (0, 230, 255, 255)
    GLOW_CYAN_HI = (180, 255, 255, 255)

    GLOW_ORANGE = (255, 110, 10, 255)
    GLOW_ORANGE_HI = (255, 210, 100, 255)

    # 1. HULL / BODY (0, 0) size 8x10x8
    # Top face: (8, 0) to (15, 7)
    rect(px, 8, 0, 15, 7, DARK_METAL)
    # Highlight borders
    for x in range(8, 16):
        put_ent(px, 64, 32, x, 0, DARK_METAL_HI)
    for y in range(0, 8):
        put_ent(px, 64, 32, 8, y, DARK_METAL_HI)
    # Cooling grille in center
    for x in (10, 12, 14):
        for y in range(2, 6):
            put_ent(px, 64, 32, x, y, DARK_METAL_DK)

    # Bottom face: (16, 0) to (23, 7)
    rect(px, 16, 0, 23, 7, DARK_METAL_DK)
    for x in range(17, 23, 2):
        for y in range(1, 7, 2):
            put_ent(px, 64, 32, x, y, DARK_METAL)

    # Side faces: West (0, 8), North (8, 8), East (16, 8), South (24, 8) -> size 8x10
    for side in range(4):
        x0 = side * 8
        rect(px, x0, 8, x0 + 7, 17, DARK_METAL)
        # Highlight border
        for x in range(x0, x0 + 8):
            put_ent(px, 64, 32, x, 8, DARK_METAL_HI)
        for y in range(8, 18):
            put_ent(px, 64, 32, x0, y, DARK_METAL_HI)
            put_ent(px, 64, 32, x0 + 7, y, DARK_METAL_DK)

    # Add warning hazard stripes on upper part of sides (y=9..11)
    for side in range(4):
        x0 = side * 8
        for y in range(9, 12):
            for x in range(x0, x0 + 8):
                if (x + y) % 4 < 2:
                    put_ent(px, 64, 32, x, y, HAZARD_Y)
                else:
                    put_ent(px, 64, 32, x, y, HAZARD_B)

    # Glowing cyan core status vents on North/South sides:
    rect(px, 11, 13, 13, 15, GLOW_CYAN)
    put_ent(px, 64, 32, 12, 14, GLOW_CYAN_HI)
    rect(px, 27, 13, 29, 15, GLOW_CYAN)
    put_ent(px, 64, 32, 28, 14, GLOW_CYAN_HI)

    # Ventilation grilles on West/East sides:
    for x in (2, 4, 6):
        rect(px, x, 13, x, 15, DARK_METAL_DK)
    for x in (18, 20, 22):
        rect(px, x, 13, x, 15, DARK_METAL_DK)

    # 2. BIT BASE (0, 16) size 6x4x6
    # Top face: (6, 16) to (11, 21)
    rect(px, 6, 16, 11, 21, SILVER_METAL)
    # Bottom face: (12, 16) to (17, 21)
    rect(px, 12, 16, 17, 21, SILVER_METAL_DK)

    # Side faces: size 6x4 each
    for side in range(4):
        x0 = side * 6
        rect(px, x0, 22, x0 + 5, 25, SILVER_METAL)
        for x in range(x0, x0 + 6):
            put_ent(px, 64, 32, x, 22, SILVER_METAL_HI)
        for y in range(22, 26):
            put_ent(px, 64, 32, x0, y, SILVER_METAL_HI)
            put_ent(px, 64, 32, x0 + 5, y, SILVER_METAL_DK)

        # Helical spiral threads
        for y in range(22, 26):
            for x in range(x0, x0 + 6):
                lx = x - x0
                if (lx * 2 + y) % 6 < 3:
                    put_ent(px, 64, 32, x, y, SILVER_METAL_HI)
                else:
                    put_ent(px, 64, 32, x, y, SILVER_METAL_DK)

    # 3. BIT TIP (20, 16) size 3x4x3
    # Top face: (23, 16) to (25, 18)
    rect(px, 23, 16, 25, 18, SILVER_METAL)
    # Bottom face: (26, 16) to (28, 18)
    rect(px, 26, 16, 28, 18, GLOW_ORANGE)

    # Side faces: size 3x4 each
    for side in range(4):
        x0 = 20 + side * 3
        rect(px, x0, 19, x0 + 2, 22, SILVER_METAL)
        # Heat plasma glow at tip:
        for y in range(21, 23):
            for x in range(x0, x0 + 3):
                put_ent(px, 64, 32, x, y, GLOW_ORANGE)
        put_ent(px, 64, 32, x0 + 1, 22, GLOW_ORANGE_HI)

        # Highlights
        put_ent(px, 64, 32, x0, 19, SILVER_METAL_HI)
        put_ent(px, 64, 32, x0 + 1, 19, SILVER_METAL_HI)
        put_ent(px, 64, 32, x0, 20, SILVER_METAL_HI)

    img.save(os.path.join(ENTITY_TEX, "drill.png"))
    print("entity drill.png")

# ------------------------------------------------------------- entities
def download_and_recolor(url, out_name):
    import urllib.request
    out_path = os.path.join(ENTITY_TEX, out_name)
    try:
        print(f"Downloading {url}...")
        req = urllib.request.Request(
            url, 
            headers={'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'}
        )
        with urllib.request.urlopen(req, timeout=10) as response:
            img = Image.open(response).convert("RGBA")
    except Exception as e:
        print(f"Failed to download {url}: {e}. Creating fallback texture.")
        w, h = (64, 64) if "phantom" in url else (64, 32)
        img = Image.new("RGBA", (w, h), (128, 128, 128, 255))
        px = img.load()
        import random
        random.seed(hash(out_name))
        for y in range(h):
            for x in range(w):
                n = random.randint(-15, 15)
                px[x, y] = (clamp(128 + n), clamp(128 + n), clamp(128 + n), 255)
    
    # Advanced visual processing per entity
    px = img.load()
    w, h = img.width, img.height
    
    if "sky_drone" in out_name:
        # Sky Drone: Dark mechanical steel with glowing neon cyan circuit bands
        for y in range(h):
            for x in range(w):
                r, g, b, a = px[x, y]
                if a > 0:
                    brightness = (r + g + b) / 3.0
                    t = brightness / 255.0
                    # Slate metallic blue/dark steel base
                    nr = clamp(18 + t * 45)
                    ng = clamp(26 + t * 75)
                    nb = clamp(40 + t * 110)
                    px[x, y] = (nr, ng, nb, a)

        GLOW = (0, 255, 220, 255)
        GLOW_HI = (180, 255, 255, 255)

        # Visor slit and eyes on Phantom head front
        for (ex, ey) in [(10, 9), (11, 9), (12, 9), (10, 10), (11, 10), (12, 10)]:
            put_ent(px, w, h, ex, ey, GLOW_HI)
        for x in range(3, 13):
            put_ent(px, w, h, x, 4, GLOW)

        # Neon geometric circuit pathways on wings:
        for x in range(11, 40):
            for y in range(24, 39):
                if (x + y) % 8 == 0:
                    r, g, b, a = px[x, y]
                    if a > 100:
                        px[x, y] = GLOW
                elif (x + y) % 8 == 1:
                    r, g, b, a = px[x, y]
                    if a > 100:
                        px[x, y] = GLOW_HI
                        
        for x in range(35, 64):
            for y in range(0, 15):
                if (x - y) % 8 == 0:
                    r, g, b, a = px[x, y]
                    if a > 100:
                        px[x, y] = GLOW
                elif (x - y) % 8 == 1:
                    r, g, b, a = px[x, y]
                    if a > 100:
                        px[x, y] = GLOW_HI

        # Mechanical ribs on underbelly
        for x in range(8, 24):
            for y in range(12, 20):
                if y % 4 == 0:
                    r, g, b, a = px[x, y]
                    if a > 100:
                        px[x, y] = GLOW

    elif "cave_lurker" in out_name:
        # Cave Lurker: Obsidian black plates with toxic lime green bioluminescent details
        for y in range(h):
            for x in range(w):
                r, g, b, a = px[x, y]
                if a > 0:
                    brightness = (r + g + b) / 3.0
                    t = brightness / 255.0
                    # obsidian dark-green base
                    nr = clamp(12 + t * 25)
                    ng = clamp(18 + t * 40)
                    nb = clamp(12 + t * 25)
                    px[x, y] = (nr, ng, nb, a)

        GLOW = (80, 255, 0, 255)
        GLOW_HI = (200, 255, 120, 255)

        # Lime glowing spider eyes:
        for (ex, ey) in [(9, 9), (10, 9), (12, 9), (13, 9), (9, 10), (10, 10), (12, 10), (13, 10)]:
            put_ent(px, w, h, ex, ey, GLOW_HI)

        # Bioluminescent green circular markings on abdomen:
        for (cx, cy) in [(6, 4), (10, 6), (14, 4), (18, 6), (8, 2), (16, 2)]:
            for dx in range(2):
                for dy in range(2):
                    put_ent(px, w, h, cx + dx, cy + dy, GLOW)
            put_ent(px, w, h, cx, cy, GLOW_HI)

        # Glowing segmented rings on leg joints
        for x in range(32, 64):
            for y in range(32):
                if x % 8 == 0 or y % 6 == 0:
                    r, g, b, a = px[x, y]
                    if a > 100:
                        px[x, y] = GLOW

    elif "acid_spitter" in out_name:
        # Acid Spitter: Toxic bone yellow-green with dripping acid slime bands and glowing eye sockets
        for y in range(h):
            for x in range(w):
                r, g, b, a = px[x, y]
                if a > 0:
                    brightness = (r + g + b) / 3.0
                    t = brightness / 255.0
                    # Corroded yellow-green bone base
                    nr = clamp(125 + t * 70)
                    ng = clamp(150 + t * 60)
                    nb = clamp(80 + t * 35)
                    px[x, y] = (nr, ng, nb, a)

        ACID = (90, 245, 20, 255)
        ACID_DK = (30, 130, 10, 255)
        ACID_HI = (200, 255, 120, 255)

        # Glowing green chemical eyes:
        for (ex, ey) in [(2, 2), (3, 2), (5, 2), (6, 2), (2, 3), (3, 3), (5, 3), (6, 3)]:
            put_ent(px, w, h, ex, ey, ACID_HI)

        # Dripping forehead slime:
        for (x, y) in [(1, 0), (2, 0), (3, 0), (4, 1), (5, 1), (2, 2), (6, 2)]:
            put_ent(px, w, h, x, y, ACID)
        for (x, y) in [(1, 1), (2, 1), (3, 2), (4, 2)]:
            put_ent(px, w, h, x, y, ACID_HI)

        # Slime chunks on the ribcage:
        for y in range(18, 28):
            for x in range(18, 30):
                if (x + y) % 5 == 0:
                    r, g, b, a = px[x, y]
                    if a > 100:
                        px[x, y] = ACID
                elif (x + y) % 5 == 1:
                    r, g, b, a = px[x, y]
                    if a > 100:
                        px[x, y] = ACID_HI

        # Acid bands around arm and leg bones:
        for x in range(40, 64):
            for y in range(32):
                if x % 6 == 0:
                    r, g, b, a = px[x, y]
                    if a > 100:
                        px[x, y] = ACID

    img.save(out_path)
    print(f"entity {out_name}")

def main():
    draw_borer()
    draw_emp_grenade()
    draw_herbal_salve()
    paint_drill()
    
    # Download and customize the 3 other new entities
    # Sky Drone (phantom-based)
    download_and_recolor(
        "https://raw.githubusercontent.com/InventivetalentDev/minecraft-assets/1.21.1/assets/minecraft/textures/entity/phantom.png",
        "sky_drone.png"
    )
    # Cave Lurker (spider-based)
    download_and_recolor(
        "https://raw.githubusercontent.com/InventivetalentDev/minecraft-assets/1.21.1/assets/minecraft/textures/entity/spider/spider.png",
        "cave_lurker.png"
    )
    # Acid Spitter (skeleton-based)
    download_and_recolor(
        "https://raw.githubusercontent.com/InventivetalentDev/minecraft-assets/1.21.1/assets/minecraft/textures/entity/skeleton/skeleton.png",
        "acid_spitter.png"
    )
    print("Done generating assets!")

if __name__ == "__main__":
    main()
