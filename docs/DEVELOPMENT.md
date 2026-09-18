# 개발 안내

## 실행

- Android Studio에서 `damharu` 폴더를 연다. Gradle JDK는 JBR 21을 선택한다.
- Xcode에서는 `iosApp/Damharu.xcodeproj`를 열고 `Damharu` scheme과 iPhone 시뮬레이터를 선택한 뒤 Run한다.
- Xcode Build Phase가 `scripts/gradle.sh`를 호출해 Kotlin framework를 빌드한다.
- 일반 개발에는 Ruby나 프로젝트 재생성이 필요 없다. Xcode 구조를 코드로 다시 만들 때만 `ruby scripts/generate-xcode-project.rb`를 사용한다(xcodeproj 1.27.0 gem 필요).
- 기기 실행 시 Signing Team을 본인 계정으로 지정한다. 현재 Bundle ID `com.mugglelab.damharu`는 개발용 임시 값이다.

```sh
./scripts/gradle.sh :shared:iosSimulatorArm64Test
```

`scripts/gradle.sh`는 JAVA_HOME이 없으면 Android Studio의 JBR을 찾는다. 전역 Java 설정은 변경하지 않는다.

## 구조와 학습 포인트

- `shared/src/commonMain`: Android/iOS에서 공유할 UI, 날짜 모델, 상태. Android 타깃 연결은 이후 진행한다.
- `shared/src/iosMain/.../MainViewController.kt`: 공통 App을 UIViewController로 감싼다.
- `iosApp/iosApp/DamharuApp.swift`: SwiftUI가 Kotlin에서 만든 컨트롤러를 호스팅한다.
- `CalendarModel.kt`: 순수 날짜 계산과 행동 결정. Compose 없이 테스트할 수 있다.
- `HomeScreen.kt`: 상태를 받아 그리고, 사용자 행동을 콜백으로 올리는 화면. 직접 저장하거나 OS 사진 API를 호출하지 않는다.
- `App.kt`: 현재 월과 개발 샘플 상태의 소유자. 후속 단계에서 AddRecord/OpenRecord 콜백을 실제 경로로 연결한다.
- `Theme.kt`: 크림·올리브 색상과 밝은/어두운 모드. 초기에는 Foundation UI만 사용해 Material 특유의 외형과 불필요한 의존성을 피했다.

`rememberSaveable`은 화면 상태 보존을 위한 도구이지 영구 기록 저장소가 아니다. 현재 DiaryEntry는 메모리 샘플이며 앱 재실행 후 사용자 기록을 복원하는 기능은 04 작업에서 구현한다.

## 개발 상태 확인

Debug 앱 하단의 ‘첫 사용 / 샘플 기록 / 오늘 완료’로 상태를 바꾼다. Release에서는 이 컨트롤이 표시되지 않는다.

샘플 이미지는 코드로 그린 일러스트다. 실제 사진 접근은 아직 없다. 현재 사진 추가와 기록 상세 버튼은 준비 중 안내를 표시하며 기록을 저장하지 않는다.

재현 가능한 시뮬레이터 검증용 Debug 실행 인자:

```
--date 2026-09-17 --scenario sample
```

`--scenario` 값: `empty`, `sample`, `saved`. Release에서는 인자를 무시한다.

## 버전 선택 근거

2026-09-17 확인:

- Kotlin / Compose compiler 2.4.10
- Compose Multiplatform 1.11.1
- Gradle 8.14, JBR 21
- kotlinx-datetime 0.7.1
- iOS 배포 타깃 16.0; ARM64 기기 및 Apple Silicon 시뮬레이터

공식 KMP 호환표에서 Kotlin 2.4.0–2.4.10의 Gradle 범위는 7.6.3–9.5.0이다. Compose compiler 버전은 Kotlin 버전과 맞췄다. 기존 로컬 캐시를 활용해 초기 다운로드를 줄였다.

- https://kotlinlang.org/docs/multiplatform/multiplatform-compatibility-guide.html
- https://kotlinlang.org/docs/multiplatform/compose-compatibility-and-versioning.html

주의: 설치된 Xcode는 26.6으로 공식 호환표의 26.4보다 높다. 이번 시뮬레이터 빌드 결과를 TASKS.md에 기록한다. Gradle 8.14는 현재 Kotlin에서 동작하지만 향후 Kotlin 2.5부터 최소 8.14.4로 올려야 한다는 deprecation 경고가 있다.
