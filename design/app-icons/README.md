# 담하루 앱 아이콘

선택: `12-photographer-silhouette.png` — 노을 바다를 카메라로 촬영하는 가까운 실루엣.

`damharu-app-icon-1024.png`는 선택 시안을 구도 변경 없이 Lanczos 방식으로 축소한 1024×1024 RGB PNG다. 투명도와 미리 둥글게 자른 모서리가 없다.

- iOS: `iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/AppIcon.png`. Xcode가 필요한 크기를 생성한다.
- Android: 밀도별 `mipmap-*/ic_launcher.png`, API 26 이상은 `mipmap-anydpi-v26/ic_launcher.xml`. 사진 배경과 투명 전경을 사용하는 adaptive icon으로 런처가 모양을 결정한다. 가장자리 일부는 런처 마스크에 따라 잘린다.
- `scripts/generate-xcode-project.rb`에서도 에셋 카탈로그와 AppIcon 연결을 유지한다.

기존 번호 파일들은 디자인 시안이며 앱에서는 선택한 이미지의 파생 에셋만 사용한다.
