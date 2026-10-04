# 09 — 카메라 필름과 둥근 프리뷰

## 먼저 읽기

AGENTS.md, docs/PRODUCT.md, docs/TASKS.md, 스토리보드 `index.html`의 02 커스텀 카메라(A 기본 / B 필름 목록 열림).

## 결정 (2026-09-21)

- "필터"가 아니라 **필름**: 찍기 전에 끼우고, 촬영 뒤에는 바꾸거나 편집하지 않는다.
- 필름이 적용된 **한 장만 저장**한다. 필름 없는 원본은 남기지 않는다.
- 강도는 **고정**. 슬라이더 없음.
- 종류는 **테스트 필름 1개**. 라인업이 확정되면 다시 작업한다.
- 프리뷰는 참고 캡처처럼 모서리를 둥글린다(20dp).

## 범위

- `data/Film.kt`: `Film(id, label, monoName, matrix)`. `None`과 `Test`. 기록에는 enum이 아니라 `id` 문자열을 저장해 필름을 나중에 빼도 옛 목록이 읽히게 한다(`Film.fromId`는 모르는 id를 `None`으로).
- `PhotoSource.capture` 콜백에 필름 id 추가. 플랫폼은 **필름 없는** 정규화 JPEG와 필름 id를 돌려주고, `DiaryStore.save`가 한 번만 필름을 굽는다. 썸네일은 필름 적용본에서 만들므로 다시 적용하지 않는다.
- `PhotoFiles.resize` → `render(source, destination, matrix?, maxSide)`. iOS Skia / Android Bitmap.
- `CameraScreen`: 프리뷰 20dp 라운드 + 좌우 12dp 여백, 브래킷 inset 20dp, 셔터 오른쪽 `FilmButton`, 줌 눈금과 같은 자리의 `FilmList`(타일은 고정 샘플 색을 필름 행렬로 그린 스와치), 프리뷰 좌하단 필름 칩. `CameraState.film`, `CameraActions.film`.
- iOS: Metal 프리뷰에 `CIColorMatrix`로 같은 행렬 적용, 프리뷰 레이어 `cornerRadius`. Android: TextureView 하드웨어 레이어 색상 필터, `clipToOutline` 라운드.
- 상세 메타 라인에 필름 이름 표시.

## 제외

필름 라인업·이름·색감 확정, 입자/비네팅/날짜 각인 같은 행렬 밖의 필름 효과를 저장 사진에 굽는 것, 필름별 실시간 썸네일, 촬영 후 필름 변경.

## 검증

- 공통: 필름 id 저장, 사진에만 행렬 적용·썸네일 재적용 없음, 모르는 id 폴백, 기존 저장소 테스트 유지.
- 네이티브 픽셀 테스트: 필름 행렬 적용 결과가 기대값과 4/255 이내(iOS Skia, Android Bitmap).
- UI: 필름 버튼 → 목록(줌 눈금 대체) → 테스트 필름 선택 → 버튼 라벨 → 목록 닫기 → 촬영 → 상세 메타에 TEST FILM.

## 2026-09-21 — 부드러운 광각·줌 조절

참고 영상(벤치마크 앱 화면 녹화)의 눈금을 프레임 단위로 확인한 결과를 반영했다.

- 영상의 눈금은 1→1.5→2→3× 간격이 52:37:52로 **로그 간격**이다. 기존 담하루 눈금은 1×당 180dp의 선형이라 0.5~1×는 급하고 3~6×는 둔했다.
- `zoomPosition = log2(zoom)`, 96dp당 2배. 드래그는 `Animatable.snapTo`, 손을 떼면 `animateDecay(exponentialDecay)`로 관성. 값이 바뀔 때마다 `CameraActions.zoom` 호출.
- 눈금: 2×까지 0.1, 3×까지 0.25, 그 위는 0.5 간격(로그 눈금에서 촘촘해지지 않게). 라벨 눈금을 지날 때 햅틱.
- 눈금과 라벨은 Canvas 그리기 단계에서만 애니메이션 값을 읽어 매 프레임 재구성을 피한다(`drawText` + `rememberTextMeasurer`).
- iOS: 프레임마다 오는 줌 요청을 최신 값 하나로 합쳐 카메라 큐에 밀리지 않게 했다.
- 테스트: 같은 거리 = 같은 비율, 왕복 변환, 눈금 범위·간격(공통 2개 추가). iOS UI 테스트는 드래그 후 배율이 2×를 넘는지 확인.
- 핀치 줌은 이번에도 구현하지 않았다. 실제 렌즈 전환 구간의 부드러움은 실기기 확인이 필요하다.

## 2026-09-21 — 촬영한 사진이 달력으로 들어가는 애니메이션

- `App.kt`의 `CaptureFlightOverlay`: 촬영 콜백 직후 임시 촬영본을 뷰파인더 자리(좌우 12dp, 3:4, 20dp 라운드)에 띄우고, 저장이 끝나고 픽셀 디코딩도 끝나면 260ms 머문 뒤 620ms 동안 오늘 칸으로 줄어든다. 칸은 정사각형이라 너비와 높이를 따로 보간하고 이미지는 Crop으로 유지, 모서리는 20dp → 2dp.
- 저장 전의 촬영본은 필름이 적용되지 않았으므로 필름 행렬을 `ColorFilter`로 입혀 저장 결과와 같은 색으로 보여 준다.
- `HomeScreen`은 오늘 칸의 `boundsInRoot()`를 알려 주고(`onTodayBounds`), 비행 중인 날짜(`arrivingDate`)의 썸네일을 투명하게 미리 합성해 둔다. 그래서 착지 순간 칸이 비어 보이는 깜빡임이 없다.
- 저장 중 "잠시만 기다려 주세요" 창은 촬영 저장에서는 띄우지 않는다(사진 자체가 화면에 있음). 저장 실패 시 오버레이를 걷고 안내한다. 임시 촬영 파일은 착지 후 삭제한다.
- 오늘 칸이 아직 배치되지 않았거나 디코딩에 실패하면 비행 없이 달력으로 넘어간다.
- iOS UI 테스트가 셔터 직후 화면을 연속 캡처해 첨부로 남긴다(`docs/screenshots/capture-flight-ios.jpg`). 애니메이션 자체는 단언하지 않는다.

