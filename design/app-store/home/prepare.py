"""Extract generated contact-sheet tiles and compose the store image from a real capture.

Run with Python 3 + Pillow from any directory. No app source or personal data is changed.
"""
import json
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parent
sheet = Image.open(ROOT / 'assets/contact-sheet.png').convert('RGB')
days = [1, 2, 3, 4, 6, 7, 8, 10, 11, 12, 14, 15, 17, 19, 21, 22]
memos = ['창가에서 천천히 마신 커피.', '고개를 드니 가을이었다.', '조금 읽고 오래 쉬었다.',
         '해가 질 때까지 바다를 봤다.', '작은 꽃 한 송이.', '비 오는 저녁의 불빛.',
         '익숙한 골목을 느리게 걸었다.', '오늘의 작은 즐거움.', '물결이 반짝이던 오후.',
         '벽에 잠시 머문 그림자.', '늦잠을 잔 아침.', '바스락거리는 산책길.',
         '집에 가는 길에 만난 하늘.', '창가의 초록.', '조용한 자리가 좋았다.', '오늘은 달이 선명했다.']
diary = ROOT / 'sample-data/diary'
diary.mkdir(parents=True, exist_ok=True)
records = []
for index, day in enumerate(days):
    x, y = index % 4, index // 4
    tile = sheet.crop((round(x * sheet.width / 4), round(y * sheet.height / 4),
                       round((x + 1) * sheet.width / 4), round((y + 1) * sheet.height / 4)))
    date = f'2026-10-{day:02d}'
    photo, thumb = f'{date}-store-photo.jpg', f'{date}-store-thumb.jpg'
    tile.save(diary / photo, quality=95)
    tile.thumbnail((180, 180), Image.Resampling.LANCZOS)
    tile.save(diary / thumb, quality=93)
    records.append(dict(date=date, image=photo, thumbnail=thumb, memo=memos[index], film=''))
(diary / 'records.json').write_text(json.dumps(dict(version=1, records=records), ensure_ascii=False, indent=2) + '\n')

capture = ROOT / 'home-screenshot.png'
if capture.exists():
    canvas = Image.new('RGB', (1206, 2622), '#12110f')
    draw = ImageDraw.Draw(canvas)
    serif = '/System/Library/Fonts/Supplemental/AppleMyungjo.ttf'
    sans = '/System/Library/Fonts/AppleSDGothicNeo.ttc'
    def text(value, y, font, color):
        draw.text((603, y), value, font=font, fill=color, anchor='mt')
    text('담  하  루', 94, ImageFont.truetype(sans, 28), '#d98551')
    text('사진 한 장이면,', 166, ImageFont.truetype(serif, 76), '#ede8dd')
    text('오늘의 기록은 끝.', 265, ImageFont.truetype(serif, 76), '#ede8dd')
    text('하루 한 장, 나만의 사진 일기', 392, ImageFont.truetype(sans, 30), '#a49b8e')
    screen = Image.open(capture).convert('RGB')
    width = 912
    height = round(screen.height * width / screen.width)
    screen = screen.resize((width, height), Image.Resampling.LANCZOS)
    x, y = (1206-width)//2, 545
    draw.rounded_rectangle((x-3, y-3, x+width+3, y+height+3), radius=64, fill='#4b453b')
    mask = Image.new('L', screen.size)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, width, height), radius=61, fill=255)
    canvas.paste(screen, (x, y), mask)
    canvas.save(ROOT / '01-home-store.png')
