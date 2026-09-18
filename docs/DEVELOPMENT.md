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

`scripts/gradle.sh`는 JAVA_HOME이 없으면 Android Studio의 JBR을 찾는다. 전역 Java 설정은 바꾸지 않는다. Xcode build phase에서도 이 스크립트를 사용한다.

## 코드와 학습 포인트

- `commonMain/App.kt`: 홈/사진 입력/필터/상세 흐름과 작업 상태. 영구 데이터와 화면 상태를 구분한다.
- `PhotoUi.kt`: 공통 이미지 미리보기, 필터 목록/강도/비교, 선택적 한 줄 입력.
- `data/PhotoFilter.kt`: 미리보기·네이티브 출력에 공유하는 색상 행렬.
- `data/DiaryStore.kt`: JSON 저장 형식과 날짜별 기록 교체/삭제. RecordFiles를 주입해 저장 실패를 테스트한다.
- `PhotoFiles`: expect/actual로 플랫폼 파일 읽기·원자적 쓰기·이미지 디코딩/출력을 분리한다.
- `PhotoSource`: OS 사진 선택을 콜백으로 공통 UI에 전달하는 인터페이스. iOS의 Swift delegate 또는 Android Activity가 구현한다.
- iOS: `DamharuApp.swift`가 PHPicker/UIImagePickerController, 카메라 권한, 방향 정규화 및 임시 파일을 담당한다. Kotlin이 UIViewController를 제공한다.
- Android: `MainActivity.kt`가 Photo Picker/TakePicture/FileProvider와 Exif 방향 정규화를 담당한다. Bitmap export는 androidMain에 있다.

이미지는 백그라운드에서 읽고 내보낸다. 필터 썸네일들은 디코딩한 이미지 한 개를 공유한다. 홈에서는 별도 180px 썸네일을 읽어 메모리를 줄인다. 전체 이미지를 달력에 디코딩하지 않는다.

`rememberSaveable`은 영구 저장소가 아니다. 기록은 앱 내부의 `diary/records.json`과 JPEG들에 저장된다. iOS는 Documents, Android는 filesDir 아래다. 앱 재실행 시 목록을 다시 읽는다. 저장 전 편집 중인 사진은 임시 파일로서 앱 종료 후 복원하지 않으며 다음 시작 때 정리한다. Android OS가 사진 선택 도중 프로세스를 종료하면 기존 기록을 유지하고 사진을 다시 선택해야 한다.

## 저장 정책

- 긴 변 최대 2048px, 방향을 바로잡은 JPEG 사본을 ‘필터 없는 원본’으로 보관한다. 카메라/보관함 원본 자체는 변경하지 않는다. GPS 등 원본 메타데이터는 전달하지 않는다.
- 이미지 결과·썸네일을 새 이름으로 쓴 뒤 목록을 원자적으로 교체한다. 이전 파일은 목록 커밋 이후에만 정리한다.
- 저장 실패: 기존 목록/사진을 유지하고 편집 화면에서 재시도한다.
- 목록 손상/미지원 버전: 자동 초기화하지 않고 오류와 재시도 표시. 기존 파일 유지.
- 같은 날짜 사진 교체: 확인 필요, 메모 유지. 삭제: 확인 후 사진/원본/썸네일/메모 제거.
- 앱 삭제 시 로컬 기록은 사라진다. 앱 차원의 서버·로그인·동기화·백업 기능은 없다.

## 검증 환경 및 버전

Kotlin/Compose compiler 2.4.10, CMP 1.11.1, datetime 0.7.1, serialization 1.9.0, coroutines 1.10.2.
Android AGP 9.1.0, Gradle 9.3.1, compile/target SDK 36, min SDK 26. iOS min 16.0, ARM64 기기/Apple Silicon 시뮬레이터.

Android 타깃 추가에 맞춰 AGP 9.1의 최소 Gradle 9.3.1로 올렸다. Kotlin 2.4.10의 공식 지원 Gradle 상한(9.5.0) 안이다. Android KMP library 플러그인을 shared에, application 플러그인을 androidApp에 분리했다.

- [KMP 호환표](https://kotlinlang.org/docs/multiplatform/multiplatform-compatibility-guide.html)
- [AGP 9.1 호환 정보](https://developer.android.com/build/releases/agp-9-1-0-release-notes)
- [Android KMP library 플러그인](https://developer.android.com/kotlin/multiplatform/plugin)
- [Android Photo Picker](https://developer.android.com/training/data-storage/shared/photo-picker)
- [Apple PHPicker](https://developer.apple.com/documentation/photosui/phpickerviewcontroller)

설치된 Xcode 26.6은 공식 Kotlin 호환표의 26.4보다 높다. 시뮬레이터 결과와 미검증 항목은 TASKS.md에 기록한다. Kotlin expect/actual class의 Beta 경고 및 Skia iOS 최소 버전 링크 경고는 숨기지 않았다.

UI 테스트는 테스트용 시뮬레이터/에뮬레이터에서 실행한다. iOS 테스트는 2026-09-02 기록을 생성/삭제하므로 해당 날짜에 개인 기록이 없는 테스트 기기를 사용한다. Android 테스트는 오늘 기록이 없는 테스트 에뮬레이터를 사용한다. 테스트 사진을 Android MediaStore에 생성한 뒤 정리한다. iOS는 시뮬레이터 기본 보관함 사진을 사용한다. Debug의 `--date 2026-09-17` 실행 인자는 테스트용 오늘 날짜만 고정한다. 개발 샘플을 실제 기록으로 표시하는 컨트롤은 제거했다.

프로젝트 구조를 재생성할 때만 `ruby scripts/generate-xcode-project.rb`를 사용한다. 이 스크립트는 로컬 서명 설정을 재생성하므로 일반 실행에는 필요 없다.