### 수정 — 카메라가 닫히면서 바로 달력으로

첫 구현은 카메라가 (슬라이드로) 닫힌 뒤 어두운 배경 위에 사진이 크게 떴다가 날아가서, 상세 화면이 한 번 나왔다 들어가는 것처럼 보였다.

- `PhotoSource.closeCamera()` 추가. 촬영에 성공하면 카메라는 마지막 프레임에 멈춘 채 화면에 남고, 공통 UI가 그 **아래에서** 사진을 디코딩해 뷰파인더와 같은 위치에 올려 둔 뒤 `closeCamera()`를 부른다. 닫기는 전환 효과 없이 즉시(iOS `.overFullScreen` + `dismiss(animated: false)`, Android는 카메라 컴포저블 제거). 취소/실패는 플랫폼이 스스로 닫는다.
- 시작 위치는 `cameraPreviewRect()`로 계산한다. `CameraScreen`의 세로 배치 상수(`CAMERA_TOP_BAR_DP`, `CAMERA_CONTROLS_DP`, `CAMERA_PREVIEW_SIDE_DP`)를 공유해 뷰파인더와 픽셀 단위로 겹친다(공통 테스트 추가).
- 머무름·어두운 배경·페이드인 제거, 비행 560ms. 저장 완료를 기다리지 않고 바로 날아가며, 착지했는데 저장이 아직이면 칸 위에서 썸네일이 생길 때까지 머문다.

## 2026-09-21 — 빈티지 필름 (입자·비네팅을 필름으로)

- 배경: 08에서 상세 화면의 모든 사진에 표시 전용 입자·비네팅을 얹었는데, 저장 사진·달력·프리뷰와 다르게 보였다. 결정: 그 효과를 **"빈티지" 필름**으로 옮기고, 이 필름을 끼우고 찍은 사진에만 나오게 한다. 기본(필름 없음)은 원본 그대로.
- `FilmTexture(vignetteAlpha, vignetteRadius, grainAlpha)` + 고정 시드 `grainCells`. 한 벌의 수치를 세 렌더러가 공유한다: Compose `Modifier.filmTexture`(뷰파인더 위 오버레이, 달력으로 날아가는 사진, 필름 타일), iOS Skia export, Android Bitmap export. 저장본은 입자 타일을 `grainScale`만큼 키워(2048px → 2배) 화면에서 보던 굵기를 유지한다.
- `Film.Vintage("vintage", "빈티지", "VINTAGE", matrix = null, texture = FilmTexture())`. `PhotoFiles.render`에 `texture` 인자 추가, 썸네일에는 다시 적용하지 않는다.
- 상세의 `LocalPhoto(film = true)` 제거: 사진은 저장된 그대로 표시.
- 네이티브 프리뷰 파이프라인은 그대로다(색상 행렬만 네이티브, 질감은 Compose 오버레이).
- 테스트: 필름 없이는 행렬·질감 모두 없음, 빈티지는 사진에만 질감, 입자 결정성·배율, iOS/Android 픽셀 테스트(모서리가 중심보다 어둡고 입자로 이웃 픽셀이 달라지며 중심 평균 밝기는 유지), iOS UI 테스트에서 빈티지로 촬영 → 상세 메타 VINTAGE.

### 수정 — 빈티지로 찍을 때 비행 애니메이션 버벅임

- 원인: `Modifier.filmTexture`가 입자 타일(96×96, 사각형 9,216개)을 `drawWithCache` 안에서 만들었다. `drawWithCache`는 크기가 바뀌면 캐시를 버리는데 비행 중에는 크기가 매 프레임 바뀌므로, 타일을 매 프레임 새로 그렸다. 측정: 타일 1회 생성 약 11.9ms(시뮬레이터, Debug) — 120Hz 프레임 예산 8.3ms를 혼자 넘는다.
- 수정: 크기와 무관한 입자 브러시는 세기별로 한 번만 만들어 재사용(`grainBrush`). 캐시 안에서는 크기에 의존하는 비네팅 그라디언트만 만든다.
- 함께: 비행의 애니메이션 값을 컴포지션이 아니라 레이아웃(`Modifier.layout`)·그리기(`graphicsLayer`) 단계에서만 읽도록 바꿔 프레임마다 재구성하지 않는다.
- 교훈: `drawWithCache`의 캐시 키는 사실상 "크기"다. 크기와 무관한 비싼 객체는 캐시 밖(remember/전역)으로 빼야 한다.

## 2026-09-21 — 홈과 동일한 광학 렌즈 셔터

- 중앙 셔터를 홈과 공유하는 `camera/LensShutter.kt`로 교체. 80dp 크기·위치·촬영 동작 유지, 비활성 상태는 전체 35% 불투명도.
- iPhone 대상 및 Android 빌드 성공. UI 테스트와 실제 촬영·화면 확인은 이번 변경 후 미실행.
