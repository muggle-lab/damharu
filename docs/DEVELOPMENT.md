# 개발 안내

Xcode에서 `iosApp/Damharu.xcodeproj` → Damharu → iPhone 시뮬레이터로 실행합니다.
Android Studio는 프로젝트 루트를 열고 Gradle JDK를 JBR 21로 선택합니다. iOS는 Kotlin Multiplatform 플러그인의 Xcode Application 구성으로 실행할 수 있습니다. Android 실행 모듈은 07 PR에서 추가합니다.

```sh
./scripts/gradle.sh :shared:linkDebugFrameworkIosSimulatorArm64
./scripts/gradle.sh :shared:iosSimulatorArm64Test
```

- 공통 UI·상태는 commonMain에, 시스템 사진 입력은 Swift PhotoCoordinator에 있습니다.
- PhotoSource 콜백은 앱이 관리하는 임시 JPEG 경로 또는 취소/오류를 전달합니다.
- 보관함 선택은 PHPicker를 사용해 전체 보관함 권한을 요청하지 않습니다.
- 이미지 방향을 정규화하고 긴 변을 2048px로 줄인 사본을 만듭니다. 사용자 보관함의 원본은 수정하지 않습니다.
- 임시 이미지와 화면 상태는 영구 저장과 다릅니다. 04 이전에는 사용자 기록을 저장하지 않습니다.
- Kotlin 2.4.10 / CMP 1.11.1 / Gradle 8.14. Android 추가 시 AGP에 맞춰 Gradle을 갱신합니다.
- 실기기 Signing Team은 본인 계정으로 설정합니다. 로컬 서명 변경은 PR 분리에 포함하지 않습니다.
