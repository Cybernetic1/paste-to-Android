from PIL import Image, ImageDraw, ImageFont
import os

base = '/home/yky/misc-programs/paste-to-Android/app/src/main/res'
size_map = {
    'mipmap-mdpi': 48,
    'mipmap-hdpi': 72,
    'mipmap-xhdpi': 96,
    'mipmap-xxhdpi': 144,
    'mipmap-xxxhdpi': 192,
}

font_candidates = [
	'/usr/share/fonts/truetype/noto/NotoSansMath-Regular.ttf',
    '/usr/share/fonts/truetype/noto/NotoSans-Regular.ttf',
    '/usr/share/fonts/truetype/dejavu/DejaVuSans-Regular.ttf',
	'/usr/share/fonts/truetype/noto/NotoSerif-BoldItalic.ttf',
	'/usr/share/fonts/truetype/noto/NotoSerifDisplay-Italic.ttf',
	'/usr/share/fonts/truetype/noto/NotoSerif-BlackItalic.ttf',
	'/usr/share/fonts/truetype/dejavu/DejaVuSerif-Italic.ttf',
	'/usr/share/fonts/truetype/dejavu/DejaVuMathTeXGyre.ttf',
    '/usr/share/fonts/truetype/liberation2/LiberationSerif-Bold.ttf',
    '/usr/share/fonts/truetype/dejavu/DejaVuSerif-Bold.ttf',
]

for dir_name, size in size_map.items():
    img = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    pad = int(size * 0.12)
    draw.ellipse((pad, pad, size - pad, size - pad), fill=(150, 30, 30, 255))

    inner = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    inner_draw = ImageDraw.Draw(inner)
    inner_draw.ellipse(
        (pad + 8, pad + 8, size - pad - 8, size - pad - 8),
        outline=(255, 255, 255, 120),
        width=max(2, size // 18),
    )
    img = Image.alpha_composite(img, inner)

    font = None
    for path in font_candidates:
        if os.path.exists(path):
            font = ImageFont.truetype(path, size=int(size * 0.60))
            break
    if font is None:
        font = ImageFont.load_default()

    symbol = '⇋'
    text_draw = ImageDraw.Draw(img)
    bbox = text_draw.textbbox((0, 0), symbol, font=font)
    text_w = bbox[2] - bbox[0]
    text_h = bbox[3] - bbox[1]
    x = (size - text_w) / 2 - bbox[0]
    y = (size - text_h) / 2 - bbox[1] + 0.02*size
    text_draw.text((x, y), symbol, font=font, fill=(255, 255, 255, 255))

    out = os.path.join(base, dir_name, 'ic_launcher.png')
    img.save(out)
    print('wrote', out)
