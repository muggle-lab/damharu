# 개발 안내

## 실행

Android Studio에서 `damharu` 루트를 열고 Gradle JDK를 JBR 21로 선택한 뒤 Sync한다.

- iOS: Xcode에서 `iosApp/Damharu.xcodeproj` → `Damharu` scheme → iPhone 시뮬레이터 → Run. Android Studio에서는 Kotlin Multiplatform 플러그인의 Xcode Application 구성으로 같은 프로젝트/scheme을 선택한다.
- Android: Android App 실행 구성에서 `androidApp` 모듈과 에뮬레이터를 선택한다.
- 실기기 iOS 서명은 본인 Signing Team을 사용한다. 개발용 Bundle/Application ID: `com.mugglelab.damharu`.
- `local.properties`의 sdk.dir은 본인 Android SDK 경로로 설정한다(커밋 제외).

```sh
./scripts/gradle.sh :shared:iosSimulatorArm64Test :shared:testAndroidHostTest
./scripts/gradle.sh :androidApp:assembleDebug
./scripts/gradle.sh :androidApp:connectedDebugAndroidTest
xcodebuild -project iosApp/Damharu.xcodeproj -scheme Damharu \
  -destination 'platform=iOS Simulator,name=iPhone 17e' \
  -derivedDataPath build/ios CODE_SIGNING_ALLOWED=NO test
```

Kotlin 프레임워크는 SDK별로 따로 만들어진다(`shared/build/xcode-frameworks/Debug/iphonesimulator*`, `iphoneos*`). 공통 API를 바꾼 뒤 Swift에서 "protocol requires function …" 오류가 나면 해당 SDK의 프레임워크가 옛 버전인 것이다. Xcode의 'Build Kotlin framework' 스크립트는 Android Studio가 넘기는 `OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED=YES`에도 건너뛰지 않는다. 이 Gradle 작업이 프레임워크뿐 아니라 Compose 리소스(`compose-resources/`, 스플래시 사진)를 앱 번들에 복사하는데, Android Studio는 자체 DerivedData에 그 복사를 해 주지 않아 건너뛰면 실행 직후 `MissingResourceException`(SIGABRT)으로 죽는다. 그래서 Android Studio의 "Missing build guard" 알림의 "Apply fix"는 적용하지 않는다. 실기기용 프레임워크만 명령줄에서 다시 만들려면 아래 명령을 쓴다.

```sh
xcodebuild -project iosApp/Damharu.xcodeproj -scheme Damharu \
  -destination 'generic/platform=iOS' -derivedDataPath build/ios-device CODE_SIGNING_ALLOWED=NO build
```

`scripts/gradle.sh`는 JAVA_HOME이 없으면 Android Studio의 JBR을 찾는다. 전역 Java 설정은 바꾸지 않는다. Xcode build phase에서도 이 스크립트를 사용한다.

## 코드와 학습 포인트

- `commonMain/App.kt`: 홈 → 카메라 → 촬영 즉시 저장 → 홈, 그리고 상세 흐름과 작업 상태. 영구 데이터와 화면 상태를 구분한다. 셔터가 곧 저장 버튼이라 촬영 뒤 확인 단계가 없다.
- `PhotoUi.kt`: 공통 이미지 표시(저장된 그대로), 필름 질감의 실시간 미리보기 `Modifier.filmTexture`(뷰파인더·비행·필름 타일용), 선택적 한 줄 입력.
- `Theme.kt`: 시스템 모드와 무관한 단일 다크 팔레트 `DiaryPalette`. 명조는 `FontFamily.Serif`, 메타데이터는 `FontFamily.Monospace`.
- `home/CalendarModel.kt`: 달력 계산과 `canRecord`/`actionForDate`. "오늘만 기록" 규칙은 UI가 아니라 이 순수 함수에 있고 공통 테스트로 검증한다.
- `data/Film.kt`: 촬영 전에 끼우는 필름. 4x5 색상 행렬과 `FilmTexture`(입자·비네팅 수치, 고정 시드 입자)로 정의해 실시간 프리뷰(iOS Core Image, Android 레이어 페인트)와 저장(Skia/Bitmap `render`)이 같은 값을 쓴다. 기록에는 id 문자열만 남긴다.
- `data/DiaryStore.kt`: JSON 저장 형식과 날짜별 기록 교체/삭제. RecordFiles를 주입해 저장 실패를 테스트한다.
- `PhotoFiles`: expect/actual로 플랫폼 파일 읽기·원자적 쓰기·이미지 디코딩/출력을 분리한다.
- `PhotoSource`: 앱 카메라 촬영 결과(임시 JPEG 경로/필름 id/실패 문구/닫기)를 콜백으로 공통 UI에 전달하는 인터페이스. 촬영 성공 후에는 카메라가 멈춘 채 남아 있다가 공통 UI의 `closeCamera()`로 즉시 닫힌다(사진이 달력으로 날아가는 연출을 위해). iOS의 PhotoCoordinator 또는 Android Activity가 구현한다. 사진 선택기 경로는 없다.
- `camera/CameraScreen.kt`: 공통 촬영 UI, 둥근 프리뷰, 뷰파인더 브래킷, 로그 간격·관성 줌 눈금(`zoomPosition`/`zoomTicks`), 금속 셔터, 필름 버튼/목록과 접근성. `CameraActions`(zoom/film/capture/close)를 통해 OS 카메라를 제어한다. Compose는 네이티브 프리뷰 뷰를 모든 플랫폼에서 둥글게 자를 수 없어서 `CAMERA_PREVIEW_CORNER_DP`를 공유하고 iOS는 레이어 cornerRadius, Android는 clipToOutline으로 직접 둥글린다.
- iOS: `DamharuApp.swift`의 PhotoCoordinator는 카메라 권한과 임시 이미지 전달을, NativeCameraController는 AVFoundation 세션·JPEG 촬영·Metal 미리보기를 담당한다. 세션과 프레임 처리는 전용 직렬 큐에서 실행하고 UI 상태는 메인 스레드에서 갱신한다.
- Android: `MainActivity.kt`는 카메라 권한·Exif 정규화를, `AndroidCamera.kt`는 CameraX 수명주기·줌·촬영을 담당한다. TextureView 기반 미리보기 위에 Compose 뷰파인더 가이드를 그린다. Bitmap 리사이즈는 androidMain에 있다.

