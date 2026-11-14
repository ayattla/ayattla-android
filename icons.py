# Builds launcher icons and splash from the site's own PWA icons.
import sys, os
from PIL import Image
src_any, src_mask, res = sys.argv[1], sys.argv[2], sys.argv[3]
a = Image.open(src_any).convert("RGBA")
m = Image.open(src_mask).convert("RGBA")
dens = {"mdpi":1, "hdpi":1.5, "xhdpi":2, "xxhdpi":3, "xxxhdpi":4}
for d, k in dens.items():
    o = os.path.join(res, "mipmap-" + d); os.makedirs(o, exist_ok=True)
    a.resize((round(48*k),)*2, Image.LANCZOS).save(os.path.join(o, "ic_launcher.png"))
    full = round(108*k); inner = round(full*0.667/0.8)
    fg = Image.new("RGBA", (full, full), (0,0,0,0))
    mi = m.resize((inner, inner), Image.LANCZOS)
    off = (full-inner)//2; fg.paste(mi, (off, off), mi)
    fg.save(os.path.join(o, "ic_launcher_foreground.png"))
sp = os.path.join(res, "drawable-nodpi"); os.makedirs(sp, exist_ok=True)
a.resize((384,384), Image.LANCZOS).save(os.path.join(sp, "splash.png"))
r,g,b,_ = m.getpixel((2,2)); print("#%02X%02X%02X" % (r,g,b))
