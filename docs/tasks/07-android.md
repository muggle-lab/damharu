# 07 — Android

- shared의 Android KMP library 타깃과 별도 androidApp 실행 모듈.
- 공통 화면/필터/저장소를 재사용하고 PhotoFiles만 Android 파일·Bitmap API로 구현한다.
- Photo Picker Activity Result API, 외부 카메라 앱과 TakePicture/FileProvider 사용.
- 전체 사진 보관함 권한이나 직접 카메라 권한은 요청하지 않는다. 외부 카메라 앱이 권한을 관리하며 앱 부재/실패는 안내한다.
- Exif 방향 정규화, 이미지 크기 제한, 가져온 URI를 임시 앱 파일로 복사한다.
- Android 뒤로가기는 화면 닫기/미저장 변경 확인과 연결한다.
- 에뮬레이터에서 선택·필터·저장·Activity 재생성·메모·삭제 및 네이티브 픽셀 출력을 검증한다.
- 실제 카메라 촬영과 최소 지원 Android 8(API 26)은 별도 기기 확인 대상이다.

## 2026-09-21 — 스토리보드 기준 변경

- Photo Picker 경로와 가져온 URI의 임시 파일 복사는 제거 대상. 촬영은 이미 CameraX 기반 `CameraScreen`을 사용하므로 직접 카메라 권한을 요청한다(위 "직접 카메라 권한은 요청하지 않는다"는 외부 카메라 앱 시절의 기록).
- 필터 출력(Bitmap 색상 행렬)과 픽셀 테스트는 08 반영 후 제거한다.
- 에뮬레이터 검증 대상은 06의 변경 목록과 동일하게 맞춘다.
