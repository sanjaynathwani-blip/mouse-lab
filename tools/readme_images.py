#!/usr/bin/env python3
# SPDX-License-Identifier: AGPL-3.0-or-later
"""Makes the README's images in docs/images from window captures.

    tools/readme_images.py [SHOTS_DIR]      (default ~/.cache/mouse-lab/shots)

SHOTS_DIR holds adb screen captures of a Mouse Lab window, cropped to the window, on a Googlebook
at 260 dpi. The readings come from the virtual mouse in tools/vmouse.py, never anyone's own use:

  polling.png    a 1600x1200 px window (`am task resize <task> 100 150 1700 1350`), pointer
                 captured, after `tools/vmouse.py circles 4 1` (1000 Hz)
  rotation.png   a 1600x1500 px window (`am task resize <task> 100 150 1700 1650`), after
                 `tools/vmouse.py hand 3.5 32 esc` then `tools/vmouse.py park`

The window's caption bar is cropped off (a debug build says "Mouse Lab Dev" there); each image
gets rounded corners and a soft shadow. icon.png is the logo from brand/mouse-lab-logo.svg
(needs rsvg-convert).
"""
import pathlib
import subprocess
import sys

from PIL import Image, ImageChops, ImageDraw, ImageFilter

ROOT = pathlib.Path(__file__).resolve().parent.parent
OUT = ROOT / "docs" / "images"
SHOTS = pathlib.Path(sys.argv[1] if len(sys.argv) > 1 else pathlib.Path.home() / ".cache/mouse-lab/shots")
CAPTION = 65  # px of window caption above the app


def rounded(im, radius):
    im = im.convert("RGBA")
    mask = Image.new("L", im.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, im.width - 1, im.height - 1), radius, fill=255)
    im.putalpha(ImageChops.multiply(im.getchannel("A"), mask))
    return im


def shadowed(im, blur=18, offset=(0, 10), alpha=110, pad=44):
    canvas = Image.new("RGBA", (im.width + 2 * pad, im.height + 2 * pad), (0, 0, 0, 0))
    shadow = Image.new("RGBA", canvas.size, (0, 0, 0, 0))
    a = im.getchannel("A").point(lambda v: v * alpha // 255)
    shadow.paste(Image.new("RGBA", im.size, (10, 12, 30, 255)), (pad + offset[0], pad + offset[1]), a)
    canvas.alpha_composite(shadow.filter(ImageFilter.GaussianBlur(blur)))
    canvas.alpha_composite(im, (pad, pad))
    return canvas


def app_only(name):
    im = Image.open(SHOTS / name)
    # Two more pixels off the bottom and sides: the window's own rounded corners show the desktop.
    return im.crop((2, CAPTION, im.width - 2, im.height - 2))


def save(im, name):
    im.save(OUT / name, optimize=True)
    print(OUT / name, im.size)


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    save(shadowed(rounded(app_only("polling.png"), 18)), "hero.png")
    save(shadowed(rounded(app_only("rotation.png"), 18)), "rotation.png")
    subprocess.run(["rsvg-convert", "-w", "256", "-h", "256", str(ROOT / "brand/mouse-lab-logo.svg"),
                    "-o", str(OUT / "icon.png")], check=True)
    print(OUT / "icon.png")


if __name__ == "__main__":
    main()