이미지는 백그라운드에서 읽고 내보낸다. 홈에서는 별도 180px 썸네일을 읽어 메모리를 줄인다. 전체 이미지를 달력에 디코딩하지 않는다.

`rememberSaveable`은 영구 저장소가 아니다. 기록은 앱 내부의 `diary/records.json`과 JPEG들에 저장된다. iOS는 Documents, Android는 filesDir 아래다. 앱 재실행 시 목록을 다시 읽는다. 촬영 직후 저장 전의 사진은 임시 파일이며 저장이 끝나면 지우고, 남은 것은 다음 시작 때 정리한다.

## 저장 정책

- 하루 한 장: 긴 변 최대 2048px, 방향을 바로잡은 JPEG 한 장(`<날짜>-<id>-photo.jpg`)과 180px 썸네일(`-thumb.jpg`)만 보관한다. 필름을 끼웠다면 필름이 적용된 결과만 저장하고 필름 없는 원본은 남기지 않는다. GPS 등 메타데이터는 저장하지 않는다.
- 오늘만 기록: 생성·교체는 오늘 날짜만. 지난 기록은 메모 편집과 삭제만 가능하다.
- 이전 형식 목록(original/filter/intensity 포함)은 그대로 읽힌다. 알 수 없는 키는 무시하고 필터가 적용된 최종 JPEG를 사진으로 승격하며, 옛 original 파일은 다음 저장 때 미참조 파일로 정리된다.
- 이미지 결과·썸네일을 새 이름으로 쓴 뒤 목록을 원자적으로 교체한다. 이전 파일은 목록 커밋 이후에만 정리한다.
- 저장 실패: 기존 목록/사진을 유지하고 안내 후 다시 촬영한다.
- 목록 손상/미지원 버전: 자동 초기화하지 않고 오류와 재시도 표시. 기존 파일 유지.
- 오늘 사진 교체: 촬영 전에 확인, 메모 유지. 삭제: 확인 후 사진/썸네일/메모 제거. 지난 기록은 다시 채울 수 없음을 확인창에 표시.
- 앱 삭제 시 로컬 기록은 사라진다. 앱 차원의 서버·로그인·동기화·백업 기능은 없다.

## 검증 환경 및 버전

Kotlin/Compose compiler 2.4.10, CMP 1.11.1, datetime 0.7.1, serialization 1.9.0, coroutines 1.10.2.
Android AGP 9.1.0, Gradle 9.3.1, compile/target SDK 36, min SDK 26. iOS min 16.0, ARM64 기기/Apple Silicon 시뮬레이터.

Android 타깃 추가에 맞춰 AGP 9.1의 최소 Gradle 9.3.1로 올렸다. Kotlin 2.4.10의 공식 지원 Gradle 상한(9.5.0) 안이다. Android KMP library 플러그인을 shared에, application 플러그인을 androidApp에 분리했다.

- [KMP 호환표](https://kotlinlang.org/docs/multiplatform/multiplatform-compatibility-guide.html)
- [AGP 9.1 호환 정보](https://developer.android.com/build/releases/agp-9-1-0-release-notes)
- [Android KMP library 플러그인](https://developer.android.com/kotlin/multiplatform/plugin)
- [Apple AVCaptureSession](https://developer.apple.com/documentation/avfoundation/avcapturesession): 세션 시작/종료는 UI를 막지 않는 전용 큐에서 처리
- [CameraX 1.5.3 릴리스](https://developer.android.com/jetpack/androidx/releases/camera#1.5.3), [PreviewView](https://developer.android.com/media/camera/camerax/preview): Android 촬영에 camera-camera2 / camera-lifecycle / camera-view 1.5.3 사용

설치된 Xcode 26.6은 공식 Kotlin 호환표의 26.4보다 높다. 시뮬레이터 결과와 미검증 항목은 TASKS.md에 기록한다. Kotlin expect/actual class의 Beta 경고 및 Skia iOS 최소 버전 링크 경고는 숨기지 않았다.

UI 테스트는 테스트용 시뮬레이터/에뮬레이터에서 실행한다. iOS 테스트는 2026-09-16·17 기록을 생성/삭제하고 2026-09-01이 비어 있다고 가정하므로 해당 날짜에 개인 기록이 없는 테스트 기기를 사용한다. Android 테스트는 에뮬레이터 카메라로 오늘 기록을 생성/삭제한다. Debug의 `--date 2026-09-17` 실행 인자는 테스트용 오늘 날짜만 고정하고, `--camera-preview`는 시뮬레이터용 가짜 카메라 프레임을 공급한다. UI 테스트 소스만 바꾼 뒤 `xcodebuild test`가 이전 테스트 번들을 다시 실행한 적이 있어, 그럴 때는 `build-for-testing` 후 `test-without-building`으로 실행한다. 개발 샘플을 실제 기록으로 표시하는 컨트롤은 제거했다.

프로젝트 구조를 재생성할 때만 `ruby scripts/generate-xcode-project.rb`를 사용한다. 이 스크립트는 로컬 서명 설정을 재생성하므로 일반 실행에는 필요 없다.
