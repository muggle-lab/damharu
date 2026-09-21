# 담하루

사진 한 장이면, 오늘의 기록은 끝.

개인용 사진 일기 앱. KMP(Kotlin Multiplatform)와 CMP(Compose Multiplatform)를 학습하며 iOS부터 구현하고, 이후 Android를 구현한다.

## 현재 상태

01–08 구현: 홈 달력 → 커스텀 카메라 → 촬영 즉시 저장 흐름, 로컬 저장, 기록 상세와 한 줄 메모를 iOS와 Android에 연결했다. 사진 선택기·필터·과거 날짜 기록은 08에서 제거했다. 검증 결과와 실기기 확인 항목은 [TASKS.md](docs/TASKS.md)에 정리한다.

Xcode에서 `iosApp/Damharu.xcodeproj` → `Damharu` scheme → iPhone 시뮬레이터로 실행한다. 개발 환경과 코드 구조는 [DEVELOPMENT.md](docs/DEVELOPMENT.md)에 정리했다.

- 서버·로그인 없이 기기에 기록 저장
- 하루 한 장, 글은 선택. 기록은 오늘만, 사진은 앱의 카메라로만
- iOS와 Android에서 공통 Kotlin/Compose 화면 사용
- 디자인: 전 화면 고정 다크. 순흑 배경, 명조 헤드라인, 모노 메타데이터, 번트 오렌지 포인트의 빈티지 카메라 무드

## 시작하기

1. `docs/PRODUCT.md`에서 확정된 기획 확인
2. `docs/TASKS.md`에서 현재 단계 확인
3. `docs/DEVELOPMENT.md`에서 플랫폼별 실행 방법 확인

화면별로 작업하고, 완료 시 TASKS.md에 변경 사항과 검증 결과를 남긴다. 새 작업에서도 전체 대화를 다시 읽을 필요가 없도록 문서를 유지한다.
