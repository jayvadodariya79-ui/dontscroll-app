"""Store graphics for SwipeStop: 512x512 icon and 1024x500 feature graphic (Play),
1024x1024 icon (App Store). Drawn at 4x and downscaled for clean edges."""
from PIL import Image, ImageDraw, ImageFont
import os

OUT = os.path.dirname(os.path.abspath(__file__))
os.makedirs(OUT, exist_ok=True)

BG = (19, 14, 34)
BODY = (42, 31, 69)
VIOLET = (167, 139, 250)
VIOLET_LIGHT = (196, 181, 253)
MINT = (94, 234, 212)
WHITE = (255, 255, 255)
TEXT_LO = (169, 161, 196)
FONT_DIR = "/usr/share/fonts/opentype/inter/"


def font(weight, size):
    return ImageFont.truetype(os.path.join(FONT_DIR, f"Inter-{weight}.otf"), size)


def draw_mark(d, cx, cy, s):
    """The SwipeStop mark: phone, swipe-up chevron, stop bar. s = phone height."""
    pw = s * 0.62
    r = s * 0.13
    d.rounded_rectangle([cx - pw / 2, cy - s / 2, cx + pw / 2, cy + s / 2], radius=r,
                        fill=BODY, outline=VIOLET, width=max(2, int(s * 0.055)))
    ch = s * 0.135
    cy_ch = cy - s * 0.2
    d.line([(cx - ch, cy_ch + ch), (cx, cy_ch), (cx + ch, cy_ch + ch)], fill=WHITE,
           width=max(2, int(s * 0.075)), joint="curve")
    for x, y in [(cx - ch, cy_ch + ch), (cx, cy_ch), (cx + ch, cy_ch + ch)]:
        rr = s * 0.0375
        d.ellipse([x - rr, y - rr, x + rr, y + rr], fill=WHITE)
    bw = s * 0.92
    bh = s * 0.125
    by = cy + s * 0.04
    d.rounded_rectangle([cx - bw / 2, by - bh / 2, cx + bw / 2, by + bh / 2], radius=bh / 2, fill=MINT)


def icon(size, name, rounded):
    k = 4
    S = size * k
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    if rounded:
        d.rounded_rectangle([0, 0, S - 1, S - 1], radius=int(S * 0.22), fill=BG)
    else:
        d.rectangle([0, 0, S, S], fill=BG)
    draw_mark(d, S / 2, S / 2, S * 0.56)
    img = img.resize((size, size), Image.LANCZOS)
    if not rounded:
        img = img.convert("RGB")
    img.save(os.path.join(OUT, name))


def feature_graphic():
    k = 2
    W, H = 1024 * k, 500 * k
    img = Image.new("RGB", (W, H), BG)
    d = ImageDraw.Draw(img)
    # soft accent shapes
    d.ellipse([W * 0.62, -H * 0.35, W * 1.15, H * 0.75], fill=(30, 23, 51))
    d.ellipse([W * 0.70, H * 0.55, W * 1.05, H * 1.3], fill=(26, 20, 45))
    # mark on the right
    draw_mark(d, W * 0.835, H * 0.50, H * 0.58)
    # text on the left
    x = W * 0.07
    d.text((x, H * 0.20), "SwipeStop", font=font("Bold", int(H * 0.16)), fill=WHITE)
    d.text((x, H * 0.42), "Count every reel.", font=font("SemiBold", int(H * 0.085)), fill=VIOLET_LIGHT)
    d.text((x, H * 0.53), "Stop when you choose.", font=font("SemiBold", int(H * 0.085)), fill=VIOLET_LIGHT)
    # chips
    chips = ["Reel counter", "Daily limits", "Free \u00b7 No login"]
    cf = font("Medium", int(H * 0.036))
    cx, cy = x, H * 0.72
    for c in chips:
        tw = d.textlength(c, font=cf)
        pad = H * 0.03
        d.rounded_rectangle([cx, cy, cx + tw + pad * 2, cy + H * 0.085], radius=H * 0.0425,
                            fill=(43, 35, 66))
        d.text((cx + pad, cy + H * 0.018), c, font=cf, fill=TEXT_LO)
        cx += tw + pad * 2 + H * 0.016
    img = img.resize((1024, 500), Image.LANCZOS)
    img.save(os.path.join(OUT, "swipestop-feature-graphic-1024x500.png"))


icon(512, "swipestop-icon-512.png", rounded=False)          # Play: square, Play rounds it
icon(1024, "swipestop-icon-1024.png", rounded=False)        # App Store: square, no transparency
icon(512, "swipestop-logo-rounded-512.png", rounded=True)    # for website / social
feature_graphic()
print(sorted(os.listdir(OUT)))
