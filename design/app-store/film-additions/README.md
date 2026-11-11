# 앱스토어 추가 이미지 — 필름 선택과 진열장

2026-11-06. 기존 0–4번과 동일한 1206×2622 RGB PNG, 어두운 배경·명조 제목·912px 화면 프레임.

- `05-film-picker-store.png`: 찍기 전, / 오늘의 색을 골라요. — 촬영 화면에서 빈티지 선택 및 필름통 목록 표시.
- `06-film-shelf-store.png`: 나의 취향을 담은 / 작은 필름 진열장. — 빈티지·흑백·준비중 카드와 기록/필름/설정 탭.
- `captures/`: 실제 iOS 캡처 원본.
- `preview.png`: 2장 비교용 미리보기. 업로드에는 개별 PNG 사용.
- `compose.py`: 기존 스토어 레이아웃으로 출력. `python3 design/app-store/film-additions/compose.py`.

기존 스토어 전용 시뮬레이터 C882447A-8047-4F7D-B5E4-1A6DE2FD02B4 사용. 기존 커피 샘플 JPEG를 Debug 카메라 프레임으로 공급했으며 UI와 빈티지 효과는 최신 앱 코드를 사용했다. 실제 카메라 센서 촬영 이미지는 아니다.

`build_capture.py`가 임시 사진 공급 코드를 빌드한 뒤 앱 및 원래 UI 테스트 소스를 복원한다. `build/store-film-additions`의 캡처 전용 빌드는 배포용이 아니다. `StoreFilmAdditionTests`를 test-without-building으로 실행하고 xcresulttool로 첨부 PNG를 추출한다. 결과 번들: `build/store-film-additions.xcresult`.

스토어 업로드는 수행하지 않는다. 검증 결과는 docs/TASKS.md 참고.
