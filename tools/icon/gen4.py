import sys, cairosvg; sys.path.insert(0, '.')
from textpath import text_path
S, C = 1024, 512
AM = 'f/amiri-arabic-700.ttf'
inner = open('logo_inner.txt').read().replace('#EEF5EF', 'LOGOFILL')
BX0, BY0, BX1, BY1 = 137, 260, 1126, 934          # ink bounds of rahl + calligraphy in the logo's 1254 box
def build(frame_w=780, logo_w_ratio=0.50, logo_dy=0.02, logo_fill='#FFFFFF', frame_fill='#FFFFFF', bg=True):
    frame, (fw, fh) = text_path('۝', AM, frame_w / 845.6 * 700, C, C)
    lw = fw * logo_w_ratio; k = lw / (BX1 - BX0); lh = (BY1 - BY0) * k
    tx = C - lw / 2 - BX0 * k; ty = C - lh / 2 - BY0 * k + fh * logo_dy
    back = ('<defs><radialGradient id="g" cx="30%" cy="18%" r="105%"><stop offset="0" stop-color="#43AC5C"/>'
            '<stop offset=".6" stop-color="#2E9145"/><stop offset="1" stop-color="#145024"/></radialGradient></defs>'
            f'<rect width="{S}" height="{S}" fill="url(#g)"/>') if bg else ''
    return (f'<svg xmlns="http://www.w3.org/2000/svg" width="{S}" height="{S}" viewBox="0 0 {S} {S}">{back}'
            f'<path d="{frame}" fill="{frame_fill}"/>'
            f'<g transform="translate({tx:.2f} {ty:.2f}) scale({k:.5f})">{inner.replace("LOGOFILL", logo_fill)}</g></svg>')
if __name__ == '__main__':
    for n, kw in [('L1', dict(frame_w=780, logo_w_ratio=0.50)), ('L2', dict(frame_w=840, logo_w_ratio=0.56)), ('L3', dict(frame_w=880, logo_w_ratio=0.60, logo_dy=0.015))]:
        s = build(**kw); open(f'{n}.svg','w').write(s); cairosvg.svg2png(bytestring=s.encode(), write_to=f'{n}.png')
