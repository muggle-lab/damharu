# 스플래시 시안

2026-09-21. 확정 아이콘의 노을 바다·촬영 실루엣을 참고해 built-in image_gen으로 생성한 검토용 이미지. 앱 코드에는 미적용.

- 01-cinematic.png: 사진을 가득 채운 몰입형.
- 02-photo-print.png: 검은 여백 속 한 장의 인화사진.
- 03-horizon.png: 노을 수평선과 어두운 여백.

## 생성 프롬프트

### 1

Use case: ui-mockup. Create one polished flat iPhone portrait splash screen concept for Korean photo diary app 담하루, aspect ratio 9:19.5. Edge-to-edge screen image, no phone hardware, no presentation board, no labels, no status bar, no buttons, no loading indicator, no watermark. Input image is the approved app icon, a visual reference for sunset sea, photographer silhouette, muted burnt orange, slate blue and analog film mood. Typography: elegant Korean serif, exact title "담하루", warm ivory, generous spacing. Only other text allowed is "사진 한 장이면, 오늘의 기록은 끝." in small readable Korean beneath title. Concept 1, cinematic immersion: extend the reference photo naturally into tall portrait full-bleed photography, photographer with camera in lower right, orange sun on left over sea at mid-height, ample dusky cloudy sky above. Place title centered at 24 percent height and subtitle below. Subtle darkening behind letters, cinematic subdued exposure, natural fine grain. Preserve reference subject and photographic identity.

### 2

Use case: ui-mockup. Create one polished flat iPhone portrait splash screen concept for Korean photo diary app 담하루, aspect ratio 9:19.5. Edge-to-edge screen image, no phone hardware, no presentation board, no labels, no status bar, no buttons, no loading indicator, no watermark. Input image is the approved app icon, a visual reference for sunset sea, photographer silhouette, muted burnt orange, slate blue and analog film mood. Typography: elegant Korean serif, exact title "담하루", warm ivory, generous spacing. Only other text allowed is "사진 한 장이면, 오늘의 기록은 끝." in small readable Korean beneath title. Concept 2, one treasured print: nearly black #070706 background, abundant negative space. Center a small square photographic print at x50%, y43%, width 48% of screen, showing the approved reference photo clearly with the photographer and sunset together. Thin warm ivory photographic paper border, squared corners, no rotation or ornament. Below print at y62% place title and then subtitle. Restrained editorial design, current dark photo diary theme. Photo must be faithful to input.

### 3

Use case: ui-mockup. Create one polished flat iPhone portrait splash screen concept for Korean photo diary app 담하루, aspect ratio 9:19.5. Edge-to-edge screen image, no phone hardware, no presentation board, no labels, no status bar, no buttons, no loading indicator, no watermark. Input image is the approved app icon, a visual reference for sunset sea, photographer silhouette, muted burnt orange, slate blue and analog film mood. Typography: elegant Korean serif, exact title "담하루", warm ivory, generous spacing. Only other text allowed is "사진 한 장이면, 오늘의 기록은 끝." in small readable Korean beneath title. Concept 3, quiet horizon: abstract the approved photo into an understated photographic sunset seascape with no person. Nearly black background top 40% blending softly into a very dim burnt orange sunset band across middle-lower screen, a tiny orange sun at x50% y55%, dark slate ocean below disappearing to black bottom. Center title at y39%, subtitle below. Huge quiet dark negative space, very subtle analog grain, sophisticated minimal brand splash, no line art, no extra symbols. Sunset colors from reference.

## 1안 확정 및 문구 제거

`01-cinematic-no-text.png`를 최종 배경으로 사용한다. Built-in image_gen 편집 프롬프트: "Remove ALL text: the large Korean title and the smaller subtitle. Seamlessly reconstruct the cloudy sunset sky behind both text regions. Keep the exact original portrait dimensions, framing, person, camera, sun, ocean, colors, exposure, film grain and every other detail unchanged. No text, no symbols, no logos."

공통 앱 리소스 `shared/src/commonMain/composeResources/drawable/splash_photo.png`와 iOS LaunchPhoto 이미지 세트에 복사했다. 원본 시안은 보존한다.
