# 필름통 이미지 제작 프롬프트

Built-in image_gen 사용. 투명 배경 PNG. 2026-10-30.

## 빈티지

Use case: stylized-concept. Create one premium app icon asset for Damharu, a quiet vintage daily photo app. A single upright 35mm photographic film cartridge, isolated on a genuinely transparent background. Inspired by the user's reference of collectible physical film canisters: dark circular top with protruding spool, brushed silver rim and bottom, short brown perforated film leader curling subtly out to the right. Front three-quarter view, slightly looking down, full object centered with generous transparent padding, square canvas, object occupies 78% height. Polished realistic 3D illustration, softly lit from upper left, tangible metal and matte paper, crisp silhouette readable at 48px. VINTAGE version: warm ivory paper wrap, broad burnt orange (#D98551) band around the middle, dark charcoal upper portion, very subtle aged paper texture. Only text is VINTAGE in clear compact dark uppercase on the ivory area; no ISO number, no logos, no extra objects, no background plate, no floor or cast shadow outside object. This is a film cartridge, not a camera. Reference attached image guides general physical shape, do not copy brand labels.

## 흑백

Use case: stylized-concept. Create one premium app icon asset for Damharu, a quiet vintage daily photo app. A single upright 35mm photographic film cartridge, isolated on a genuinely transparent background. Inspired by the user's reference of collectible physical film canisters: dark circular top with protruding spool, brushed silver rim and bottom, short dark gray perforated film leader curling subtly out to the right. Front three-quarter view, slightly looking down, full object centered with generous transparent padding, square canvas, object occupies 78% height. Polished realistic 3D illustration, softly lit from upper left, tangible metal and matte paper, crisp silhouette readable at 48px. MONO version: entirely neutral black, ivory-white, and silver palette, no chromatic hues. Mostly matte charcoal black paper wrap with a broad ivory-white horizontal band around the middle. Only text is MONO in clear bold compact black uppercase centered on white band; no ISO number, no logos, no extra objects, no background plate, no floor or cast shadow outside object. This is a film cartridge, not a camera. Match a sister VINTAGE cartridge with warm ivory and burnt orange stripe in geometry, perspective and studio lighting. Reference attached image guides general physical shape, do not copy brand labels.


## 필름 없음 — film-none-v1.png

Built-in image_gen, precise-object-edit. Input: film-mono-v1.png. Transparent background.

Prompt: Remove the entire ivory paper label band and MONO lettering, leaving a continuous plain matte charcoal-black cylindrical body without markings. Preserve cartridge shape, framing, perspective, silver top and bottom rims, black spool, right film leader, lighting and realistic texture. No text, logos, symbols or new elements. Preserve transparency and perforation holes.

App resource: shared/src/commonMain/composeResources/drawable/film_cartridge_none.png (512px).

## 2026-11-06 — 빈티지 이름 중앙 정렬 (film-vintage-v2.png)

Built-in image_gen / precise-object-edit. Edit target: film-vintage-v1.png; position reference: film-mono-v1.png.
Prompt: Move VINTAGE lettering from the lower ivory band to the center of the orange band, vertically matching MONO. Remove old lower lettering. Preserve orange/ivory bands, aged texture, silhouette, framing, silver rims, brown leader, lighting and transparency. Output edited vintage cartridge only.
App resource film_cartridge_vintage.png replaced at 512px; v1 retained.
