"""Subsets Material Symbols Rounded to the icons Snap-to-Site uses.

Keeps only the FILL axis (weight 400, grade 0, optical size 24) and maps icons by codepoint, so the
font is a few KB instead of 15 MB. Usage:

    python3 tools/subset_symbols.py

then copy any new codepoints it prints into ui/Symbol.kt. Downloads are cached in tools/.cache/.
"""
import os
import urllib.request

from fontTools import subset
from fontTools.ttLib import TTFont
from fontTools.varLib import instancer

ICONS = """
photo_camera mic web arrow_back smartphone edit stop arrow_forward close check_circle storefront
restaurant_menu palette event cameraswitch photo_library image check remove record_voice_over
edit_note cloud open_in_new lock menu history account_circle undo public language keyboard
event_busy soup_kitchen local_cafe location_on schedule restart_alt key logout rate_review
chevron_right error call mail directions info send auto_awesome warning shopping_bag delete
""".split()

BASE = "https://github.com/google/material-design-icons/raw/master/variablefont/"
NAME = "MaterialSymbolsRounded%5BFILL,GRAD,opsz,wght%5D"
HERE = os.path.dirname(os.path.abspath(__file__))
CACHE = os.path.join(HERE, ".cache")
OUT = os.path.join(HERE, "..", "app", "src", "main", "res", "font", "material_symbols_rounded.ttf")


def fetch(suffix):
    os.makedirs(CACHE, exist_ok=True)
    path = os.path.join(CACHE, "symbols" + suffix)
    if not os.path.exists(path):
        urllib.request.urlretrieve(BASE + NAME + suffix, path)
    return path


def main():
    codepoints = dict(line.split() for line in open(fetch(".codepoints")))
    font = instancer.instantiateVariableFont(TTFont(fetch(".ttf")), {"GRAD": 0, "opsz": 24, "wght": 400})
    # Reload eagerly; subsetting a lazily loaded instance fails on glyph variations.
    pinned = os.path.join(CACHE, "pinned.ttf")
    font.save(pinned)
    font = TTFont(pinned, lazy=False)

    options = subset.Options()
    options.layout_features = []
    options.notdef_outline = True
    subsetter = subset.Subsetter(options)
    subsetter.populate(unicodes=[int(codepoints[name], 16) for name in ICONS])
    subsetter.subset(font)
    font.save(OUT)

    for name in ICONS:
        print(f'    "{name}" to \'\\u{codepoints[name].upper()}\',')


if __name__ == "__main__":
    main()
