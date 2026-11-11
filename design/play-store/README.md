# Google Play 등록 이미지

2026-10-23. iOS용 시안과 같은 사진·문구·팔레트를 사용하고, 화면은 Android에서 다시 캡처했다.

| 파일 | 크기 | 용도 |
|---|---|---|
| `00-splash.png` | 1080×1920 | 노을 사진과 ‘오늘 하루의 기록은 “담하루”’ 표지 |
| `01-home.png` | 1080×1920 | 사진 달력 소개 |
| `02-camera.png` | 1080×1920 | 커스텀 카메라 소개 |
| `03-film.png` | 1080×1920 | 빈티지 필름 선택 소개 |
| `04-memo.png` | 1080×1920 | 사진과 한 줄 메모 소개 |
| `feature-graphic-1024x500.png` | 1024×500 | 피처 그래픽, 별도 업로드 |
| `app-icon-512.png` | 512×512 | 기존 확정 아이콘을 내보낸 Play 아이콘 |
| `all-five-preview.png` | 1560×552 | 검토용 모음, 업로드 대상 아님 |
| `damharu-google-play-images.zip` | — | 스크린샷 5장 + 피처 그래픽 + 아이콘 |

Google Play [공식 규격](https://support.google.com/googleplay/android-developer/answer/9866151?hl=en) 확인: 스크린샷은 JPEG 또는 24-bit PNG(알파 없음), 긴 변이 짧은 변의 2배 이내. 앱 추천용 권장 세로 규격 1080×1920(9:16), 최소 4장을 충족하도록 5장 구성. 피처 그래픽은 1024×500. 아이콘은 512×512 32-bit PNG, 1MB 미만. 휴대전화용으로만 준비했으며 태블릿 이미지는 포함하지 않는다.

## 제작 및 재현

- `captures/`: Pixel 10 Pro / Android 17 에뮬레이터에서 캡처한 Android 실제 UI(1280×2856). iOS 화면은 사용하지 않았다.
- `fixtures/`: 캡처 전용 Android 카메라 입력과 instrumentation 시나리오. 센서 대신 이전에 생성한 창가 커피 사진을 공급한다. commonMain의 실제 CameraScreen, 필름 행렬·질감·아이콘, 홈과 상세 UI를 사용한다.
- `build_capture.py`: 별도 앱 ID `com.mugglelab.damharu.storepreview`와 2026-10-23 날짜로 임시 APK를 만든다. 샘플 기록 16건·메모·사진을 포함하며, 앱의 원래 데이터는 변경하지 않는다. 빌드 종료 시 원래 Gradle·MainActivity·AndroidCamera 내용을 복원하고 임시 테스트·에셋을 제거한다. 이 APK는 배포용이 아니다.
- `compose.py`: 실제 캡처에 문구와 프레임을 합성하고 이미지·미리보기·ZIP을 내보낸다. 루트에서 `python3 design/play-store/compose.py`.
- `assets/`: built-in image_gen으로 기존 스플래시 사진을 각각 9:16 세로 표지와 1024:500 가로 피처 그래픽으로 편집한 원본. 최종 프롬프트는 `splash-prompt.txt`, `feature-prompt.txt`. 출력은 sips로 규격을 맞추고 RGB PNG로 저장한다.

캡처 순서: build_capture.py 실행 → build/play-store/store-preview.apk 및 store-preview-test.apk를 에뮬레이터에 설치 → `com.mugglelab.damharu.android.StoreScreenshotTest` instrumentation 실행 → 별도 캡처 앱의 external files 폴더에서 4장 가져오기 → compose.py 실행. 상태 표시줄 데모 모드는 임시 사용 후 원래 설정으로 복원한다.

## 검증

- Android APK와 instrumentation APK 빌드 성공. 홈·카메라 진입·빈티지 선택·메모 표시 캡처 시나리오 통과.
- UI semantics가 표시돼도 전환 직전 프레임이 캡처될 수 있어, 화면 안정화 대기 후 다시 촬영했다.
- 실제 Android 서체·시스템 바·렌즈·필름 아이콘·메모와 5장 전체 배치를 시각 확인. 파일 규격·색상 모드·ZIP 구성을 검사했다.
- 실제 카메라 센서·촬영 저장·실기기·Play Console 업로드는 이번 작업에서 검증하지 않았다. 원래 앱 소스와 개인 기록은 유지했다.

다음: 전체 시안 검토, 출시 빌드의 화면과 최종 일치 확인 후 각 Play Console 항목에 업로드.
