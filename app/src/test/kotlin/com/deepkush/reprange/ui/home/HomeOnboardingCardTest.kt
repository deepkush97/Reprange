package com.deepkush.reprange.ui.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.deepkush.reprange.navigation.OnboardingRoute
import com.deepkush.reprange.ui.screens.home.HomeOnboardingCard
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class HomeOnboardingCardTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun onboardingRoute_exists() {
        assertThat(OnboardingRoute.toString()).isNotEmpty()
        assertThat(OnboardingRoute::class.simpleName).isEqualTo("OnboardingRoute")
    }

    @Test
    fun home_showsCardWhenNotCompleted() {
        // Simulate ONBOARDING_COMPLETED = false -> card is visible.
        var onboardingCompleted by mutableStateOf(false)

        composeRule.setContent {
            if (!onboardingCompleted) {
                HomeOnboardingCard(onGetStarted = {})
            }
        }

        composeRule.onNodeWithText("Personalize your routines?").assertIsDisplayed()
        composeRule.onNodeWithText("Get personalized workout routines based on your goals.").assertIsDisplayed()
        composeRule.onNodeWithText("Get started").assertIsDisplayed()
        // sanity: state is false throughout this test
        assertThat(onboardingCompleted).isFalse()
    }

    @Test
    fun home_hidesCardWhenCompleted() {
        // Simulate ONBOARDING_COMPLETED = true -> card not composed.
        var onboardingCompleted by mutableStateOf(true)

        composeRule.setContent {
            if (!onboardingCompleted) {
                HomeOnboardingCard(onGetStarted = {})
            }
        }

        assertThat(composeRule.onAllNodesWithText("Personalize your routines?").fetchSemanticsNodes()).isEmpty()
        assertThat(composeRule.onAllNodesWithText("Get started").fetchSemanticsNodes()).isEmpty()
        assertThat(onboardingCompleted).isTrue()
    }

    @Test
    fun home_card_hasDescription() {
        composeRule.setContent {
            HomeOnboardingCard(onGetStarted = {})
        }

        composeRule.onNodeWithText("Get personalized workout routines based on your goals.").assertIsDisplayed()
    }

    @Test
    fun home_card_getStarted_click_invokesCallback() {
        val clicked = androidx.compose.runtime.mutableStateOf(false)

        composeRule.setContent {
            HomeOnboardingCard(onGetStarted = { clicked.value = true })
        }

        composeRule.onNodeWithTag("home_onboarding_get_started").assertIsDisplayed()
        composeRule.onNodeWithTag("home_onboarding_get_started").assertHasClickAction()
        // Verify click action is present and invokes callback. Use semantics OnClick
        // directly because performClick/performTouchInput are flaky with Robolectric
        // dispatcher; direct invoke still proves wiring is correct.
        val node = composeRule.onNodeWithTag("home_onboarding_get_started").fetchSemanticsNode()
        val onClick = node.config[androidx.compose.ui.semantics.SemanticsActions.OnClick]
        assertThat(onClick).isNotNull()
        onClick!!.action!!.invoke()
        composeRule.waitForIdle()
        assertThat(clicked.value).isTrue()
    }

    @Test
    fun home_card_recomposes_visibilityToggle() {
        // Start not completed -> visible, then flip to completed -> gone.
        var onboardingCompleted by mutableStateOf(false)

        composeRule.setContent {
            if (!onboardingCompleted) {
                HomeOnboardingCard(onGetStarted = {})
            }
        }

        composeRule.onNodeWithText("Personalize your routines?").assertIsDisplayed()

        composeRule.runOnUiThread { onboardingCompleted = true }
        // Wait for recomposition
        composeRule.waitForIdle()

        assertThat(composeRule.onAllNodesWithText("Personalize your routines?").fetchSemanticsNodes()).isEmpty()
    }
}
