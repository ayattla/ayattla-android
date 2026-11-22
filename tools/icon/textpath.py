import uharfbuzz as hb
from fontTools.ttLib import TTFont
from fontTools.pens.svgPathPen import SVGPathPen
from fontTools.pens.transformPen import TransformPen
from fontTools.pens.boundsPen import BoundsPen
def text_path(text, fontfile, size, cx, cy):
    """Shaped Arabic text as one SVG path, centred on (cx, cy) by its ink bounds."""
    blob = hb.Blob.from_file_path(fontfile); face = hb.Face(blob); font = hb.Font(face)
    buf = hb.Buffer(); buf.add_str(text); buf.guess_segment_properties(); hb.shape(font, buf, {})
    tt = TTFont(fontfile); gs = tt.getGlyphSet(); order = tt.getGlyphOrder(); upm = tt['head'].unitsPerEm
    s = size / upm
    def draw(pen_factory):
        x = 0
        for info, pos in zip(buf.glyph_infos, buf.glyph_positions):
            g = order[info.codepoint]
            gs[g].draw(pen_factory(x + pos.x_offset, pos.y_offset)); x += pos.x_advance
    bp = BoundsPen(gs); draw(lambda dx, dy: TransformPen(bp, (1, 0, 0, 1, dx, dy)))
    x0, y0, x1, y1 = bp.bounds
    ox = cx - (x0 + x1) / 2 * s; oy = cy + (y0 + y1) / 2 * s
    sp = SVGPathPen(gs); draw(lambda dx, dy: TransformPen(sp, (s, 0, 0, -s, ox + dx * s, oy - dy * s)))
    return sp.getCommands(), ((x1 - x0) * s, (y1 - y0) * s)
