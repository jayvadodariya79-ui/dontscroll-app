"""Frame raw emulator screenshots for the Play listing: 1080x1920, caption on top,
phone screen below bleeding off the bottom edge."""
import os, sys
from PIL import Image, ImageDraw, ImageFont, ImageFilter

SRC = sys.argv[1]
OUT = os.path.join(os.path.dirname(__file__), "shots")
os.makedirs(OUT, exist_ok=True)
BG = (19, 14, 34); VIOLET = (167, 139, 250); MINT = (94, 234, 212)
WHITE = (255, 255, 255); TEXT_LO = (186, 178, 214)
F = "/usr/share/fonts/opentype/inter/"
def font(w, s): return ImageFont.truetype(F + f"Inter-{w}.otf", s)

SHOTS = [
    ("01-today", "Every swipe, counted", "Reels and Shorts on Instagram, YouTube,\nSnapchat and Facebook"),
    ("03-insights", "See when you scroll", "Hour-by-hour chart and time per app"),
    ("05-goals", "Build a streak", "Daily goals, challenges and levels"),
    ("07-blocking", "Stop at your limit", "Daily limits, warnings and blocking"),
    ("04-insights-more", "Know where time goes", "Reels and minutes per app, every day"),
    ("06-settings", "Private by design", "No account. No internet.\nYour data stays on your phone."),
]

W, H = 1080, 1920
def frame(name, title, sub, idx):
    p = os.path.join(SRC, name + ".png")
    if not os.path.exists(p):
        print("missing", name); return
    shot = Image.open(p).convert("RGB")
    img = Image.new("RGB", (W, H), BG)
    # soft glow behind the phone
    glow = Image.new("RGB", (W, H), BG); g = ImageDraw.Draw(glow)
    g.ellipse([W * 0.1, 520, W * 0.9, 1500], fill=(70, 45, 130))
    img = Image.blend(img, glow.filter(ImageFilter.GaussianBlur(160)), 0.85)
    d = ImageDraw.Draw(img)
    # caption
    y = 120
    tf = font("ExtraBold", 84)
    d.text((W / 2, y), title, font=tf, fill=WHITE, anchor="ma")
    y += 112
    d.multiline_text((W / 2, y), sub, font=font("Medium", 40), fill=TEXT_LO, anchor="ma", align="center", spacing=12)
    # accent bar
    d.rounded_rectangle([W / 2 - 46, 84, W / 2 + 46, 96], radius=6, fill=MINT)
    # phone
    pw = 820
    sh = int(shot.height * pw / shot.width)
    shot = shot.resize((pw, sh), Image.LANCZOS)
    top = 470
    bez = 22
    x0 = (W - pw) // 2
    d.rounded_rectangle([x0 - bez, top - bez, x0 + pw + bez, top + sh + bez], radius=96, fill=(10, 8, 18), outline=(78, 64, 120), width=4)
    mask = Image.new("L", (pw, sh), 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, pw - 1, sh - 1], radius=76, fill=255)
    img.paste(shot, (x0, top), mask)
    out = os.path.join(OUT, f"swipestop-phone-{idx}.png")
    img.save(out); print("wrote", out)

for i, (n, t, s) in enumerate(SHOTS, 1):
    frame(n, t, s, i)
