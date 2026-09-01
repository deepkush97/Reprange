package com.deepkush.reprange.domain.onboarding
import com.google.common.truth.Truth.assertThat
import org.junit.Test
class OnboardingProfileTest {
    @Test fun allowedEquipment_bodyweight_isSingle() {
        assertThat(EquipmentProfile.BODYWEIGHT.allowedEquipment()).containsExactly("body weight")
    }
    @Test fun maxDifficulty_beginner_isBeginner() {
        val p = OnboardingProfile(Goal.GENERAL_FITNESS, Experience.BEGINNER, EquipmentProfile.FULL_GYM, 3, Split.FULL_BODY)
        assertThat(p.maxDifficulty().name).isEqualTo("BEGINNER")
    }
}
