# 홈 스토어 이미지 시안

2026-10-23. 담하루 실제 iOS 홈 화면에 캡처 전용 샘플 기록 16건을 채웠다.

- `01-home-store.png`: 소개 문구와 실제 캡처를 합성한 1206×2622 RGB PNG.
- `home-screenshot.png`: iPhone 17 Pro / iOS 27.0 실제 앱 캡처(1206×2622).
- `assets/contact-sheet.png`: built-in image_gen으로 만든 4×4 일상 사진 원본. 앱의 필름 효과 비교용 사진이 아니다.
- `sample-data/diary/`: 날짜별 JPEG, 180px 썸네일, 앱이 읽는 version 1 `records.json`. 메모도 포함.
- `prepare.py`: Pillow로 컨택트 시트를 분할하고, 캡처가 있으면 스토어 이미지를 다시 출력한다. `python3 design/app-store/home/prepare.py`.

커피·은행잎·책·바다·꽃·비·골목·빵·물결·그림자·침구·산책길·노을·화분·카페·달. 10월 1–22일에 16건을 배치하고 빈 날과 오늘(23일)을 남겼다. 실제 사용자 데이터나 앱 소스는 변경하지 않았다. 생성 사진은 작은 달력 시안용이며 원본 타일은 약 314px이다.

캡처 전용 시뮬레이터: `Damharu Store Preview` (`C882447A-8047-4F7D-B5E4-1A6DE2FD02B4`). 기존 `build/xcode27-check/Build/Products/Debug-iphonesimulator/Damharu.app`을 설치하고 새 앱 데이터 컨테이너의 Documents에 sample-data/diary를 복사했다. `--date 2026-10-23` Debug 인자로 날짜를 고정했다. 상태 표시줄은 simctl로 9:41·신호 최대·배터리 100%로 지정했다. 하단 카운트다운은 앱의 실제 동작이다.

검증: 실제 앱에서 썸네일 16장 로드, 날짜·빈 칸·오늘 표시와 하단 렌즈 가시성 확인. 최종 PNG의 크기·RGB·알파 없음과 기록별 파일 참조 확인. 앱 재빌드·자동 테스트·실기기·App Store Connect 업로드 검증은 미실행. 앱의 한글 명조 번들 전 상태를 그대로 캡처했다.

## 이미지 생성 프롬프트

Built-in image_gen 사용, 투명 배경 없음.

> Use case: photorealistic-natural. Create ONE photographic contact sheet asset for a Korean daily photo diary called Damharu. Exact 4 columns by 4 rows, 16 equally sized square photographs edge to edge, no borders, no gutters, no text, no numbers, no UI, no watermark. Each tile must be independently cropable. Candid ordinary moments on a compact film camera, subtle fine grain, soft autumn light, muted warm colors, deep charcoal shadows and occasional ocean blue. Realistic humble personal snapshots, not commercial product photography, no identifiable people. Row 1: ivory ceramic coffee cup on dark wood by window; sunlit gingko leaves against blue sky; open paperback with soft window shadow no legible writing; calm sea at amber sunset. Row 2: small vase with one stem on cream wall; rainy window looking at blurred warm street lights; narrow quiet Seoul alley warm afternoon; brown croissant on ceramic plate. Row 3: ripples on blue-gray river; black and white tree shadow on wall; rumpled cream linen and morning light; autumn park footpath with fallen leaves. Row 4: orange clouds above low rooftops; small green plant on windowsill; black and white empty cafe chair; silver moon in twilight blue sky. Coherent quiet nostalgic life, varying close and wide compositions, clear subjects at tiny thumbnail size. Square overall image.

다음: 홈 시안의 사진 배치·카피 검토 후 필요하면 수정. 나머지 화면의 스토어 이미지는 별도 요청 시 진행.
