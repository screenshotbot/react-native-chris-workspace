package com.rnstorybookautoscreenshots

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ComponentSurfaceTest {

    @Test
    fun screenshotAll() {
        ReactHostFixture.screenshotAll(listOf(
            // Button story variants
            "Button_Primary",
            "Button_Secondary",
            "Button_Large",
            "Button_Small",

            // Header story variants
            "Header_LoggedIn",
            "Header_LoggedOut",

            // MyFeature story variants
            "MyFeature_Initial",
            "MyFeature_WithClicks",
            "MyFeature_ManyClicks",

            // SwitchFeature story variants
            "SwitchFeature_Off",
            "SwitchFeature_On",

            // TimerFeature story variants
            "TimerFeature_Initial",
            "TimerFeature_Running",
            "TimerFeature_Paused",
            "TimerFeature_LongDuration",

            // CoinFlipFeature story variants
            "CoinFlipFeature_Default",
            "CoinFlipFeature_Heads",
            "CoinFlipFeature_Tails",
            "CoinFlipFeature_ManyFlips",
        ))
    }
}
