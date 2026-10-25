package com.reverb.app

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReverbAppUiTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun onboarding_displaysTitleAndApiKeyFields() {
        // Assert that the Reverb title is present on the Onboarding screen
        composeTestRule.onNodeWithText("Reverb").assertExists()

        // Assert that the API key card title exists
        composeTestRule.onNodeWithText("1. Your OpenAI Key \ud83d\udd11").assertExists()

        // Assert that the launch card exists
        composeTestRule.onNodeWithText("3. Launch Reverb \ud83d\ude80").assertExists()
    }
}
