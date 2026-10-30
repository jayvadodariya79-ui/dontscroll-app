"""Print the tap point of the first on-screen node matching a label in a uiautomator dump.
Usage: ui_find.py dump.xml LABEL WIDTH HEIGHT   (LABEL may be 're:<regex>')"""
import re, sys
import xml.etree.ElementTree as ET

path, label, W, H = sys.argv[1], sys.argv[2], int(sys.argv[3]), int(sys.argv[4])
try:
    root = ET.parse(path).getroot()
except Exception:
    sys.exit(0)
rx = re.compile(label[3:]) if label.startswith("re:") else None
for n in root.iter("node"):
    vals = [n.get("text") or "", n.get("content-desc") or ""]
    ok = any(rx.fullmatch(v) for v in vals) if rx else label in vals
    if not ok:
        continue
    m = re.findall(r"\d+", n.get("bounds", ""))
    if len(m) != 4:
        continue
    x1, y1, x2, y2 = map(int, m)
    cx, cy = (x1 + x2) // 2, (y1 + y2) // 2
    if 0 < cx < W and 100 < cy < H - 40 and x2 > x1 and y2 > y1:
        print(cx, cy)
        break
