#!/usr/bin/env python3
import hashlib
import io
import os
import sys
import urllib.request

from PIL import Image, ImageDraw, ImageFont

RES_DIR = "app/src/main/res"
SIZES = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
MAX_BYTES = 8 * 1024 * 1024
PALETTE = ["#ffd23f", "#7ce7f0", "#ff90b3", "#8bf09a", "#ffb86b"]
FONT_CANDIDATES = [
    "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",
    "/usr/share/fonts/truetype/liberation/LiberationSans-Bold.ttf",
]


def load_from_url(url: str) -> Image.Image:
    if not (url.startswith("https://") or url.startswith("http://")):
        sys.exit("invalid icon url")
    request = urllib.request.Request(url, headers={"User-Agent": "webview-app-builder"})
    with urllib.request.urlopen(request, timeout=30) as response:
        data = response.read(MAX_BYTES + 1)
    if len(data) > MAX_BYTES:
        sys.exit("icon too large")
    return Image.open(io.BytesIO(data)).convert("RGBA")


def placeholder(name: str) -> Image.Image:
    digest = int(hashlib.sha256(name.encode("utf-8")).hexdigest(), 16)
    background = PALETTE[digest % len(PALETTE)]
    size = 512
    image = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    draw.rounded_rectangle(
        [0, 0, size - 1, size - 1],
        radius=80,
        fill=background,
        outline="#111111",
        width=18,
    )

    letter = (name.strip()[:1] or "A").upper()
    font = None
    for candidate in FONT_CANDIDATES:
        if os.path.exists(candidate):
            font = ImageFont.truetype(candidate, 300)
            break
    if font is None:
        font = ImageFont.load_default()

    box = draw.textbbox((0, 0), letter, font=font)
    width = box[2] - box[0]
    height = box[3] - box[1]
    draw.text(
        ((size - width) / 2 - box[0], (size - height) / 2 - box[1]),
        letter,
        font=font,
        fill="#111111",
    )
    return image


def square(image: Image.Image) -> Image.Image:
    width, height = image.size
    side = max(width, height)
    canvas = Image.new("RGBA", (side, side), (0, 0, 0, 0))
    canvas.paste(image, ((side - width) // 2, (side - height) // 2), image)
    return canvas


def main() -> None:
    name = os.environ.get("APP_NAME", "").strip() or "App"
    icon_url = os.environ.get("ICON_URL", "").strip()

    source = load_from_url(icon_url) if icon_url else placeholder(name)
    source = square(source)

    for density, pixels in SIZES.items():
        target = os.path.join(RES_DIR, "mipmap-" + density)
        os.makedirs(target, exist_ok=True)
        source.resize((pixels, pixels), Image.LANCZOS).save(
            os.path.join(target, "ic_launcher.png"), "PNG", optimize=True
        )

    print(f"icons generated ({'custom' if icon_url else 'placeholder'})")


if __name__ == "__main__":
    main()
