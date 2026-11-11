# 스플래시 기반 스토어 이미지

2026-10-23. 요청 문구 `오늘 하루의 기록은 "담하루"`를 기존 스플래시 사진 위에 2줄로 배치했다. 아이보리 명조, 큰 브랜드명, 화면 전체를 채우는 노을 사진으로 구성.

- 최종: `00-splash-store.png` — 1206×2622 RGB PNG, 알파 없음.
- 편집 원본: `generated-original.png` — built-in image_gen 결과.
- 입력 사진: `shared/src/commonMain/composeResources/drawable/splash_photo.png`.
- 최종 프롬프트: `prompt.txt`. Built-in image_gen 편집 후 sips로 규격 변환, RGB PNG로 저장.
- 검증: 문구·따옴표·가독성·노을과 인물 구도 시각 확인, 출력 크기·RGB·알파 없음 확인. App Store Connect 업로드 미실행.
- 앱의 스플래시 리소스·코드는 변경하지 않았다. 다음: 스토어 이미지의 표지로 사용할지 검토.
