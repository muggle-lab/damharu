package com.mugglelab.damharu

import com.mugglelab.damharu.data.Film
import damharu.shared.generated.resources.*
import org.jetbrains.compose.resources.DrawableResource

/** UI metadata shared by the camera picker and the shelf; image processing stays in Film. */
val Film.cartridge: DrawableResource
    get() = when (this) {
        Film.None, Film.Test -> Res.drawable.film_cartridge_none
        Film.Vintage -> Res.drawable.film_cartridge_vintage
        Film.Monochrome -> Res.drawable.film_cartridge_mono
        Film.Warm -> Res.drawable.film_cartridge_warm
        Film.Clear -> Res.drawable.film_cartridge_clear
        Film.Fade -> Res.drawable.film_cartridge_fade
        Film.Natural -> Res.drawable.film_cartridge_natural
    }

val Film.shelfCaption: String
    get() = when (this) {
        Film.None -> "찍힌 그대로의 순간"
        Film.Test -> "이전 기록의 테스트 필름"
        Film.Vintage -> "작은 입자와 깊은 여운"
        Film.Monochrome -> "빛과 그림자의 기록"
        Film.Warm -> "오후의 빛을 한 겹 담아요"
        Film.Clear -> "맑은 빛과 선명한 공기"
        Film.Fade -> "조금 흐려져도 남는 장면"
        Film.Natural -> "그날의 색에 작은 온도만"
    }

val Film.previewDescription: String
    get() = when (this) {
        Film.None -> "필름 효과 없이 찍힌 그대로 남겨요."
        Film.Test -> "이전 기록에 사용한 테스트 필름이에요."
        Film.Vintage -> "원래의 색 위에 작은 입자를 더해요. 가장자리에 드리운 그늘이 순간을 차분하게 감싸요."
        Film.Monochrome -> "색을 덜어내고 빛과 그림자에 집중해요. 선명한 명암과 작은 입자가 장면의 깊이를 남겨요."
        Film.Warm -> "은은한 꿀빛과 작은 입자를 더해요. 카페와 오후의 실내에 따뜻한 여운을 남겨요."
        Film.Clear -> "미세한 푸른빛과 또렷한 명암을 더해요. 하늘과 바다의 맑은 공기를 담아요."
        Film.Fade -> "검정을 부드럽게 밝히고 색을 덜어내요. 작은 입자가 오래된 인화지 같은 느낌을 남겨요."
        Film.Natural -> "그날의 색에 아주 작은 온기만 더해요. 입자와 가장자리 그늘 없이 자연스럽게 남겨요."
    }
