"""Compose actual iOS screenshots into the same layout as the first store image."""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parent
SERIF = '/System/Library/Fonts/Supplemental/AppleMyungjo.ttf'
SANS = '/System/Library/Fonts/AppleSDGothicNeo.ttc'
SCREENS = [
    ('05-film-picker', '찍기 전,', '오늘의 색을 골라요.', '빈티지와 흑백, 원하는 분위기로'),
    ('06-film-shelf', '나의 취향을 담은', '작은 필름 진열장.', '필름마다 다른 질감을 미리 만나보세요'),
]
for name, first, second, subtitle in SCREENS:
    source = ROOT / 'captures' / f'{name}.png'
    screenshot = Image.open(source).convert('RGB')
    assert screenshot.size == (1206, 2622), (source, screenshot.size)
    screenshot.save(source)
    canvas = Image.new('RGB', (1206, 2622), '#12110f')
    draw = ImageDraw.Draw(canvas)
    def text(value, y, font, color):
        draw.text((603, y), value, font=font, fill=color, anchor='mt')
    text('담  하  루', 94, ImageFont.truetype(SANS, 28), '#d98551')
    for value, y in [(first, 166), (second, 265)]:
        font = ImageFont.truetype(SERIF, 76)
        assert draw.textlength(value, font=font) <= 1040, value
        text(value, y, font, '#ede8dd')
    text(subtitle, 392, ImageFont.truetype(SANS, 30), '#a49b8e')
    width = 912
    height = round(screenshot.height * width / screenshot.width)
    screenshot = screenshot.resize((width, height), Image.Resampling.LANCZOS)
    x, y = (1206-width)//2, 545
    draw.rounded_rectangle((x-3, y-3, x+width+3, y+height+3), radius=64, fill='#4b453b')
    mask = Image.new('L', screenshot.size)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, width, height), radius=61, fill=255)
    canvas.paste(screenshot, (x, y), mask)
    canvas.save(ROOT / f'{name}-store.png')

paths = [ROOT / f'{s[0]}-store.png' for s in SCREENS]
board = Image.new('RGB', (768, 831), '#24211d')
for index, path in enumerate(paths):
    image = Image.open(path)
    assert image.size == (1206, 2622) and image.mode == 'RGB'
    board.paste(image.resize((360, 783), Image.Resampling.LANCZOS), (16 + index * 376, 24))
board.save(ROOT / 'preview.png')
print('Saved 2 RGB PNGs (1206x2622) and preview.')
