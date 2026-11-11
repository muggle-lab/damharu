# 스토어 이미지 2·3·4

2026-10-23. 1번 홈 이미지와 같은 1206×2622 RGB PNG, 검정·아이보리·번트 오렌지, 명조 카피, 912px 화면 프레임으로 구성했다.

| 파일 | 소개 문구 | 실제 캡처 |
|---|---|---|
| `02-camera-store.png` | 오늘의 순간을, / 한 장에 담아요. | 필름 없는 카메라와 줌 눈금 |
| `03-film-store.png` | 오늘의 분위기에 / 맞는 필름. | 빈티지를 선택한 실제 프리뷰, 카메라 아이콘 5칸 |
| `04-memo-store.png` | 기억하고 싶은 말은 / 한 줄로. | 10월 1일 사진과 ‘창가에서 천천히 마신 커피.’ 메모 |

- `captures/`: 실제 앱의 원본 캡처, RGB로 변환한 PNG. 화면 자체는 재구성하지 않았다.
- `all-four-preview.png`: 1–4번 나란히 보기. 스토어 업로드용은 개별 원본 PNG를 사용한다.
- `assets/coffee-window.png`: built-in image_gen으로 생성한 고해상도 3:4 샘플 사진(1086×1448).
- `assets/prompt.txt`: 최종 생성 프롬프트. `store-preview.jpg`는 시뮬레이터 입력용 960×1280 JPEG.
- `compose.py`: `python3 design/app-store/screens/compose.py`로 캡처에서 개별 이미지와 미리보기를 다시 출력한다.

## 캡처 방법과 검증

전용 `Damharu Store Preview` 시뮬레이터(iPhone 17 Pro / iOS 27.0)를 재사용했다. `Documents/store-preview.jpg`에 샘플 사진을 넣고, 기존 10월 1일 샘플 기록의 사진도 같은 고해상도 사진으로 대체했다. 다른 기기·개인 기록은 변경하지 않았다.

`build_capture.py`는 기존 Debug 카메라 테스트 프레임 대신 샘플 사진을 공급하는 임시 Swift 함수를 빌드한다. UI·필름 처리·메모 화면은 실제 앱 코드를 사용한다. `--camera-preview --store-photo`로만 동작하며, **빌드 종료 후 앱 소스와 기존 UI 테스트 파일을 작업 전 내용으로 복원**한다. 캡처용 빌드·소스는 스토어 제출용 빌드가 아니다. 코드 변경을 남기지 않고 촬영 센서 없이 화면만 재현하기 위한 도구다.

재현 순서: 샘플 데이터가 있는 전용 시뮬레이터에 JPEG를 준비 → 저장소 루트에서 `python3 design/app-store/screens/build_capture.py` → 같은 derivedDataPath `build/store-capture`로 `StoreScreenshotTests`만 `test-without-building` → xcresulttool로 첨부 이미지 3장 내보내기 → captures의 이름에 맞춰 복사 → compose.py 실행. 시뮬레이터가 교체되면 스크립트의 UUID를 갱신한다.

- Xcode `build-for-testing` 성공. UI 캡처 시나리오 1개 통과(실제 카메라 진입, 빈티지 선택·선택 라벨, 상세·날짜·메모 표시).
- 테스트 결과: `build/store-screens.xcresult`, 빌드·실행 로그: `build/store-capture-*.log`.
- 3장 모두 실제 화면·이미지 방향·아이콘·필름 질감·한글 메모·잘림 여부 시각 확인. 1–4번 전체 배열 확인. 출력 1206×2622·RGB·알파 없음 확인.
- 카메라 센서 입력·실기기·실제 필름 저장 결과·App Store Connect 업로드 검증은 미실행. 이번 3번은 샘플 입력에 앱의 실제 빈티지 프리뷰 처리를 적용한 화면이다.

다음: 4장 전체의 카피·사진 검토 및 최종 출시 빌드가 정해지면 UI 일치 여부 확인.
