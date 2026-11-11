"""Export Google Play assets from Android captures and approved brand artwork."""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
from zipfile import ZipFile, ZIP_DEFLATED

ROOT = Path(__file__).resolve().parent
SERIF = '/System/Library/Fonts/Supplemental/AppleMyungjo.ttf'
SANS = '/System/Library/Fonts/AppleSDGothicNeo.ttc'
SCREENS = [
    ('01-home', '사진 한 장이면,', '오늘의 기록은 끝.', '하루 한 장, 나만의 사진 일기'),
    ('02-camera', '오늘의 순간을,', '한 장에 담아요.', '앱 안에서 찍고, 바로 기록해요'),
    ('03-film', '오늘의 분위기에', '맞는 필름.', '찍기 전에 고르는 빈티지와 흑백'),
    ('04-memo', '기억하고 싶은 말은', '한 줄로.', '사진 아래 짧게 남기는 오늘의 마음'),
]
for name, first, second, subtitle in SCREENS:
    screenshot = Image.open(ROOT / 'captures' / f'{name}.png').convert('RGB')
    canvas = Image.new('RGB', (1080, 1920), '#12110f')
    draw = ImageDraw.Draw(canvas)
    def text(value, y, font, color):
        draw.text((540, y), value, font=font, fill=color, anchor='mt')
    text('담  하  루', 40, ImageFont.truetype(SANS, 23), '#d98551')
    for value, y in [(first, 90), (second, 170)]:
        font = ImageFont.truetype(SERIF, 64)
        assert draw.textlength(value, font=font) <= 980, value
        text(value, y, font, '#ede8dd')
    text(subtitle, 263, ImageFont.truetype(SANS, 25), '#a49b8e')
    width = 700
    height = round(screenshot.height * width / screenshot.width)
    screenshot = screenshot.resize((width, height), Image.Resampling.LANCZOS)
    x, y = (1080-width)//2, 330
    assert y+height+3 < 1920
    draw.rounded_rectangle((x-3, y-3, x+width+3, y+height+3), radius=48, fill='#4b453b')
    mask = Image.new('L', screenshot.size)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, width, height), radius=45, fill=255)
    canvas.paste(screenshot, (x, y), mask)
    canvas.save(ROOT / f'{name}.png')

for name in ['00-splash.png', 'feature-graphic-1024x500.png']:
    image = Image.open(ROOT / name).convert('RGB')
    image.save(ROOT / name)

icon = Image.open(ROOT.parent / 'app-icons/damharu-app-icon-1024.png').convert('RGBA')
icon.resize((512, 512), Image.Resampling.LANCZOS).save(ROOT / 'app-icon-512.png')
assert (ROOT / 'app-icon-512.png').stat().st_size < 1024*1024
names = ['00-splash.png'] + [f'{s[0]}.png' for s in SCREENS]
thumb_width, gap = 288, 20
thumb_height = 512
board = Image.new('RGB', (thumb_width*5+gap*6, thumb_height+gap*2), '#24211d')
for index, name in enumerate(names):
    image = Image.open(ROOT / name)
    assert image.mode == 'RGB' and image.size == (1080, 1920)
    board.paste(image.resize((thumb_width, thumb_height), Image.Resampling.LANCZOS), (gap+index*(thumb_width+gap), gap))
board.save(ROOT / 'all-five-preview.png')
with ZipFile(ROOT / 'damharu-google-play-images.zip', 'w', ZIP_DEFLATED) as archive:
    for name in names:
        archive.write(ROOT / name, 'phone-screenshots/' + name)
    for name in ['feature-graphic-1024x500.png', 'app-icon-512.png']:
        archive.write(ROOT / name, name)
    for name in ['README.md', 'alt-text.txt']:
        archive.write(ROOT / name, name)
print('Exported 5 phone screenshots, feature graphic, app icon, overview, ZIP.')
