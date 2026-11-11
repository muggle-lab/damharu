"""Compose actual iOS screenshots into the same layout as the first store image."""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parent
SERIF = '/System/Library/Fonts/Supplemental/AppleMyungjo.ttf'
SANS = '/System/Library/Fonts/AppleSDGothicNeo.ttc'
SCREENS = [
    ('02-camera', '오늘의 순간을,', '한 장에 담아요.', '앱 안에서 찍고, 바로 기록해요'),
    ('03-film', '오늘의 분위기에', '맞는 필름.', '찍기 전에 고르는 빈티지와 흑백'),
    ('04-memo', '기억하고 싶은 말은', '한 줄로.', '사진 아래 짧게 남기는 오늘의 마음'),
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

paths = [ROOT.parent / 'home/01-home-store.png'] + [ROOT / f'{s[0]}-store.png' for s in SCREENS]
thumb_width = 360
thumb_height = round(2622 * thumb_width / 1206)
board = Image.new('RGB', (thumb_width*4+24*5, thumb_height+48), '#24211d')
for index, path in enumerate(paths):
    image = Image.open(path).convert('RGB')
    assert image.size == (1206, 2622)
    board.paste(image.resize((thumb_width, thumb_height), Image.Resampling.LANCZOS), (24+index*(thumb_width+24), 24))
board.save(ROOT / 'all-four-preview.png')
print('Saved 3 RGB store PNGs (1206x2622) and the four-image overview.')
