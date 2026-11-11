# 10 — 홈·설정 탭과 화면 모드

## 요청 및 결정

- 기록 / 필름 / 설정 세 탭(2026-10-30 필름 추가, 12-film-shelf.md 참고). 기록 탭은 책(일기장) 아이콘으로 표시. 기존 홈의 월 선택·기록·촬영 흐름 유지.
- iOS는 SwiftUI TabView가 탭바를 소유하고 각 화면은 ComposeUIViewController로 표시. iOS 26 이상에서는 시스템 Liquid Glass, 이전 버전에서는 기본 네이티브 탭바. 커스텀 blur로 흉내 내지 않는다.
- Android는 Material 3 NavigationBar 1.4.0. 다크 패널과 번트 오렌지 선택 표시, 시스템 동적 색상 미사용.
- 설정 화면·AppearanceState·다크/라이트 팔레트는 commonMain. 저장은 iOS UserDefaults / Android SharedPreferences.
- 라이트 모드 기본 Off. On이면 홈·상세·설정과 시스템 UI가 아이보리 팔레트로 전환되고 재실행 후 유지. 카메라도 같은 테마를 적용하며 프리뷰 영상·필름 처리는 테마와 독립적이다.
- 설정은 라이트 모드 스위치 하나만. 행 전체를 접근 가능한 스위치로 제공.
- 상세 편집 중 탭 이동으로 메모를 잃지 않도록 상세에서는 탭바를 숨기고, 카메라는 전체 화면으로 표시.

## 검증

- 플랫폼 빌드, 탭 전환 시 월 유지, 토글·재실행 저장, Android 뒤로 가기, 카메라·상세 회귀 확인.
- 실행 결과와 미검증 항목은 docs/TASKS.md 참고.

## 참고

- https://kotlinlang.org/docs/multiplatform/ios-liquid-glass.html
- https://developer.apple.com/documentation/technologyoverviews/adopting-liquid-glass
- https://developer.android.com/develop/ui/compose/components/navigation-bar
- https://developer.android.com/jetpack/androidx/releases/compose-material3

## 2026-10-25 — 스플래시와 카메라 테마 수정

- iOS 탭바 초기 상태를 숨김으로 시작하고, 공통 스플래시 완료 여부와 홈 경로를 함께 표시 조건으로 사용. Android도 스플래시 중 탭을 구성하지 않으며 홈의 하단 여백은 유지.
- CameraScreen의 고정 다크 상수를 LocalDiaryColors로 변경. 별도 iOS 카메라 컨트롤러에도 같은 AppearanceState를 전달하고 Android 카메라를 DiaryTheme으로 감싼다.
- 상태 표시줄 강제 다크 설정 제거. 사진 위 필름 이름·초점 가이드는 프리뷰 위 가독성을 유지하고 실제 촬영 이미지에는 UI 테마를 적용하지 않는다.
- iOS Debug의 `--splash-hold-ms`(650–10000ms)는 스플래시 중 탭 노출 회귀 테스트용. 일반 실행 정지 시간은 기존 650ms 유지.

## 2026-10-26 — 스플래시와 탭바를 한 전환으로 통합

- 이전의 종료 후 탭바 표시 방식은 탭바 삽입과 safe area 변경이 렌즈 착지 뒤에 일어나 부자연스러웠다. 이번 변경이 10월 25일의 시작 표시 조건을 대체한다.
- iOS: SwiftUI ZStack의 TabView 위에 투명 ComposeUIViewController로 스플래시를 표시. 공통 StartupTransition으로 홈의 실제 렌즈 좌표와 데이터 준비 여부를 전달한다. 탭바와 홈은 처음부터 최종 레이아웃에 있으므로 종료 시 위치 변화가 없다.
- Android: 기존 공통 화면 내부에서 탭바를 먼저 배치하고 그 위 스플래시가 함께 덮는다.
- 스플래시 정지 구간에는 사진만 보이고 축소 중에는 홈·탭바가 함께 드러난다. 터치와 접근성은 스플래시가 소유하고 끝난 뒤 해제한다. 일반 650ms 정지·780ms 축소 시간은 유지한다.

## 2026-10-26 — 기록 탭 이름·아이콘

사용자 요청으로 하단 홈 탭의 표시 이름을 기록으로 변경. iOS는 `book.closed`, Android는 책등·책갈피를 그린 일기장 아이콘을 사용한다. 표시만 변경하므로 내부 home 경로와 기존 화면 상태는 유지한다. 양 플랫폼 앱 빌드 및 공백 검사 통과, UI 재실행·실기기 검증은 미실행.

## 2026-11-06 — 설정 안내 문구 정리

라이트 모드 아래의 촬영 화면 적용 안내와 전용 상단 여백을 제거했다.
