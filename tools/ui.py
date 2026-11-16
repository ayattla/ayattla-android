#!/usr/bin/env python3
"""Tiny UI driver over adb + uiautomator for the functional tool checks.

  ui.py tap TEXT [n]     tap the n-th (0-based) element whose text/description contains TEXT
  ui.py has TEXT         exit 0 if some element contains TEXT
  ui.py wait TEXT SECS   wait until TEXT appears (exit 1 on timeout)
  ui.py gone TEXT SECS   wait until TEXT disappears
  ui.py texts            print every visible text (for the report)
  ui.py type TEXT        type text into the focused field (unicode via clipboard-free ADB keyboard fallback)
"""
import re, subprocess, sys, time, xml.etree.ElementTree as ET

def sh(*a):
    return subprocess.run(["adb", *a], capture_output=True, text=True, timeout=60)

def nodes():
    for _ in range(3):
        sh("shell", "uiautomator", "dump", "/sdcard/u.xml")
        r = sh("exec-out", "cat", "/sdcard/u.xml")
        if r.stdout.strip().startswith("<?xml"):
            try:
                root = ET.fromstring(r.stdout)
                return list(root.iter("node"))
            except ET.ParseError:
                pass
        time.sleep(1)
    return []

def label(n):
    return (n.get("text") or "") + " " + (n.get("content-desc") or "")

def find(t):
    return [n for n in nodes() if t in label(n)]

def center(n):
    x1, y1, x2, y2 = map(int, re.findall(r"\d+", n.get("bounds")))
    return (x1 + x2) // 2, (y1 + y2) // 2

def swipe_up():
    sh("shell", "input", "swipe", "540", "1700", "540", "700", "300"); time.sleep(1)

cmd = sys.argv[1]
if cmd == "tap":
    # Scrolls down (up to 8 times) to reach an element below the screen edge.
    t = sys.argv[2]; i = int(sys.argv[3]) if len(sys.argv) > 3 else 0
    for _ in range(9):
        ns = find(t)
        if len(ns) > i: break
        swipe_up()
    else:
        print(f"  ! not found: {t}"); sys.exit(1)
    x, y = center(ns[i]); sh("shell", "input", "tap", str(x), str(y)); time.sleep(1.2)
elif cmd == "field":
    # Taps the n-th text field (class EditText), so typing goes into it.
    i = int(sys.argv[2]) if len(sys.argv) > 2 else 0
    ns = [n for n in nodes() if n.get("class", "").endswith("EditText")]
    if len(ns) <= i:
        print("  ! no text field"); sys.exit(1)
    x, y = center(ns[i]); sh("shell", "input", "tap", str(x), str(y)); time.sleep(1)
elif cmd == "has":
    sys.exit(0 if find(sys.argv[2]) else 1)
elif cmd in ("wait", "gone"):
    t = sys.argv[2]; end = time.time() + float(sys.argv[3]); k = 0
    while time.time() < end:
        ok = bool(find(t))
        if not ok and cmd == "wait" and k % 3 == 2: swipe_up()   # the result may be below the fold
        k += 1
        if ok == (cmd == "wait"): sys.exit(0)
        time.sleep(1.5)
    sys.exit(1)
elif cmd == "texts":
    seen = []
    for n in nodes():
        s = label(n).strip()
        if s and s not in seen: seen.append(s)
    print(" | ".join(x[:60] for x in seen)[:1500])
elif cmd == "type":
    # Arabic cannot go through "input text"; the app's search fields also accept Latin digits.
    sh("shell", "input", "text", sys.argv[2].replace(" ", "%s"))
